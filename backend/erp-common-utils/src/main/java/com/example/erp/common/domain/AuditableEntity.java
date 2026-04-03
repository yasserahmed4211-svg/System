package com.example.erp.common.domain;

import com.example.erp.common.audit.AuditEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Base entity providing standard audit fields via {@link AuditEntityListener}.
 * <p>
 * Use this for all business entities that do NOT need multi-tenancy.
 * For tenant-scoped entities (erp-security only), extend {@link TenantAuditableEntity} instead.
 * <p>
 * Audit fields populated automatically by {@link AuditEntityListener}:
 * <ul>
 *   <li>{@code createdAt} / {@code createdBy} — set on @PrePersist</li>
 *   <li>{@code updatedAt} / {@code updatedBy} — set on @PrePersist and @PreUpdate</li>
 * </ul>
 *
 * @author ERP Team
 */
@MappedSuperclass
@EntityListeners(AuditEntityListener.class)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class AuditableEntity {

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    private Instant createdAt;

    @Column(name = "CREATED_BY", length = 100, nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "UPDATED_AT")
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    private Instant updatedAt;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;
}
