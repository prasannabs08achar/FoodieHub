package com.foodiehub.wallet_service.controller;

import com.foodiehub.wallet_service.dto.AddFundsRequest;
import com.foodiehub.wallet_service.dto.AddFundsResponse;
import com.foodiehub.wallet_service.dto.WalletResponse;
import com.foodiehub.wallet_service.dto.WalletTransactionResponse;
import com.foodiehub.wallet_service.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;
    @GetMapping
    public ResponseEntity<WalletResponse> getWallet(
            @RequestHeader("X-User-Id") UUID userId
    ) {

        return ResponseEntity.ok(
                walletService.getWallet(userId)
        );
    }
    @PostMapping("/funds")
    public ResponseEntity<AddFundsResponse> addFunds(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AddFundsRequest request
    ) {

        return ResponseEntity.ok(
                walletService.addFunds(
                        userId,
                        request,
                        idempotencyKey
                )
        );
    }
    @GetMapping("/transactions")
    public ResponseEntity<Page<WalletTransactionResponse>> getTransactions(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        int safeSize = Math.min(size, 100);

        return ResponseEntity.ok(
                walletService.getTransactions(
                        userId,
                        PageRequest.of(page, safeSize)
                )
        );
    }
}
