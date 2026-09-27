package com.foodiehub.order_service.client;

import com.foodiehub.order_service.dto.DebitWalletRequest;
import com.foodiehub.order_service.dto.DebitWalletResponse;
import com.foodiehub.order_service.dto.RefundWalletRequest;
import com.foodiehub.order_service.dto.RefundWalletResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "wallet-service")
public interface WalletClient {

    @PostMapping("/api/wallet/debit")
    DebitWalletResponse debitWallet(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody DebitWalletRequest request
    );

    @PostMapping("/api/wallet/refund")
    RefundWalletResponse refundWallet(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody RefundWalletRequest request
    );
}