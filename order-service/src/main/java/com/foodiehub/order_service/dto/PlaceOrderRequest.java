package com.foodiehub.order_service.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PlaceOrderRequest(

        @NotNull(message = "Restaurant ID is required")
        UUID restaurantId,

        @NotNull(message = "Delivery latitude is required")
        @DecimalMin(value = "-90.0", message = "Invalid delivery latitude")
        @DecimalMax(value = "90.0", message = "Invalid delivery latitude")
        BigDecimal deliveryLatitude,

        @NotNull(message = "Delivery longitude is required")
        @DecimalMin(value = "-180.0", message = "Invalid delivery longitude")
        @DecimalMax(value = "180.0", message = "Invalid delivery longitude")
        BigDecimal deliveryLongitude

) {
}