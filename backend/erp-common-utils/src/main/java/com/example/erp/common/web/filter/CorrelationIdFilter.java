package com.example.erp.common.web.filter;

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
import java.util.UUID;

/**
 * Filter that ensures every HTTP request has a unique correlation ID for tracing.
 * The correlation ID is:
 * - Generated if not provided in X-Correlation-Id header
 * - Added to MDC for logging
 * - Included in response header
 * 
 * Usage in logs: [correlationId] will automatically appear when configured in logback.
 * 
 * Architecture Rule: 15.1 - Request Tracing with Correlation ID
 * 
 * @author ERP Team
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Get or generate correlation ID
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = generateCorrelationId();
            }

            // Add to MDC for logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

            // Add to response header
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            filterChain.doFilter(request, response);

        } finally {
            // Always clean up MDC to prevent memory leaks
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    /**
     * Generate a unique correlation ID using UUID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
