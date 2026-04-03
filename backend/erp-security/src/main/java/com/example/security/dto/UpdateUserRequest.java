package com.example.security.dto;

import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateUserRequest(
        @Size(min = 3, max = 80, message = "{validation.size}") String username,
        @Size(min = 6, max = 120, message = "{validation.size}") String password,
        Boolean enabled,
        Set<String> roleNames
) {}
