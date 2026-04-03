package com.example.security.domain;

import com.example.erp.common.converter.BooleanNumberConverter;
import com.example.erp.common.domain.TenantAuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Page Entity - Represents UI screens/pages in the system
 * Each Page auto-generates 4 CRUD permissions: VIEW, CREATE, UPDATE, DELETE
 * 
 * Pages are the DETAIL in the RBAC model, with Roles as MASTER.
 * A Role is assigned Pages, and each Page assignment automatically includes VIEW permission.
 */
@Entity
@Table(name = "SEC_PAGES",
       uniqueConstraints = {
           @UniqueConstraint(name = "UK_PAGES_TENANT_CODE", columnNames = {"TENANT_ID", "PAGE_CODE"}),
           @UniqueConstraint(name = "UK_PAGES_TENANT_ROUTE", columnNames = {"TENANT_ID", "ROUTE"})
       },
       indexes = {
           @Index(name = "IDX_PAGES_MODULE", columnList = "MODULE"),
           @Index(name = "IDX_PAGES_ACTIVE", columnList = "IS_ACTIVE")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Page extends TenantAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "page_seq")
    @SequenceGenerator(name = "page_seq", sequenceName = "SEC_PAGES_SEQ", allocationSize = 1)
    @Column(name = "ID_PK")
    private Long id;

    /** Unique page code (uppercase, e.g., USER, MENU, CONTRACT) */
    @Column(name = "PAGE_CODE", length = 50, nullable = false)
    private String pageCode;

    /** Arabic name for the page */
    @Column(name = "NAME_AR", length = 100, nullable = false)
    private String nameAr;

    /** English name for the page */
    @Column(name = "NAME_EN", length = 100, nullable = false)
    private String nameEn;

    /** Angular route path (unique) */
    @Column(name = "ROUTE", length = 200, nullable = false)
    private String route;

    /** Icon class or name */
    @Column(name = "ICON", length = 50)
    private String icon;

    /** Module grouping (e.g., SECURITY, FINANCE, HR) */
    @Column(name = "MODULE", length = 50)
    private String module;

    /** Parent page ID for hierarchical structure */
    @Column(name = "PARENT_ID_FK")
    private Long parentId;

    /** Display order for sorting */
    @Column(name = "DISPLAY_ORDER")
    private Integer displayOrder;

    /** Active status */
    @Column(name = "IS_ACTIVE")
    @Convert(converter = BooleanNumberConverter.class)
    @Builder.Default
    private Boolean active = true;

    /** Optional description */
    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    /**
     * Returns active status.
     * Note: NOT named isActive() to avoid Hibernate interpreting it as
     * a boolean property accessor and creating a phantom 'ACTIVE' column mapping.
     * Use Lombok's getActive() for the raw field value.
     */
    public Boolean getActiveStatus() {
        return active;
    }

    /** Business rules enforced before persist/update (RULE 24.8 safety net) */
    @PrePersist
    protected void onCreate() {
        if (active == null) {
            active = true;
        }
        if (pageCode != null) {
            pageCode = pageCode.toUpperCase().trim();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (pageCode != null) {
            pageCode = pageCode.toUpperCase().trim();
        }
    }
}
