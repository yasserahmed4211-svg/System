package com.example.masterdata.exception;

/**
 * Centralized Error Codes for MasterData Module
 * 
 * All codes must have corresponding messages in:
 * - erp-main/src/main/resources/i18n/messages.properties (English)
 * - erp-main/src/main/resources/i18n/messages_ar.properties (Arabic)
 */
public final class MasterDataErrorCodes {

    private MasterDataErrorCodes() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== Master Lookup Errors ====================
    public static final String MASTER_LOOKUP_NOT_FOUND = "MASTER_LOOKUP_NOT_FOUND";
    public static final String MASTER_LOOKUP_ACCESS_DENIED = "MASTER_LOOKUP_ACCESS_DENIED";
    public static final String MASTER_LOOKUP_IN_USE = "MASTER_LOOKUP_IN_USE";
    public static final String MASTER_LOOKUP_KEY_DUPLICATE = "MASTER_LOOKUP_KEY_DUPLICATE";
    public static final String MASTER_LOOKUP_ACTIVE_DETAILS_EXIST = "MASTER_LOOKUP_ACTIVE_DETAILS_EXIST";
    public static final String MASTER_LOOKUP_DETAILS_EXIST = "MASTER_LOOKUP_DETAILS_EXIST";
    public static final String MASTER_LOOKUP_FK_VIOLATION = "MASTER_LOOKUP_FK_VIOLATION";
    public static final String MASTER_LOOKUP_INACTIVE = "MASTER_LOOKUP_INACTIVE";
    public static final String LOOKUP_VALUE_INVALID = "LOOKUP_VALUE_INVALID";

    // ==================== Lookup Detail Errors ====================
    public static final String LOOKUP_DETAIL_NOT_FOUND = "LOOKUP_DETAIL_NOT_FOUND";
    public static final String LOOKUP_DETAIL_ACCESS_DENIED = "LOOKUP_DETAIL_ACCESS_DENIED";
    public static final String LOOKUP_DETAIL_CODE_DUPLICATE = "LOOKUP_DETAIL_CODE_DUPLICATE";
    public static final String LOOKUP_DETAIL_IN_USE = "LOOKUP_DETAIL_IN_USE";
    public static final String LOOKUP_DETAIL_FK_VIOLATION = "LOOKUP_DETAIL_FK_VIOLATION";

    // ==================== Activity Errors ====================
    public static final String ACTIVITY_NOT_FOUND = "ACTIVITY_NOT_FOUND";
    public static final String ACTIVITY_CODE_ALREADY_EXISTS = "ACTIVITY_CODE_ALREADY_EXISTS";
    public static final String ACTIVITY_REFERENCES_EXIST = "ACTIVITY_REFERENCES_EXIST";
    public static final String ACTIVITY_ACCESS_DENIED = "ACTIVITY_ACCESS_DENIED";
    public static final String ACTIVITY_HAS_ACTIVE_CATEGORIES = "ACTIVITY_HAS_ACTIVE_CATEGORIES";
    public static final String ACTIVITY_CANNOT_DELETE_HAS_CATEGORIES = "ACTIVITY_CANNOT_DELETE_HAS_CATEGORIES";
    public static final String ACTIVITY_VARIABLE_REQUIRES_WEIGHT = "ACTIVITY_VARIABLE_REQUIRES_WEIGHT";
}
