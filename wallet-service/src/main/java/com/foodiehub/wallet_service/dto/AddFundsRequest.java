package com.foodiehub.wallet_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddFundsRequest(@NotNull @DecimalMin(value = "0.01")BigDecimal amount) {
}
