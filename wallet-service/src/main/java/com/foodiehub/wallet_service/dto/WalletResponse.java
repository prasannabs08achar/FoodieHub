package com.foodiehub.wallet_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletResponse(UUID walletId,
                             UUID userId,
                             BigDecimal balance,
                             Instant createdAt,
                             Instant updatedAt) {
}
