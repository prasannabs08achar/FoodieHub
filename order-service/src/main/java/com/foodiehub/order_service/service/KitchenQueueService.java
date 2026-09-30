package com.foodiehub.order_service.service;

import com.foodiehub.order_service.dao.KitchenQueueDao;
import com.foodiehub.order_service.model.KitchenQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KitchenQueueService {

    private final KitchenQueueDao kitchenQueueDao;


    /*
     * =========================================================
     * ENQUEUE ORDER
     * =========================================================
     *
     * Adds an ACCEPTED order to the kitchen FIFO queue.
     *
     * Duplicate queue entries are prevented.
     */
    @Transactional
    public void enqueue(
            UUID orderId,
            UUID restaurantId
    ) {

        if (kitchenQueueDao.existsByOrderId(orderId)) {
            return;
        }

        KitchenQueue queueEntry =
                KitchenQueue.builder()
                        .orderId(orderId)
                        .restaurantId(restaurantId)
                        .build();

        kitchenQueueDao.save(queueEntry);
    }


    /*
     * =========================================================
     * GET QUEUED ORDERS
     * =========================================================
     *
     * Returns orders for one restaurant in FIFO order.
     */
    @Transactional(readOnly = true)
    public List<KitchenQueue> getQueuedOrders(
            UUID restaurantId
    ) {

        return kitchenQueueDao
                .findByRestaurantIdOrderByQueuedAtAsc(
                        restaurantId
                );
    }


    /*
     * =========================================================
     * GET QUEUE ENTRY BY ORDER
     * =========================================================
     */
    @Transactional(readOnly = true)
    public Optional<KitchenQueue> getByOrderId(
            UUID orderId
    ) {

        return kitchenQueueDao.findByOrderId(
                orderId
        );
    }


    /*
     * =========================================================
     * REMOVE ORDER FROM QUEUE
     * =========================================================
     */
    @Transactional
    public void remove(
            UUID orderId
    ) {

        kitchenQueueDao.deleteByOrderId(
                orderId
        );
    }


    /*
     * =========================================================
     * FIND RESTAURANTS WITH QUEUED ORDERS
     * =========================================================
     *
     * Used by the background promotion worker.
     */
    @Transactional(readOnly = true)
    public List<UUID> findRestaurantsWithQueuedOrders() {

        return kitchenQueueDao
                .findDistinctRestaurantIds();
    }
}