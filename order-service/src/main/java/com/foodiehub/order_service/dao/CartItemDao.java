package com.foodiehub.order_service.dao;

import com.foodiehub.order_service.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemDao extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByCartId(UUID cartId);

    Optional<CartItem> findByCartIdAndMenuItemId(
            UUID cartId,
            UUID menuItemId
    );

    void deleteByCartId(UUID cartId);
}