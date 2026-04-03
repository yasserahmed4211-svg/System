package com.erp.common.search;

import com.example.erp.common.converter.BooleanNumberConverter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Utility class for building JPA query predicates for active/inactive flag filtering.
 * 
 * <h2>Purpose:</h2>
 * This helper provides reusable methods for building query conditions that handle
 * Boolean parameters filtering against NUMBER(1) database columns.
 * 
 * <h2>Query Pattern:</h2>
 * The standard pattern for nullable active flag filtering is:
 * <pre>{@code
 * -- When isActive is null: returns ALL records
 * -- When isActive is true: returns only active records (IS_ACTIVE = 1)
 * -- When isActive is false: returns only inactive records (IS_ACTIVE = 0)
 * 
 * WHERE (:isActive IS NULL OR IS_ACTIVE = CASE WHEN :isActive = TRUE THEN 1 ELSE 0 END)
 * }</pre>
 * 
 * <h2>Usage Examples:</h2>
 * 
 * <h3>In JPA Specification:</h3>
 * <pre>{@code
 * public static Specification<MyEntity> hasActiveStatus(Boolean isActive) {
 *     return (root, query, cb) -> 
 *         ActiveFlagQueryHelper.buildActivePredicate(cb, root.get("isActive"), isActive);
 * }
 * }</pre>
 * 
 * <h3>In Native Query:</h3>
 * <pre>{@code
 * @Query(value = "SELECT * FROM MY_TABLE WHERE " + 
 *        ActiveFlagQueryHelper.NATIVE_ACTIVE_CONDITION, nativeQuery = true)
 * List<MyEntity> findByActiveStatus(@Param("isActive") Integer isActive);
 * }</pre>
 * 
 * @author ERP Team
 * @since 1.0
 * @see BooleanNumberConverter
 */
public final class ActiveFlagQueryHelper {

    /**
     * SQL fragment for native queries with nullable active filter.
     * Use with Integer parameter converted via {@link BooleanNumberConverter#toDbValue(Boolean)}.
     * 
     * Example: WHERE (:isActive IS NULL OR IS_ACTIVE = :isActive)
     */
    public static final String NATIVE_ACTIVE_CONDITION = 
        "(:isActive IS NULL OR IS_ACTIVE = :isActive)";

    /**
     * JPQL fragment for queries with nullable active filter.
     * 
     * Example: (:isActive IS NULL OR e.isActive = :isActive)
     */
    public static final String JPQL_ACTIVE_CONDITION = 
        "(:isActive IS NULL OR e.isActive = :isActive)";

    private ActiveFlagQueryHelper() {
        // Utility class - prevent instantiation
    }

    /**
     * Builds a JPA Predicate for filtering by active status.
     * 
     * <p>Behavior:</p>
     * <ul>
     *   <li>If {@code isActive} is null: returns a predicate that matches ALL records</li>
     *   <li>If {@code isActive} is true: returns predicate for active records only</li>
     *   <li>If {@code isActive} is false: returns predicate for inactive records only</li>
     * </ul>
     * 
     * @param cb the CriteriaBuilder
     * @param activeExpression the expression for the active field (e.g., root.get("isActive"))
     * @param isActive the filter value (null = ALL, true = active only, false = inactive only)
     * @return a Predicate for the active filter condition
     */
    public static Predicate buildActivePredicate(
            CriteriaBuilder cb,
            Expression<Boolean> activeExpression,
            Boolean isActive) {
        
        if (isActive == null) {
            // Return a predicate that always evaluates to true (match all)
            return cb.conjunction();
        }
        
        // Return equality predicate
        return cb.equal(activeExpression, isActive);
    }

    /**
     * Builds a JPA Predicate for filtering by active status using Integer expression.
     * Use this when the entity field is mapped directly to Integer (not using converter).
     * 
     * @param cb the CriteriaBuilder
     * @param activeExpression the expression for the active field as Integer
     * @param isActive the filter value (null = ALL, true = active only, false = inactive only)
     * @return a Predicate for the active filter condition
     */
    public static Predicate buildActivePredicateForIntegerColumn(
            CriteriaBuilder cb,
            Expression<Integer> activeExpression,
            Boolean isActive) {
        
        if (isActive == null) {
            return cb.conjunction();
        }
        
        Integer dbValue = BooleanNumberConverter.toDbValue(isActive);
        return cb.equal(activeExpression, dbValue);
    }

    /**
     * Creates a Specification that filters by active status.
     * 
     * @param <T> the entity type
     * @param fieldName the name of the active field (e.g., "isActive", "active", "enabled")
     * @param isActive the filter value (null = ALL)
     * @return a Specification for the active filter
     */
    public static <T> Specification<T> hasActiveStatus(String fieldName, Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get(fieldName), isActive);
        };
    }

    /**
     * Creates a Specification that filters for active records only.
     * 
     * @param <T> the entity type
     * @param fieldName the name of the active field
     * @return a Specification for active records
     */
    public static <T> Specification<T> isActive(String fieldName) {
        return hasActiveStatus(fieldName, Boolean.TRUE);
    }

    /**
     * Creates a Specification that filters for inactive records only.
     * 
     * @param <T> the entity type
     * @param fieldName the name of the active field
     * @return a Specification for inactive records
     */
    public static <T> Specification<T> isInactive(String fieldName) {
        return hasActiveStatus(fieldName, Boolean.FALSE);
    }

    /**
     * Converts Boolean filter value to Integer for native queries.
     * 
     * @param isActive the Boolean filter value
     * @return Integer value for database (1, 0, or null)
     */
    public static Integer toNativeQueryParam(Boolean isActive) {
        return BooleanNumberConverter.toDbValue(isActive);
    }

    /**
     * Combines multiple specifications with AND logic, including active status filter.
     * 
     * @param <T> the entity type
     * @param activeFieldName the name of the active field
     * @param isActive the active filter value (null = ALL)
     * @param otherSpecs additional specifications to combine (can be null or empty)
     * @return combined Specification
     */
    @SafeVarargs
    public static <T> Specification<T> withActiveFilter(
            String activeFieldName,
            Boolean isActive,
            Specification<T>... otherSpecs) {
        
        Specification<T> result = hasActiveStatus(activeFieldName, isActive);
        
        if (otherSpecs != null) {
            for (Specification<T> spec : otherSpecs) {
                if (spec != null) {
                    result = result.and(spec);
                }
            }
        }
        
        return result;
    }
}
