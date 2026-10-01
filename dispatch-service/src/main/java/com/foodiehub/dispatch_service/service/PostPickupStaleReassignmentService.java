package com.foodiehub.dispatch_service.service;

import com.foodiehub.dispatch_service.client.OrderClient;
import com.foodiehub.dispatch_service.dao.AgentDao;
import com.foodiehub.dispatch_service.dao.OrderAssignmentDao;
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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostPickupStaleReassignmentService {

    private static final long STALE_THRESHOLD_SECONDS = 120;

    private final AgentDao agentDao;
    private final OrderAssignmentDao orderAssignmentDao;
    private final OrderClient orderClient;
    private final AgentAssignmentService agentAssignmentService;

    @Value("${dispatch.assignment.stale-agent-cooldown-seconds:60}")
    private long staleAgentCooldownSeconds;

    @Scheduled(
            fixedDelayString =
                    "${dispatch.assignment.stale-agent-reassignment-check-interval-ms:5000}"
    )
    public void checkForStaleAgentsAfterPickup() {

        List<OrderAssignment> acceptedAssignments =
                orderAssignmentDao.findByStatus(
                        AssignmentStatus.ACCEPTED
                );

        for (OrderAssignment assignment : acceptedAssignments) {

            try {

                processAssignment(
                        assignment
                );

            } catch (Exception ex) {

                log.error(
                        "Failed to process post-pickup stale assignment. " +
                                "orderId={}, assignmentId={}",
                        assignment.getOrderId(),
                        assignment.getId(),
                        ex
                );
            }
        }
    }

    private void processAssignment(
            OrderAssignment assignment
    ) {

        DispatchOrderResponse order =
                orderClient.getOrder(
                        assignment.getOrderId()
                );

        /*
         * We only perform stale-agent reassignment
         * after the order has been picked up.
         */
        if (!"PICKED_UP".equals(order.status())) {
            return;
        }

        Agent assignedAgent =
                agentDao.findById(
                        assignment.getAgentId()
                ).orElse(null);

        if (assignedAgent == null) {
            return;
        }

        if (!isStale(assignedAgent)) {
            return;
        }

        log.warn(
                "Assigned agent became stale after pickup. " +
                        "orderId={}, agentId={}, lastHeartbeatAt={}",
                assignment.getOrderId(),
                assignedAgent.getId(),
                assignedAgent.getLastHeartbeatAt()
        );

        reassignAfterStaleAgent(
                assignment.getOrderId(),
                assignment.getId(),
                assignedAgent
        );
    }

    private boolean isStale(
            Agent agent
    ) {

        /*
         * If the stale detector has already marked the agent
         * offline, it is immediately considered stale.
         */
        if (!Boolean.TRUE.equals(agent.getOnline())) {
            return true;
        }

        Instant lastHeartbeat =
                agent.getLastHeartbeatAt();

        if (lastHeartbeat == null) {
            return true;
        }

        long secondsSinceHeartbeat =
                Duration.between(
                        lastHeartbeat,
                        Instant.now()
                ).getSeconds();

        return secondsSinceHeartbeat >=
                STALE_THRESHOLD_SECONDS;
    }

    @Transactional
    public void reassignAfterStaleAgent(
            UUID orderId,
            UUID previousAssignmentId,
            Agent staleAgent
    ) {

        /*
         * Lock the assignment so that two scheduler executions
         * cannot reassign the same order simultaneously.
         */
        OrderAssignment previousAssignment =
                orderAssignmentDao.findByIdForUpdate(
                        previousAssignmentId
                ).orElse(null);

        if (previousAssignment == null) {
            return;
        }

        /*
         * Another process may have already handled this
         * assignment.
         */
        if (previousAssignment.getStatus()
                != AssignmentStatus.ACCEPTED) {
            return;
        }

        /*
         * Re-check order status immediately before modifying
         * the assignment.
         */
        DispatchOrderResponse order =
                orderClient.getOrder(orderId);

        if (!"PICKED_UP".equals(order.status())) {
            return;
        }

        /*
         * Put the failed agent into cooldown.
         */
        Instant cooldownUntil =
                Instant.now()
                        .plusSeconds(
                                staleAgentCooldownSeconds
                        );

        staleAgent.setAssignmentCooldownUntil(
                cooldownUntil
        );

        staleAgent.setOnline(false);

        agentDao.save(staleAgent);

        /*
         * Mark the previous assignment as expired in the
         * assignment audit history.
         */
        LocalDateTime now =
                LocalDateTime.now();

        previousAssignment.setStatus(
                AssignmentStatus.EXPIRED
        );

        previousAssignment.setRespondedAt(
                now
        );

        previousAssignment.setReason(
                "Agent became stale after pickup"
        );

        orderAssignmentDao.save(
                previousAssignment
        );

        log.warn(
                "Stale agent placed in assignment cooldown. " +
                        "agentId={}, cooldownUntil={}",
                staleAgent.getId(),
                cooldownUntil
        );

        /*
         * Create a new accepted assignment for the
         * highest-scored eligible agent.
         *
         * This is a separate flow from READY_FOR_PICKUP
         * because the order is already PICKED_UP.
         */
        try {

            agentAssignmentService.reassignPickedUpOrder(
                    orderId,
                    staleAgent.getId()
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to reassign picked-up order. " +
                            "orderId={}, staleAgentId={}",
                    orderId,
                    staleAgent.getId(),
                    ex
            );
        }
    }
}