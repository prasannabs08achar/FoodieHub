package com.foodiehub.dispatch_service.dto;
import com.foodiehub.dispatch_service.model.BatchStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BatchResponse(
        UUID batchId,
        UUID restaurantId,
        UUID agentId,
        BatchStatus status,
        LocalDateTime offeredAt,
        LocalDateTime expiresAt,
        LocalDateTime acceptedAt,
        LocalDateTime pickedUpAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<BatchOrderResponse> orders
) {
}