package com.example.erp.common.domain.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Single source of truth for all business status codes in the ERP system.
 * 
 * Architecture Rules:
 * - This enum contains NO HTTP-related logic
 * - Service layer must use these statuses, not HTTP codes
 * - HTTP mapping is handled by OperationCodeImpl in the web layer
 * - All message keys must have corresponding entries in i18n bundles
 *
 * Naming Convention: UPPER_SNAKE_CASE
 * Message Key Pattern: status.<category>.<code>
 *
 * @author ERP Team
 */
@Getter
@RequiredArgsConstructor
public enum Status implements StatusCode {

    // ==================== Success Statuses ====================
    
    SUCCESS("SUCCESS", "status.success", StatusCategory.SUCCESS),
    CREATED("CREATED", "status.created", StatusCategory.SUCCESS),
    UPDATED("UPDATED", "status.updated", StatusCategory.SUCCESS),
    DELETED("DELETED", "status.deleted", StatusCategory.SUCCESS),
    OK("OK", "status.ok", StatusCategory.SUCCESS),

    // ==================== Client Error Statuses ====================
    
    VALIDATION_ERROR("VALIDATION_ERROR", "status.validation.error", StatusCategory.CLIENT_ERROR),
    INVALID_INPUT("INVALID_INPUT", "status.invalid.input", StatusCategory.CLIENT_ERROR),
    MISSING_REQUIRED_FIELD("MISSING_REQUIRED_FIELD", "status.missing.required.field", StatusCategory.CLIENT_ERROR),
    INVALID_FORMAT("INVALID_FORMAT", "status.invalid.format", StatusCategory.CLIENT_ERROR),
    INVALID_JSON("INVALID_JSON", "status.invalid.json", StatusCategory.CLIENT_ERROR),
    BAD_REQUEST("BAD_REQUEST", "status.bad.request", StatusCategory.CLIENT_ERROR),
    TYPE_MISMATCH("TYPE_MISMATCH", "status.type.mismatch", StatusCategory.CLIENT_ERROR),
    MISSING_PARAMETER("MISSING_PARAMETER", "status.missing.parameter", StatusCategory.CLIENT_ERROR),
    BINDING_ERROR("BINDING_ERROR", "status.binding.error", StatusCategory.CLIENT_ERROR),

    // ==================== Not Found Statuses ====================
    
    NOT_FOUND("NOT_FOUND", "status.not.found", StatusCategory.NOT_FOUND),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "status.resource.not.found", StatusCategory.NOT_FOUND),
    ENDPOINT_NOT_FOUND("ENDPOINT_NOT_FOUND", "status.endpoint.not.found", StatusCategory.NOT_FOUND),
    USER_NOT_FOUND("USER_NOT_FOUND", "status.user.not.found", StatusCategory.NOT_FOUND),

    // ==================== Business Error Statuses ====================
    
    BUSINESS_RULE_VIOLATION("BUSINESS_RULE_VIOLATION", "status.business.rule.violation", StatusCategory.BUSINESS_ERROR),
    OPERATION_NOT_ALLOWED("OPERATION_NOT_ALLOWED", "status.operation.not.allowed", StatusCategory.BUSINESS_ERROR),
    INVALID_STATE("INVALID_STATE", "status.invalid.state", StatusCategory.BUSINESS_ERROR),
    PRECONDITION_FAILED("PRECONDITION_FAILED", "status.precondition.failed", StatusCategory.BUSINESS_ERROR),

    // ==================== Conflict Statuses ====================
    
    CONFLICT("CONFLICT", "status.conflict", StatusCategory.CONFLICT),
    DUPLICATE("DUPLICATE", "status.duplicate", StatusCategory.CONFLICT),
    ALREADY_EXISTS("ALREADY_EXISTS", "status.already.exists", StatusCategory.CONFLICT),
    DB_CONSTRAINT_VIOLATION("DB_CONSTRAINT_VIOLATION", "status.db.constraint.violation", StatusCategory.CONFLICT),
    CONCURRENT_MODIFICATION("CONCURRENT_MODIFICATION", "status.concurrent.modification", StatusCategory.CONFLICT),

    // ==================== Auth Error Statuses ====================
    
    UNAUTHORIZED("UNAUTHORIZED", "status.unauthorized", StatusCategory.AUTH_ERROR),
    FORBIDDEN("FORBIDDEN", "status.forbidden", StatusCategory.AUTH_ERROR),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "status.invalid.credentials", StatusCategory.AUTH_ERROR),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "status.token.expired", StatusCategory.AUTH_ERROR),
    ACCESS_DENIED("ACCESS_DENIED", "status.access.denied", StatusCategory.AUTH_ERROR),

    // ==================== Server Error Statuses ====================
    
    INTERNAL_ERROR("INTERNAL_ERROR", "status.internal.error", StatusCategory.SERVER_ERROR),
    DB_ERROR("DB_ERROR", "status.db.error", StatusCategory.SERVER_ERROR),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "status.service.unavailable", StatusCategory.SERVER_ERROR),
    TIMEOUT("TIMEOUT", "status.timeout", StatusCategory.SERVER_ERROR),

    // ==================== Method/Media Type Errors ====================
    
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "status.method.not.allowed", StatusCategory.CLIENT_ERROR),
    UNSUPPORTED_MEDIA_TYPE("UNSUPPORTED_MEDIA_TYPE", "status.unsupported.media.type", StatusCategory.CLIENT_ERROR);

    private final String code;
    private final String messageKey;
    private final StatusCategory category;

    /**
     * Find a Status by its code (case-insensitive).
     * Returns null if not found.
     */
    public static Status fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (Status status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * Find a Status by its code, or return a default if not found.
     */
    public static Status fromCodeOrDefault(String code, Status defaultStatus) {
        Status found = fromCode(code);
        return found != null ? found : defaultStatus;
    }
}
