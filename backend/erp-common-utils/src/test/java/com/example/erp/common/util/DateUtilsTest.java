package com.example.erp.common.util;

import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DateUtils
 */
class DateUtilsTest {

    @Test
    void testNow_ReturnsCurrentInstant() {
        Instant before = Instant.now();
        Instant result = DateUtils.now();
        Instant after = Instant.now();
        
        assertTrue(result.isAfter(before.minusSeconds(1)));
        assertTrue(result.isBefore(after.plusSeconds(1)));
    }

    @Test
    void testToInstant_ConvertsLocalDateTimeToInstant() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 12, 27, 10, 30);
        ZoneId zone = ZoneId.of("UTC");
        
        Instant result = DateUtils.toInstant(dateTime, zone);
        
        assertNotNull(result);
        assertEquals(dateTime, DateUtils.toLocalDateTime(result, zone));
    }

    @Test
    void testToLocalDateTime_ConvertsInstantToLocalDateTime() {
        Instant instant = Instant.parse("2025-12-27T10:30:00Z");
        ZoneId zone = ZoneId.of("UTC");
        
        LocalDateTime result = DateUtils.toLocalDateTime(instant, zone);
        
        assertEquals(2025, result.getYear());
        assertEquals(12, result.getMonthValue());
        assertEquals(27, result.getDayOfMonth());
        assertEquals(10, result.getHour());
        assertEquals(30, result.getMinute());
    }

    @Test
    void testPlusDays_AddsSpecifiedDays() {
        Instant instant = Instant.parse("2025-12-27T10:00:00Z");
        
        Instant result = DateUtils.plusDays(instant, 5);
        
        assertEquals(Instant.parse("2026-01-01T10:00:00Z"), result);
    }

    @Test
    void testPlusDays_WithNegativeDays_SubtractsDays() {
        Instant instant = Instant.parse("2025-12-27T10:00:00Z");
        
        Instant result = DateUtils.plusDays(instant, -2);
        
        assertEquals(Instant.parse("2025-12-25T10:00:00Z"), result);
    }

    @Test
    void testPlusHours_AddsSpecifiedHours() {
        Instant instant = Instant.parse("2025-12-27T10:00:00Z");
        
        Instant result = DateUtils.plusHours(instant, 3);
        
        assertEquals(Instant.parse("2025-12-27T13:00:00Z"), result);
    }

    @Test
    void testPlusSeconds_AddsSpecifiedSeconds() {
        Instant instant = Instant.parse("2025-12-27T10:00:00Z");
        
        Instant result = DateUtils.plusSeconds(instant, 90);
        
        assertEquals(Instant.parse("2025-12-27T10:01:30Z"), result);
    }

    @Test
    void testIsExpired_WithPastInstant_ReturnsTrue() {
        Instant pastInstant = Instant.now().minusSeconds(60);
        
        assertTrue(DateUtils.isExpired(pastInstant));
    }

    @Test
    void testIsExpired_WithFutureInstant_ReturnsFalse() {
        Instant futureInstant = Instant.now().plusSeconds(60);
        
        assertFalse(DateUtils.isExpired(futureInstant));
    }

    @Test
    void testSecondsBetween_CalculatesCorrectDifference() {
        Instant instant1 = Instant.parse("2025-12-27T10:00:00Z");
        Instant instant2 = Instant.parse("2025-12-27T10:05:00Z");
        
        long result = DateUtils.secondsBetween(instant1, instant2);
        
        assertEquals(300, result);
    }

    @Test
    void testSecondsBetween_WithReversedOrder_ReturnsNegative() {
        Instant instant1 = Instant.parse("2025-12-27T10:05:00Z");
        Instant instant2 = Instant.parse("2025-12-27T10:00:00Z");
        
        long result = DateUtils.secondsBetween(instant1, instant2);
        
        assertEquals(-300, result);
    }

    @Test
    void testConstructor_ThrowsException() throws Exception {
        var constructor = DateUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        var exception = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            constructor.newInstance();
        });
        
        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
    }
}
