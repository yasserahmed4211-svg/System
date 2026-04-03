package com.example.erp.common.exception;

import com.example.erp.common.domain.status.Status;
import com.example.erp.common.domain.status.StatusCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Localized exception with i18n support.
 * Contains a message key for translation instead of a fixed message.
 * 
 * Architecture Rules:
 * - Service layer should use constructors with StatusCode (domain-level)
 * - HttpStatus constructors are deprecated for internal use only
 * - Message resolution happens in GlobalExceptionHandler via LocalizationService
 * 
 * Usage in Service Layer:
 * <pre>
 * // Preferred: Using StatusCode (Clean Architecture compliant)
 * throw new LocalizedException(Status.NOT_FOUND, "user.not.found", userId);
 * throw new LocalizedException(Status.ALREADY_EXISTS, "username.duplicate", username);
 * 
 * // Simple: Using just message key (defaults to BAD_REQUEST)
 * throw new LocalizedException("validation.failed", fieldName);
 * </pre>
 * 
 * @author ERP Team
 */
@Getter
public class LocalizedException extends RuntimeException {
    
    private final HttpStatus status;
    private final StatusCode statusCode;
    private final String messageKey;
    private final Object[] args;
    
    /**
     * Creates a localized exception with a domain StatusCode.
     * This is the PREFERRED constructor for service layer usage.
     * HTTP status is derived from StatusCode category in the web layer.
     * 
     * @param statusCode Domain-level status code (e.g., Status.NOT_FOUND)
     * @param messageKey i18n message key
     * @param args Optional message arguments
     */
    public LocalizedException(StatusCode statusCode, String messageKey, Object... args) {
        super(messageKey);
        this.statusCode = statusCode;
        this.messageKey = messageKey;
        this.args = args;
        // Map to HttpStatus for backward compatibility
        this.status = mapStatusCodeToHttpStatus(statusCode);
    }
    
    /**
     * Creates a localized exception with just a message key.
     * Defaults to BAD_REQUEST status.
     * 
     * @param messageKey i18n message key
     * @param args Optional message arguments
     */
    public LocalizedException(String messageKey, Object... args) {
        this(Status.BAD_REQUEST, messageKey, args);
    }
    
    /**
     * @deprecated Use {@link #LocalizedException(StatusCode, String, Object...)} instead.
     * Service layer should not use HttpStatus directly.
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public LocalizedException(HttpStatus status, String messageKey, Object... args) {
        super(messageKey);
        this.status = status;
        this.statusCode = mapHttpStatusToStatusCode(status);
        this.messageKey = messageKey;
        this.args = args;
    }
    
    /**
     * Maps StatusCode to HttpStatus for backward compatibility.
     * This logic should ideally be in OperationCode, but we inline it here
     * to avoid circular dependencies.
     */
    private static HttpStatus mapStatusCodeToHttpStatus(StatusCode statusCode) {
        if (statusCode == null) {
            return HttpStatus.BAD_REQUEST;
        }
        return switch (statusCode.getCategory()) {
            case SUCCESS -> HttpStatus.OK;
            case CLIENT_ERROR -> HttpStatus.BAD_REQUEST;
            case BUSINESS_ERROR -> HttpStatus.UNPROCESSABLE_ENTITY;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case AUTH_ERROR -> {
                if (statusCode == Status.FORBIDDEN || statusCode == Status.ACCESS_DENIED) {
                    yield HttpStatus.FORBIDDEN;
                }
                yield HttpStatus.UNAUTHORIZED;
            }
            case SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            case CONFLICT -> HttpStatus.CONFLICT;
        };
    }
    
    /**
     * Maps HttpStatus to StatusCode for backward compatibility with deprecated constructor.
     */
    private static StatusCode mapHttpStatusToStatusCode(HttpStatus httpStatus) {
        if (httpStatus == null) {
            return Status.BAD_REQUEST;
        }
        return switch (httpStatus) {
            case OK, CREATED -> Status.SUCCESS;
            case BAD_REQUEST -> Status.BAD_REQUEST;
            case NOT_FOUND -> Status.NOT_FOUND;
            case UNAUTHORIZED -> Status.UNAUTHORIZED;
            case FORBIDDEN -> Status.FORBIDDEN;
            case CONFLICT -> Status.DUPLICATE;
            case UNPROCESSABLE_ENTITY -> Status.BUSINESS_RULE_VIOLATION;
            default -> {
                if (httpStatus.is4xxClientError()) {
                    yield Status.BAD_REQUEST;
                } else if (httpStatus.is5xxServerError()) {
                    yield Status.INTERNAL_ERROR;
                }
                yield Status.BAD_REQUEST;
            }
        };
    }
}
