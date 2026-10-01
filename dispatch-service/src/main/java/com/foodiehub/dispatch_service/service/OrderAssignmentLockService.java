package com.foodiehub.dispatch_service.service;


import com.foodiehub.dispatch_service.dao.OrderAssignmentLockDao;
import com.foodiehub.dispatch_service.model.OrderAssignmentLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderAssignmentLockService {

    private final OrderAssignmentLockDao orderAssignmentLockDao;

    @Transactional
    public void ensureLockRow(UUID orderId) {

        if (!orderAssignmentLockDao.existsById(orderId)) {
            try {
                orderAssignmentLockDao.saveAndFlush(
                        new OrderAssignmentLock(orderId)
                );
            } catch (Exception ignored) {
                /*
                 * Another transaction may have created the row
                 * concurrently.
                 *
                 * The important part is that after this method
                 * returns the row exists.
                 */
            }
        }
    }

    public OrderAssignmentLock lock(UUID orderId) {

        return orderAssignmentLockDao.findByOrderIdForUpdate(orderId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Assignment lock not found for order: " + orderId
                        )
                );
    }
}