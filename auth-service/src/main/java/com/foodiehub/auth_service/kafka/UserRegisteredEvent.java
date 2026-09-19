package com.foodiehub.auth_service.kafka;

import com.foodiehub.auth_service.model.Role;

import java.util.UUID;

public record UserRegisteredEvent(
        UUID userId,
        String email,
        String fullName,
        Role role
) {
}
