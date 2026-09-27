package com.foodiehub.order_service.dto;


import java.time.LocalDateTime;
import java.util.UUID;

public record CatalogRestaurantResponse(

        UUID id,

        UUID ownerId,

        String name,

        String description,

        String address,

        String city,

        String cuisine,

        Integer maxConcurrentOrders,

        Boolean open,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {
}