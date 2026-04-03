package com.example.erp.common.web.filter;

import com.example.erp.common.multitenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that adds tenant ID to MDC (Mapped Diagnostic Context) for logging.
 * This ensures all log entries include the tenant ID for multi-tenancy tracking.
 * 
 * Usage in logs: [tenantId] will automatically appear when configured in logback.
 * 
 * Architecture Rule: 15.4 - Tenant-Aware Logging
 * 
 * @author ERP Team
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // After CorrelationIdFilter
@Slf4j
public class TenantLoggingFilter extends OncePerRequestFilter {

    private static final String TENANT_ID_MDC_KEY = "tenantId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Get current tenant ID from TenantContext
            String tenantId = TenantContext.getTenantId();
            if (tenantId != null && !tenantId.isBlank()) {
                MDC.put(TENANT_ID_MDC_KEY, tenantId);
            } else {
                MDC.put(TENANT_ID_MDC_KEY, "NONE");
            }

            filterChain.doFilter(request, response);

        } finally {
            // Always clean up MDC to prevent memory leaks
            MDC.remove(TENANT_ID_MDC_KEY);
        }
    }
}
