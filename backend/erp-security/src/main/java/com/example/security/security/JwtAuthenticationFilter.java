package com.example.security.security;

import com.example.erp.common.multitenancy.TenantContext;
import com.example.erp.common.util.SecurityContextHelper;
import com.example.security.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        boolean tenantSet = false;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            
            // Skip if token is empty or clearly invalid
            if (token.isEmpty() || token.split("\\.").length != 3) {
                log.debug("Invalid or empty JWT token in Authorization header");
            } else {
                try {
                    String tenant = jwtService.extractTenant(token);
                    if (tenant != null && !tenant.isBlank()) {
                        TenantContext.setTenantId(tenant);
                        tenantSet = true;
                    }
                    
                    // Extract userId from token (more efficient than username)
                    Long userId = jwtService.extractUserId(token);
                    if (userId != null && !SecurityContextHelper.isAuthenticated()) {
                        UserDetails user = userDetailsService.loadUserById(userId);
                        var authToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                } catch (Exception ex) {
                    // Log only if token looks valid but parsing failed
                    log.debug("JWT authentication failed: {}", ex.getMessage());
                }
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            if (tenantSet) {
                try { TenantContext.clear(); } catch (Exception e) { log.debug("Error clearing TenantContext", e); }
            }
        }
    }
}
