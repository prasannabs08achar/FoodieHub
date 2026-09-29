package com.foodiehub.order_service.client;

import com.foodiehub.order_service.dto.DispatchAssignmentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@FeignClient(name = "dispatch-service")
public interface DispatchClient {

    @PostMapping("/api/dispatch/assignments/orders/{orderId}")
    DispatchAssignmentResponse assignStaticAgent(
            @PathVariable UUID orderId
    );
}