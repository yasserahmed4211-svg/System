package com.erp.common.search;

import com.example.erp.common.exception.CommonErrorCodes;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.PluralAttribute;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for building JPA {@link Specification} from {@link SearchRequest}.
 * <p>
 * This builder supports:
 * <ul>
 *   <li>Simple field filters: "username", "enabled"</li>
 *   <li>Nested field filters with dot notation: "tenant.id", "profile.email"</li>
 *   <li>Collection joins for ManyToMany/OneToMany: "roles.name", "roles.permissions.name"</li>
 *   <li>Case-insensitive string matching: LIKE, STARTS_WITH, ENDS_WITH</li>
 *   <li>Comparison operators: EQ, NE, GT, GTE, LT, LTE, BETWEEN</li>
 *   <li>Null checks: IS_NULL, IS_NOT_NULL</li>
 *   <li>List matching: IN</li>
 *   <li>Automatic DISTINCT when joins are used</li>
 * </ul>
 * </p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * AllowedFields allowedFields = new SetAllowedFields(Set.of("username", "roles.name"));
 * FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;
 * 
 * SearchRequest request = new SearchRequest();
 * request.addFilter(new SearchFilter("username", Op.LIKE, "john"));
 * request.addFilter(new SearchFilter("roles.name", Op.IN, List.of("ADMIN", "USER")));
 * 
 * Specification&lt;User&gt; spec = SpecBuilder.build(request, allowedFields, converter);
 * Page&lt;User&gt; results = userRepository.findAll(spec, pageable);
 * </pre>
 *
 * @author ERP System
 * @since 1.0
 */
public class SpecBuilder {

    private SpecBuilder() {
        // Utility class
    }

    /**
     * Validates filter value based on operator requirements.
     */
    private static void validateFilterValue(SearchFilter filter) {
        Op op = filter.getOp();
        Object value = filter.getValue();
        String field = filter.getField();

        // IS_NULL and IS_NOT_NULL don't require values
        if (op == Op.IS_NULL || op == Op.IS_NOT_NULL) {
            return;
        }

        // All other operators require non-null values
        if (value == null) {
            throw new SearchException(CommonErrorCodes.SEARCH_VALUE_REQUIRED, 
                "Operator " + op + " requires a non-null value for field '" + field + "'");
        }

        // Validate IN operator
        if (op == Op.IN) {
            Collection<?> values;
            if (value instanceof Collection) {
                values = (Collection<?>) value;
            } else if (value instanceof String) {
                String stringValue = (String) value;
                values = Arrays.asList(stringValue.split("\\s*,\\s*"));
            } else {
                values = Collections.singletonList(value);
            }

            if (values.isEmpty()) {
                throw new SearchException(CommonErrorCodes.SEARCH_IN_EMPTY_VALUES, 
                    "IN operator requires at least one value for field '" + field + "'");
            }
        }

        // Validate BETWEEN operator
        if (op == Op.BETWEEN) {
            if (!(value instanceof List)) {
                throw new SearchException(CommonErrorCodes.SEARCH_BETWEEN_INVALID, 
                    "BETWEEN operator requires a list of [from, to] values for field '" + field + "'");
            }

            List<?> values = (List<?>) value;
            if (values.size() != 2) {
                throw new SearchException(CommonErrorCodes.SEARCH_BETWEEN_TWO_VALUES, 
                    "BETWEEN operator requires exactly 2 values [from, to] for field '" + field + "', got " + values.size());
            }
        }
    }

