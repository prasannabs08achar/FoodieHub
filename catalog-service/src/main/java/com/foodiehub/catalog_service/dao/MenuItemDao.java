package com.foodiehub.catalog_service.dao;

import com.foodiehub.catalog_service.model.MenuItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuItemDao extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findByRestaurantId(UUID restaurantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT m
            FROM MenuItem m
            WHERE m.id = :menuItemId
            """)
    Optional<MenuItem> findByIdForUpdate(
            @Param("menuItemId") UUID menuItemId
    );

    /*
     * Reset the runtime stock counter for every menu item.
     *
     * DailyQuantity is the configured amount for the day.
     * RemainingToday is the runtime counter.
     */
    @Modifying
    @Query("""
            UPDATE MenuItem m
            SET m.remainingToday = m.dailyQuantity
            """)
    int resetRemainingToday();
}