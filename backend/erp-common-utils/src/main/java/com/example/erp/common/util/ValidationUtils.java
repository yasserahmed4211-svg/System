package com.example.erp.common.util;

import java.util.regex.Pattern;

/**
 * Utility class للـ validations
 */
public final class ValidationUtils {
    
    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[1-9]\\d{1,14}$"
    );
    
    /**
     * التحقق من صحة البريد الإلكتروني
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * التحقق من صحة رقم الهاتف (international format)
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }
    
    /**
     * التحقق من أن القيمة في النطاق المحدد
     */
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }
    
    /**
     * التحقق من أن القيمة في النطاق المحدد
     */
    public static boolean isInRange(long value, long min, long max) {
        return value >= min && value <= max;
    }
    
    /**
     * التحقق من أن String يحتوي على أرقام فقط
     */
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.matches("\\d+");
    }
    
    /**
     * التحقق من أن String يحتوي على حروف فقط
     */
    public static boolean isAlpha(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.matches("[a-zA-Z]+");
    }
    
    /**
     * التحقق من أن String يحتوي على حروف وأرقام فقط
     */
    public static boolean isAlphanumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.matches("[a-zA-Z0-9]+");
    }
}
