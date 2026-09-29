package com.foodiehub.order_service.dao;

import com.foodiehub.order_service.model.KitchenQueue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KitchenQueueDao
        extends JpaRepository<KitchenQueue, UUID> {

    Optional<KitchenQueue> findByOrderId(
            UUID orderId
    );

    /*
     * FIFO ordering.
     *
     * Oldest queued order comes first.
     */
    List<KitchenQueue> findByRestaurantIdOrderByQueuedAtAsc(
            UUID restaurantId
    );

    boolean existsByOrderId(
            UUID orderId
    );

    void deleteByOrderId(
            UUID orderId
    );
    List<KitchenQueue> findAllByOrderByQueuedAtAsc();
}