package com.foodiehub.dispatch_service.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AgentProvisionRequest(
        @NotNull(message = "User ID is required")
        UUID userId
) {
}