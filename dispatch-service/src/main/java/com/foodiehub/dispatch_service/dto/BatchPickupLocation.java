package com.foodiehub.dispatch_service.dto;

import java.math.BigDecimal;

public record BatchPickupLocation(
        BigDecimal latitude,
        BigDecimal longitude
) {
}