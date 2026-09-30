package com.foodiehub.dispatch_service.dto;


import java.math.BigDecimal;
import java.util.UUID;

public record AgentScoreResponse(
        UUID agentId,
        UUID userId,
        BigDecimal distanceKm,
        BigDecimal distanceScore,
        BigDecimal loadScore,
        BigDecimal acceptanceScore,
        BigDecimal idleTimeScore,
        BigDecimal finalScore
) {
}