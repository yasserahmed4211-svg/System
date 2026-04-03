package com.example.erp.common.domain.status;

/**
 * Contract for all business status codes in the ERP system.
 * 
 * Architecture Rules:
 * - This interface is domain-level (NO HTTP dependencies)
 * - All status enums must implement this interface
 * - Service layer uses StatusCode, not HTTP status codes
 * - HTTP mapping is handled by OperationCode in the web layer
 *
 * @author ERP Team
 */
public interface StatusCode {

    /**
     * Returns the unique code identifier (e.g., "SUCCESS", "NOT_FOUND", "VALIDATION_ERROR")
     * This is used as the error code in API responses.
     */
    String getCode();

    /**
     * Returns the i18n message key for localization.
     * The key should exist in message bundles (messages.properties, messages_ar.properties).
     */
    String getMessageKey();

    /**
     * Returns the category this status belongs to.
     * Used for consistent handling and HTTP status mapping.
     */
    StatusCategory getCategory();

    /**
     * Returns true if this status represents a successful operation.
     */
    default boolean isSuccess() {
        return getCategory() == StatusCategory.SUCCESS;
    }

    /**
     * Returns true if this status represents an error.
     */
    default boolean isError() {
        return !isSuccess();
    }
}
