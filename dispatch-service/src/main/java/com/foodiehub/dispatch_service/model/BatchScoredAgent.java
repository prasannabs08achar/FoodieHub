package com.foodiehub.dispatch_service.model;

import java.math.BigDecimal;
import java.util.UUID;

public record BatchScoredAgent(
        UUID agentId,
        UUID agentUserId,
        BigDecimal totalScore,
        BigDecimal distanceScore,
        BigDecimal loadScore,
        BigDecimal acceptanceScore,
        BigDecimal idleTimeScore,
        int activeDeliveryCount,
        BigDecimal acceptanceRate,
        long idleMinutes
) {
}