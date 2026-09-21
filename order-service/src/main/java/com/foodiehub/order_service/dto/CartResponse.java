package com.foodiehub.order_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponse(

        UUID id,

        UUID customerId,

        UUID restaurantId,

        List<CartItemResponse> items,

        BigDecimal totalAmount,

        Instant createdAt,

        Instant updatedAt

) {
}