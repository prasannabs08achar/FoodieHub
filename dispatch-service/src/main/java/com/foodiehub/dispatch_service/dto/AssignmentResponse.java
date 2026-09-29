package com.foodiehub.dispatch_service.dto;

import com.foodiehub.dispatch_service.model.AssignmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AssignmentResponse(
        UUID assignmentId,
        UUID orderId,
        UUID agentId,
        UUID agentUserId,
        AssignmentStatus status,
        LocalDateTime assignedAt
) {
}