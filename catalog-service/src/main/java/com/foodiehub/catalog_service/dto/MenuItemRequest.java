package com.foodiehub.catalog_service.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalTime;

public record MenuItemRequest(@NotBlank(message = "Menu item name is required")
                              @Size(max = 150)
                              String name,

                              @Size(max = 500)
                              String description,

                              @NotNull(message = "Price is required")
                              @DecimalMin(value = "0.01", message = "Price must be greater than 0")
                              BigDecimal price,

                              @NotNull(message = "Daily quantity is required")
                              @Min(value = 1, message = "Daily quantity must be greater than 0")
                              Integer dailyQuantity,

                              LocalTime availableFrom,

                              LocalTime availableTo) {
}
