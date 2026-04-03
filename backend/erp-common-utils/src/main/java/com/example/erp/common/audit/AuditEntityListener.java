package com.example.erp.common.audit;

import com.example.erp.common.domain.AuditableEntity;
import com.example.erp.common.util.SecurityContextHelper;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;

/**
 * JPA EntityListener that automatically populates audit fields on
 * any entity extending {@link AuditableEntity}.
 * <p>
 * Replaces Spring Data JPA's {@code AuditingEntityListener} with a
 * framework-independent, purely JPA-based approach.
 * <p>
 * Uses {@link SecurityContextHelper#getUsernameOrSystem()} to resolve
 * the current user (falls back to "system" when no security context exists).
 *
 * @author ERP Team
 */
public class AuditEntityListener {

    @PrePersist
    public void prePersist(AuditableEntity entity) {
        Instant now = Instant.now();
        String user = SecurityContextHelper.getUsernameOrSystem();

        entity.setCreatedAt(now);
        entity.setCreatedBy(user);
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(user);
    }

    @PreUpdate
    public void preUpdate(AuditableEntity entity) {
        entity.setUpdatedAt(Instant.now());
        entity.setUpdatedBy(SecurityContextHelper.getUsernameOrSystem());
    }
}
