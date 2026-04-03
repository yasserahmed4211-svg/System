package com.example.erp.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utility class for creating standardized ResponseEntity objects
 * with ApiResponse envelope.
 *
 * @author ERP Team
 */
public final class ResponseUtil {

    private ResponseUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Create 200 OK response with data
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * Create 200 OK response with data and custom message
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok(ApiResponse.ok(data, message));
    }

    /**
     * Create 200 OK response with just a message (no data)
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(String message) {
        return ResponseEntity.ok(ApiResponse.ok(message));
    }

    /**
     * Create 201 Created response with data
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, "Created"));
    }

    /**
     * Create 201 Created response with data and custom message
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, message));
    }

    /**
     * Create 204 No Content response
     * Note: No body is sent with 204 responses
     */
    public static ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Create 200 OK response for deletion with message
     */
    public static <T> ResponseEntity<ApiResponse<T>> deleted() {
        return ResponseEntity.ok(ApiResponse.ok("Deleted successfully"));
    }

    /**
     * Create 200 OK response for deletion with custom message
     */
    public static <T> ResponseEntity<ApiResponse<T>> deleted(String message) {
        return ResponseEntity.ok(ApiResponse.ok(message));
    }

    /**
     * Create 400 Bad Request response
     */
    public static <T> ResponseEntity<ApiResponse<T>> badRequest(String message, ApiError error) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(message, error));
    }

    /**
     * Create 400 Bad Request response with error code
     */
    public static <T> ResponseEntity<ApiResponse<T>> badRequest(String message, String errorCode) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(message, new ApiError(errorCode)));
    }

    /**
     * Create 404 Not Found response
     */
    public static <T> ResponseEntity<ApiResponse<T>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(message, new ApiError("NOT_FOUND")));
    }

    /**
     * Create 409 Conflict response
     */
    public static <T> ResponseEntity<ApiResponse<T>> conflict(String message, String errorCode) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(message, new ApiError(errorCode)));
    }

    /**
     * Create 422 Unprocessable Entity response (business logic error)
     */
    public static <T> ResponseEntity<ApiResponse<T>> unprocessableEntity(String message, String errorCode) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.fail(message, new ApiError(errorCode)));
    }

    /**
     * Create custom status response
     */
    public static <T> ResponseEntity<ApiResponse<T>> status(HttpStatus status, ApiResponse<T> response) {
        return ResponseEntity.status(status).body(response);
    }
}

