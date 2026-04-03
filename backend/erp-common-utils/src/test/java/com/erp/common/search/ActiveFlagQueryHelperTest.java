package com.erp.common.search;

import com.example.erp.common.converter.BooleanNumberConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ActiveFlagQueryHelper.
 * 
 * Tests the utility class for building JPA predicates for active/inactive flag filtering.
 * Note: Predicate building tests are simplified since Mockito is not available.
 */
@DisplayName("ActiveFlagQueryHelper Tests")
class ActiveFlagQueryHelperTest {

    @Nested
    @DisplayName("toNativeQueryParam")
    class ToNativeQueryParam {

        @Test
        @DisplayName("true → 1")
        void true_returnsOne() {
            assertEquals(1, ActiveFlagQueryHelper.toNativeQueryParam(true));
        }

        @Test
        @DisplayName("false → 0")
        void false_returnsZero() {
            assertEquals(0, ActiveFlagQueryHelper.toNativeQueryParam(false));
        }

        @Test
        @DisplayName("null → null")
        void null_returnsNull() {
            assertNull(ActiveFlagQueryHelper.toNativeQueryParam(null));
        }
    }

    @Nested
    @DisplayName("Specification factory methods")
    class SpecificationFactoryMethods {

        @Test
        @DisplayName("hasActiveStatus with true returns non-null specification")
        void hasActiveStatus_true_returnsSpecification() {
            Specification<Object> spec = ActiveFlagQueryHelper.hasActiveStatus("isActive", true);
            assertNotNull(spec);
        }

        @Test
        @DisplayName("hasActiveStatus with false returns non-null specification")
        void hasActiveStatus_false_returnsSpecification() {
            Specification<Object> spec = ActiveFlagQueryHelper.hasActiveStatus("isActive", false);
            assertNotNull(spec);
        }

        @Test
        @DisplayName("hasActiveStatus with null returns non-null specification")
        void hasActiveStatus_null_returnsSpecification() {
            Specification<Object> spec = ActiveFlagQueryHelper.hasActiveStatus("isActive", null);
            assertNotNull(spec);
        }

        @Test
        @DisplayName("isActive returns specification for true")
        void isActive_returnsActiveSpecification() {
            Specification<Object> spec = ActiveFlagQueryHelper.isActive("isActive");
            assertNotNull(spec);
        }

        @Test
        @DisplayName("isInactive returns specification for false")
        void isInactive_returnsInactiveSpecification() {
            Specification<Object> spec = ActiveFlagQueryHelper.isInactive("isActive");
            assertNotNull(spec);
        }

        @Test
        @DisplayName("withActiveFilter combines specifications")
        void withActiveFilter_combinesSpecifications() {
            Specification<Object> otherSpec = (root, query, cb) -> cb.conjunction();
            Specification<Object> combined = ActiveFlagQueryHelper.withActiveFilter(
                "isActive", true, otherSpec);
            assertNotNull(combined);
        }

        @Test
        @DisplayName("withActiveFilter works with null specs array")
        void withActiveFilter_worksWithNullSpecs() {
            Specification<Object> combined = ActiveFlagQueryHelper.withActiveFilter(
                "isActive", true, (Specification<Object>[]) null);
            // Should not throw, specs array can be empty
        }

        @Test
        @DisplayName("withActiveFilter works with empty varargs")
        void withActiveFilter_worksWithEmptyVarargs() {
            Specification<Object> combined = ActiveFlagQueryHelper.withActiveFilter(
                "isActive", true);
            assertNotNull(combined);
        }
    }

    @Nested
    @DisplayName("SQL/JPQL fragments")
    class SqlFragments {

        @Test
        @DisplayName("NATIVE_ACTIVE_CONDITION contains proper SQL pattern")
        void nativeCondition_containsProperPattern() {
            String condition = ActiveFlagQueryHelper.NATIVE_ACTIVE_CONDITION;
            assertNotNull(condition);
            assertTrue(condition.contains(":isActive IS NULL"), 
                "Should contain null check for parameter");
            assertTrue(condition.contains("IS_ACTIVE = :isActive"), 
                "Should contain column comparison");
            assertTrue(condition.contains("OR"), 
                "Should use OR for null handling");
        }

        @Test
        @DisplayName("JPQL_ACTIVE_CONDITION contains proper JPQL pattern")
        void jpqlCondition_containsProperPattern() {
            String condition = ActiveFlagQueryHelper.JPQL_ACTIVE_CONDITION;
            assertNotNull(condition);
            assertTrue(condition.contains(":isActive IS NULL"), 
                "Should contain null check for parameter");
            assertTrue(condition.contains("e.isActive = :isActive"), 
                "Should contain entity field comparison");
            assertTrue(condition.contains("OR"), 
                "Should use OR for null handling");
        }

        @Test
        @DisplayName("Native condition can be used in SQL WHERE clause")
        void nativeCondition_validForWhereClause() {
            String sql = "SELECT * FROM MY_TABLE WHERE " + ActiveFlagQueryHelper.NATIVE_ACTIVE_CONDITION;
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("IS_ACTIVE"));
        }

        @Test
        @DisplayName("JPQL condition can be used in JPQL WHERE clause")
        void jpqlCondition_validForWhereClause() {
            String jpql = "SELECT e FROM Entity e WHERE " + ActiveFlagQueryHelper.JPQL_ACTIVE_CONDITION;
            assertTrue(jpql.contains("WHERE"));
            assertTrue(jpql.contains("e.isActive"));
        }
    }

    @Nested
    @DisplayName("Parameter conversion consistency")
    class ParameterConversionConsistency {

        @Test
        @DisplayName("toNativeQueryParam matches BooleanNumberConverter.toDbValue")
        void toNativeQueryParam_matchesConverter() {
            assertEquals(BooleanNumberConverter.toDbValue(true), 
                ActiveFlagQueryHelper.toNativeQueryParam(true));
            assertEquals(BooleanNumberConverter.toDbValue(false), 
                ActiveFlagQueryHelper.toNativeQueryParam(false));
            assertEquals(BooleanNumberConverter.toDbValue(null), 
                ActiveFlagQueryHelper.toNativeQueryParam(null));
        }
    }
}
