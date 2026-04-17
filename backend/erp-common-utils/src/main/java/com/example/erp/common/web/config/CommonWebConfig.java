package com.example.erp.common.web.config;

import com.example.erp.common.web.interceptor.RequestLoggingInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * ✅ CENTRALIZED Web MVC Configuration for ALL ERP modules
 * 
 * This @Configuration is automatically picked up by @ComponentScan in all modules.
 * NO NEED to create WebConfig.java in each module!
 * 
 * Provides:
 * - Pagination defaults (Rule 17.1, 17.2): default=20, max=100
 * - CORS configuration for Angular frontend
 * - Request/Response logging interceptor (Rule 15.2)
 * 
 * Architecture Rules:
 * - Rule 12: Common configurations belong in erp-common-utils
 * - Rule 15.2: Request/Response logging
 * - Rule 17.1: Use Spring Data Pageable
 * - Rule 17.2: Enforce max page size = 100
 * 
 * Usage:
 * Just add @ComponentScan("com.example.erp.common.web") in your module's main class:
 * 
 * <pre>
 * &#64;SpringBootApplication
 * &#64;ComponentScan(basePackages = {
 *     "com.example.your.module",
 *     "com.example.erp.common.web",        // ← This picks up CommonWebConfig
 *     "com.example.erp.common.multitenancy"
 * })
 * public class YourModuleApplication { }
 * </pre>
 * 
 * @author ERP Team
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CommonWebConfig implements WebMvcConfigurer {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final RequestLoggingInterceptor requestLoggingInterceptor;

    /**
     * Configure request/response logging interceptor (Rule 15.2)
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/api/auth/login",      // Don't log passwords
                    "/api/auth/logout",     // Sensitive endpoints
                    "/actuator/**",         // Health checks
                    "/swagger-ui/**",       // Swagger UI
                    "/v3/api-docs/**"       // OpenAPI docs
                );
        
        log.info("✅ RequestLoggingInterceptor registered for /api/** (excluding sensitive endpoints)");
    }

    /**
     * Configure pagination defaults for all modules
     * Rule 17.1, 17.2: Standard pagination
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        
        // Default: page=0, size=20
        resolver.setFallbackPageable(PageRequest.of(0, DEFAULT_PAGE_SIZE));
        
        // Maximum size = 100 (prevent performance issues)
        resolver.setMaxPageSize(MAX_PAGE_SIZE);
        
        resolvers.add(resolver);
        
        log.debug("Pageable resolver registered: default size={}, max size={}", 
            DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
    }

    // CORS is handled entirely by Spring Security's CorsFilter (SecurityConfig.corsConfigurationSource()).
    // Do NOT add addCorsMappings() here — it would create a second CORS handler at the MVC layer
    // that hardcodes origins and overrides Security's dynamic configuration, causing 403 rejections.
}
