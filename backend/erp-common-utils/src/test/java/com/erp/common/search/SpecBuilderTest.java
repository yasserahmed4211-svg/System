package com.erp.common.search;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SpecBuilder}.
 * <p>
 * Note: Full integration testing with JPA requires entity setup.
 * These tests focus on validation logic and filter processing.
 * </p>
 *
 * @author ERP System
 * @since 1.0
 */
class SpecBuilderTest {

    @Test
    void testBuild_NullRequest() {
        AllowedFields allowedFields = new SetAllowedFields(Set.of("username"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        var spec = SpecBuilder.build(null, allowedFields, converter);

        assertNotNull(spec);
    }

    @Test
    void testBuild_EmptyFilters() {
        SearchRequest request = new SearchRequest();
        AllowedFields allowedFields = new SetAllowedFields(Set.of("username"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        var spec = SpecBuilder.build(request, allowedFields, converter);

        assertNotNull(spec);
    }

    @Test
    void testBuild_DisallowedField_ThrowsException() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("password", Op.EQ, "secret"));

        AllowedFields allowedFields = new SetAllowedFields(Set.of("username", "email"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        SearchException exception = assertThrows(
                SearchException.class,
                () -> SpecBuilder.build(request, allowedFields, converter)
        );

        assertTrue(exception.getMessage().contains("password"));
        assertTrue(exception.getMessage().contains("not allowed"));
    }

    @Test
    void testBuild_NullFilterField_Skipped() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter(null, Op.EQ, "value"));
        request.addFilter(new SearchFilter("username", Op.EQ, "john"));

        AllowedFields allowedFields = new SetAllowedFields(Set.of("username"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        // Should not throw - null filter is skipped
        var spec = SpecBuilder.build(request, allowedFields, converter);
        assertNotNull(spec);
    }

    @Test
    void testBuild_InOperator_WithList() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("status", Op.IN, Arrays.asList("ACTIVE", "PENDING")));

        AllowedFields allowedFields = new SetAllowedFields(Set.of("status"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        var spec = SpecBuilder.build(request, allowedFields, converter);
        assertNotNull(spec);
    }

    @Test
    void testBuild_InOperator_WithCommaSeparatedString() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("status", Op.IN, "ACTIVE,PENDING,CLOSED"));

        AllowedFields allowedFields = new SetAllowedFields(Set.of("status"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        var spec = SpecBuilder.build(request, allowedFields, converter);
        assertNotNull(spec);
    }

    @Test
    void testBuild_InOperator_EmptyList_ThrowsException() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("status", Op.IN, List.of()));

        AllowedFields allowedFields = new SetAllowedFields(Set.of("status"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        SearchException exception = assertThrows(
                SearchException.class,
                () -> SpecBuilder.build(request, allowedFields, converter)
        );

        assertTrue(exception.getMessage().contains("at least one value"));
    }

    @Test
    void testBuild_BetweenOperator_ValidList() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("createdAt", Op.BETWEEN,
                Arrays.asList(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))));

        AllowedFields allowedFields = new SetAllowedFields(Set.of("createdAt"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        var spec = SpecBuilder.build(request, allowedFields, converter);
        assertNotNull(spec);
    }

    @Test
    void testBuild_BetweenOperator_InvalidSize_ThrowsException() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("amount", Op.BETWEEN, Arrays.asList(100)));

        AllowedFields allowedFields = new SetAllowedFields(Set.of("amount"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        SearchException exception = assertThrows(
                SearchException.class,
                () -> SpecBuilder.build(request, allowedFields, converter)
        );

        assertTrue(exception.getMessage().contains("exactly 2 values"));
        assertTrue(exception.getMessage().contains("got 1"));
    }

    @Test
    void testBuild_BetweenOperator_NotList_ThrowsException() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("amount", Op.BETWEEN, "100-200"));

        AllowedFields allowedFields = new SetAllowedFields(Set.of("amount"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        SearchException exception = assertThrows(
                SearchException.class,
                () -> SpecBuilder.build(request, allowedFields, converter)
        );

        assertTrue(exception.getMessage().contains("list of [from, to]"));
    }

    @Test
    void testBuild_IsNullOperator() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("deletedAt", Op.IS_NULL, null));

        AllowedFields allowedFields = new SetAllowedFields(Set.of("deletedAt"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        var spec = SpecBuilder.build(request, allowedFields, converter);
        assertNotNull(spec);
    }

    @Test
    void testBuild_IsNotNullOperator() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("lastLoginAt", Op.IS_NOT_NULL, null));

        AllowedFields allowedFields = new SetAllowedFields(Set.of("lastLoginAt"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        var spec = SpecBuilder.build(request, allowedFields, converter);
        assertNotNull(spec);
    }

    @Test
    void testBuild_OperatorRequiresValue_ThrowsException() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("username", Op.EQ, null));

        AllowedFields allowedFields = new SetAllowedFields(Set.of("username"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        SearchException exception = assertThrows(
                SearchException.class,
                () -> SpecBuilder.build(request, allowedFields, converter)
        );

        assertTrue(exception.getMessage().contains("requires a non-null value"));
    }

    @Test
    void testBuild_NestedField() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("tenant.id", Op.EQ, 1L));

        AllowedFields allowedFields = new SetAllowedFields(Set.of("tenant.id"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        var spec = SpecBuilder.build(request, allowedFields, converter);
        assertNotNull(spec);
    }

    @Test
    void testBuild_CollectionJoinField() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("roles.name", Op.IN, Arrays.asList("ADMIN", "USER")));

        AllowedFields allowedFields = new SetAllowedFields(Set.of("roles.name"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        var spec = SpecBuilder.build(request, allowedFields, converter);
        assertNotNull(spec);
    }

    @Test
    void testBuild_MultiLevelCollectionJoin() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("roles.permissions.name", Op.EQ, "READ_USERS"));

        AllowedFields allowedFields = new SetAllowedFields(Set.of("roles.permissions.name"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        var spec = SpecBuilder.build(request, allowedFields, converter);
        assertNotNull(spec);
    }

    @Test
    void testBuild_MultipleFilters_AndLogic() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("username", Op.LIKE, "john"));
        request.addFilter(new SearchFilter("enabled", Op.EQ, true));
        request.addFilter(new SearchFilter("tenant.id", Op.EQ, 1L));

        AllowedFields allowedFields = new SetAllowedFields(
                Set.of("username", "enabled", "tenant.id")
        );
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        var spec = SpecBuilder.build(request, allowedFields, converter);
        assertNotNull(spec);
    }

    @Test
    void testBuild_WithCustomConverter() {
        FieldValueConverter customConverter = (field, rawValue, op) -> {
            if ("createdAt".equals(field) && rawValue instanceof String) {
                return LocalDate.parse((String) rawValue);
            }
            return rawValue;
        };

        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("createdAt", Op.EQ, "2024-01-15"));

        AllowedFields allowedFields = new SetAllowedFields(Set.of("createdAt"));

        var spec = SpecBuilder.build(request, allowedFields, customConverter);
        assertNotNull(spec);
    }

