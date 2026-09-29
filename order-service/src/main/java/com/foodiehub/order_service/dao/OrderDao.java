package com.foodiehub.order_service.dao;

import com.foodiehub.order_service.model.Order;
import com.foodiehub.order_service.model.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderDao extends JpaRepository<Order, UUID> {

    List<Order> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<Order> findByRestaurantIdAndStatus(
            UUID restaurantId,
            OrderStatus status
    );
    List<Order> findByStatusAndUpdatedAtBefore(
            OrderStatus status,
            Instant cutoffTime
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o
        FROM Order o
        WHERE o.id = :orderId
        """)
    Optional<Order> findByIdForUpdate(
            @Param("orderId") UUID orderId
    );
}