package com.example.erp.finance.gl.exception;

/**
 * GL Module Error Codes — centralized constants.
 * Each code MUST have a corresponding entry in messages.properties and messages_ar.properties.
 */
public final class GlErrorCodes {

    private GlErrorCodes() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ==================== Chart of Accounts ====================

    public static final String GL_ACCOUNT_NOT_FOUND = "GL_ACCOUNT_NOT_FOUND";
    public static final String GL_DUPLICATE_ACCOUNT_CODE = "GL_DUPLICATE_ACCOUNT_CODE";
    public static final String GL_ACCOUNT_IN_USE = "GL_ACCOUNT_IN_USE";
    public static final String GL_ACCOUNT_HAS_CHILDREN = "GL_ACCOUNT_HAS_CHILDREN";
    public static final String GL_ACCOUNT_IN_ACTIVE_RULE = "GL_ACCOUNT_IN_ACTIVE_RULE";
    public static final String GL_ACCOUNT_HAS_BALANCE = "GL_ACCOUNT_HAS_BALANCE";
    public static final String GL_ACCOUNT_ORG_LOCKED = "GL_ACCOUNT_ORG_LOCKED";
    public static final String GL_ACCOUNT_NOT_LEAF = "GL_ACCOUNT_NOT_LEAF";
    public static final String GL_INACTIVE_ACCOUNT = "GL_INACTIVE_ACCOUNT";
    public static final String GL_ACCOUNT_CIRCULAR_REF = "GL_ACCOUNT_CIRCULAR_REF";
    public static final String GL_ACCOUNT_TYPE_MISMATCH = "GL_ACCOUNT_TYPE_MISMATCH";
    public static final String GL_ACCOUNT_PARENT_NOT_FOUND = "GL_ACCOUNT_PARENT_NOT_FOUND";
    public static final String GL_ACCOUNT_PARENT_INACTIVE = "GL_ACCOUNT_PARENT_INACTIVE";
    public static final String GL_ACCOUNT_SELF_REFERENCE = "GL_ACCOUNT_SELF_REFERENCE";
    public static final String GL_ACCOUNT_DESCENDANT_AS_PARENT = "GL_ACCOUNT_DESCENDANT_AS_PARENT";
    public static final String GL_ACCOUNT_TYPE_CHANGE_WITH_CHILDREN = "GL_ACCOUNT_TYPE_CHANGE_WITH_CHILDREN";
    public static final String GL_ACCOUNT_NO_GENERATION_FAILED = "GL_ACCOUNT_NO_GENERATION_FAILED";
    public static final String GL_INVALID_ROOT_ACCOUNT_TYPE = "GL_INVALID_ROOT_ACCOUNT_TYPE";
    public static final String GL_ACCOUNT_NO_MANUAL_OVERRIDE = "GL_ACCOUNT_NO_MANUAL_OVERRIDE";

    // ==================== Accounting Rules ====================

    public static final String GL_RULE_NOT_FOUND = "GL_RULE_NOT_FOUND";
    public static final String GL_DUPLICATE_ACTIVE_RULE = "GL_DUPLICATE_ACTIVE_RULE";
    public static final String GL_RULE_IN_USE = "GL_RULE_IN_USE";
    public static final String GL_RULE_HAS_PENDING_POSTINGS = "GL_RULE_HAS_PENDING_POSTINGS";
    public static final String GL_UNBALANCED_ENTRY = "GL_UNBALANCED_ENTRY";
    public static final String GL_MISSING_RULE = "GL_MISSING_RULE";
    public static final String GL_RULE_NO_LINES = "GL_RULE_NO_LINES";
    public static final String GL_RULE_MISSING_SIDES = "GL_RULE_MISSING_SIDES";
    public static final String GL_RULE_DUPLICATE_PRIORITY = "GL_RULE_DUPLICATE_PRIORITY";
    public static final String GL_RULE_INVALID_AMOUNT_SOURCE = "GL_RULE_INVALID_AMOUNT_SOURCE";
    public static final String GL_RULE_INVALID_ENTRY_SIDE = "GL_RULE_INVALID_ENTRY_SIDE";
    public static final String GL_RULE_INVALID_AMOUNT_SOURCE_TYPE = "GL_RULE_INVALID_AMOUNT_SOURCE_TYPE";
    public static final String GL_RULE_INVALID_PRIORITY = "GL_RULE_INVALID_PRIORITY";
    public static final String GL_RULE_INVALID_SOURCE_MODULE = "GL_RULE_INVALID_SOURCE_MODULE";
    public static final String GL_RULE_INVALID_SOURCE_DOC_TYPE = "GL_RULE_INVALID_SOURCE_DOC_TYPE";
    public static final String GL_RULE_AMOUNT_TOTAL_NO_VALUE = "GL_RULE_AMOUNT_TOTAL_NO_VALUE";
    public static final String GL_RULE_AMOUNT_FIXED_POSITIVE = "GL_RULE_AMOUNT_FIXED_POSITIVE";
    public static final String GL_RULE_AMOUNT_PERCENT_RANGE = "GL_RULE_AMOUNT_PERCENT_RANGE";
    public static final String GL_RULE_AMOUNT_REMAINING_NO_VALUE = "GL_RULE_AMOUNT_REMAINING_NO_VALUE";

