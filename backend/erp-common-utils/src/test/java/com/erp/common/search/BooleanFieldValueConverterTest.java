package com.erp.common.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BooleanFieldValueConverter.
 * 
 * Tests the search layer converter that handles Boolean → Integer conversion
 * for fields stored as NUMBER(1) in Oracle database.
 */
@DisplayName("BooleanFieldValueConverter Tests")
class BooleanFieldValueConverterTest {

    private final BooleanFieldValueConverter converter = 
        new BooleanFieldValueConverter(Set.of("isActive", "enabled"));

    @Nested
    @DisplayName("Boolean field conversion")
    class BooleanFieldConversion {

        @Test
        @DisplayName("Boolean.TRUE → 1 for configured fields")
        void booleanTrue_returnsOne() {
            Object result = converter.convert("isActive", Boolean.TRUE, Op.EQ);
            assertEquals(1, result);
        }

        @Test
        @DisplayName("Boolean.FALSE → 0 for configured fields")
        void booleanFalse_returnsZero() {
            Object result = converter.convert("isActive", Boolean.FALSE, Op.EQ);
            assertEquals(0, result);
        }

        @Test
        @DisplayName("null → null for configured fields (allows 'ALL' queries)")
        void nullValue_returnsNull() {
            Object result = converter.convert("isActive", null, Op.EQ);
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("String value conversion")
    class StringConversion {

        @ParameterizedTest
        @ValueSource(strings = {"true", "TRUE", "True", "TrUe", "1", "yes", "YES", "Yes"})
        @DisplayName("Truthy strings → 1")
        void truthyString_returnsOne(String value) {
            Object result = converter.convert("isActive", value, Op.EQ);
            assertEquals(1, result);
        }

        @ParameterizedTest
        @ValueSource(strings = {"false", "FALSE", "False", "FaLsE", "0", "no", "NO", "No"})
        @DisplayName("Falsy strings → 0")
        void falsyString_returnsZero(String value) {
            Object result = converter.convert("isActive", value, Op.EQ);
            assertEquals(0, result);
        }

        @Test
        @DisplayName("Empty string → null")
        void emptyString_returnsNull() {
            Object result = converter.convert("isActive", "", Op.EQ);
            assertNull(result);
        }

        @Test
        @DisplayName("Whitespace string → null")
        void whitespaceString_returnsNull() {
            Object result = converter.convert("isActive", "   ", Op.EQ);
            assertNull(result);
        }

        @ParameterizedTest
        @ValueSource(strings = {"invalid", "maybe", "active", "2", "-1"})
        @DisplayName("Invalid strings throw SearchException")
        void invalidString_throws(String invalidValue) {
            SearchException ex = assertThrows(
                SearchException.class,
                () -> converter.convert("isActive", invalidValue, Op.EQ)
            );
            assertTrue(ex.getMessage().contains("Invalid boolean value"));
        }
    }

    @Nested
    @DisplayName("Numeric value conversion")
    class NumericConversion {

        @Test
        @DisplayName("Integer 1 → 1")
        void integerOne_passThrough() {
            Object result = converter.convert("isActive", 1, Op.EQ);
            assertEquals(1, result);
        }

        @Test
        @DisplayName("Integer 0 → 0")
        void integerZero_passThrough() {
            Object result = converter.convert("isActive", 0, Op.EQ);
            assertEquals(0, result);
        }

        @Test
        @DisplayName("Invalid Integer throws SearchException")
        void invalidInteger_throws() {
            assertThrows(SearchException.class, () -> converter.convert("isActive", 2, Op.EQ));
            assertThrows(SearchException.class, () -> converter.convert("isActive", -1, Op.EQ));
        }

        @Test
        @DisplayName("Long 1L → 1")
        void longOne_converts() {
            Object result = converter.convert("isActive", 1L, Op.EQ);
            assertEquals(1, result);
        }

        @Test
        @DisplayName("Long 0L → 0")
        void longZero_converts() {
            Object result = converter.convert("isActive", 0L, Op.EQ);
            assertEquals(0, result);
        }
    }

    @Nested
    @DisplayName("Non-boolean fields passthrough")
    class NonBooleanFields {

        @Test
        @DisplayName("Non-configured field returns value unchanged")
        void nonConfiguredField_passThrough() {
            Object stringValue = "test";
            Object result = converter.convert("name", stringValue, Op.EQ);
            assertEquals(stringValue, result);
        }

        @Test
        @DisplayName("Non-configured field with Boolean value unchanged")
        void nonConfiguredFieldWithBoolean_passThrough() {
            // For fields not in the boolean set, value passes through unchanged
            Object result = converter.convert("someOtherField", Boolean.TRUE, Op.EQ);
            assertEquals(Boolean.TRUE, result);
        }
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("forActiveFields() includes common boolean fields")
        void forActiveFields_includesCommonFields() {
            BooleanFieldValueConverter factoryConverter = BooleanFieldValueConverter.forActiveFields();
            
            // Test common field names
            assertEquals(1, factoryConverter.convert("isActive", true, Op.EQ));
            assertEquals(1, factoryConverter.convert("active", true, Op.EQ));
            assertEquals(1, factoryConverter.convert("enabled", true, Op.EQ));
            assertEquals(0, factoryConverter.convert("deleted", false, Op.EQ));
        }
    }

    @Nested
    @DisplayName("Different operators")
    class DifferentOperators {

        @ParameterizedTest
        @CsvSource({
            "EQ, true, 1",
            "NE, false, 0",
            "EQ, false, 0",
            "NE, true, 1"
        })
        @DisplayName("Conversion works with different operators")
        void conversionWorksWithOperators(Op op, Boolean value, Integer expected) {
            Object result = converter.convert("isActive", value, op);
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("Unsupported types")
    class UnsupportedTypes {

        @Test
        @DisplayName("Unsupported type throws SearchException")
        void unsupportedType_throws() {
            assertThrows(SearchException.class, 
                () -> converter.convert("isActive", new Object(), Op.EQ));
        }
    }
}
