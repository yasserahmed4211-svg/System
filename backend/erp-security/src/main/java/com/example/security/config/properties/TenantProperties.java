package com.example.security.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Multi-Tenancy Configuration Properties.
 * 
 * Bound to properties with prefix: erp.tenant
 * 
 * Example in application.properties:
 * erp.tenant.header-name=X-Tenant-ID
 * erp.tenant.default-id=default
 * 
 * @author ERP Team
 */
@Validated
@ConfigurationProperties(prefix = "erp.tenant")
public record TenantProperties(
    
    /**
     * HTTP header name used to pass tenant ID.
     * Default: X-Tenant-ID
     */
    String headerName,
    
    /**
     * Default tenant ID when none is provided.
     * Default: default
     */
    @NotBlank(message = "Default tenant ID is required")
    String defaultId
) {
    
    /**
     * Default constructor with sensible defaults.
     */
    public TenantProperties {
        if (headerName == null || headerName.isBlank()) {
            headerName = "X-Tenant-ID";
        }
        if (defaultId == null || defaultId.isBlank()) {
            defaultId = "default";
        }
    }
}
