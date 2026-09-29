package com.foodiehub.catalog_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record MenuItemResponse(

        UUID id,

        UUID restaurantId,

        String name,

        String description,

        BigDecimal price,

        Integer dailyQuantity,

        Integer remainingToday,

        LocalTime availableFrom,

        LocalTime availableTo,

        Boolean active,

        MenuItemAvailabilityStatus availabilityStatus,

        Instant createdAt,

        Instant updatedAt
) {
}