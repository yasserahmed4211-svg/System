package com.example.security.domain;

import com.example.erp.common.converter.BooleanNumberConverter;
import com.example.erp.common.domain.TenantAuditableEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.HashSet;
import java.util.Set;

/**
 * Role Entity - Represents system roles for RBAC
 * 
 * Governance: BE-REQ-ROLEACCESS-001
 * Contract: role-access.contract.md
 * 
 * Roles are the MASTER in the Role-Pages relationship.
 * Each Role can be assigned multiple Pages with VIEW + optional CRUD permissions.
 */
@Entity
@Table(name = "ROLES",
       uniqueConstraints = {
           @UniqueConstraint(name = "UK_ROLES_TENANT_NAME", columnNames = {"TENANT_ID", "NAME"})
       },
       indexes = {
           @Index(name = "IDX_ROLES_TENANT", columnList = "TENANT_ID"),
           @Index(name = "IDX_ROLES_IS_ACTIVE", columnList = "IS_ACTIVE")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Role extends TenantAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /** Role display name */
    @Column(name = "NAME", length = 60, nullable = false)
    private String roleName;

    // Fields present in API/legacy code but not stored in the current ROLES table
    @Transient
    private String roleCode;

    @Transient
    private String description;

    /**
     * Active status flag.
     * 
     * Database: IS_ACTIVE NUMBER(1) - 1=Active, 0=Inactive
     * Java: Boolean - true/false/null
     */
    @Column(name = "IS_ACTIVE", nullable = false)
    @Convert(converter = BooleanNumberConverter.class)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @JsonIgnore  // Prevent lazy loading exception during JSON serialization
    @ManyToMany(fetch = FetchType.LAZY)
        @JoinTable(name = "ROLE_PERMISSIONS",
            joinColumns = @JoinColumn(name = "ROLE_ID", referencedColumnName = "ID"),
            inverseJoinColumns = @JoinColumn(name = "PERM_ID", referencedColumnName = "ID"))
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    // Legacy compatibility - maps to roleCode for existing code
    @Deprecated
    public String getName() {
        return roleName;
    }

    @Deprecated
    public void setName(String name) {
        this.roleName = name;
    }

    /**
     * Returns active status with null-safety.
     * Returns Boolean.TRUE as default if not set.
     * Note: NOT named isActive() to avoid Hibernate interpreting it as
     * a boolean property accessor and creating a phantom 'ACTIVE' column mapping.
     */
    public Boolean getActiveStatus() {
        return active != null ? active : Boolean.TRUE;
    }

    /**
     * Activate this role
     */
    public void activate() {
        this.active = Boolean.TRUE;
    }

    /**
     * Deactivate this role
     */
    public void deactivate() {
        this.active = Boolean.FALSE;
    }
}
