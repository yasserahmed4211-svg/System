package com.example.erp.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility class للتعامل مع التواريخ والأوقات
 */
public final class DateUtils {
    
    private DateUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_DATE_TIME;
    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_DATE;
    
    /**
     * الحصول على Instant الحالي
     */
    public static Instant now() {
        return Instant.now();
    }
    
    /**
     * تحويل LocalDateTime إلى Instant
     */
    public static Instant toInstant(LocalDateTime dateTime, ZoneId zone) {
        return dateTime.atZone(zone).toInstant();
    }
    
    /**
     * تحويل Instant إلى LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Instant instant, ZoneId zone) {
        return LocalDateTime.ofInstant(instant, zone);
    }
    
    /**
     * إضافة أيام لـ Instant
     */
    public static Instant plusDays(Instant instant, long days) {
        return instant.plus(days, ChronoUnit.DAYS);
    }
    
    /**
     * إضافة ساعات لـ Instant
     */
    public static Instant plusHours(Instant instant, long hours) {
        return instant.plus(hours, ChronoUnit.HOURS);
    }
    
    /**
     * إضافة ثواني لـ Instant
     */
    public static Instant plusSeconds(Instant instant, long seconds) {
        return instant.plusSeconds(seconds);
    }
    
    /**
     * التحقق من انتهاء صلاحية Instant
     */
    public static boolean isExpired(Instant instant) {
        return instant.isBefore(Instant.now());
    }
    
    /**
     * حساب الفرق بالثواني بين instant1 و instant2
     */
    public static long secondsBetween(Instant instant1, Instant instant2) {
        return ChronoUnit.SECONDS.between(instant1, instant2);
    }
}
