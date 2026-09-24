package com.foodiehub.order_service.dto;

import com.foodiehub.order_service.model.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderStateHistoryResponse(

        UUID id,

        OrderStatus fromStatus,

        OrderStatus toStatus,

        UUID changedBy,

        Instant changedAt,

        String reason

) {
}