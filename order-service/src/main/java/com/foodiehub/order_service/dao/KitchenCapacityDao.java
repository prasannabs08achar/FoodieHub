package com.foodiehub.order_service.dao;

import com.foodiehub.order_service.model.KitchenCapacity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface KitchenCapacityDao
        extends JpaRepository<KitchenCapacity, UUID> {

    Optional<KitchenCapacity> findByRestaurantId(
            UUID restaurantId
    );

    /*
     * Pessimistically locks the kitchen capacity row.
     *
     * This is the synchronization point for concurrent
     * ACCEPTED -> PREPARING requests.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT k
            FROM KitchenCapacity k
            WHERE k.restaurantId = :restaurantId
            """)
    Optional<KitchenCapacity> findByRestaurantIdForUpdate(
            @Param("restaurantId") UUID restaurantId
    );
}