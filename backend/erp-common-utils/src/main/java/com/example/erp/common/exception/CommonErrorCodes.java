package com.example.erp.common.exception;

/**
 * Centralized Error Codes for Common Utils Module
 * 
 * Rule 31.3: Error codes must follow UPPERCASE_SNAKE_CASE format
 * Pattern: COMMON_<AREA>_<ERROR_DESCRIPTION>
 * 
 * All codes must have corresponding messages in:
 * - erp-main/src/main/resources/i18n/messages.properties (English)
 * - erp-main/src/main/resources/i18n/messages_ar.properties (Arabic)
 */
public final class CommonErrorCodes {

    private CommonErrorCodes() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== Pagination Errors ====================
    public static final String PAGEABLE_NULL = "PAGEABLE_NULL";
    public static final String PAGEABLE_INVALID_MAX_SIZE = "PAGEABLE_INVALID_MAX_SIZE";
    public static final String INVALID_SORT_FIELD = "INVALID_SORT_FIELD";

    // ==================== Search/Filter Errors ====================
    public static final String SEARCH_VALUE_REQUIRED = "SEARCH_VALUE_REQUIRED";
    public static final String SEARCH_IN_EMPTY_VALUES = "SEARCH_IN_EMPTY_VALUES";
    public static final String SEARCH_BETWEEN_INVALID = "SEARCH_BETWEEN_INVALID";
    public static final String SEARCH_BETWEEN_TWO_VALUES = "SEARCH_BETWEEN_TWO_VALUES";
    public static final String SEARCH_FIELD_NOT_ALLOWED = "SEARCH_FIELD_NOT_ALLOWED";
    public static final String SEARCH_INVALID_OPERATOR = "SEARCH_INVALID_OPERATOR";

    // ==================== Tenant Errors ====================
    public static final String MISSING_TENANT = "MISSING_TENANT";

    // ==================== Access Errors ====================
    public static final String ACCESS_DENIED = "ACCESS_DENIED";

    // ==================== Validation Errors ====================
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
}
