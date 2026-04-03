package com.erp.common.search;

import com.example.erp.common.exception.BusinessException;

/**
 * Exception thrown when search/filter operations fail validation or processing.
 * <p>
 * Extends {@link BusinessException} per Rule 31.1 - only approved exception types allowed.
 * This exception is used throughout the dynamic search framework to signal
 * configuration errors, validation failures, or unsupported operations.
 * </p>
 *
 * <p><b>Common scenarios:</b></p>
 * <ul>
 *   <li>Invalid field name or field not in allowed list</li>
 *   <li>Unsupported operator for field type</li>
 *   <li>Invalid value format (e.g., BETWEEN without exactly 2 values)</li>
 *   <li>Invalid sort field</li>
 * </ul>
 *
 * @author ERP System
 * @since 1.0
 */
public class SearchException extends BusinessException {

    /**
     * Constructs a new SearchException with the specified error code and message.
     *
     * @param code    the error code (Rule 31.3 format)
     * @param message the detail message
     */
    public SearchException(String code, String message) {
        super(code, message);
    }

    /**
     * Constructs a new SearchException with the specified detail message.
     * Uses SEARCH_ERROR as default error code for backward compatibility.
     *
     * @param message the detail message
     * @deprecated Use {@link #SearchException(String, String)} with explicit error code
     */
    @Deprecated
    public SearchException(String message) {
        super("SEARCH_ERROR", message);
    }

    /**
     * Constructs a new SearchException with the specified detail message and cause.
     * Uses SEARCH_ERROR as default error code for backward compatibility.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     * @deprecated Use constructor with explicit error code
     */
    @Deprecated
    public SearchException(String message, Throwable cause) {
        super("SEARCH_ERROR", message, cause);
    }
}
