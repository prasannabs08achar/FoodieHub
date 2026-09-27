package com.foodiehub.order_service.dto;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record CatalogMenuItemResponse(

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

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {
}
