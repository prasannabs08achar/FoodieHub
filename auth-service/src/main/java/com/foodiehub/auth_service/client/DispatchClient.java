package com.foodiehub.auth_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "dispatch-service")
public interface DispatchClient {

    @PostMapping("/api/dispatch/agents")
    void provisionAgent(@RequestBody AgentProvisionRequest request);

    record AgentProvisionRequest(UUID userId) {
    }
}