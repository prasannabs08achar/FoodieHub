package com.foodiehub.wallet_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DebitWalletResponse(
        UUID transactionId,
        UUID walletId,
        BigDecimal amount,
        BigDecimal balance
) {
}