package com.foodiehub.order_service.dao;

import com.foodiehub.order_service.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartDao extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByCustomerIdAndRestaurantId(
            UUID customerId,
            UUID restaurantId
    );
}