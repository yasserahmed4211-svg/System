package com.example.erp.common.config;

import org.springframework.context.annotation.Configuration;

/**
 * Audit Configuration — marker class.
 * <p>
 * Audit fields (createdAt/By, updatedAt/By) are populated by
 * {@link com.example.erp.common.audit.AuditEntityListener}, which is a plain
 * JPA {@code @EntityListeners} registered on {@link com.example.erp.common.domain.AuditableEntity}.
 * <p>
 * No Spring Data JPA Auditing ({@code @EnableJpaAuditing}) is used.
 * The auditor is resolved via {@link com.example.erp.common.util.SecurityContextHelper#getUsernameOrSystem()}.
 *
 * @author ERP Team
 */
@Configuration
public class AuditConfig {
    // Intentionally empty — AuditEntityListener handles everything via JPA lifecycle callbacks
}
