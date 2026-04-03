package com.example.security.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
    String accessToken, 
    long expiresIn,
    String refreshToken,
    Long refreshExpiresIn
) {
    // Constructor for backward compatibility (without refresh token)
    public AuthResponse(String accessToken, long expiresIn) {
        this(accessToken, expiresIn, null, null);
    }
    
    // Constructor with refresh token
    public AuthResponse(String accessToken, long expiresIn, String refreshToken, long refreshExpiresIn) {
        this(accessToken, expiresIn, refreshToken, Long.valueOf(refreshExpiresIn));
    }
}
