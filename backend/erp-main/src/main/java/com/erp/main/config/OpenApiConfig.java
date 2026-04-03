package com.erp.main.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration for ERP Main Application
 * 
 * ✅ Architecture Compliance (Rule 22.1):
 * - All Swagger grouping defined in erp-main only
 * - Each module has its own group
 * - packages-to-scan used (not paths-to-match)
 * - @Primary to override CommonOpenApiConfig from erp-common-utils
 * 
 * Access Swagger UI at: http://localhost:7272/swagger-ui.html
 * Access OpenAPI JSON at: http://localhost:7272/api-docs
 * 
 * @author ERP Team
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:7272}")
    private String serverPort;

    /**
     * Global OpenAPI configuration
     * @Primary ensures this bean takes precedence over CommonOpenApiConfig
     */
    @Bean
    @Primary
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "Bearer Authentication";
        
        return new OpenAPI()
            // JWT Bearer token security scheme
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter JWT token from /api/auth/login (without 'Bearer' prefix)")
                )
            )
            // Apply security globally to all endpoints
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            
            // API Information
            .info(new Info()
                .title("ERP System API")
                .description("""
                    # 🏢 ERP System - Enterprise Resource Planning
                    
                    نظام تخطيط موارد المؤسسة المتكامل
                    
                    ## 📚 Available Modules:
                    
                    | Module | Description | Group |
                    |--------|-------------|-------|
                    | 🔐 **Security** | Authentication, Users, Roles, Permissions | `1-security` |
                    | 📋 **Master Data** | Activities and reference data | `2-masterdata` |
                    | 💰 **Finance GL** | General Ledger, Journals, Posting | `3-finance-gl` |
                    
                    ## 🔑 Authentication:
                    
                    1. Call `POST /api/auth/login` with username/password
                    2. Copy the `accessToken` from response
                    3. Click **Authorize** button (🔒) above
                    4. Enter token (without "Bearer" prefix)
                    5. Click **Authorize** → **Close**
                    
                    ## 📄 Pagination:
                    - Use `page` (0-based), `size` (default: 20, max: 100)
                    - Sort: `sort=field,direction` (e.g., `sort=name,asc`)
                    
                    ## 🔍 Advanced Search:
                    - POST `/search` endpoints support dynamic filters
                    - Operators: EQUALS, CONTAINS, GREATER_THAN, IN, etc.
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("ERP Development Team")
                    .email("dev@erp-system.com")
                    .url("https://github.com/erp-system"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Development Server"),
                new Server()
                    .url("https://api.erp-system.com")
                    .description("Production Server")
            ));
    }

    // ========================================
    // API Groups - Each module gets its own group
    // ========================================

    /**
     * 🔐 Security Module APIs
     * - Authentication (login, refresh, logout)
     * - User Management
     * - Role Management  
     * - Permission Management
     * - Page Management
     * - Menu Management
     */
    @Bean
    public GroupedOpenApi securityApi() {
        return GroupedOpenApi.builder()
            .group("1-security")
            .displayName("🔐 Security & Authentication")
            .packagesToScan("com.example.security.controller")
            .build();
    }

    /**
     * 📋 Master Data Module APIs
     * - Activities
     * - (Future: Customers, Suppliers, Products, etc.)
     */
    @Bean
    public GroupedOpenApi masterDataApi() {
        return GroupedOpenApi.builder()
            .group("2-masterdata")
            .displayName("📋 Master Data")
            .packagesToScan("com.example.masterdata.controller")
            .build();
    }

    /**
     * 💰 Finance GL Module APIs
     * - GL Accounts
     * - Manual Journals
     * - Journal Queries
     * - Posting Engine
     * - Account Balances
     * - Fiscal Periods
     * - Financial Reports
     */
    @Bean
    public GroupedOpenApi financeGlApi() {
        return GroupedOpenApi.builder()
            .group("3-finance-gl")
            .displayName("💰 Finance - General Ledger")
            .packagesToScan("com.example.erp.finance.gl.controller")
            .build();
    }

    /**
     * 📊 All APIs (Combined view)
     * Shows all endpoints from all modules
     */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
            .group("0-all")
            .displayName("📊 All APIs")
            .packagesToScan(
                "com.example.security.controller",
                "com.example.masterdata.controller",
                "com.example.erp.finance.gl.controller"
            )
            .build();
    }
}
