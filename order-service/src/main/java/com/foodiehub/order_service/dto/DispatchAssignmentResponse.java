package com.foodiehub.order_service.dto;

import com.foodiehub.order_service.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record DispatchAssignmentResponse(
        UUID assignmentId,
        UUID orderId,
        UUID agentId,
        UUID agentUserId,
        String status,
        LocalDateTime assignedAt
) {
}