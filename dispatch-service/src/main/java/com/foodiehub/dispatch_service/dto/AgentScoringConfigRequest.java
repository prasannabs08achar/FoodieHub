package com.foodiehub.dispatch_service.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AgentScoringConfigRequest(

        @NotNull(message = "Distance weight is required")
        @DecimalMin(value = "0.0", message = "Distance weight cannot be negative")
        @DecimalMax(value = "1.0", message = "Distance weight cannot exceed 1")
        BigDecimal distanceWeight,

        @NotNull(message = "Load weight is required")
        @DecimalMin(value = "0.0", message = "Load weight cannot be negative")
        @DecimalMax(value = "1.0", message = "Load weight cannot exceed 1")
        BigDecimal loadWeight,

        @NotNull(message = "Acceptance weight is required")
        @DecimalMin(value = "0.0", message = "Acceptance weight cannot be negative")
        @DecimalMax(value = "1.0", message = "Acceptance weight cannot exceed 1")
        BigDecimal acceptanceWeight,

        @NotNull(message = "Idle time weight is required")
        @DecimalMin(value = "0.0", message = "Idle time weight cannot be negative")
        @DecimalMax(value = "1.0", message = "Idle time weight cannot exceed 1")
        BigDecimal idleTimeWeight
) {
}