package com.foodiehub.wallet_service.dto;

import com.foodiehub.wallet_service.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletTransactionResponse(UUID transactionId,
                                        TransactionType type,
                                        BigDecimal amount,
                                        BigDecimal balanceBefore,
                                        BigDecimal balanceAfter,
                                        String referenceType,
                                        String referenceId,
                                        String description,
                                        Instant createdAt) {
}
