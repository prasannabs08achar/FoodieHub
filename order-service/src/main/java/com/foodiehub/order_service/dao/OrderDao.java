package com.foodiehub.order_service.dao;

import com.foodiehub.order_service.model.Order;
import com.foodiehub.order_service.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderDao extends JpaRepository<Order, UUID> {

    List<Order> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<Order> findByRestaurantIdAndStatus(
            UUID restaurantId,
            OrderStatus status
    );
}