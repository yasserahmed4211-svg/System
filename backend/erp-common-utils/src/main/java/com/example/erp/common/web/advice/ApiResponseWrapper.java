package com.example.erp.common.web.advice;

import com.example.erp.common.web.ApiError;
import com.example.erp.common.web.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Automatically wraps all REST API responses in the standard ApiResponse envelope.
 * 
 * This ensures consistent response structure across all endpoints:
 * {
 *   "success": true,
 *   "message": "OK",
 *   "data": { ... actual response ... },
 *   "error": null
 * }
 * 
 * Exceptions:
 * - Already wrapped responses (ApiResponse)
 * - Error responses (handled by GlobalExceptionHandler)
 * - File downloads (binary content)
 * - Swagger/OpenAPI documentation
 * - Actuator endpoints
 * 
 * Architecture Rule: 14.2 - Automatic Response Wrapping
 * 
 * @author ERP Team
 */
@RestControllerAdvice
public class ApiResponseWrapper implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(
            MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        
        // Don't wrap if already wrapped
        if (returnType.getParameterType().equals(ApiResponse.class)) {
            return false;
        }

        // Don't wrap binary responses (e.g., springdoc returns byte[])
        if (returnType.getParameterType().equals(byte[].class)) {
            return false;
        }

        // Don't wrap when Spring selected the byte[] converter
        return !ByteArrayHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        // Preserve empty bodies (e.g., 204 No Content). Do not wrap null into ApiResponse.
        if (body == null) {
            return null;
        }

        // Don't wrap if already wrapped
        if (body instanceof ApiResponse) {
            return body;
        }

        // Don't wrap error responses (handled by GlobalExceptionHandler)
        if (body instanceof ApiError) {
            return body;
        }

        // Don't wrap for non-JSON responses (file downloads, etc.)
        if (selectedContentType != null && 
            !selectedContentType.includes(MediaType.APPLICATION_JSON)) {
            return body;
        }

        // Don't wrap binary responses (prevents ByteArrayHttpMessageConverter casting issues)
        if (body instanceof byte[]) {
            return body;
        }

        // Don't wrap Swagger/OpenAPI documentation
        String path = request.getURI().getPath();
        if (path.contains("/api-docs") ||
            path.contains("/v3/api-docs") || 
            path.contains("/swagger") || 
            path.contains("/actuator")) {
            return body;
        }

        // Wrap in standard ApiResponse
        return ApiResponse.ok(body);
    }
}
