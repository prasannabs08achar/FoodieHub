package com.foodiehub.dispatch_service.dao;



import com.foodiehub.dispatch_service.model.AssignmentStatus;
import com.foodiehub.dispatch_service.model.OrderAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

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

    boolean existsByOrderIdAndAgentIdAndStatusIn(
            UUID orderId,
            UUID agentId,
            List<AssignmentStatus> statuses
    );

    long countByOrderIdAndStatusIn(
            UUID orderId,
            List<AssignmentStatus> statuses
    );

    List<OrderAssignment> findByStatusAndExpiresAtBefore(
            AssignmentStatus status,
            java.time.LocalDateTime time
    );
}