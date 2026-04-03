package com.example.erp.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationUtils
 */
class ValidationUtilsTest {

    @Test
    void testIsValidEmail_WithNull_ReturnsFalse() {
        assertFalse(ValidationUtils.isValidEmail(null));
    }

    @Test
    void testIsValidEmail_WithValidEmail_ReturnsTrue() {
        assertTrue(ValidationUtils.isValidEmail("user@example.com"));
        assertTrue(ValidationUtils.isValidEmail("test.user@domain.co.uk"));
        assertTrue(ValidationUtils.isValidEmail("admin+tag@company.org"));
    }

    @Test
    void testIsValidEmail_WithInvalidEmail_ReturnsFalse() {
        assertFalse(ValidationUtils.isValidEmail("invalid"));
        assertFalse(ValidationUtils.isValidEmail("@example.com"));
        assertFalse(ValidationUtils.isValidEmail("user@"));
        assertFalse(ValidationUtils.isValidEmail("user @example.com"));
        assertFalse(ValidationUtils.isValidEmail("user@example"));
    }

    @Test
    void testIsValidPhone_WithNull_ReturnsFalse() {
        assertFalse(ValidationUtils.isValidPhone(null));
    }

    @Test
    void testIsValidPhone_WithValidPhone_ReturnsTrue() {
        assertTrue(ValidationUtils.isValidPhone("+1234567890"));
        assertTrue(ValidationUtils.isValidPhone("1234567890"));
        assertTrue(ValidationUtils.isValidPhone("+201234567890"));
    }

    @Test
    void testIsValidPhone_WithInvalidPhone_ReturnsFalse() {
        assertFalse(ValidationUtils.isValidPhone("0123"));  // starts with 0
        assertFalse(ValidationUtils.isValidPhone("abc123"));
        assertFalse(ValidationUtils.isValidPhone(""));
        assertFalse(ValidationUtils.isValidPhone("+01234")); // + followed by 0
    }

    @Test
    void testIsInRange_Int_WithinRange_ReturnsTrue() {
        assertTrue(ValidationUtils.isInRange(5, 1, 10));
        assertTrue(ValidationUtils.isInRange(1, 1, 10));
        assertTrue(ValidationUtils.isInRange(10, 1, 10));
    }

    @Test
    void testIsInRange_Int_OutsideRange_ReturnsFalse() {
        assertFalse(ValidationUtils.isInRange(0, 1, 10));
        assertFalse(ValidationUtils.isInRange(11, 1, 10));
        assertFalse(ValidationUtils.isInRange(-5, 1, 10));
    }

    @Test
    void testIsInRange_Long_WithinRange_ReturnsTrue() {
        assertTrue(ValidationUtils.isInRange(5L, 1L, 10L));
        assertTrue(ValidationUtils.isInRange(1L, 1L, 10L));
        assertTrue(ValidationUtils.isInRange(10L, 1L, 10L));
    }

    @Test
    void testIsInRange_Long_OutsideRange_ReturnsFalse() {
        assertFalse(ValidationUtils.isInRange(0L, 1L, 10L));
        assertFalse(ValidationUtils.isInRange(11L, 1L, 10L));
    }

    @Test
    void testIsNumeric_WithNull_ReturnsFalse() {
        assertFalse(ValidationUtils.isNumeric(null));
    }

    @Test
    void testIsNumeric_WithEmptyString_ReturnsFalse() {
        assertFalse(ValidationUtils.isNumeric(""));
    }

    @Test
    void testIsNumeric_WithNumericString_ReturnsTrue() {
        assertTrue(ValidationUtils.isNumeric("12345"));
        assertTrue(ValidationUtils.isNumeric("0"));
    }

    @Test
    void testIsNumeric_WithNonNumericString_ReturnsFalse() {
        assertFalse(ValidationUtils.isNumeric("123abc"));
        assertFalse(ValidationUtils.isNumeric("abc"));
        assertFalse(ValidationUtils.isNumeric("12.34"));
    }

    @Test
    void testIsAlpha_WithNull_ReturnsFalse() {
        assertFalse(ValidationUtils.isAlpha(null));
    }

    @Test
    void testIsAlpha_WithEmptyString_ReturnsFalse() {
        assertFalse(ValidationUtils.isAlpha(""));
    }

    @Test
    void testIsAlpha_WithAlphaString_ReturnsTrue() {
        assertTrue(ValidationUtils.isAlpha("abc"));
        assertTrue(ValidationUtils.isAlpha("ABC"));
        assertTrue(ValidationUtils.isAlpha("AbCdEf"));
    }

    @Test
    void testIsAlpha_WithNonAlphaString_ReturnsFalse() {
        assertFalse(ValidationUtils.isAlpha("abc123"));
        assertFalse(ValidationUtils.isAlpha("123"));
        assertFalse(ValidationUtils.isAlpha("abc "));
    }

    @Test
    void testIsAlphanumeric_WithNull_ReturnsFalse() {
        assertFalse(ValidationUtils.isAlphanumeric(null));
    }

    @Test
    void testIsAlphanumeric_WithEmptyString_ReturnsFalse() {
        assertFalse(ValidationUtils.isAlphanumeric(""));
    }

    @Test
    void testIsAlphanumeric_WithAlphanumericString_ReturnsTrue() {
        assertTrue(ValidationUtils.isAlphanumeric("abc123"));
        assertTrue(ValidationUtils.isAlphanumeric("ABC"));
        assertTrue(ValidationUtils.isAlphanumeric("123"));
    }

    @Test
    void testIsAlphanumeric_WithNonAlphanumericString_ReturnsFalse() {
        assertFalse(ValidationUtils.isAlphanumeric("abc-123"));
        assertFalse(ValidationUtils.isAlphanumeric("abc "));
        assertFalse(ValidationUtils.isAlphanumeric("abc@123"));
    }

    @Test
    void testConstructor_ThrowsException() throws Exception {
        var constructor = ValidationUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        var exception = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            constructor.newInstance();
        });
        
        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
    }
}
