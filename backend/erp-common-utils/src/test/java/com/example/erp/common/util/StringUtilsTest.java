package com.example.erp.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StringUtils
 */
class StringUtilsTest {

    @Test
    void testIsEmpty_WithNull_ReturnsTrue() {
        assertTrue(StringUtils.isEmpty(null));
    }

    @Test
    void testIsEmpty_WithEmptyString_ReturnsTrue() {
        assertTrue(StringUtils.isEmpty(""));
    }

    @Test
    void testIsEmpty_WithWhitespace_ReturnsFalse() {
        assertFalse(StringUtils.isEmpty(" "));
    }

    @Test
    void testIsEmpty_WithText_ReturnsFalse() {
        assertFalse(StringUtils.isEmpty("hello"));
    }

    @Test
    void testIsBlank_WithNull_ReturnsTrue() {
        assertTrue(StringUtils.isBlank(null));
    }

    @Test
    void testIsBlank_WithEmptyString_ReturnsTrue() {
        assertTrue(StringUtils.isBlank(""));
    }

    @Test
    void testIsBlank_WithWhitespace_ReturnsTrue() {
        assertTrue(StringUtils.isBlank("   "));
    }

    @Test
    void testIsBlank_WithText_ReturnsFalse() {
        assertFalse(StringUtils.isBlank("hello"));
    }

    @Test
    void testIsNotEmpty_WithNull_ReturnsFalse() {
        assertFalse(StringUtils.isNotEmpty(null));
    }

    @Test
    void testIsNotEmpty_WithText_ReturnsTrue() {
        assertTrue(StringUtils.isNotEmpty("hello"));
    }

    @Test
    void testIsNotBlank_WithWhitespace_ReturnsFalse() {
        assertFalse(StringUtils.isNotBlank("   "));
    }

    @Test
    void testIsNotBlank_WithText_ReturnsTrue() {
        assertTrue(StringUtils.isNotBlank("hello"));
    }

    @Test
    void testDefaultIfNull_WithNull_ReturnsEmptyString() {
        assertEquals("", StringUtils.defaultIfNull(null));
    }

    @Test
    void testDefaultIfNull_WithText_ReturnsText() {
        assertEquals("hello", StringUtils.defaultIfNull("hello"));
    }

    @Test
    void testDefaultIfNull_WithNullAndDefault_ReturnsDefault() {
        assertEquals("default", StringUtils.defaultIfNull(null, "default"));
    }

    @Test
    void testDefaultIfNull_WithTextAndDefault_ReturnsText() {
        assertEquals("hello", StringUtils.defaultIfNull("hello", "default"));
    }

    @Test
    void testTruncate_WithNull_ReturnsNull() {
        assertNull(StringUtils.truncate(null, 5));
    }

    @Test
    void testTruncate_WithShorterString_ReturnsOriginal() {
        assertEquals("hello", StringUtils.truncate("hello", 10));
    }

    @Test
    void testTruncate_WithLongerString_ReturnsTruncated() {
        assertEquals("hello", StringUtils.truncate("hello world", 5));
    }

    @Test
    void testTruncate_WithExactLength_ReturnsOriginal() {
        assertEquals("hello", StringUtils.truncate("hello", 5));
    }

    @Test
    void testCapitalize_WithNull_ReturnsNull() {
        assertNull(StringUtils.capitalize(null));
    }

    @Test
    void testCapitalize_WithEmptyString_ReturnsEmptyString() {
        assertEquals("", StringUtils.capitalize(""));
    }

    @Test
    void testCapitalize_WithLowercase_ReturnsCapitalized() {
        assertEquals("Hello", StringUtils.capitalize("hello"));
    }

    @Test
    void testCapitalize_WithAlreadyCapitalized_ReturnsSame() {
        assertEquals("Hello", StringUtils.capitalize("Hello"));
    }

    @Test
    void testCapitalize_WithSingleChar_ReturnsCapitalized() {
        assertEquals("H", StringUtils.capitalize("h"));
    }

    @Test
    void testConstructor_ThrowsException() throws Exception {
        var constructor = StringUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        var exception = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            constructor.newInstance();
        });
        
        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
    }
}
