package com.example.erp.common.util;

/**
 * Utility class للتعامل مع Strings
 */
public final class StringUtils {
    
    private StringUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * التحقق من أن String فارغة أو null
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
    
    /**
     * التحقق من أن String فارغة أو null أو تحتوي على مسافات فقط
     */
    public static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }
    
    /**
     * التحقق من أن String ليست فارغة
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * التحقق من أن String ليست فارغة وليست مسافات فقط
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
    
    /**
     * تحويل null إلى empty string
     */
    public static String defaultIfNull(String str) {
        return str == null ? "" : str;
    }
    
    /**
     * تحويل null إلى default value
     */
    public static String defaultIfNull(String str, String defaultValue) {
        return str == null ? defaultValue : str;
    }
    
    /**
     * Truncate string إلى maxLength
     */
    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength);
    }
    
    /**
     * Capitalize first letter
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
