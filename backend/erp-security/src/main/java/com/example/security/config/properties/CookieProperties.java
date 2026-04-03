package com.example.security.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cookie Configuration Properties.
 * 
 * Bound to properties with prefix: erp.security.cookie
 * 
 * Example in application.properties:
 * erp.security.cookie.domain=localhost
 * erp.security.cookie.path=/
 * erp.security.cookie.secure=false
 * erp.security.cookie.http-only=true
 * erp.security.cookie.same-site=Lax
 * 
 * @author ERP Team
 */
@Validated
@ConfigurationProperties(prefix = "erp.security.cookie")
public record CookieProperties(
    
    /**
     * Cookie domain.
     * Default: localhost
     */
    String domain,
    
    /**
     * Cookie path.
     * Default: /
     */
    String path,
    
    /**
     * Whether cookie requires HTTPS.
     * MUST be true in production!
     * Default: false (for development)
     */
    boolean secure,
    
    /**
     * Whether cookie is HTTP-only (not accessible via JavaScript).
     * Should always be true for security tokens.
     * Default: true
     */
    boolean httpOnly,
    
    /**
     * SameSite attribute for CSRF protection.
     * Options: Strict, Lax, None
     * Default: Lax
     */
    String sameSite
) {
    
    /**
     * Default constructor with sensible defaults.
     */
    public CookieProperties {
        if (domain == null || domain.isBlank()) {
            domain = "localhost";
        }
        if (path == null || path.isBlank()) {
            path = "/";
        }
        if (sameSite == null || sameSite.isBlank()) {
            sameSite = "Lax";
        }
    }
}
