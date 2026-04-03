package com.example.erp.common.exception;

import lombok.Getter;

/**
 * Exception thrown when a requested resource is not found.
 * Results in HTTP 404 with error code NOT_FOUND.
 *
 * @deprecated Use {@link LocalizedException} with {@link com.example.erp.common.domain.status.Status#NOT_FOUND}
 *             and the appropriate module ErrorCodes constant instead.
 *             Example: {@code throw new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.ROLE_NOT_FOUND, id)}
 * @author ERP Team
 */
@Deprecated
@Getter
public class NotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public NotFoundException(String message) {
        super(message);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }

    public NotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s='%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }
}