    // ==================== GL Journals ====================

    public static final String GL_JOURNAL_NOT_FOUND = "GL_JOURNAL_NOT_FOUND";
    public static final String GL_JOURNAL_NOT_BALANCED = "GL_JOURNAL_NOT_BALANCED";
    public static final String GL_JOURNAL_NO_LINES = "GL_JOURNAL_NO_LINES";
    public static final String GL_JOURNAL_LINE_XOR = "GL_JOURNAL_LINE_XOR";
    public static final String GL_JOURNAL_NOT_DRAFT = "GL_JOURNAL_NOT_DRAFT";
    public static final String GL_JOURNAL_NOT_APPROVED = "GL_JOURNAL_NOT_APPROVED";
    public static final String GL_JOURNAL_POSTED_IMMUTABLE = "GL_JOURNAL_POSTED_IMMUTABLE";
    public static final String GL_JOURNAL_REVERSE_REQUIRES_POSTED = "GL_JOURNAL_REVERSE_REQUIRES_POSTED";
    public static final String GL_JOURNAL_ALREADY_FINALIZED = "GL_JOURNAL_ALREADY_FINALIZED";
    public static final String GL_JOURNAL_AUTOMATIC_NO_UPDATE = "GL_JOURNAL_AUTOMATIC_NO_UPDATE";
    public static final String GL_JOURNAL_AUTOMATIC_REQUIRES_SOURCE = "GL_JOURNAL_AUTOMATIC_REQUIRES_SOURCE";
    public static final String GL_JOURNAL_TYPE_IMMUTABLE = "GL_JOURNAL_TYPE_IMMUTABLE";

    // ==================== Posting Engine ====================

    public static final String GL_POSTING_RULE_NOT_FOUND = "GL_POSTING_RULE_NOT_FOUND";
    public static final String GL_POSTING_NO_RULE_LINES = "GL_POSTING_NO_RULE_LINES";
    public static final String GL_POSTING_UNBALANCED = "GL_POSTING_UNBALANCED";
    public static final String GL_POSTING_ZERO_AMOUNT = "GL_POSTING_ZERO_AMOUNT";
    public static final String GL_POSTING_NEGATIVE_AMOUNT = "GL_POSTING_NEGATIVE_AMOUNT";
    public static final String GL_POSTING_REMAINING_NEGATIVE = "GL_POSTING_REMAINING_NEGATIVE";
    public static final String GL_POSTING_UNKNOWN_AMOUNT_TYPE = "GL_POSTING_UNKNOWN_AMOUNT_TYPE";
    public static final String GL_POSTING_ACCOUNT_INVALID = "GL_POSTING_ACCOUNT_INVALID";
    public static final String GL_POSTING_UNKNOWN_ENTITY_TYPE = "GL_POSTING_UNKNOWN_ENTITY_TYPE";
    public static final String GL_POSTING_ENTITY_NOT_PROVIDED = "GL_POSTING_ENTITY_NOT_PROVIDED";

    // ==================== Posting → Journal Bridge ====================

    public static final String GL_POSTING_NOT_FOUND = "GL_POSTING_NOT_FOUND";
    public static final String GL_POSTING_NOT_READY = "GL_POSTING_NOT_READY";
    public static final String GL_POSTING_JOURNAL_ALREADY_EXISTS = "GL_POSTING_JOURNAL_ALREADY_EXISTS";
    public static final String GL_POSTING_NO_DETAILS_FOR_JOURNAL = "GL_POSTING_NO_DETAILS_FOR_JOURNAL";

    // ==================== General ====================

    public static final String GL_VALIDATION_ERROR = "GL_VALIDATION_ERROR";
    public static final String GL_ACCESS_DENIED = "GL_ACCESS_DENIED";
}
