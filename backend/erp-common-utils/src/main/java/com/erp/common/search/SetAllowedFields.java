package com.erp.common.search;

import java.util.Collections;
import java.util.Set;

/**
 * Default implementation of {@link AllowedFields} using a Set of allowed field names.
 * <p>
 * This implementation performs exact string matching (case-sensitive) against
 * a predefined set of allowed fields. Field names may include dot notation
 * for nested properties (e.g., "tenant.id", "roles.name").
 * </p>
 *
 * <p><b>Example:</b></p>
 * <pre>
 * AllowedFields userFields = new SetAllowedFields(Set.of(
 *     "id", "username", "email", "enabled",
 *     "tenant.id", "tenant.name",
 *     "roles.name", "roles.permissions.name"
 * ));
 * </pre>
 *
 * @author ERP System
 * @since 1.0
 */
public class SetAllowedFields implements AllowedFields {

    private final Set<String> allowedFields;

    /**
     * Constructs a new SetAllowedFields with the specified allowed fields.
     *
     * @param allowedFields the set of allowed field names (case-sensitive)
     */
    public SetAllowedFields(Set<String> allowedFields) {
        this.allowedFields = allowedFields != null
                ? Collections.unmodifiableSet(allowedFields)
                : Collections.emptySet();
    }

    @Override
    public boolean isAllowed(String field) {
        return field != null && allowedFields.contains(field);
    }

    /**
     * Returns an unmodifiable view of the allowed fields.
     *
     * @return the set of allowed fields
     */
    public Set<String> getAllowedFields() {
        return allowedFields;
    }
}
