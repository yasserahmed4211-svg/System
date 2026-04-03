package com.example.erp.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Error details included in ApiResponse when success=false
 *
 * @author ERP Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    /**
     * Machine-readable error code (e.g., VALIDATION_ERROR, NOT_FOUND)
     */
    private String code;

    /**
     * Additional error details or context
     */
    private String details;

    /**
     * Field-level validation errors (optional)
     */
    private List<FieldErrorItem> fieldErrors;

    /**
     * Timestamp when the error occurred
     */
    private Instant timestamp;

    /**
     * Request path that caused the error
     */
    private String path;

    public ApiError(String code, String details) {
        this(code, details, null, Instant.now(), null);
    }

    public ApiError(String code) {
        this(code, null, null, Instant.now(), null);
    }

    public ApiError(String code, String details, List<FieldErrorItem> fieldErrors) {
        this(code, details, fieldErrors, Instant.now(), null);
    }

    public ApiError(String code, String details, Instant timestamp, String path) {
        this(code, details, null, timestamp, path);
    }
}

