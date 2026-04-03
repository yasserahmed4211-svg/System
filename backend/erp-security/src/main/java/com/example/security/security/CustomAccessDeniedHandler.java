package com.example.security.security;

import com.example.erp.common.web.ApiError;
import com.example.erp.common.web.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom handler for Access Denied (403) exceptions from Spring Security.
 * Returns standardized ApiResponse format.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, 
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        log.debug("Access denied for path: {} - {}", request.getRequestURI(), accessDeniedException.getMessage());

        // Build standardized error response
        ApiError error = new ApiError(
            "FORBIDDEN", 
            "You don't have permission to access this resource",
            java.time.Instant.now(),
            request.getRequestURI()
        );
        
        ApiResponse<Void> apiResponse = ApiResponse.fail("Access denied", error);

        // Write JSON response
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        objectMapper.writeValue(response.getWriter(), apiResponse);
    }
}
