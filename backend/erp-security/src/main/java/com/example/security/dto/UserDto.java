package com.example.security.dto;

import java.time.Instant;
import java.util.Set;

public record UserDto(
    Long id,
    String username,
    String tenantId,
    boolean enabled,
    Set<String> roles,
    Set<String> permissions,
    Instant createdAt,
    String createdBy,
    Instant updatedAt,
    String updatedBy
) {}
