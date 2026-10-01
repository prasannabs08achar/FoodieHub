package com.foodiehub.dispatch_service.service;

import com.foodiehub.dispatch_service.client.OrderClient;
import com.foodiehub.dispatch_service.dao.AgentDao;
import com.foodiehub.dispatch_service.dao.OrderAssignmentDao;
import com.foodiehub.dispatch_service.dto.AssignmentResponse;
import com.foodiehub.dispatch_service.dto.DispatchOrderResponse;
import com.foodiehub.dispatch_service.dto.OrderStatusUpdateRequest;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentAssignmentService {

    private final OrderClient orderClient;
    private final OrderAssignmentDao orderAssignmentDao;
    private final AgentDao agentDao;
    private final AgentScoringService agentScoringService;
    private final OrderAssignmentLockService orderAssignmentLockService;

    @Value("${dispatch.assignment.offer-timeout-seconds:30}")
    private long offerTimeoutSeconds;

    private static final int MAX_UNSUCCESSFUL_OFFERS = 5;

    private static final List<AssignmentStatus> UNSUCCESSFUL_STATUSES =
            List.of(
                    AssignmentStatus.DECLINED,
                    AssignmentStatus.EXPIRED
            );

    @Scheduled(
            fixedDelayString =
                    "${dispatch.assignment.check-interval-ms:5000}"
    )
    public void processReadyOrders() {

        try {

            List<DispatchOrderResponse> orders =
                    orderClient.getReadyForPickupOrders();

            for (DispatchOrderResponse order : orders) {

                try {
                    assignOrder(order.id());
                } catch (Exception ex) {
                    log.error(
                            "Failed to assign order. orderId={}",
                            order.id(),
                            ex
                    );
                }
            }

        } catch (Exception ex) {

            log.error(
                    "Failed to retrieve READY_FOR_PICKUP orders",
                    ex
            );
        }
    }

    @Transactional
    public AssignmentResponse assignOrder(UUID orderId) {

        orderAssignmentLockService.ensureLockRow(orderId);
        orderAssignmentLockService.lock(orderId);

        /*
         * If an active offer already exists, do not create another one.
         */
        var existingOffer =
                orderAssignmentDao.findByOrderIdAndStatus(
                        orderId,
                        AssignmentStatus.OFFERED
                );

        if (existingOffer.isPresent()) {
            return mapToResponse(existingOffer.get());
        }

        /*
         * If an agent already accepted the order,
         * assignment is already complete.
         */
        if (orderAssignmentDao.existsByOrderIdAndStatus(
                orderId,
                AssignmentStatus.ACCEPTED
        )) {

            throw new IllegalStateException(
                    "Order already has an accepted assignment: " + orderId
            );
        }

        /*
         * Verify the order.
         */
        DispatchOrderResponse order =
                orderClient.getOrder(orderId);

        if (!"READY_FOR_PICKUP".equals(order.status())) {

            throw new IllegalStateException(
                    "Order is not READY_FOR_PICKUP: " + order.status()
            );
        }

        /*
         * Count previous unsuccessful offers.
         */
        long unsuccessfulOffers =
                orderAssignmentDao.countByOrderIdAndStatusIn(
                        orderId,
                        UNSUCCESSFUL_STATUSES
                );

        if (unsuccessfulOffers >= MAX_UNSUCCESSFUL_OFFERS) {

            cancelOrderAfterAssignmentFailure(
                    orderId,
                    "Order cancelled after "
                            + MAX_UNSUCCESSFUL_OFFERS
                            + " unsuccessful agent offers"
            );

            throw new IllegalStateException(
                    "Maximum agent assignment attempts reached"
            );
        }

        /*
         * Determine which agents were already offered this order.
         */
        List<OrderAssignment> previousAssignments =
                orderAssignmentDao.findByOrderId(orderId);

        Set<UUID> excludedAgentIds =
                new HashSet<>(
                        previousAssignments.stream()
                                .map(OrderAssignment::getAgentId)
                                .toList()
                );

        /*
         * Get the scored eligible agents.
         */
        var scoredAgents =
                agentScoringService.scoreEligibleAgents(
                        order.deliveryLatitude(),
                        order.deliveryLongitude()
                );

        for (var scoredAgent : scoredAgents) {

            Agent agent =
                    agentDao.findById(scoredAgent.agentId())
                            .orElse(null);

            if (agent == null) {
                continue;
            }

            if (!Boolean.TRUE.equals(agent.getActive())) {
                continue;
            }

            if (!Boolean.TRUE.equals(agent.getOnline())) {
                continue;
            }

            if (excludedAgentIds.contains(agent.getId())) {
                continue;
            }

            /*
             * Create the next offer.
             */
            int attemptNumber =
                    previousAssignments.size() + 1;

            LocalDateTime offeredAt =
                    LocalDateTime.now();

            LocalDateTime expiresAt =
                    offeredAt.plusSeconds(
                            offerTimeoutSeconds
                    );

            OrderAssignment assignment =
                    OrderAssignment.builder()
                            .orderId(orderId)
                            .agentId(agent.getId())
                            .status(AssignmentStatus.OFFERED)
                            .attemptNumber(attemptNumber)
                            .offeredAt(offeredAt)
                            .expiresAt(expiresAt)
                            .reason("Agent offer created")
                            .build();

            OrderAssignment saved =
                    orderAssignmentDao.save(assignment);

            log.info(
                    "Agent offer created. orderId={}, agentId={}, attempt={}, expiresAt={}",
                    orderId,
                    agent.getId(),
                    attemptNumber,
                    expiresAt
            );

            return mapToResponse(saved);
        }

        /*
         * No eligible agents remain.
         */
        cancelOrderAfterAssignmentFailure(
                orderId,
                "No eligible delivery agents available"
        );

        throw new IllegalStateException(
                "No eligible delivery agents available"
        );
    }

    @Transactional
    public AssignmentResponse acceptOffer(
            UUID orderId,
            UUID agentUserId
    ) {

        orderAssignmentLockService.ensureLockRow(orderId);
        orderAssignmentLockService.lock(orderId);

        OrderAssignment assignment =
                orderAssignmentDao.findByOrderIdAndStatusForUpdate(
                                orderId,
                                AssignmentStatus.OFFERED
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No active offer found for order: "
                                                + orderId
                                )
                        );

        validateAgent(
                assignment,
                agentUserId
        );

        LocalDateTime now =
                LocalDateTime.now();

        if (assignment.getExpiresAt() != null
                && !now.isBefore(assignment.getExpiresAt())) {

            throw new IllegalStateException(
                    "Assignment offer has expired"
            );
        }

        Agent agent =
                agentDao.findByUserId(agentUserId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Agent not found: "
                                                + agentUserId
                                ));

        if (!Boolean.TRUE.equals(agent.getOnline())) {

            throw new IllegalStateException(
                    "Offline agent cannot accept an offer"
            );
        }

        assignment.setStatus(
                AssignmentStatus.ACCEPTED
        );

        assignment.setRespondedAt(now);

        assignment.setReason(
                "Offer accepted"
        );

        OrderAssignment saved =
                orderAssignmentDao.save(assignment);

        log.info(
                "Assignment accepted. orderId={}, agentId={}, assignmentId={}",
                orderId,
                agent.getId(),
                assignment.getId()
        );

        return mapToResponse(saved);
    }

    @Transactional
    public AssignmentResponse declineOffer(
            UUID orderId,
            UUID agentUserId,
            String reason
    ) {

        orderAssignmentLockService.ensureLockRow(orderId);
        orderAssignmentLockService.lock(orderId);

        OrderAssignment assignment =
                orderAssignmentDao.findByOrderIdAndStatusForUpdate(
                                orderId,
                                AssignmentStatus.OFFERED
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No active offer found for order: "
                                                + orderId
                                )
                        );

        validateAgent(
                assignment,
                agentUserId
        );

        LocalDateTime now =
                LocalDateTime.now();

        if (assignment.getExpiresAt() != null
                && !now.isBefore(assignment.getExpiresAt())) {

            throw new IllegalStateException(
                    "Assignment offer has expired"
            );
        }

        assignment.setStatus(
                AssignmentStatus.DECLINED
        );

        assignment.setRespondedAt(now);

        assignment.setReason(
                reason == null || reason.isBlank()
                        ? "Offer declined"
                        : reason
        );

        OrderAssignment saved =
                orderAssignmentDao.save(assignment);

        log.info(
                "Assignment declined. orderId={}, agentId={}, assignmentId={}",
                orderId,
                assignment.getAgentId(),
                assignment.getId()
        );

        /*
         * Do NOT recursively call assignOrder() inside the same
         * transaction. The current transaction must commit the
         * DECLINED audit record first.
         */
        return mapToResponse(saved);
    }

    @Transactional
    public void cancelOrderAfterAssignmentFailure(
            UUID orderId,
            String reason
    ) {

        orderAssignmentLockService.ensureLockRow(orderId);
        orderAssignmentLockService.lock(orderId);

        long unsuccessfulOffers =
                orderAssignmentDao.countByOrderIdAndStatusIn(
                        orderId,
                        UNSUCCESSFUL_STATUSES
                );

        if (unsuccessfulOffers < MAX_UNSUCCESSFUL_OFFERS) {
            return;
        }

        try {

            orderClient.systemCancel(
                    orderId,
                    new OrderStatusUpdateRequest(
                            "CANCELLED",
                            reason
                    )
            );

            log.info(
                    "Order cancelled after assignment failures. orderId={}, failures={}",
                    orderId,
                    unsuccessfulOffers
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to system-cancel order after assignment failures. orderId={}",
                    orderId,
                    ex
            );

            throw ex;
        }
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
                                ));

        if (!agent.getId().equals(
                assignment.getAgentId()
        )) {

            throw new IllegalArgumentException(
                    "Agent is not assigned this offer"
            );
        }

        if (!Boolean.TRUE.equals(
                agent.getActive()
        )) {

            throw new IllegalArgumentException(
                    "Agent is inactive"
            );
        }
    }
    @Transactional
    public AssignmentResponse reassignPickedUpOrder(
            UUID orderId,
            UUID failedAgentId
    ) {

        orderAssignmentLockService.ensureLockRow(orderId);
        orderAssignmentLockService.lock(orderId);

        DispatchOrderResponse order =
                orderClient.getOrder(orderId);

        if (!"PICKED_UP".equals(order.status())) {
            throw new IllegalStateException(
                    "Order is not PICKED_UP: " + order.status()
            );
        }

        /*
         * Do not create another assignment if another agent
         * has already been assigned.
         */
        var existingAcceptedAssignment =
                orderAssignmentDao.findByOrderIdAndStatus(
                        orderId,
                        AssignmentStatus.ACCEPTED
                );

        if (existingAcceptedAssignment.isPresent()) {
            return mapToResponse(
                    existingAcceptedAssignment.get()
            );
        }

        /*
         * Get all agents that were already involved
         * with this order.
         */
        List<OrderAssignment> previousAssignments =
                orderAssignmentDao.findByOrderId(orderId);

        Set<UUID> excludedAgentIds =
                new HashSet<>(
                        previousAssignments.stream()
                                .map(OrderAssignment::getAgentId)
                                .toList()
                );

        /*
         * Explicitly exclude the stale agent.
         */
        excludedAgentIds.add(failedAgentId);

        /*
         * Score the currently eligible agents.
         */
        var scoredAgents =
                agentScoringService.scoreEligibleAgents(
                        order.deliveryLatitude(),
                        order.deliveryLongitude()
                );

        for (var scoredAgent : scoredAgents) {

            Agent agent =
                    agentDao.findById(
                            scoredAgent.agentId()
                    ).orElse(null);

            if (agent == null) {
                continue;
            }

            if (!Boolean.TRUE.equals(agent.getActive())) {
                continue;
            }

            if (!Boolean.TRUE.equals(agent.getOnline())) {
                continue;
            }

            if (excludedAgentIds.contains(agent.getId())) {
                continue;
            }

            /*
             * This is a post-pickup reassignment.
             *
             * The new agent is immediately ACCEPTED because
             * the order has already been picked up.
             */
            int attemptNumber =
                    previousAssignments.size() + 1;

            LocalDateTime now =
                    LocalDateTime.now();

            OrderAssignment reassignment =
                    OrderAssignment.builder()
                            .orderId(orderId)
                            .agentId(agent.getId())
                            .status(AssignmentStatus.ACCEPTED)
                            .attemptNumber(attemptNumber)
                            .offeredAt(now)
                            .respondedAt(now)
                            .reason(
                                    "Order reassigned after previous agent became stale after pickup"
                            )
                            .build();

            OrderAssignment saved =
                    orderAssignmentDao.save(
                            reassignment
                    );

            log.info(
                    "Order reassigned after post-pickup stale agent. " +
                            "orderId={}, previousAgentId={}, newAgentId={}",
                    orderId,
                    failedAgentId,
                    agent.getId()
            );

            return mapToResponse(saved);
        }

        throw new IllegalStateException(
                "No eligible delivery agent available for post-pickup reassignment: "
                        + orderId
        );
    }

    private AssignmentResponse mapToResponse(
            OrderAssignment assignment
    ) {

        UUID agentUserId =
                agentDao.findById(assignment.getAgentId())
                        .map(Agent::getUserId)
                        .orElse(null);

        return new AssignmentResponse(
                assignment.getId(),
                assignment.getOrderId(),
                assignment.getAgentId(),
                agentUserId,
                assignment.getStatus(),
                assignment.getAttemptNumber(),
                assignment.getOfferedAt(),
                assignment.getExpiresAt(),
                assignment.getRespondedAt(),
                assignment.getReason()
        );
    }
}