package com.example.erp.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for working with timestamps and dates.
 * Provides consistent timestamp formatting across all ERP modules.
 * 
 * Architecture Rule: 14.5 - Consistent Timestamp Format
 * 
 * @author ERP Team
 */
public final class TimestampUtils {

    private TimestampUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Get current timestamp as ISO-8601 string in UTC.
     * Format: 2024-01-15T10:30:45.123Z
     * 
     * @return ISO-8601 formatted timestamp string
     */
    public static String getCurrentTimestamp() {
        return Instant.now().toString();
    }

    /**
     * Convert Instant to ISO-8601 string
     * 
     * @param instant The instant to format
     * @return ISO-8601 formatted string
     */
    public static String format(Instant instant) {
        return instant != null ? instant.toString() : null;
    }

    /**
     * Convert LocalDateTime to ISO-8601 string in UTC
     * 
     * @param dateTime The LocalDateTime to format
     * @return ISO-8601 formatted string in UTC
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault())
                       .withZoneSameInstant(ZoneId.of("UTC"))
                       .format(DateTimeFormatter.ISO_INSTANT);
    }

    /**
     * Convert ZonedDateTime to ISO-8601 string
     * 
     * @param zonedDateTime The ZonedDateTime to format
     * @return ISO-8601 formatted string
     */
    public static String format(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"))
                            .format(DateTimeFormatter.ISO_INSTANT);
    }

    /**
     * Parse ISO-8601 string to Instant
     * 
     * @param timestamp ISO-8601 formatted string
     * @return Instant
     */
    public static Instant parse(String timestamp) {
        return timestamp != null ? Instant.parse(timestamp) : null;
    }

    /**
     * Get current timestamp as Instant
     * 
     * @return Current Instant
     */
    public static Instant now() {
        return Instant.now();
    }
}
