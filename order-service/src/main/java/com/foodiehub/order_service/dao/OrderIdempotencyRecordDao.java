package com.foodiehub.order_service.dao;


import com.foodiehub.order_service.model.OrderIdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderIdempotencyRecordDao
        extends JpaRepository<OrderIdempotencyRecord, UUID> {

    Optional<OrderIdempotencyRecord>
    findByIdempotencyKey(String idempotencyKey);
}