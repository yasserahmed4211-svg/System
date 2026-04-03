package com.erp.common.search;

/**
 * Interface for validating whether a field is allowed for searching/filtering.
 * <p>
 * Implementations of this interface define which fields are permitted for
 * dynamic search operations on a specific entity, preventing SQL injection
 * and restricting access to sensitive or non-searchable fields.
 * </p>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * AllowedFields userFields = new SetAllowedFields(
 *     Set.of("username", "email", "enabled", "tenant.id", "roles.name")
 * );
 * </pre>
 *
 * @author ERP System
 * @since 1.0
 */
public interface AllowedFields {

    /**
     * Checks if the given field is allowed for search operations.
     *
     * @param field the field name (may include dot notation for nested properties)
     * @return true if the field is allowed, false otherwise
     */
    boolean isAllowed(String field);

    /**
     * Validates that the given field is allowed, throwing an exception if not.
     *
     * @param field the field name to validate
     * @throws SearchException if the field is not allowed
     */
    default void validateField(String field) {
        if (!isAllowed(field)) {
            throw new SearchException("Field '" + field + "' is not allowed for searching");
        }
    }
}
