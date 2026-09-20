package com.foodiehub.catalog_service.dto;

import jakarta.validation.constraints.*;

public record RestaurantRequest(@NotBlank(message = "Restaurant name is required")
                                @Size(max = 150)
                                String name,

                                @Size(max = 500)
                                String description,

                                @NotBlank(message = "Address is required")
                                @Size(max = 100)
                                String address,

                                @NotBlank(message = "City is required")
                                @Size(max = 100)
                                String city,

                                @NotBlank(message = "Cuisine is required")
                                @Size(max = 100)
                                String cuisine,

                                @NotNull(message = "Max concurrent orders is required")
                                @Min(value = 1, message = "Max concurrent orders must be greater than 0")
                                Integer maxConcurrentOrders) {
}
