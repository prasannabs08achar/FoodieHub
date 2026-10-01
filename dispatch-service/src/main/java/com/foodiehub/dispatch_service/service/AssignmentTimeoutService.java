package com.foodiehub.dispatch_service.service;

import com.foodiehub.dispatch_service.dao.OrderAssignmentDao;
import com.foodiehub.dispatch_service.model.AssignmentStatus;
import com.foodiehub.dispatch_service.model.OrderAssignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentTimeoutService {

    private final OrderAssignmentDao orderAssignmentDao;
    private final AgentAssignmentService agentAssignmentService;

    @Scheduled(
            fixedDelayString =
                    "${dispatch.assignment.timeout-check-interval-ms:1000}"
    )
    public void expireOffers() {

        LocalDateTime now =
                LocalDateTime.now();

        List<OrderAssignment> expiredOffers =
                orderAssignmentDao.findByStatusAndExpiresAtBefore(
                        AssignmentStatus.OFFERED,
                        now
                );

        for (OrderAssignment candidate : expiredOffers) {

            try {

                UUID orderId =
                        expireOffer(candidate.getId());

                if (orderId != null) {

                    /*
                     * The expired transaction has already committed.
                     * Now create the next offer.
                     */
                    try {
                        agentAssignmentService.assignOrder(
                                orderId
                        );
                    } catch (Exception ex) {

                        log.error(
                                "Failed to assign next agent after offer expiration. orderId={}",
                                orderId,
                                ex
                        );
                    }
                }

            } catch (Exception ex) {

                log.error(
                        "Failed to expire assignment. assignmentId={}",
                        candidate.getId(),
                        ex
                );
            }
        }
    }

    @Transactional
    public UUID expireOffer(
            UUID assignmentId
    ) {

        OrderAssignment assignment =
                orderAssignmentDao.findByIdForUpdate(
                                assignmentId
                        )
                        .orElse(null);

        if (assignment == null) {
            return null;
        }

        /*
         * Agent may have accepted/declined the offer between the
         * initial query and this transaction.
         */
        if (assignment.getStatus()
                != AssignmentStatus.OFFERED) {

            return null;
        }

        LocalDateTime now =
                LocalDateTime.now();

        if (assignment.getExpiresAt() == null
                || now.isBefore(
                assignment.getExpiresAt()
        )) {

            return null;
        }

        assignment.setStatus(
                AssignmentStatus.EXPIRED
        );

        assignment.setRespondedAt(now);

        assignment.setReason(
                "Offer expired after 30 seconds"
        );

        orderAssignmentDao.save(
                assignment
        );

        log.info(
                "Assignment offer expired. assignmentId={}, orderId={}, agentId={}",
                assignment.getId(),
                assignment.getOrderId(),
                assignment.getAgentId()
        );

        return assignment.getOrderId();
    }
}