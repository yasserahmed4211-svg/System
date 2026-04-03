package com.example.erp.common.domain.status;

/**
 * Categorization of business status codes.
 * Used to group related statuses for consistent handling.
 * 
 * Architecture Rule: This is a domain-level concept with NO HTTP dependencies.
 * HTTP mapping is handled separately in OperationCodeImpl.
 *
 * @author ERP Team
 */
public enum StatusCategory {

    /**
     * Operation completed successfully
     */
    SUCCESS,

    /**
     * Operation failed due to client error (bad input, validation failure)
     */
    CLIENT_ERROR,

    /**
     * Operation failed due to business rule violation
     */
    BUSINESS_ERROR,

    /**
     * Requested resource not found
     */
    NOT_FOUND,

    /**
     * Authentication or authorization failure
     */
    AUTH_ERROR,

    /**
     * Operation failed due to server/system error
     */
    SERVER_ERROR,

    /**
     * Conflict with current state (duplicate, concurrent modification)
     */
    CONFLICT
}
