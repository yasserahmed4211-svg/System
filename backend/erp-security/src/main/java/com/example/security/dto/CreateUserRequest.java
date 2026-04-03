package com.example.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "{validation.required}") 
        @Size(min = 3, max = 80, message = "{validation.size}") 
        String username,
        
        @NotBlank(message = "{validation.required}") 
        @Size(min = 6, max = 120, message = "{validation.size}") 
        String password
) {}
