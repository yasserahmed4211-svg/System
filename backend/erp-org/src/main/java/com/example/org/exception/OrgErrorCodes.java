package com.example.org.exception;

/**
 * Centralized Error Codes for Organization Module
 *
 * All codes must have corresponding messages in:
 * - erp-main/src/main/resources/i18n/messages.properties (English)
 * - erp-main/src/main/resources/i18n/messages_ar.properties (Arabic)
 */
public final class OrgErrorCodes {

    private OrgErrorCodes() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== Legal Entity ====================
    public static final String LEGAL_ENTITY_NOT_FOUND = "LEGAL_ENTITY_NOT_FOUND";
    public static final String LEGAL_ENTITY_CODE_DUPLICATE = "LEGAL_ENTITY_CODE_DUPLICATE";
    public static final String LEGAL_ENTITY_INVALID_EMAIL = "LEGAL_ENTITY_INVALID_EMAIL";
    public static final String LEGAL_ENTITY_INVALID_URL = "LEGAL_ENTITY_INVALID_URL";
    public static final String LEGAL_ENTITY_INVALID_MONTH = "LEGAL_ENTITY_INVALID_MONTH";
    public static final String LEGAL_ENTITY_CURRENCY_LOCKED = "LEGAL_ENTITY_CURRENCY_LOCKED";
    public static final String LEGAL_ENTITY_HAS_ACTIVE_BRANCHES = "LEGAL_ENTITY_HAS_ACTIVE_BRANCHES";
    public static final String LEGAL_ENTITY_LAST_ACTIVE = "LEGAL_ENTITY_LAST_ACTIVE";
    public static final String LEGAL_ENTITY_INACTIVE = "LEGAL_ENTITY_INACTIVE";
    public static final String LEGAL_ENTITY_ALREADY_INACTIVE = "LEGAL_ENTITY_ALREADY_INACTIVE";
    public static final String LEGAL_ENTITY_REACTIVATION_NOT_ALLOWED = "LEGAL_ENTITY_REACTIVATION_NOT_ALLOWED";

    // ==================== Region ====================
    public static final String REGION_NOT_FOUND = "REGION_NOT_FOUND";
    public static final String REGION_CODE_DUPLICATE = "REGION_CODE_DUPLICATE";
    public static final String REGION_INACTIVE_LEGAL_ENTITY = "REGION_INACTIVE_LEGAL_ENTITY";
    public static final String REGION_LEGAL_ENTITY_LOCKED = "REGION_LEGAL_ENTITY_LOCKED";
    public static final String REGION_HAS_ACTIVE_BRANCHES = "REGION_HAS_ACTIVE_BRANCHES";
    public static final String REGION_ALREADY_INACTIVE = "REGION_ALREADY_INACTIVE";
    public static final String REGION_REACTIVATION_NOT_ALLOWED = "REGION_REACTIVATION_NOT_ALLOWED";

    // ==================== Branch ====================
    public static final String BRANCH_NOT_FOUND = "BRANCH_NOT_FOUND";
    public static final String BRANCH_CODE_DUPLICATE = "BRANCH_CODE_DUPLICATE";
    public static final String BRANCH_INACTIVE_LEGAL_ENTITY = "BRANCH_INACTIVE_LEGAL_ENTITY";
    public static final String BRANCH_REGION_MISMATCH = "BRANCH_REGION_MISMATCH";
    public static final String BRANCH_HQ_EXISTS = "BRANCH_HQ_EXISTS";
    public static final String BRANCH_INVALID_EMAIL = "BRANCH_INVALID_EMAIL";
    public static final String BRANCH_LEGAL_ENTITY_LOCKED = "BRANCH_LEGAL_ENTITY_LOCKED";
    public static final String BRANCH_LAST_ACTIVE_HQ = "BRANCH_LAST_ACTIVE_HQ";
    public static final String BRANCH_HAS_ACTIVE_USERS = "BRANCH_HAS_ACTIVE_USERS";
    public static final String BRANCH_HAS_OPEN_TRANSACTIONS = "BRANCH_HAS_OPEN_TRANSACTIONS";
    public static final String BRANCH_ALREADY_INACTIVE = "BRANCH_ALREADY_INACTIVE";
    public static final String BRANCH_REACTIVATION_NOT_ALLOWED = "BRANCH_REACTIVATION_NOT_ALLOWED";
    public static final String BRANCH_INACTIVE_FOR_DEPARTMENT = "BRANCH_INACTIVE_FOR_DEPARTMENT";

    // ==================== Department ====================
    public static final String DEPARTMENT_NOT_FOUND = "DEPARTMENT_NOT_FOUND";
    public static final String DEPARTMENT_CODE_DUPLICATE = "DEPARTMENT_CODE_DUPLICATE";
    public static final String DEPARTMENT_HAS_ACTIVE_USERS = "DEPARTMENT_HAS_ACTIVE_USERS";
    public static final String DEPARTMENT_HAS_LINKED_RECORDS = "DEPARTMENT_HAS_LINKED_RECORDS";
    public static final String DEPARTMENT_ALREADY_INACTIVE = "DEPARTMENT_ALREADY_INACTIVE";
    public static final String DEPARTMENT_REACTIVATION_NOT_ALLOWED = "DEPARTMENT_REACTIVATION_NOT_ALLOWED";
    public static final String DEPARTMENT_HARD_DELETE_NOT_ALLOWED = "DEPARTMENT_HARD_DELETE_NOT_ALLOWED";

    // ==================== Branch Atomic Save ====================
    public static final String PARTIAL_SAVE_FAILED = "PARTIAL_SAVE_FAILED";
}
