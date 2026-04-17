package com.example.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

import java.util.Locale;

/**
 * Security Module Application
 * 
 * Note: @EnableJpaRepositories is configured in JpaConfig to avoid duplicate bean definitions.
 * This ensures proper entity/repository scanning without bean override warnings.
 */
@EnableCaching
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.example.security",                      // Security module
    "com.example.erp.common.web",               // Common web components (includes CommonWebConfig)
    "com.example.erp.common.multitenancy",      // Multi-tenancy support
    "com.example.erp.common.exception",         // Exception handling
    "com.example.erp.common.search",            // Search components
    "com.example.erp.common.i18n"               // Localization support (required by GlobalExceptionHandler)
})
public class SecurityOracleJwtApplication {

    public static void main(String[] args) {
        Locale.setDefault(Locale.forLanguageTag("en-US-u-nu-latn"));
        System.setProperty("user.language", "en");
        System.setProperty("user.country", "US");
        SpringApplication.run(SecurityOracleJwtApplication.class, args);
    }
}
