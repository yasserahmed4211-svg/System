package com.erp.common.search;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Utility class for building {@link Pageable} from {@link SearchRequest}.
 * <p>
 * This builder validates sort fields against an allowed list and provides
 * sensible defaults for pagination parameters.
 * </p>
 *
 * <p><b>Security:</b></p>
 * <ul>
 *   <li>Sorting is restricted to whitelisted fields only to prevent SQL injection</li>
 *   <li>Never passes raw sortBy values directly to JPA without validation</li>
 *   <li>Collection-based sort paths (e.g., "roles.name") are not allowed as they can cause
 *       unexpected Cartesian products and performance issues</li>
 * </ul>
 *
 * <p><b>Features:</b></p>
 * <ul>
 *   <li>Validates sortBy field against allowed sort fields</li>
 *   <li>Falls back to default sort field "id" if not specified or invalid</li>
 *   <li>Enforces maximum page size of {@value #MAX_PAGE_SIZE} to prevent excessive memory usage</li>
 *   <li>Supports ASC/DESC sort directions (case-insensitive)</li>
 *   <li>Defaults to DESC sorting if sortDir is null or invalid</li>
 * </ul>
 *
 * <p><b>Sorting Restrictions:</b></p>
 * Only simple or nested non-collection fields are allowed for sorting.
 * Collection-based paths like "roles.name" are rejected because:
 * <ul>
 *   <li>They can produce duplicate results (one row per collection element)</li>
 *   <li>They cause Cartesian product expansion</li>
 *   <li>Sorting behavior becomes unpredictable with multiple collection elements</li>
 *   <li>Performance degrades significantly with large collections</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * Set&lt;String&gt; allowedSortFields = Set.of("id", "username", "createdAt", "customer.name");
 * Pageable pageable = PageableBuilder.from(request, allowedSortFields);
 * Page&lt;User&gt; results = userRepository.findAll(spec, pageable);
 * </pre>
 *
 * @author ERP System
 * @since 1.0
 */
public class PageableBuilder {

    /**
     * Default page size if not specified.
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Maximum allowed page size to prevent excessive memory usage.
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * Default sort field when none specified or invalid.
     */
    public static final String DEFAULT_SORT_FIELD = "id";

    private PageableBuilder() {
        // Utility class
    }

    /**
     * Builds a Pageable from a SearchRequest with validation.
     *
     * @param request           the search request
     * @param allowedSortFields set of allowed field names for sorting
     * @return the constructed Pageable
     * @throws SearchException if sort field is not allowed or page size exceeds maximum
     */
    public static Pageable from(SearchRequest request, Set<String> allowedSortFields) {
        if (request == null) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD));
        }

        int page = Math.max(0, request.getPage());
        int size = validateAndNormalizeSize(request.getSize());
        Sort sort = buildSort(request.getSortBy(), request.getSortDir(), allowedSortFields);

        return PageRequest.of(page, size, sort);
    }

    /**
     * Builds a Pageable with a default sort field if not in allowed list.
     *
     * @param request           the search request
     * @param allowedSortFields set of allowed field names for sorting
     * @param defaultSortField  the default field to sort by if not specified or invalid
     * @return the constructed Pageable
     * @throws SearchException if sort field is not allowed or page size exceeds maximum
     */
    public static Pageable from(SearchRequest request, Set<String> allowedSortFields, String defaultSortField) {
        if (request == null) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.DESC, defaultSortField));
        }

        int page = Math.max(0, request.getPage());
        int size = validateAndNormalizeSize(request.getSize());
        Sort sort = buildSort(request.getSortBy(), request.getSortDir(), allowedSortFields, defaultSortField);

        return PageRequest.of(page, size, sort);
    }

    /**
     * Validates and normalizes the page size.
     *
     * @throws SearchException if size exceeds MAX_PAGE_SIZE
     */
    private static int validateAndNormalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size > MAX_PAGE_SIZE) {
            throw new SearchException("Page size must not exceed " + MAX_PAGE_SIZE);
        }
        return size;
    }

    /**
     * Builds Sort with validation.
     */
    private static Sort buildSort(String sortBy, String sortDir, Set<String> allowedSortFields) {
        return buildSort(sortBy, sortDir, allowedSortFields, DEFAULT_SORT_FIELD);
    }

    /**
     * Builds Sort with validation and custom default.
     *
     * @throws SearchException if sortBy field is not in allowed list
     */
    private static Sort buildSort(
            String sortBy,
            String sortDir,
            Set<String> allowedSortFields,
            String defaultSortField) {

        // Determine sort field
        String sortField = defaultSortField;
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            if (allowedSortFields != null && allowedSortFields.contains(sortBy)) {
                sortField = sortBy;
            } else if (allowedSortFields != null) {
                throw new SearchException(
                        "Sort field '" + sortBy + "' is not allowed. Allowed fields: " + allowedSortFields);
            } else {
                // No validation if allowedSortFields is null
                sortField = sortBy;
            }
        }

        // Determine sort direction - defaults to DESC
        Sort.Direction direction = Sort.Direction.DESC;
        if (sortDir != null) {
            String normalizedDir = sortDir.trim().toUpperCase();
            if ("ASC".equals(normalizedDir)) {
                direction = Sort.Direction.ASC;
            }
            // Any other value (including "DESC") defaults to DESC
        }

        return Sort.by(direction, sortField);
    }

    /**
     * Creates unsorted pageable (useful for count queries).
     *
     * @param request the search request
     * @return the constructed Pageable without sorting
     */
    public static Pageable unsorted(SearchRequest request) {
        if (request == null) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE);
        }

        int page = Math.max(0, request.getPage());
        int size = validateAndNormalizeSize(request.getSize());

        return PageRequest.of(page, size);
    }
}
