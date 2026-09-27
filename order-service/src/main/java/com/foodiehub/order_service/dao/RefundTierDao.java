package com.foodiehub.order_service.dao;

import com.foodiehub.order_service.model.OrderStatus;
import com.foodiehub.order_service.model.RefundActor;
import com.foodiehub.order_service.model.RefundTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefundTierDao extends JpaRepository<RefundTier, UUID> {

    Optional<RefundTier> findByActorAndOrderStatusAndActiveTrue(
            RefundActor actor,
            OrderStatus orderStatus
    );
}