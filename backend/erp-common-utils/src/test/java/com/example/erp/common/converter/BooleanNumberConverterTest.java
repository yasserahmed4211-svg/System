package com.example.erp.common.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BooleanNumberConverter.
 * 
 * Tests the JPA AttributeConverter that maps:
 * - Java Boolean ↔ Oracle NUMBER(1)
 * - true ↔ 1
 * - false ↔ 0
 * - null ↔ null
 */
@DisplayName("BooleanNumberConverter Tests")
class BooleanNumberConverterTest {

    private final BooleanNumberConverter converter = new BooleanNumberConverter();

    @Nested
    @DisplayName("convertToDatabaseColumn (Java → DB)")
    class ConvertToDatabaseColumn {

        @Test
        @DisplayName("null → null")
        void null_returnsNull() {
            assertNull(converter.convertToDatabaseColumn(null));
        }

        @Test
        @DisplayName("true → 1")
        void true_returnsOne() {
            assertEquals(1, converter.convertToDatabaseColumn(true));
            assertEquals(1, converter.convertToDatabaseColumn(Boolean.TRUE));
        }

        @Test
        @DisplayName("false → 0")
        void false_returnsZero() {
            assertEquals(0, converter.convertToDatabaseColumn(false));
            assertEquals(0, converter.convertToDatabaseColumn(Boolean.FALSE));
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute (DB → Java)")
    class ConvertToEntityAttribute {

        @Test
        @DisplayName("null → null")
        void null_returnsNull() {
            assertNull(converter.convertToEntityAttribute(null));
        }

        @Test
        @DisplayName("1 → true")
        void one_returnsTrue() {
            Boolean result = converter.convertToEntityAttribute(1);
            assertTrue(result);
            assertEquals(Boolean.TRUE, result);
        }

        @Test
        @DisplayName("0 → false")
        void zero_returnsFalse() {
            Boolean result = converter.convertToEntityAttribute(0);
            assertFalse(result);
            assertEquals(Boolean.FALSE, result);
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, 2, 3, 10, 100, -100, Integer.MAX_VALUE, Integer.MIN_VALUE})
        @DisplayName("Invalid values throw IllegalArgumentException")
        void invalidValue_throws(int invalidValue) {
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> converter.convertToEntityAttribute(invalidValue)
            );
            assertTrue(ex.getMessage().contains("Invalid NUMBER(1) boolean value"));
            assertTrue(ex.getMessage().contains(String.valueOf(invalidValue)));
        }
    }

    @Nested
    @DisplayName("Static utility methods")
    class StaticMethods {

        @Test
        @DisplayName("toDbValue: true → 1")
        void toDbValue_true_returnsOne() {
            assertEquals(1, BooleanNumberConverter.toDbValue(true));
        }

        @Test
        @DisplayName("toDbValue: false → 0")
        void toDbValue_false_returnsZero() {
            assertEquals(0, BooleanNumberConverter.toDbValue(false));
        }

        @Test
        @DisplayName("toDbValue: null → null")
        void toDbValue_null_returnsNull() {
            assertNull(BooleanNumberConverter.toDbValue(null));
        }

        @Test
        @DisplayName("fromDbValue: 1 → true")
        void fromDbValue_one_returnsTrue() {
            assertTrue(BooleanNumberConverter.fromDbValue(1));
        }

        @Test
        @DisplayName("fromDbValue: 0 → false")
        void fromDbValue_zero_returnsFalse() {
            assertFalse(BooleanNumberConverter.fromDbValue(0));
        }

        @Test
        @DisplayName("fromDbValue: null → null")
        void fromDbValue_null_returnsNull() {
            assertNull(BooleanNumberConverter.fromDbValue(null));
        }

        @Test
        @DisplayName("fromDbValue: invalid → throws")
        void fromDbValue_invalid_throws() {
            assertThrows(IllegalArgumentException.class, () -> BooleanNumberConverter.fromDbValue(2));
            assertThrows(IllegalArgumentException.class, () -> BooleanNumberConverter.fromDbValue(-1));
        }
    }

    @Nested
    @DisplayName("Constants")
    class Constants {

        @Test
        @DisplayName("DB_TRUE equals 1")
        void dbTrue_equalsOne() {
            assertEquals(Integer.valueOf(1), BooleanNumberConverter.DB_TRUE);
        }

        @Test
        @DisplayName("DB_FALSE equals 0")
        void dbFalse_equalsZero() {
            assertEquals(Integer.valueOf(0), BooleanNumberConverter.DB_FALSE);
        }
    }

    @Nested
    @DisplayName("Round-trip conversion")
    class RoundTrip {

        @Test
        @DisplayName("true → 1 → true")
        void trueRoundTrip() {
            Boolean original = Boolean.TRUE;
            Integer dbValue = converter.convertToDatabaseColumn(original);
            Boolean result = converter.convertToEntityAttribute(dbValue);
            assertEquals(original, result);
        }

        @Test
        @DisplayName("false → 0 → false")
        void falseRoundTrip() {
            Boolean original = Boolean.FALSE;
            Integer dbValue = converter.convertToDatabaseColumn(original);
            Boolean result = converter.convertToEntityAttribute(dbValue);
            assertEquals(original, result);
        }

        @Test
        @DisplayName("null → null → null")
        void nullRoundTrip() {
            Boolean original = null;
            Integer dbValue = converter.convertToDatabaseColumn(original);
            Boolean result = converter.convertToEntityAttribute(dbValue);
            assertEquals(original, result);
        }
    }
}
