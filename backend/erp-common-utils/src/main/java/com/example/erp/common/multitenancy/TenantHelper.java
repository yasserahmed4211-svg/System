package com.example.erp.common.multitenancy;

import com.example.erp.common.exception.CommonErrorCodes;
import com.example.erp.common.exception.LocalizedException;
import org.springframework.http.HttpStatus;

/**
 * Helper لـ Tenant operations
 */
public final class TenantHelper {
    
    private TenantHelper() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * الحصول على tenant ID الحالي أو رمي استثناء إذا لم يكن موجود
     * Normalizes to lowercase for case-insensitive comparison
     */
    public static String requireTenant() {
        String tenant = TenantContext.getTenantId();
        if (tenant == null || tenant.isBlank()) {
            throw new LocalizedException(HttpStatus.BAD_REQUEST, CommonErrorCodes.MISSING_TENANT);
        }
        return tenant.toLowerCase(); // Normalize to lowercase
    }
    
    /**
     * الحصول على tenant ID الحالي أو إرجاع default
     * Normalizes to lowercase for case-insensitive comparison
     */
    public static String getTenantOrDefault(String defaultTenant) {
        String tenant = TenantContext.getTenantId();
        if (tenant == null || tenant.isBlank()) {
            return defaultTenant != null ? defaultTenant.toLowerCase() : null;
        }
        return tenant.toLowerCase(); // Normalize to lowercase
    }
}
