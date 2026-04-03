package com.example.security.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * CORS Configuration Properties.
 * 
 * Bound to properties with prefix: erp.security.cors
 * 
 * Example in application.properties:
 * erp.security.cors.allowed-origins=http://localhost:4200,http://localhost:4201
 * 
 * @author ERP Team
 */
@Validated
@ConfigurationProperties(prefix = "erp.security.cors")
public record CorsProperties(
    
    /**
     * Comma-separated list of allowed origins.
     * Example: http://localhost:4200,https://app.example.com
     */
    @NotBlank(message = "At least one CORS allowed origin is required")
    String allowedOrigins
) {
    
    /**
     * Get allowed origins as a List.
     */
    public List<String> getAllowedOriginsList() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return List.of("http://localhost:4200");
        }
        return List.of(allowedOrigins.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
