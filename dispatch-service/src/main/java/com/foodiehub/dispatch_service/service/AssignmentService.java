package com.foodiehub.dispatch_service.service;


import com.foodiehub.dispatch_service.client.OrderClient;
import com.foodiehub.dispatch_service.dao.AgentDao;
import com.foodiehub.dispatch_service.dao.OrderAssignmentDao;
import com.foodiehub.dispatch_service.dto.AssignmentResponse;
import com.foodiehub.dispatch_service.dto.OrderStatusUpdateRequest;
import com.foodiehub.dispatch_service.model.Agent;
import com.foodiehub.dispatch_service.model.AssignmentStatus;
import com.foodiehub.dispatch_service.model.OrderAssignment;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final OrderClient orderClient;
    private final AgentDao agentDao;
    private final OrderAssignmentDao orderAssignmentDao;

    @Value("${dispatch.static-agent.user-id}")
    private UUID staticAgentUserId;

    @Transactional
    public AssignmentResponse assignStaticAgent(UUID orderId) {

        /*
         * If this order has already been assigned,
         * return the existing assignment.
         */
        var existingAssignment =
                orderAssignmentDao.findByOrderIdAndStatus(
                        orderId,
                        AssignmentStatus.ACCEPTED
                );

        if (existingAssignment.isPresent()) {
            return mapToResponse(existingAssignment.get());
        }

        /*
         * Find the configured static delivery agent.
         */
        Agent agent = agentDao
                .findByUserId(staticAgentUserId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Static delivery agent not found"
                        )
                );

        if (!Boolean.TRUE.equals(agent.getActive())) {
            throw new IllegalArgumentException(
                    "Static delivery agent is inactive"
            );
        }

        /*
         * M2 has no scoring and no offer workflow.
         * The single static agent is immediately assigned.
         */
        OrderAssignment assignment =
                OrderAssignment.builder()
                        .orderId(orderId)
                        .agentId(agent.getId())
                        .status(AssignmentStatus.ACCEPTED)
                        .offeredAt(LocalDateTime.now())
                        .respondedAt(LocalDateTime.now())
                        .createdAt(LocalDateTime.now())
                        .build();

        assignment = orderAssignmentDao.save(assignment);

        return mapToResponse(assignment);
    }

    @Transactional
    public void pickupOrder(
            UUID orderId,
            UUID agentUserId
    ) {

        OrderAssignment assignment =
                getActiveAssignment(orderId);

        Agent agent =
                getAgent(agentUserId);

        validateAssignedAgent(
                assignment,
                agent
        );

        orderClient.updateStatus(
                orderId,
                agentUserId,
                new OrderStatusUpdateRequest(
                        "PICKED_UP",
                        "Order picked up by assigned agent"
                )
        );
        assignment.setCompletedAt(
                LocalDateTime.now()
        );

        orderAssignmentDao.save(
                assignment
        );
    }

    @Transactional
    public void deliverOrder(
            UUID orderId,
            UUID agentUserId
    ) {

        OrderAssignment assignment =
                getActiveAssignment(orderId);

        Agent agent =
                getAgent(agentUserId);

        validateAssignedAgent(
                assignment,
                agent
        );

        orderClient.updateStatus(
                orderId,
                agentUserId,
                new OrderStatusUpdateRequest(
                        "DELIVERED",
                        "Order delivered by assigned agent"
                )
        );
    }

    private AssignmentResponse mapToResponse(
            OrderAssignment assignment
    ) {

        Agent agent = agentDao
                .findById(assignment.getAgentId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Agent not found: "
                                        + assignment.getAgentId()
                        )
                );

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
    private OrderAssignment getActiveAssignment(
            UUID orderId
    ) {

        return orderAssignmentDao
                .findByOrderIdAndStatus(
                        orderId,
                        AssignmentStatus.ACCEPTED
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No active assignment found for order: "
                                        + orderId
                        )
                );
    }
    private Agent getAgent(
            UUID agentUserId
    ) {

        return agentDao
                .findByUserId(agentUserId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Agent not found: "
                                        + agentUserId
                        )
                );
    }
    private void validateAssignedAgent(
            OrderAssignment assignment,
            Agent agent
    ) {

        if (!assignment.getAgentId()
                .equals(agent.getId())) {

            throw new IllegalArgumentException(
                    "Agent is not assigned to this order"
            );
        }

        if (!Boolean.TRUE.equals(agent.getActive())) {

            throw new IllegalArgumentException(
                    "Agent is inactive"
            );
        }
    }
}