package com.example.security.dto;

import java.util.Set;

public record UserDto(
    Long id, 
    String username, 
    String tenantId,
    boolean enabled, 
    Set<String> roles, 
    Set<String> permissions
) {}
