package com.example.erp.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Standard API response envelope for all REST endpoints.
 * Ensures consistent response structure across the entire ERP system.
 *
 * Every response includes:
 * <pre>
 * {
 *   "success": true|false,
 *   "message": "...",
 *   "data": { ... },
 *   "error": { ... },
 *   "timestamp": "2025-06-27T12:00:00Z"
 * }
 * </pre>
 *
 * @param <T> The type of data payload
 * @author ERP Team
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * Indicates if the request was successful
     */
    private boolean success;

    /**
     * Human-readable message (can be localized)
     */
    private String message;

    /**
     * Response payload (null for errors)
     */
    private T data;

    /**
     * Error details (null for success)
     */
    private ApiError error;

    /**
     * UTC timestamp of when the response was generated.
     * Initialized at field level to ensure it's always set regardless of constructor used.
     */
    private Instant timestamp = Instant.now();

    // ============== Constructors ==============

    /**
     * Backward-compatible 4-arg constructor.
     * Automatically sets timestamp to current time.
     */
    public ApiResponse(boolean success, String message, T data, ApiError error) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
        this.timestamp = Instant.now();
    }

    /**
     * Full 5-arg constructor (for testing or explicit timestamp control).
     */
    public ApiResponse(boolean success, String message, T data, ApiError error, Instant timestamp) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
        this.timestamp = timestamp;
    }

    // ============== Static Factory Methods ==============

    /**
     * Create a success response with data and message
     */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data, null);
    }

    /**
     * Create a success response with data and default "OK" message
     */
    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, "OK");
    }

    /**
     * Create a success response without data
     */
    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    /**
     * Create a success response with default message and no data
     */
    public static <T> ApiResponse<T> ok() {
        return ok("OK");
    }

    /**
     * Create an error response with message and error details
     */
    public static <T> ApiResponse<T> fail(String message, ApiError error) {
        return new ApiResponse<>(false, message, null, error);
    }

    /**
     * Create an error response with just an error code and message
     */
    public static <T> ApiResponse<T> fail(String message, String errorCode) {
        return fail(message, new ApiError(errorCode, null, null));
    }

    /**
     * Create an error response with error code, message, and details
     */
    public static <T> ApiResponse<T> fail(String message, String errorCode, String details) {
        return fail(message, new ApiError(errorCode, details, null));
    }
}

