package com.foodiehub.wallet_service.kafka;

import java.util.UUID;

public record UserRegisteredEvent(
        UUID userId,
        String email,
        String fullName,
        String role
) {
}