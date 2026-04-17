package com.example.masterdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * ERP Master Data Module - Main Application
 * 
 * Manages master data entities like MasterLookup, LookupDetail, etc.
 * 
 * Architecture Rules:
 * - Rule 12.3: Integration with erp-common-utils
 * - Component scanning includes common-utils packages
 * 
 * @author ERP Team
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.example.masterdata",                   // This module
    "com.example.erp.common.web",               // Common web components (Rule 12.3)
    "com.example.erp.common.multitenancy",      // Multi-tenancy support (Rule 12.3)
    "com.example.erp.common.exception",         // Exception handling
    "com.erp.common.search",                    // Search components (PageableBuilder, SearchRequest)
    "com.example.erp.common.i18n"               // Localization support (required by GlobalExceptionHandler)
})
public class MasterDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(MasterDataApplication.class, args);
    }
}
