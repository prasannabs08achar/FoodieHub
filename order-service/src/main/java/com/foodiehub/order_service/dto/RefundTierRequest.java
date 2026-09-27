package com.foodiehub.order_service.dto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RefundTierRequest(

        @NotNull(message = "Refund percentage is required")
        @DecimalMin(
                value = "0.00",
                message = "Refund percentage cannot be negative"
        )
        @DecimalMax(
                value = "100.00",
                message = "Refund percentage cannot exceed 100"
        )
        BigDecimal refundPercentage

) {
}