package com.foodiehub.dispatch_service.client;

import com.foodiehub.dispatch_service.dto.DispatchOrderResponse;
import com.foodiehub.dispatch_service.dto.OrderStatusUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "order-service")
public interface OrderClient {

    @GetMapping("/api/orders/{orderId}")
    DispatchOrderResponse getOrder(
            @PathVariable UUID orderId
    );

    @GetMapping("/api/orders/internal/ready-for-pickup")
    List<DispatchOrderResponse> getReadyForPickupOrders();

    @PatchMapping("/api/orders/{orderId}/status")
    void updateStatus(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID changedBy,
            @RequestBody OrderStatusUpdateRequest request
    );

    @PostMapping("/api/orders/{orderId}/system-cancel")
    void systemCancel(
            @PathVariable UUID orderId,
            @RequestBody OrderStatusUpdateRequest request
    );
}