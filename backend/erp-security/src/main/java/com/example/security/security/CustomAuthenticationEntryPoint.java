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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom handler for Authentication errors (401) from Spring Security.
 * Returns standardized ApiResponse format.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, 
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        
        log.debug("Authentication failed for path: {} - {}", request.getRequestURI(), authException.getMessage());

        // Build standardized error response
        ApiError error = new ApiError(
            "UNAUTHORIZED", 
            "Authentication required. Please provide valid credentials.",
            java.time.Instant.now(),
            request.getRequestURI()
        );
        
        ApiResponse<Void> apiResponse = ApiResponse.fail("Authentication failed", error);

        // Write JSON response
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        objectMapper.writeValue(response.getWriter(), apiResponse);
    }
}
