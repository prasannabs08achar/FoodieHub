package com.foodiehub.order_service.controller;

import com.foodiehub.order_service.dto.AddCartItemRequest;
import com.foodiehub.order_service.dto.CartResponse;
import com.foodiehub.order_service.dto.UpdateCartItemRequest;
import com.foodiehub.order_service.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/{restaurantId}/items")
    public ResponseEntity<CartResponse> addItem(
            @RequestHeader("X-User-Id") UUID customerId,
            @PathVariable UUID restaurantId,
            @Valid @RequestBody AddCartItemRequest request
    ) {

        CartResponse response =
                cartService.addItem(
                        customerId,
                        restaurantId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{restaurantId}")
    public ResponseEntity<CartResponse> getCart(
            @RequestHeader("X-User-Id") UUID customerId,
            @PathVariable UUID restaurantId
    ) {

        CartResponse response =
                cartService.getCart(
                        customerId,
                        restaurantId
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{restaurantId}/items/{menuItemId}")
    public ResponseEntity<CartResponse> updateItem(
            @RequestHeader("X-User-Id") UUID customerId,
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuItemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {

        CartResponse response =
                cartService.updateItem(
                        customerId,
                        restaurantId,
                        menuItemId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{restaurantId}/items/{menuItemId}")
    public ResponseEntity<Void> removeItem(
            @RequestHeader("X-User-Id") UUID customerId,
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuItemId
    ) {

        cartService.removeItem(
                customerId,
                restaurantId,
                menuItemId
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{restaurantId}")
    public ResponseEntity<Void> clearCart(
            @RequestHeader("X-User-Id") UUID customerId,
            @PathVariable UUID restaurantId
    ) {

        cartService.clearCart(
                customerId,
                restaurantId
        );

        return ResponseEntity.noContent().build();
    }
}