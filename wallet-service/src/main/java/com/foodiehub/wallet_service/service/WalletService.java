package com.foodiehub.wallet_service.service;

import com.foodiehub.wallet_service.dao.IdempotencyKeyDao;
import com.foodiehub.wallet_service.dao.WalletDao;
import com.foodiehub.wallet_service.dao.WalletTransactionDao;
import com.foodiehub.wallet_service.dto.AddFundsRequest;
import com.foodiehub.wallet_service.dto.AddFundsResponse;
import com.foodiehub.wallet_service.dto.WalletResponse;
import com.foodiehub.wallet_service.dto.WalletTransactionResponse;
import com.foodiehub.wallet_service.exception.WalletNotFoundException;
import com.foodiehub.wallet_service.model.IdempotencyKey;
import com.foodiehub.wallet_service.model.TransactionType;
import com.foodiehub.wallet_service.model.Wallet;
import com.foodiehub.wallet_service.model.WalletTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {
    private static final String ADD_FUNDS = "ADD_FUNDS";

    private final WalletDao walletRepository;
    private final WalletTransactionDao transactionRepository;
    private final IdempotencyKeyDao idempotencyKeyRepository;

    @Transactional
    public Wallet createWalletIfNotExists(UUID userId) {

        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {

                    Wallet wallet = Wallet.builder()
                            .userId(userId)
                            .balance(BigDecimal.ZERO)
                            .build();

                    return walletRepository.save(wallet);
                });
    }
    @Transactional(readOnly = true)
    public WalletResponse getWallet(UUID userId) {

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Wallet not found for user: " + userId
                        )
                );

        return toWalletResponse(wallet);
    }
    @Transactional
    public AddFundsResponse addFunds(
            UUID userId,
            AddFundsRequest request,
            String idempotencyKey
    ) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key header is required"
            );
        }

        var existingKey =
                idempotencyKeyRepository
                        .findByUserIdAndKeyAndOperation(
                                userId,
                                idempotencyKey,
                                ADD_FUNDS
                        );

        if (existingKey.isPresent()) {
            return deserializeAddFundsResponse(
                    existingKey.get().getResponse()
            );
        }

        Wallet wallet = walletRepository
                .findByUserIdForUpdate(userId)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Wallet not found for user: " + userId
                        )
                );

        BigDecimal balanceBefore = wallet.getBalance();

        BigDecimal balanceAfter =
                balanceBefore.add(request.amount());

        wallet.setBalance(balanceAfter);

        walletRepository.save(wallet);

        WalletTransaction transaction =
                WalletTransaction.builder()
                        .walletId(wallet.getId())
                        .type(TransactionType.CREDIT)
                        .amount(request.amount())
                        .balanceBefore(balanceBefore)
                        .balanceAfter(balanceAfter)
                        .referenceType("ADD_FUNDS")
                        .description("Wallet funds added")
                        .build();

        WalletTransaction savedTransaction =
                transactionRepository.save(transaction);

        AddFundsResponse response =
                new AddFundsResponse(
                        savedTransaction.getId(),
                        wallet.getId(),
                        request.amount(),
                        balanceAfter
                );

        IdempotencyKey key =
                IdempotencyKey.builder()
                        .userId(userId)
                        .key(idempotencyKey)
                        .operation(ADD_FUNDS)
                        .response(serializeAddFundsResponse(response))
                        .build();

        idempotencyKeyRepository.save(key);

        return response;
    }
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getTransactions(
            UUID userId,
            Pageable pageable
    ) {

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Wallet not found for user: " + userId
                        )
                );

        return transactionRepository
                .findByWalletId(wallet.getId(), pageable)
                .map(this::toTransactionResponse);
    }
    private WalletResponse toWalletResponse(Wallet wallet) {

        return new WalletResponse(
                wallet.getId(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }
    private WalletTransactionResponse toTransactionResponse(
            WalletTransaction transaction
    ) {

        return new WalletTransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceBefore(),
                transaction.getBalanceAfter(),
                transaction.getReferenceType(),
                transaction.getReferenceId(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }

    private String serializeAddFundsResponse(
            AddFundsResponse response
    ) {

        return response.transactionId()
                + "|" +
                response.walletId()
                + "|" +
                response.amount()
                + "|" +
                response.balance();
    }

    private AddFundsResponse deserializeAddFundsResponse(
            String response
    ) {

        String[] values = response.split("\\|");

        return new AddFundsResponse(
                UUID.fromString(values[0]),
                UUID.fromString(values[1]),
                new BigDecimal(values[2]),
                new BigDecimal(values[3])
        );
    }
}
