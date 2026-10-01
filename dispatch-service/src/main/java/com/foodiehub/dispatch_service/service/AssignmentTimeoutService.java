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
    @Transactional
    public void expireOffers() {

        LocalDateTime now =
                LocalDateTime.now();

        List<OrderAssignment> expiredOffers =
                orderAssignmentDao
                        .findByStatusAndExpiresAtBefore(
                                AssignmentStatus.OFFERED,
                                now
                        );

        for (OrderAssignment assignment :
                expiredOffers) {

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
                    "Assignment offer expired. orderId={}, agentId={}",
                    assignment.getOrderId(),
                    assignment.getAgentId()
            );

            /*
             * Advance to the next scored agent.
             */
            agentAssignmentService.assignOrder(
                    assignment.getOrderId()
            );
        }
    }
}