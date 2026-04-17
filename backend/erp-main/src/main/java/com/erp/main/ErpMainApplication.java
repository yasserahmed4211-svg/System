package com.erp.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

import java.util.Locale;

/**
 * ERP System - Unified Main Application
 * 
 * Aggregates all ERP modules:
 * - Security & Authentication (Port 7272 standalone)
 * - Master Data Management (Port 7373 standalone)
 * - Finance - General Ledger (Port 7474 standalone)
 * 
 * All APIs accessible through single Swagger UI at port 7272
 * 
 * Architecture: Rule 6 - One-Way Dependencies (DAG)
 * common-utils → security → masterdata → finance-gl → main
 * 
 * @author ERP Team
 */
@SpringBootApplication(
        excludeName = {
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
                "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
        }
)
// @EnableCaching  // ❌ DISABLED: Redis caching disabled - will be enabled later
@ComponentScan(basePackages = {
    // Main module (must be included when overriding component scan)
    "com.erp.main",

    // Core modules
    "com.example.security",                      // Security module
    "com.example.masterdata",                    // Master Data module
    "com.example.erp.finance.gl",               // Finance GL module
    "com.example.org",                              // Organization module
    
    // Common utilities
    "com.example.erp.common.web",               // Web components
    "com.example.erp.common.multitenancy",      // Multi-tenancy
    "com.example.erp.common.exception",         // Exception handling
    "com.example.erp.common.search",            // Search components
    "com.erp.common.search",                    // Search components (alternative package)
    "com.example.erp.common.i18n"               // Localization
})
public class ErpMainApplication {

    public static void main(String[] args) {
        // Set default locale to English with Western-Arabic numerals
        Locale.setDefault(Locale.forLanguageTag("en-US-u-nu-latn"));
        System.setProperty("user.language", "en");
        System.setProperty("user.country", "US");
        
        SpringApplication.run(ErpMainApplication.class, args);
    }
}
