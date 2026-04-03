package com.example.security.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT Configuration Properties.
 * 
 * Bound to properties with prefix: erp.security.jwt
 * 
 * Example in application.properties:
 * erp.security.jwt.secret=${JWT_SECRET}
 * erp.security.jwt.access-expiration-seconds=3600
 * erp.security.jwt.refresh-expiration-seconds=86400
 * erp.security.jwt.tenant-claim=tenant
 * 
 * @author ERP Team
 */
@Validated
@ConfigurationProperties(prefix = "erp.security.jwt")
public record JwtProperties(
    
    /**
     * Secret key for signing JWT tokens.
     * MUST be at least 256 bits (32 characters) for HS256.
     * In production, use environment variable: ${JWT_SECRET}
     */
    @NotBlank(message = "JWT secret is required")
    String secret,
    
    /**
     * Access token expiration time in seconds.
     * Default: 3600 (1 hour)
     */
    @Min(value = 60, message = "Access token expiration must be at least 60 seconds")
    long accessExpirationSeconds,
    
    /**
     * Refresh token expiration time in seconds.
     * Default: 604800 (7 days)
     */
    @Min(value = 3600, message = "Refresh token expiration must be at least 1 hour")
    long refreshExpirationSeconds,
    
    /**
     * Claim name used for storing tenant ID in JWT.
     * Default: "tenant"
     */
    String tenantClaim
) {
    
    /**
     * Default constructor with sensible defaults.
     */
    public JwtProperties {
        if (tenantClaim == null || tenantClaim.isBlank()) {
            tenantClaim = "tenant";
        }
        if (accessExpirationSeconds <= 0) {
            accessExpirationSeconds = 3600;
        }
        if (refreshExpirationSeconds <= 0) {
            refreshExpirationSeconds = 604800;
        }
    }
}
