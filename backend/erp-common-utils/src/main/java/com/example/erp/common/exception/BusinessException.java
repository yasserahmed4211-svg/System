package com.example.erp.common.exception;

import lombok.Getter;

/**
 * Exception for business logic violations.
 * Results in HTTP 422 (Unprocessable Entity) or 409 (Conflict) with a custom error code.
 *
 * @author ERP Team
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * Machine-readable error code (e.g., DUPLICATE_ACCOUNT_CODE, PERIOD_CLOSED)
     */
    private final String code;

    /**
     * Additional details about the error
     */
    private final String details;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.details = null;
    }

    public BusinessException(String code, String message, String details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.details = null;
    }

    public BusinessException(String code, String message, String details, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.details = details;
    }
}

