package com.example.erp.common.domain;

import com.example.erp.common.multitenancy.TenantEntityListener;
import com.example.erp.common.multitenancy.TenantScoped;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Base entity for auditable + tenant-scoped entities.
 * <p>
 * Extends {@link AuditableEntity} (audit fields via {@code AuditEntityListener})
 * and adds multi-tenancy support via {@link TenantEntityListener}.
 * <p>
 * Use this ONLY for entities that belong to a tenant (erp-security module).
 * For business entities (masterdata, GL, etc.), extend {@link AuditableEntity} directly.
 *
 * @author ERP Team
 */
@MappedSuperclass
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class TenantAuditableEntity extends AuditableEntity implements TenantScoped {

    /**
     * Tenant identifier for multi-tenancy isolation.
     * Used exclusively by erp-security module entities.
     */
    @Column(name = "TENANT_ID", length = 64, nullable = false)
    private String tenantId;
}
