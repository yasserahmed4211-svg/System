package com.example.org.entity;

import com.example.erp.common.converter.BooleanNumberConverter;
import com.example.erp.common.domain.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.List;

/**
 * Branch — الوحدة التشغيلية اليومية — كل معاملة في النظام تحمل branchFk
 *
 * @author ERP Team
 */
@Entity
@Table(name = "ORG_BRANCH",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_ORG_BRANCH_CODE", columnNames = {"BRANCH_CODE"})
    },
    indexes = {
        @Index(name = "IDX_ORG_BRANCH_LEGAL_ENTITY", columnList = "LEGAL_ENTITY_ID_FK"),
        @Index(name = "IDX_ORG_BRANCH_REGION", columnList = "REGION_ID_FK"),
        @Index(name = "IDX_ORG_BRANCH_ACTIVE", columnList = "IS_ACTIVE")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OrgBranch extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "branch_seq")
    @SequenceGenerator(name = "branch_seq", sequenceName = "ORG_BRANCH_SEQ", allocationSize = 1)
    @Column(name = "ID_PK")
    private Long id;

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Column(name = "BRANCH_CODE", length = 20, nullable = false)
    private String branchCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "BRANCH_NAME_AR", length = 200, nullable = false)
    private String branchNameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "BRANCH_NAME_EN", length = 200, nullable = false)
    private String branchNameEn;

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Column(name = "BRANCH_TYPE_ID", length = 20, nullable = false)
    private String branchTypeId;

    @Column(name = "IS_HEADQUARTER", nullable = false)
    @Builder.Default
    @Convert(converter = BooleanNumberConverter.class)
    private Boolean isHeadquarter = Boolean.FALSE;

    @Size(max = 250, message = "{validation.size}")
    @Column(name = "ADDRESS_LINE_1", length = 250)
    private String addressLine1;

    @Size(max = 250, message = "{validation.size}")
    @Column(name = "ADDRESS_LINE_2", length = 250)
    private String addressLine2;

    @Size(max = 100, message = "{validation.size}")
    @Column(name = "CITY_NAME", length = 100)
    private String cityName;

    @Size(max = 30, message = "{validation.size}")
    @Column(name = "PHONE", length = 30)
    private String phone;

    @Size(max = 200, message = "{validation.size}")
    @Column(name = "EMAIL", length = 200)
    private String email;

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Column(name = "STATUS_ID", length = 20, nullable = false)
    @Builder.Default
    private String statusId = "ACTIVE";

    @Column(name = "IS_ACTIVE", nullable = false)
    @Builder.Default
    @Convert(converter = BooleanNumberConverter.class)
    private Boolean isActive = Boolean.TRUE;

    // ============================================
    // Relationships
    // ============================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LEGAL_ENTITY_ID_FK", nullable = false,
        foreignKey = @ForeignKey(name = "FK_ORG_BRANCH_LEGAL_ENTITY"))
    private OrgLegalEntity legalEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REGION_ID_FK",
        foreignKey = @ForeignKey(name = "FK_ORG_BRANCH_REGION"))
    private OrgRegion region;

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrgDepartment> departments = new ArrayList<>();

    // ============================================
    // Computed Counts
    // ============================================

    @Formula("(SELECT COUNT(*) FROM ORG_DEPARTMENT d WHERE d.BRANCH_ID_FK = ID_PK)")
    private Integer departmentCount;

    // ============================================
    // Lifecycle Callbacks
    // ============================================

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
        if (isHeadquarter == null) {
            isHeadquarter = Boolean.FALSE;
        }
        if (branchCode != null) {
            branchCode = branchCode.toUpperCase();
        }
        if (branchTypeId != null) {
            branchTypeId = branchTypeId.toUpperCase();
        }
        if (statusId != null) {
            statusId = statusId.toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (branchCode != null) {
            branchCode = branchCode.toUpperCase();
        }
        if (branchTypeId != null) {
            branchTypeId = branchTypeId.toUpperCase();
        }
        if (statusId != null) {
            statusId = statusId.toUpperCase();
        }
    }

    // ============================================
    // Helper Methods
    // ============================================

    public void activate() {
        this.isActive = Boolean.TRUE;
    }

    public void deactivate() {
        this.isActive = Boolean.FALSE;
    }
}
