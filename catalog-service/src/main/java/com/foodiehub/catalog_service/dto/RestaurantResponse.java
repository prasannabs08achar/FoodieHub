package com.foodiehub.catalog_service.dto;

import java.time.Instant;
import java.util.UUID;

public record RestaurantResponse(UUID id,
                                 UUID ownerId,
                                 String name,
                                 String description,
                                 String address,
                                 String city,
                                 String cuisine,
                                 Integer maxConcurrentOrders,
                                 Boolean open,
                                 Instant createdAt,
                                 Instant updatedAt) {
}
