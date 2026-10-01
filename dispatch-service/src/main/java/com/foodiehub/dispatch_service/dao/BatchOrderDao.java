package com.foodiehub.dispatch_service.dao;


import com.foodiehub.dispatch_service.model.BatchOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BatchOrderDao extends JpaRepository<BatchOrder, UUID> {

    List<BatchOrder> findByBatchIdOrderByStopSequenceAsc(
            UUID batchId
    );

    List<BatchOrder> findByOrderId(
            UUID orderId
    );

    Optional<BatchOrder> findByBatchIdAndOrderId(
            UUID batchId,
            UUID orderId
    );

    boolean existsByBatchIdAndOrderId(
            UUID batchId,
            UUID orderId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT bo
            FROM BatchOrder bo
            WHERE bo.id = :batchOrderId
            """)
    Optional<BatchOrder> findByIdForUpdate(
            @Param("batchOrderId") UUID batchOrderId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT bo
            FROM BatchOrder bo
            WHERE bo.batchId = :batchId
            ORDER BY bo.stopSequence ASC
            """)
    List<BatchOrder> findByBatchIdForUpdate(
            @Param("batchId") UUID batchId
    );

    List<BatchOrder> findByOrderIdForUpdate(UUID id);
}