package com.example.erp.common.multitenancy;

/**
 * واجهة للكيانات التي تدعم multi-tenancy
 */
public interface TenantScoped {
    String getTenantId();
    void setTenantId(String tenantId);
}
