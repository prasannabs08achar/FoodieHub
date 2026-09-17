package com.foodiehub.auth_service.dto;

import com.foodiehub.auth_service.model.Role;

import java.util.UUID;

public record LoginResponse(String accessToken,
                            String tokenType,
                            long expiresInSeconds,
                            UUID userId,
                            Role role) {
}
