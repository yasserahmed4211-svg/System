package com.example.erp.common.multitenancy;

/**
 * ThreadLocal context للـ tenant الحالي
 * يُستخدم لتخزين واسترجاع tenant ID للـ request الحالي
 */
public class TenantContext {
    
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    
    private TenantContext() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Set tenant ID (normalized to lowercase for case-insensitive comparison)
     */
    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId != null ? tenantId.toLowerCase() : null);
    }
    
    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }
    
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
