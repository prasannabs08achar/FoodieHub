package com.foodiehub.order_service.controller;

import com.foodiehub.order_service.client.CatalogClient;
import com.foodiehub.order_service.client.WalletClient;
import com.foodiehub.order_service.dto.CatalogMenuItemResponse;
import com.foodiehub.order_service.dto.DebitWalletRequest;
import com.foodiehub.order_service.dto.DebitWalletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/test/wallet")
@RequiredArgsConstructor
public class WalletClientTestController {

    private final WalletClient walletClient;


    @PostMapping("/debit")
    public DebitWalletResponse debitWallet(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestParam BigDecimal amount
    ) {

        DebitWalletRequest request =
                new DebitWalletRequest(
                        amount,
                        "FEIGN-TEST"
                );

        return walletClient.debitWallet(
                userId,
                idempotencyKey,
                request
        );
    }

}
