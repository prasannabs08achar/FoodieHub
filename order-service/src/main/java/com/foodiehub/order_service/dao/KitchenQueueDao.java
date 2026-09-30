package com.foodiehub.order_service.dao;

import com.foodiehub.order_service.model.KitchenQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    /*
     * Get restaurants that currently have
     * at least one order waiting in the kitchen queue.
     */
    @Query("""
            SELECT DISTINCT k.restaurantId
            FROM KitchenQueue k
            """)
    List<UUID> findDistinctRestaurantIds();
}