    @Test
    void testBuild_LikeOperator() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("username", Op.LIKE, "john"));

        AllowedFields allowedFields = new SetAllowedFields(Set.of("username"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        var spec = SpecBuilder.build(request, allowedFields, converter);
        assertNotNull(spec);
    }

    @Test
    void testBuild_StartsWithOperator() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("email", Op.STARTS_WITH, "john"));

        AllowedFields allowedFields = new SetAllowedFields(Set.of("email"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        var spec = SpecBuilder.build(request, allowedFields, converter);
        assertNotNull(spec);
    }

    @Test
    void testBuild_EndsWithOperator() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("email", Op.ENDS_WITH, "@example.com"));

        AllowedFields allowedFields = new SetAllowedFields(Set.of("email"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        var spec = SpecBuilder.build(request, allowedFields, converter);
        assertNotNull(spec);
    }

    @Test
    void testBuild_ComparisonOperators() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("age", Op.GT, 18));
        request.addFilter(new SearchFilter("salary", Op.GTE, 50000));
        request.addFilter(new SearchFilter("score", Op.LT, 100));
        request.addFilter(new SearchFilter("rating", Op.LTE, 5));

        AllowedFields allowedFields = new SetAllowedFields(
                Set.of("age", "salary", "score", "rating")
        );
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        var spec = SpecBuilder.build(request, allowedFields, converter);
        assertNotNull(spec);
    }

    @Test
    void testBuild_NotEqualOperator() {
        SearchRequest request = new SearchRequest();
        request.addFilter(new SearchFilter("status", Op.NE, "DELETED"));

        AllowedFields allowedFields = new SetAllowedFields(Set.of("status"));
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        var spec = SpecBuilder.build(request, allowedFields, converter);
        assertNotNull(spec);
    }
}
