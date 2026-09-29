package com.foodiehub.dispatch_service.client;

import com.foodiehub.dispatch_service.dto.OrderStatusUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "order-service")
public interface OrderClient {

    @PatchMapping("/api/orders/{orderId}/status")
    void updateStatus(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID changedBy,
            @RequestBody OrderStatusUpdateRequest request
    );
}