    /**
     * Builds a JPA Specification from a SearchRequest.
     *
     * @param <T>           the entity type
     * @param request       the search request containing filters
     * @param allowedFields validator for allowed field names
     * @param converter     converter for field values
     * @return the constructed Specification, or a match-all spec if no filters
     * @throws SearchException if validation fails or filter is invalid
     */
    public static <T> Specification<T> build(
            SearchRequest request,
            AllowedFields allowedFields,
            FieldValueConverter converter) {

        if (request == null || request.getFilters() == null || request.getFilters().isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }

        // Early validation before building the specification
        for (SearchFilter filter : request.getFilters()) {
            if (filter == null || filter.getField() == null || filter.getOp() == null) {
                continue;
            }

            // Validate field is allowed
            allowedFields.validateField(filter.getField());

            // Validate value for operators that require it
            validateFilterValue(filter);
        }

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            boolean hasJoins = false;

            for (SearchFilter filter : request.getFilters()) {
                if (filter == null || filter.getField() == null || filter.getOp() == null) {
                    continue;
                }

                // Validate field is allowed
                allowedFields.validateField(filter.getField());

                // Convert value if needed
                Object convertedValue = converter.convert(filter.getField(), filter.getValue(), filter.getOp());

                // Check if this filter requires a join
                if (requiresJoin(filter.getField(), root)) {
                    hasJoins = true;
                }

                // Build predicate
                Predicate predicate = buildPredicate(root, criteriaBuilder, filter, convertedValue);
                if (predicate != null) {
                    predicates.add(predicate);
                }
            }

            // Apply DISTINCT if joins were used to avoid duplicates
            if (hasJoins && query != null) {
                query.distinct(true);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Checks if a field path requires a join (contains dot and intermediate path is a collection).
     * Uses a simplified heuristic approach.
     */
    private static <T> boolean requiresJoin(String fieldPath, Root<T> root) {
        // If no dot notation, it's a simple field
        if (!fieldPath.contains(".")) {
            return false;
        }

        // For fields with dot notation, we'll determine if join is needed during expression building
        // This is a conservative approach: assume join might be needed for nested paths
        return true;
    }

    /**
     * Builds a predicate for a single filter.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Predicate buildPredicate(
            Root<T> root,
            CriteriaBuilder cb,
            SearchFilter filter,
            Object value) {

        String field = filter.getField();
        Op op = filter.getOp();

        // Handle IS_NULL and IS_NOT_NULL (no value needed)
        if (op == Op.IS_NULL) {
            Expression<?> expression = getExpression(root, field);
            return cb.isNull(expression);
        }

        if (op == Op.IS_NOT_NULL) {
            Expression<?> expression = getExpression(root, field);
            return cb.isNotNull(expression);
        }

        // For other operators, value is required
        if (value == null) {
            throw new SearchException("Operator " + op + " requires a non-null value for field '" + field + "'");
        }

        switch (op) {
            case EQ: {
                Expression<?> expr = getExpression(root, field);
                return cb.equal(expr, coerceToFieldType(expr, value));
            }

            case NE: {
                Expression<?> expr = getExpression(root, field);
                return cb.notEqual(expr, coerceToFieldType(expr, value));
            }

            case LIKE:
                return buildLikePredicate(root, cb, field, value, "%%%s%%");

            case STARTS_WITH:
                return buildLikePredicate(root, cb, field, value, "%s%%");

            case ENDS_WITH:
                return buildLikePredicate(root, cb, field, value, "%%%s");

            case IN:
                return buildInPredicate(root, field, value);

            case GT:
                return buildComparisonPredicate(root, cb, field, value, "GT");

            case GTE:
                return buildComparisonPredicate(root, cb, field, value, "GTE");

            case LT:
                return buildComparisonPredicate(root, cb, field, value, "LT");

            case LTE:
                return buildComparisonPredicate(root, cb, field, value, "LTE");

            case BETWEEN:
                return buildBetweenPredicate(root, cb, field, value);

            default:
                throw new SearchException("Unsupported operator: " + op);
        }
    }

    /**
     * Coerces a String value to the Java type of the given JPA expression.
     * Fixes BUG-01: Boolean/numeric fields receive String values from the frontend
     * (e.g. "true"/"false" for Boolean, "1" for Integer/Long) causing type mismatch
     * when Oracle compares a NUMBER column with a VARCHAR literal.
     */
    private static Object coerceToFieldType(Expression<?> expression, Object value) {
        if (!(value instanceof String)) {
            return value;
        }
        String str = (String) value;
        Class<?> javaType = expression.getJavaType();
        if (javaType == null) {
            return value;
        }
        if (Boolean.class.equals(javaType) || boolean.class.equals(javaType)) {
            return Boolean.parseBoolean(str);
        }
        if (Integer.class.equals(javaType) || int.class.equals(javaType)) {
            try { return Integer.parseInt(str); } catch (NumberFormatException e) { return value; }
        }
        if (Long.class.equals(javaType) || long.class.equals(javaType)) {
            try { return Long.parseLong(str); } catch (NumberFormatException e) { return value; }
        }
        if (Double.class.equals(javaType) || double.class.equals(javaType)) {
            try { return Double.parseDouble(str); } catch (NumberFormatException e) { return value; }
        }
        return value;
    }

    /**
     * Builds a case-insensitive LIKE predicate.
     */
    private static <T> Predicate buildLikePredicate(
            Root<T> root,
            CriteriaBuilder cb,
            String field,
            Object value,
            String pattern) {

        Expression<String> expression = getExpression(root, field);
        String stringValue = value.toString();
        String likePattern = String.format(pattern, stringValue);

        return cb.like(cb.lower(expression), likePattern.toLowerCase());
    }

    /**
     * Builds a comparison predicate (GT, GTE, LT, LTE).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Predicate buildComparisonPredicate(
            Root<T> root,
            CriteriaBuilder cb,
            String field,
            Object value,
            String comparisonType) {

        Expression expression = getExpression(root, field);
        Comparable comparableValue = (Comparable) value;

        switch (comparisonType) {
            case "GT":
                return cb.greaterThan(expression, comparableValue);
            case "GTE":
                return cb.greaterThanOrEqualTo(expression, comparableValue);
            case "LT":
                return cb.lessThan(expression, comparableValue);
            case "LTE":
                return cb.lessThanOrEqualTo(expression, comparableValue);
            default:
                throw new SearchException("Unknown comparison type: " + comparisonType);
        }
    }

    /**
     * Builds an IN predicate, handling both List and comma-separated String values.
     */
    private static <T> Predicate buildInPredicate(Root<T> root, String field, Object value) {
        Expression<?> expression = getExpression(root, field);

        Collection<?> values;
        if (value instanceof Collection) {
            values = (Collection<?>) value;
        } else if (value instanceof String) {
            // Handle comma-separated string
            String stringValue = (String) value;
            values = Arrays.asList(stringValue.split("\\s*,\\s*"));
        } else {
            // Single value
            values = Collections.singletonList(value);
        }

        if (values.isEmpty()) {
            throw new SearchException("IN operator requires at least one value for field '" + field + "'");
        }

        return expression.in(values);
    }

    /**
     * Builds a BETWEEN predicate.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Predicate buildBetweenPredicate(
            Root<T> root,
            CriteriaBuilder cb,
            String field,
            Object value) {

        List<?> values;
        if (value instanceof List) {
            values = (List<?>) value;
        } else {
            throw new SearchException("BETWEEN operator requires a list of [from, to] values for field '" + field + "'");
        }

        if (values.size() != 2) {
            throw new SearchException("BETWEEN operator requires exactly 2 values [from, to] for field '" + field + "', got " + values.size());
        }

        Expression expression = getExpression(root, field);
        Comparable from = (Comparable) values.get(0);
        Comparable to = (Comparable) values.get(1);

        return cb.between(expression, from, to);
    }

    /**
     * Gets an expression for a field, handling dot notation and joins.
     * <p>
     * For simple fields: root.get("username")
     * For nested singular: root.get("tenant").get("id")
     * For collections: root.join("roles").get("name")
     * For multi-level: root.join("roles").join("permissions").get("name")
     * </p>
     */
    @SuppressWarnings("unchecked")
    private static <T> Expression<T> getExpression(Root<?> root, String fieldPath) {
        if (!fieldPath.contains(".")) {
            return (Expression<T>) root.get(fieldPath);
        }

        String[] parts = fieldPath.split("\\.");
        From<?, ?> from = root;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];

            // Try as a regular property first (ManyToOne, OneToOne, Embedded)
            try {
                from = (From<?, ?>) from.get(part);
            } catch (IllegalArgumentException | IllegalStateException e) {
                // If that fails, try as a join (OneToMany, ManyToMany)
                from = getOrCreateJoin(from, part);
            }
        }

        return (Expression<T>) from.get(parts[parts.length - 1]);
    }

    /**
     * Gets an existing join or creates a new one.
     */
    @SuppressWarnings("unchecked")
    private static <X, Y> Join<X, Y> getOrCreateJoin(From<?, X> from, String attributeName) {
        // Check if join already exists
        for (Join<X, ?> join : from.getJoins()) {
            if (join.getAttribute().getName().equals(attributeName)) {
                return (Join<X, Y>) join;
            }
        }

        // Create new LEFT join
        return from.join(attributeName, JoinType.LEFT);
    }
}
