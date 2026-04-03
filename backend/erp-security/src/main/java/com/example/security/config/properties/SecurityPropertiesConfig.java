package com.example.security.config.properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables and registers all security-related configuration properties.
 * 
 * This class ensures that all @ConfigurationProperties records are:
 * - Registered as Spring beans
 * - Validated at startup (fail-fast)
 * - Available for dependency injection
 * 
 * @author ERP Team
 */
@Configuration
@EnableConfigurationProperties({
    JwtProperties.class,
    CookieProperties.class,
    CorsProperties.class,
    TenantProperties.class
})
public class SecurityPropertiesConfig {
    // Configuration properties are automatically registered
}
