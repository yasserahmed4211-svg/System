package com.example.security.domain;

import com.example.erp.common.domain.TenantAuditableEntity;
import com.example.security.dto.PermissionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Permission Entity - Represents a single permission in the RBAC system
 * 
 * Each page permission is linked to a Page via PAGE_ID_FK for:
 * - Better query performance (JOIN instead of string parsing)
 * - Referential integrity (FK constraint)
 * - Cleaner architecture (proper relational model)
 * 
 * System permissions (not linked to pages) have PAGE_ID_FK = null
 */
@Entity
@Table(name = "PERMISSIONS",
       uniqueConstraints = {@UniqueConstraint(name="UK_PERMS_TENANT_NAME", columnNames={"TENANT_ID","NAME"})},
       indexes = {
           @Index(name = "IDX_PERMS_TENANT", columnList = "TENANT_ID"),
           @Index(name = "IDX_PERMS_NAME", columnList = "NAME"),
           @Index(name = "IDX_PERMS_PAGE_FK", columnList = "PAGE_ID_FK"),
           @Index(name = "IDX_PERMS_TYPE", columnList = "PERMISSION_TYPE")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Permission extends TenantAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME", nullable = false, length = 150)
    private String name; // PERM_<PAGE_CODE>_<TYPE>

    /**
     * Direct link to the Page entity
     * Nullable for system permissions that are not page-related
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PAGE_ID_FK", referencedColumnName = "ID_PK")
    private Page page;

    /**
     * Permission type: VIEW, CREATE, UPDATE, DELETE
     * Stored separately for efficient queries without string parsing
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "PERMISSION_TYPE", length = 20)
    private PermissionType permissionType;

    /**
     * Check if this is a page-related permission
     */
    public boolean isPagePermission() {
        return page != null;
    }

    /**
     * Check if this is a VIEW permission
     */
    public boolean isViewPermission() {
        return permissionType == PermissionType.VIEW;
    }
}
