package com.foodiehub.dispatch_service.dto;

import java.util.UUID;

public record BatchEligibilityResponse(
        UUID orderId,
        boolean eligible,
        String reason
) {
}
