package com.foodiehub.dispatch_service.dto;

public record OrderStatusUpdateRequest(
        String status,
        String reason
) {
}