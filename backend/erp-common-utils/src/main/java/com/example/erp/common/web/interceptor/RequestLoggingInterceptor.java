package com.example.erp.common.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that logs all HTTP requests/responses with performance metrics.
 * 
 * Features:
 * - Logs request method, URI, and parameters
 * - Measures request execution time
 * - Warns if request takes longer than threshold (1000ms)
 * - Logs exceptions during request processing
 * 
 * Architecture Rule: 15.2 - Request/Response Logging with Performance Metrics
 * 
 * @author ERP Team
 */
@Component
@Slf4j
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "startTime";
    private static final long SLOW_REQUEST_THRESHOLD_MS = 1000L;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {

        // Store start time for duration calculation
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());

        log.info("Request started: {} {}", 
                request.getMethod(), 
                request.getRequestURI());

        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {

        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;

            // Log based on performance
            if (duration > SLOW_REQUEST_THRESHOLD_MS) {
                log.warn("SLOW REQUEST: {} {} completed in {}ms with status {}",
                        request.getMethod(),
                        request.getRequestURI(),
                        duration,
                        response.getStatus());
            } else {
                log.info("Request completed: {} {} in {}ms with status {}",
                        request.getMethod(),
                        request.getRequestURI(),
                        duration,
                        response.getStatus());
            }
        }

        // Log exception if present
        if (ex != null) {
            log.error("Request failed: {} {} - Error: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    ex.getMessage(),
                    ex);
        }
    }
}
