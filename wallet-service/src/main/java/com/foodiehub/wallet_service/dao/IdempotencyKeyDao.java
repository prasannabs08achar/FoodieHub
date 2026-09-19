package com.foodiehub.wallet_service.dao;

import com.foodiehub.wallet_service.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyDao extends JpaRepository<IdempotencyKey, UUID> {
    Optional<IdempotencyKey> findByUserIdAndKeyAndOperation(
            UUID userId,
            String key,
            String operation
    );
}
