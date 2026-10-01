package com.foodiehub.dispatch_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DispatchOrderResponse(
        UUID id,
        UUID restaurantId,
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude,
        String status
) {
}