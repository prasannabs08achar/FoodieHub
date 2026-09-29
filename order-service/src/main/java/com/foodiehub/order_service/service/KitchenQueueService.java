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
     * Add an order to the kitchen queue.
     */
    @Transactional
    public void enqueue(
            UUID orderId,
            UUID restaurantId
    ) {

        /*
         * Prevent duplicate queue entries.
         */
        if (kitchenQueueDao.existsByOrderId(
                orderId
        )) {
            return;
        }

        KitchenQueue queueEntry =
                KitchenQueue.builder()
                        .orderId(orderId)
                        .restaurantId(restaurantId)
                        .build();

        kitchenQueueDao.save(
                queueEntry
        );
    }

    /*
     * Get all queued orders for a restaurant
     * in FIFO order.
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
     * Get a queue entry by order ID.
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
     * Remove an order from the kitchen queue.
     */
    @Transactional
    public void remove(
            UUID orderId
    ) {

        kitchenQueueDao.deleteByOrderId(
                orderId
        );
    }
}