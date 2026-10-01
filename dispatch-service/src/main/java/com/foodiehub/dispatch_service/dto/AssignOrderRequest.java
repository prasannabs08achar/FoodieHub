package com.foodiehub.dispatch_service.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignOrderRequest(
        @NotNull
        UUID orderId
) {
}