package com.foodiehub.dispatch_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AgentStatusResponse(
        UUID agentId,
        UUID userId,
        Boolean online,
        Boolean active,
        BigDecimal currentLatitude,
        BigDecimal currentLongitude,
        Instant lastHeartbeatAt
) {
}