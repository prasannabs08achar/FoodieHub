package com.foodiehub.order_service.controller;

import com.foodiehub.order_service.dto.*;
import com.foodiehub.order_service.model.OrderStatus;
import com.foodiehub.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID orderId
    ) {

        return ResponseEntity.ok(
                orderService.getOrder(orderId)
        );
    }

    @GetMapping("/customer")
    public ResponseEntity<List<OrderResponse>> getCustomerOrders(
            @RequestHeader("X-User-Id") UUID customerId
    ) {

        return ResponseEntity.ok(
                orderService.getCustomerOrders(customerId)
        );
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<OrderResponse>> getRestaurantOrders(
            @PathVariable UUID restaurantId,
            @RequestParam OrderStatus status
    ) {

        return ResponseEntity.ok(
                orderService.getRestaurantOrders(
                        restaurantId,
                        status
                )
        );
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID changedBy,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {

        return ResponseEntity.ok(
                orderService.updateStatus(
                        orderId,
                        changedBy,
                        request
                )
        );
    }

    @GetMapping("/{orderId}/history")
    public ResponseEntity<List<OrderStateHistoryResponse>>
    getStateHistory(
            @PathVariable UUID orderId
    ) {

        return ResponseEntity.ok(
                orderService.getStateHistory(orderId)
        );
    }
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(

            @RequestHeader("X-User-Id")
            UUID customerId,

            @RequestHeader("Idempotency-Key")
            String idempotencyKey,

            @Valid
            @RequestBody
            PlaceOrderRequest request
    ) {

        return ResponseEntity.ok(
                orderService.placeOrder(
                        customerId,
                        request,
                        idempotencyKey
                )
        );
    }
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID customerId,
            @Valid @RequestBody CancelOrderRequest request
    ) {

        return ResponseEntity.ok(
                orderService.cancelOrder(
                        orderId,
                        customerId,
                        request.reason()
                )
        );
    }

    @PostMapping("/{orderId}/restaurant-cancel")
    public ResponseEntity<OrderResponse> restaurantCancelOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID restaurantOwnerId,
            @Valid @RequestBody CancelOrderRequest request
    ) {

        return ResponseEntity.ok(
                orderService.restaurantCancelOrder(
                        orderId,
                        restaurantOwnerId,
                        request.reason()
                )
        );
    }
    @GetMapping("/internal/ready-for-pickup")
    public ResponseEntity<List<OrderResponse>> getReadyForPickupOrders() {

        return ResponseEntity.ok(
                orderService.getReadyForPickupOrders()
        );
    }

}