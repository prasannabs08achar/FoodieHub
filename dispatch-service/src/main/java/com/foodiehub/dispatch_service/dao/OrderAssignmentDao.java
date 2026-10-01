package com.foodiehub.dispatch_service.dao;

import com.foodiehub.dispatch_service.model.AssignmentStatus;
import com.foodiehub.dispatch_service.model.OrderAssignment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderAssignmentDao
        extends JpaRepository<OrderAssignment, UUID> {

    List<OrderAssignment> findByOrderId(UUID orderId);

    List<OrderAssignment> findByAgentId(UUID agentId);

    Optional<OrderAssignment> findByOrderIdAndStatus(
            UUID orderId,
            AssignmentStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT oa
            FROM OrderAssignment oa
            WHERE oa.orderId = :orderId
            AND oa.status = :status
            """)
    Optional<OrderAssignment> findByOrderIdAndStatusForUpdate(
            @Param("orderId") UUID orderId,
            @Param("status") AssignmentStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT oa
            FROM OrderAssignment oa
            WHERE oa.id = :assignmentId
            """)
    Optional<OrderAssignment> findByIdForUpdate(
            @Param("assignmentId") UUID assignmentId
    );

    boolean existsByOrderIdAndAgentIdAndStatusIn(
            UUID orderId,
            UUID agentId,
            List<AssignmentStatus> statuses
    );

    boolean existsByOrderIdAndStatus(
            UUID orderId,
            AssignmentStatus status
    );

    long countByOrderIdAndStatusIn(
            UUID orderId,
            List<AssignmentStatus> statuses
    );

    /*
     * Number of currently active deliveries for an agent.
     *
     * ACCEPTED assignment + completedAt NULL
     */
    long countByAgentIdAndStatusAndCompletedAtIsNull(
            UUID agentId,
            AssignmentStatus status
    );

    Optional<OrderAssignment>
    findTopByAgentIdAndStatusOrderByOfferedAtDesc(
            UUID agentId,
            AssignmentStatus status
    );

    List<OrderAssignment> findByOrderIdOrderByAttemptNumberAsc(
            UUID orderId
    );

    List<OrderAssignment> findByStatusAndExpiresAtBefore(
            AssignmentStatus status,
            LocalDateTime time
    );

    List<OrderAssignment> findByStatus(
            AssignmentStatus assignmentStatus
    );
}