package com.foodiehub.dispatch_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BatchAgentScoreResponse(
        UUID agentId,
        UUID agentUserId,
        BigDecimal distanceScore,
        BigDecimal loadScore,
        BigDecimal acceptanceScore,
        BigDecimal idleTimeScore,
        BigDecimal totalScore,
        Integer activeDeliveryCount,
        BigDecimal acceptanceRate,
        long idleMinutes
) {
}