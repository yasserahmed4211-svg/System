package com.example.erp.common.web.config;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.util.List;

/**
 * Base Web MVC Configuration helper for all ERP modules.
 * 
 * Provides:
 * - Standard pagination configuration
 * - Request interceptor registration helper
 * 
 * Each module's WebConfig should call these methods to ensure consistency.
 * 
 * Architecture Rules:
 * - Rule 17.1: Default page size = 20
 * - Rule 17.2: Maximum page size = 100
 * 
 * Usage in module WebConfig:
 * <pre>
 * &#64;Configuration
 * public class WebConfig implements WebMvcConfigurer {
 *     
 *     &#64;Override
 *     public void addArgumentResolvers(List&lt;HandlerMethodArgumentResolver&gt; resolvers) {
 *         resolvers.add(CommonWebConfigurer.createPageableResolver());
 *     }
 * }
 * </pre>
 * 
 * @author ERP Team
 */
public class CommonWebConfigurer {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Create standard PageableHandlerMethodArgumentResolver with ERP defaults.
     * 
     * @return Configured PageableHandlerMethodArgumentResolver
     */
    public static PageableHandlerMethodArgumentResolver createPageableResolver() {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        
        // Set default pageable when none provided
        resolver.setFallbackPageable(PageRequest.of(0, DEFAULT_PAGE_SIZE));
        
        // Enforce maximum page size to prevent performance issues
        resolver.setMaxPageSize(MAX_PAGE_SIZE);
        
        return resolver;
    }

    /**
     * Validate that requested page size doesn't exceed maximum.
     * 
     * @param pageable The pageable to validate
     * @return Validated pageable (size capped at MAX_PAGE_SIZE)
     */
    public static Pageable enforcePaginationLimits(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            return PageRequest.of(
                pageable.getPageNumber(), 
                MAX_PAGE_SIZE, 
                pageable.getSort()
            );
        }
        return pageable;
    }

    /**
     * Get default page size
     */
    public static int getDefaultPageSize() {
        return DEFAULT_PAGE_SIZE;
    }

    /**
     * Get maximum allowed page size
     */
    public static int getMaxPageSize() {
        return MAX_PAGE_SIZE;
    }
}
