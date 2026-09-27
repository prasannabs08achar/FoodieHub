package com.foodiehub.order_service.controller;

import com.foodiehub.order_service.dto.OrderResponse;
import com.foodiehub.order_service.dto.OrderStateHistoryResponse;
import com.foodiehub.order_service.dto.PlaceOrderRequest;
import com.foodiehub.order_service.dto.UpdateOrderStatusRequest;
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
}