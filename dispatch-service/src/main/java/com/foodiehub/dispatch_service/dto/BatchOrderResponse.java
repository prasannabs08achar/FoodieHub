package com.foodiehub.dispatch_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record BatchOrderResponse(
        UUID batchOrderId,
        UUID orderId,
        Integer stopSequence,
        LocalDateTime deliveredAt
) {
}