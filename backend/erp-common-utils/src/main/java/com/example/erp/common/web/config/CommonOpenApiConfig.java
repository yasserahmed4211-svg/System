package com.example.erp.common.web.config;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

/**
 * ✅ Common OpenAPI (Swagger) Utilities for ERP modules
 * 
 * Provides:
 * - Pageable parameter handling (fixes ["string"] issue in Swagger UI)
 * 
 * Architecture Rules:
 * - Rule 12: Common configurations belong in erp-common-utils
 * - Rule 22.1: OpenAPI bean definitions MUST be in erp-main only
 * 
 * ⚠️ NOTE: This class does NOT define OpenAPI bean. 
 * erp-main/OpenApiConfig.java provides the main OpenAPI configuration
 * with API grouping and JWT security.
 * 
 * @author ERP Team
 */
@Configuration
public class CommonOpenApiConfig {

    static {
        // Fix Pageable parameter display in Swagger UI
        // Prevents ["string"] default values in page/size/sort parameters
        SpringDocUtils.getConfig().replaceWithClass(
                org.springframework.data.domain.Pageable.class,
                org.springdoc.core.converters.models.Pageable.class
        );
    }

    // No @Bean methods - all OpenAPI configuration is in erp-main
}
