package com.example.erp.common.web.util;

import com.example.erp.common.exception.BusinessException;
import com.example.erp.common.exception.CommonErrorCodes;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility for validating and sanitizing pagination and sorting parameters.
 * 
 * Implements Rule 17.3: Sort field whitelist to prevent SQL injection and performance issues.
 * 
 * @author ERP Team
 */
public final class PageableValidator {

    private PageableValidator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Validate that all sort fields are in the whitelist.
     * Prevents SQL injection and performance issues from sorting on non-indexed columns.
     * 
     * @param pageable The pageable to validate
     * @param allowedFields Set of allowed field names for sorting
     * @return Validated pageable
     * @throws BusinessException if sort field not in whitelist
     */
    public static Pageable validateSortFields(Pageable pageable, Set<String> allowedFields) {
        return validateSortFields(pageable, allowedFields, Collections.emptyMap());
    }

    /**
     * Validate sort fields against a whitelist, while also allowing clients to use aliases.
     *
     * Example: accepting sort=name,asc while mapping it to roleName.
     *
     * @param pageable The pageable to validate
     * @param allowedFields Set of allowed field names for sorting (include aliases if you want them listed as allowed)
     * @param aliases Map of client-facing sort field -> canonical sort field
     * @return Validated pageable with cleaned + mapped sort
     * @throws BusinessException if sort field not in whitelist
     */
    public static Pageable validateSortFields(Pageable pageable, Set<String> allowedFields, Map<String, String> aliases) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }

        Map<String, String> safeAliases = (aliases == null) ? Collections.emptyMap() : aliases;

        // Clean and rebuild sort if needed (handles Swagger UI array format) and apply aliases
        List<Sort.Order> cleanedOrders = pageable.getSort().stream()
                .map(order -> {
                    String requested = cleanSortProperty(order.getProperty());
                    String mapped = safeAliases.getOrDefault(requested, requested);
                    return new Sort.Order(order.getDirection(), mapped);
                })
                .collect(Collectors.toList());

        // Check all requested sort fields against whitelist (after alias mapping)
        List<String> invalidFields = pageable.getSort().stream()
                .map(order -> cleanSortProperty(order.getProperty()))
                .filter(requested -> !allowedFields.contains(safeAliases.getOrDefault(requested, requested)))
                .distinct()
                .collect(Collectors.toList());

        if (!invalidFields.isEmpty()) {
            throw new BusinessException(
                CommonErrorCodes.INVALID_SORT_FIELD,
                "Invalid sort fields: " + invalidFields,
                "Allowed fields: " + String.join(", ", allowedFields)
            );
        }

        // Return new pageable with cleaned + mapped sort
        Sort cleanedSort = Sort.by(cleanedOrders);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), cleanedSort);
    }
    
    /**
     * Clean sort property name by removing JSON array formatting from Swagger UI.
     * Handles cases like: ["fieldName"] -> fieldName
     */
    private static String cleanSortProperty(String property) {
        if (property == null) {
            return property;
        }
        
        // Remove JSON array brackets and quotes: ["fieldName"] -> fieldName
        String cleaned = property.trim();
        if (cleaned.startsWith("[\"") && cleaned.endsWith("\"]")) {
            cleaned = cleaned.substring(2, cleaned.length() - 2);
        } else if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
            // Remove quotes if present
            if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
                cleaned = cleaned.substring(1, cleaned.length() - 1);
            }
        }
        
        return cleaned.trim();
    }

    /**
     * Validate sort fields and enforce pagination limits.
     * 
     * @param pageable The pageable to validate
     * @param allowedFields Set of allowed field names for sorting
     * @param maxPageSize Maximum page size (e.g., 100)
     * @return Validated and limited pageable
     */
    public static Pageable validateAndLimit(Pageable pageable, Set<String> allowedFields, int maxPageSize) {
        // Validate sort fields first
        Pageable validated = validateSortFields(pageable, allowedFields);

        // Enforce max page size
        if (validated.getPageSize() > maxPageSize) {
            return PageRequest.of(
                validated.getPageNumber(),
                maxPageSize,
                validated.getSort()
            );
        }

        return validated;
    }

    /**
     * Create whitelist from entity field names.
     * Use this in controllers/services to define allowed sort fields.
     * 
     * Example:
     * <pre>
     * private static final Set&lt;String&gt; ALLOWED_SORT_FIELDS = Set.of(
     *     "id", "username", "createdAt", "updatedAt"
     * );
     * </pre>
     */
    public static Set<String> allowedFields(String... fields) {
        return Set.of(fields);
    }
}
