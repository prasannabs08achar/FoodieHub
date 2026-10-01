package com.foodiehub.dispatch_service.dao;

import com.foodiehub.dispatch_service.model.Batch;
import com.foodiehub.dispatch_service.model.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BatchDao extends JpaRepository<Batch, UUID> {

    List<Batch> findByRestaurantId(UUID restaurantId);

    List<Batch> findByStatus(BatchStatus status);

    List<Batch> findByRestaurantIdAndStatus(
            UUID restaurantId,
            BatchStatus status
    );

    Optional<Batch> findByAgentIdAndStatus(
            UUID agentId,
            BatchStatus status
    );

    List<Batch> findByIdIn(List<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b
            FROM Batch b
            WHERE b.id = :batchId
            """)
    Optional<Batch> findByIdForUpdate(
            @Param("batchId") UUID batchId
    );

}