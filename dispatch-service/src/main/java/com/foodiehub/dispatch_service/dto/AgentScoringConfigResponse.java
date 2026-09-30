package com.foodiehub.dispatch_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AgentScoringConfigResponse(
        UUID id,
        BigDecimal distanceWeight,
        BigDecimal loadWeight,
        BigDecimal acceptanceWeight,
        BigDecimal idleTimeWeight,
        Instant updatedAt
) {
}
