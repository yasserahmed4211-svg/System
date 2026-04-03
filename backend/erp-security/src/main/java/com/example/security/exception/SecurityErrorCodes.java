package com.example.security.exception;

/**
 * Centralized Error Codes for Security Module
 * 
 * Rule 31.3: Error codes must follow UPPERCASE_SNAKE_CASE format
 * Pattern: SEC_<ENTITY>_<ERROR_DESCRIPTION>
 * 
 * All codes must have corresponding messages in:
 * - erp-main/src/main/resources/i18n/messages.properties (English)
 * - erp-main/src/main/resources/i18n/messages_ar.properties (Arabic)
 */
public final class SecurityErrorCodes {

    private SecurityErrorCodes() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== User Errors ====================
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String USER_ENTITY_NOT_FOUND = "USER_ENTITY_NOT_FOUND";
    public static final String USERNAME_ALREADY_EXISTS = "USERNAME_ALREADY_EXISTS";
    public static final String USER_HAS_ACTIVE_REFRESH_TOKENS = "USER_HAS_ACTIVE_REFRESH_TOKENS";
    public static final String USER_HAS_DEPENDENCIES = "USER_HAS_DEPENDENCIES";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";

    // ==================== Role Errors ====================
    public static final String ROLE_NOT_FOUND = "ROLE_NOT_FOUND";
    public static final String ROLE_ALREADY_EXISTS = "ROLE_ALREADY_EXISTS";
    public static final String DUPLICATE_ROLE_CODE = "DUPLICATE_ROLE_CODE";
    public static final String DUPLICATE_ROLE_NAME = "DUPLICATE_ROLE_NAME";
    public static final String ROLE_IN_USE = "ROLE_IN_USE";

    // ==================== Permission Errors ====================
    public static final String PERMISSION_NOT_FOUND = "PERMISSION_NOT_FOUND";
    public static final String PERMISSION_ALREADY_EXISTS = "PERMISSION_ALREADY_EXISTS";
    public static final String PERMISSION_NOT_ASSIGNED_TO_ROLE = "PERMISSION_NOT_ASSIGNED_TO_ROLE";
    public static final String PERMISSIONS_NOT_FOUND = "PERMISSIONS_NOT_FOUND";
    public static final String INVALID_PERMISSION_TYPE = "INVALID_PERMISSION_TYPE";

    // ==================== Page Errors ====================
    public static final String PAGE_NOT_FOUND = "PAGE_NOT_FOUND";
    public static final String PAGE_NOT_FOUND_BY_CODE = "PAGE_NOT_FOUND_BY_CODE";
    public static final String DUPLICATE_PAGE_CODE = "DUPLICATE_PAGE_CODE";
    public static final String DUPLICATE_ROUTE = "DUPLICATE_ROUTE";
    public static final String INVALID_PAGE_CODE_FORMAT = "INVALID_PAGE_CODE_FORMAT";
    public static final String INVALID_PAGE_CODE_LENGTH = "INVALID_PAGE_CODE_LENGTH";
    public static final String INVALID_ROUTE_FORMAT = "INVALID_ROUTE_FORMAT";
    public static final String PARENT_PAGE_NOT_FOUND = "PARENT_PAGE_NOT_FOUND";
    public static final String INVALID_PARENT_PAGE = "INVALID_PARENT_PAGE";
    public static final String CANNOT_REMOVE_VIEW_PERMISSION = "CANNOT_REMOVE_VIEW_PERMISSION";
    public static final String PAGE_ALREADY_ASSIGNED_TO_ROLE = "PAGE_ALREADY_ASSIGNED_TO_ROLE";
    public static final String PAGE_NOT_ASSIGNED_TO_ROLE = "PAGE_NOT_ASSIGNED_TO_ROLE";

    // ==================== Token / Authentication Errors ====================
    public static final String NO_REFRESH_COOKIE = "NO_REFRESH_COOKIE";
    public static final String REFRESH_REVOKED = "REFRESH_REVOKED";
    public static final String REFRESH_EXPIRED_OR_REVOKED = "REFRESH_EXPIRED_OR_REVOKED";

    // ==================== Tenant Errors ====================
    public static final String MISSING_TENANT = "MISSING_TENANT";

    // ==================== General Operation Errors ====================
    public static final String INVALID_OPERATION = "INVALID_OPERATION";
    public static final String NO_PERMISSIONS_TO_COPY = "NO_PERMISSIONS_TO_COPY";
    public static final String DB_CONSTRAINT_VIOLATION = "DB_CONSTRAINT_VIOLATION";
}
