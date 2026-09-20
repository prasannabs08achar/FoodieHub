package com.foodiehub.catalog_service.dao;

import com.foodiehub.catalog_service.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordDao
        extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByIdempotencyKey(
            String idempotencyKey
    );
}