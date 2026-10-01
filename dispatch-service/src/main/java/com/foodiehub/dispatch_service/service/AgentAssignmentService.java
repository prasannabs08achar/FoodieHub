package com.foodiehub.dispatch_service.service;

import com.foodiehub.dispatch_service.client.OrderClient;
import com.foodiehub.dispatch_service.dao.AgentDao;
import com.foodiehub.dispatch_service.dao.OrderAssignmentDao;
import com.foodiehub.dispatch_service.dto.AgentScoreResponse;
import com.foodiehub.dispatch_service.dto.AssignmentResponse;
import com.foodiehub.dispatch_service.dto.DispatchOrderResponse;
import com.foodiehub.dispatch_service.model.Agent;
import com.foodiehub.dispatch_service.model.AssignmentStatus;
import com.foodiehub.dispatch_service.model.OrderAssignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentAssignmentService {

    private final OrderClient orderClient;
    private final OrderAssignmentDao orderAssignmentDao;
    private final AgentScoringService agentScoringService;
    private final AgentDao agentDao;

    @Value("${dispatch.assignment.offer-timeout-seconds:30}")
    private long offerTimeoutSeconds;

    private static final int MAX_UNSUCCESSFUL_OFFERS = 5;

    /**
     * Background worker.
     *
     * Finds ReadyForPickup orders and starts assignment.
     */
    @Scheduled(
            fixedDelayString =
                    "${dispatch.assignment.check-interval-ms:5000}"
    )
    public void processReadyOrders() {

        List<DispatchOrderResponse> orders =
                orderClient.getReadyForPickupOrders();

        for (DispatchOrderResponse order : orders) {

            try {
                assignOrder(order.id());
            } catch (Exception exception) {

                log.error(
                        "Failed to assign order. orderId={}",
                        order.id(),
                        exception
                );
            }
        }
    }

    @Transactional
    public AssignmentResponse assignOrder(UUID orderId) {

        /*
         * Do not create another offer if there is already
         * an active OFFERED assignment.
         */
        var currentOffer =
                orderAssignmentDao.findByOrderIdAndStatus(
                        orderId,
                        AssignmentStatus.OFFERED
                );

        if (currentOffer.isPresent()) {
            return mapToResponse(currentOffer.get());
        }

        /*
         * If the order is already accepted by an agent,
         * assignment is complete.
         */
        var acceptedAssignment =
                orderAssignmentDao.findByOrderIdAndStatus(
                        orderId,
                        AssignmentStatus.ACCEPTED
                );

        if (acceptedAssignment.isPresent()) {
            return mapToResponse(
                    acceptedAssignment.get()
            );
        }

        DispatchOrderResponse order =
                orderClient.getOrder(orderId);

        if (!"READY_FOR_PICKUP".equals(order.status())) {

            throw new IllegalArgumentException(
                    "Order is not ReadyForPickup: "
                            + orderId
            );
        }

        List<AgentScoreResponse> scoredAgents =
                agentScoringService.scoreEligibleAgents(
                        order.deliveryLatitude(),
                        order.deliveryLongitude()
                );

        /*
         * Remove agents that have already received an offer
         * for this order.
         */
        List<AgentScoreResponse> eligibleAgents =
                scoredAgents.stream()
                        .filter(agent ->
                                !hasPreviousOffer(
                                        orderId,
                                        agent.agentId()
                                )
                        )
                        .toList();

        if (eligibleAgents.isEmpty()) {

            return handleNoEligibleAgent(orderId);
        }

        AgentScoreResponse selectedAgent =
                eligibleAgents.get(0);

        long unsuccessfulOffers =
                countUnsuccessfulOffers(orderId);

        if (unsuccessfulOffers >= MAX_UNSUCCESSFUL_OFFERS) {

            cancelOrderAfterMaximumFailures(
                    orderId
            );

            return null;
        }

        int attemptNumber =
                (int) unsuccessfulOffers + 1;

        LocalDateTime now =
                LocalDateTime.now();

        OrderAssignment assignment =
                OrderAssignment.builder()
                        .orderId(orderId)
                        .agentId(selectedAgent.agentId())
                        .status(AssignmentStatus.OFFERED)
                        .attemptNumber(attemptNumber)
                        .offeredAt(now)
                        .expiresAt(
                                now.plusSeconds(
                                        offerTimeoutSeconds
                                )
                        )
                        .build();

        assignment =
                orderAssignmentDao.save(
                        assignment
                );

        log.info(
                "Agent offer created. orderId={}, agentId={}, attempt={}, expiresAt={}",
                orderId,
                selectedAgent.agentId(),
                attemptNumber,
                assignment.getExpiresAt()
        );

        return mapToResponse(
                assignment
        );
    }

    @Transactional
    public AssignmentResponse acceptOffer(
            UUID orderId,
            UUID agentUserId
    ) {

        OrderAssignment assignment =
                getCurrentOffer(orderId);

        validateAgent(
                assignment,
                agentUserId
        );

        if (assignment.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Assignment offer has expired"
            );
        }

        assignment.setStatus(
                AssignmentStatus.ACCEPTED
        );

        assignment.setRespondedAt(
                LocalDateTime.now()
        );

        assignment.setReason(
                "Offer accepted"
        );

        assignment =
                orderAssignmentDao.save(
                        assignment
                );

        log.info(
                "Agent accepted assignment. orderId={}, agentUserId={}",
                orderId,
                agentUserId
        );

        return mapToResponse(
                assignment
        );
    }

    @Transactional
    public AssignmentResponse declineOffer(
            UUID orderId,
            UUID agentUserId,
            String reason
    ) {

        OrderAssignment assignment =
                getCurrentOffer(orderId);

        validateAgent(
                assignment,
                agentUserId
        );

        assignment.setStatus(
                AssignmentStatus.DECLINED
        );

        assignment.setRespondedAt(
                LocalDateTime.now()
        );

        assignment.setReason(
                reason == null || reason.isBlank()
                        ? "Offer declined"
                        : reason
        );

        assignment =
                orderAssignmentDao.save(
                        assignment
                );

        log.info(
                "Agent declined assignment. orderId={}, agentUserId={}",
                orderId,
                agentUserId
        );

        /*
         * Immediately advance to next agent.
         */
        assignOrder(orderId);

        return mapToResponse(
                assignment
        );
    }

    private OrderAssignment getCurrentOffer(
            UUID orderId
    ) {

        return orderAssignmentDao
                .findByOrderIdAndStatus(
                        orderId,
                        AssignmentStatus.OFFERED
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No active assignment offer found"
                        )
                );
    }

    private void validateAgent(
            OrderAssignment assignment,
            UUID agentUserId
    ) {

        Agent agent =
                agentDao.findByUserId(agentUserId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Agent not found: "
                                                + agentUserId
                                )
                        );

        if (!agent.getId()
                .equals(assignment.getAgentId())) {

            throw new IllegalArgumentException(
                    "Agent is not assigned this offer"
            );
        }

        if (!Boolean.TRUE.equals(agent.getActive())) {

            throw new IllegalArgumentException(
                    "Agent is inactive"
            );
        }
    }

    private boolean hasPreviousOffer(
            UUID orderId,
            UUID agentId
    ) {

        return orderAssignmentDao
                .existsByOrderIdAndAgentIdAndStatusIn(
                        orderId,
                        agentId,
                        List.of(
                                AssignmentStatus.ACCEPTED,
                                AssignmentStatus.DECLINED,
                                AssignmentStatus.EXPIRED
                        )
                );
    }

    private long countUnsuccessfulOffers(
            UUID orderId
    ) {

        return orderAssignmentDao
                .countByOrderIdAndStatusIn(
                        orderId,
                        List.of(
                                AssignmentStatus.DECLINED,
                                AssignmentStatus.EXPIRED
                        )
                );
    }

    private AssignmentResponse handleNoEligibleAgent(
            UUID orderId
    ) {

        long unsuccessfulOffers =
                countUnsuccessfulOffers(orderId);

        if (unsuccessfulOffers >= MAX_UNSUCCESSFUL_OFFERS) {

            cancelOrderAfterMaximumFailures(
                    orderId
            );
        }

        return null;
    }

    private void cancelOrderAfterMaximumFailures(
            UUID orderId
    ) {

        log.warn(
                "Maximum agent offers reached. Cancelling order. orderId={}",
                orderId
        );

        orderClient.systemCancel(
                orderId,
                new com.foodiehub.dispatch_service.dto.OrderStatusUpdateRequest(
                        "CANCELLED",
                        "No delivery agent accepted the order after 5 offers"
                )
        );
    }

    private AssignmentResponse mapToResponse(
            OrderAssignment assignment
    ) {

        return new AssignmentResponse(
                assignment.getId(),
                assignment.getOrderId(),
                assignment.getAgentId(),
                null,
                assignment.getStatus(),
                assignment.getAttemptNumber(),
                assignment.getOfferedAt(),
                assignment.getExpiresAt(),
                assignment.getRespondedAt(),
                assignment.getReason()
        );
    }
}