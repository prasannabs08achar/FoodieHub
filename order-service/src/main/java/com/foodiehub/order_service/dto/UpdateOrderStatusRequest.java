package com.foodiehub.order_service.dto;

import com.foodiehub.order_service.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(

        @NotNull(message = "Status is required")
        OrderStatus status,

        String reason

) {
}