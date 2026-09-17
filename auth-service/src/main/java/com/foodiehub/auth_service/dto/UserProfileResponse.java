package com.foodiehub.auth_service.dto;

import com.foodiehub.auth_service.model.Role;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(UUID id,
                                  String email,
                                  String fullName,
                                  Role role,
                                  Instant createdAt) {
}
