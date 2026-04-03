package com.example.erp.common.multitenancy;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * JPA Entity Listener لتعبئة tenantId تلقائياً
 */
public class TenantEntityListener {
    
    @PrePersist
    @PreUpdate
    public void setTenant(Object entity) {
        if (entity instanceof TenantScoped tenantScoped) {
            String tenantId = TenantContext.getTenantId();
            if (tenantId != null && tenantScoped.getTenantId() == null) {
                tenantScoped.setTenantId(tenantId);
            }
        }
    }
}
