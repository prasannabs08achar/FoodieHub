package com.foodiehub.order_service.dto;

import com.foodiehub.order_service.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID customerId,
        UUID restaurantId,
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude,
        BigDecimal totalAmount,
        OrderStatus status,
        List<OrderItemResponse> items,
        List<OrderStateHistoryResponse> history,
        LocalDateTime predictedReadyAt,
        Instant createdAt,
        Instant updatedAt
) {
}