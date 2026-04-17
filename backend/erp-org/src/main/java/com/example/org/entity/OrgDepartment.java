package com.example.org.entity;

import com.example.erp.common.converter.BooleanNumberConverter;
import com.example.erp.common.domain.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Department — تصنيف وظيفي داخل الفرع — يُدار كـ Inline مع الفرع في الواجهة
 *
 * @author ERP Team
 */
@Entity
@Table(name = "ORG_DEPARTMENT",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_ORG_DEPARTMENT_CODE", columnNames = {"DEPARTMENT_CODE"})
    },
    indexes = {
        @Index(name = "IDX_ORG_DEPARTMENT_BRANCH", columnList = "BRANCH_ID_FK"),
        @Index(name = "IDX_ORG_DEPARTMENT_ACTIVE", columnList = "IS_ACTIVE")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OrgDepartment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "department_seq")
    @SequenceGenerator(name = "department_seq", sequenceName = "ORG_DEPARTMENT_SEQ", allocationSize = 1)
    @Column(name = "ID_PK")
    private Long id;

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Column(name = "DEPARTMENT_CODE", length = 20, nullable = false)
    private String departmentCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "DEPARTMENT_NAME_AR", length = 200, nullable = false)
    private String departmentNameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "DEPARTMENT_NAME_EN", length = 200, nullable = false)
    private String departmentNameEn;

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Column(name = "DEPARTMENT_TYPE_ID", length = 20, nullable = false)
    private String departmentTypeId;

    @Column(name = "IS_ACTIVE", nullable = false)
    @Builder.Default
    @Convert(converter = BooleanNumberConverter.class)
    private Boolean isActive = Boolean.TRUE;

    // ============================================
    // Relationships
    // ============================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BRANCH_ID_FK", nullable = false,
        foreignKey = @ForeignKey(name = "FK_ORG_DEPARTMENT_BRANCH"))
    private OrgBranch branch;

    // ============================================
    // Lifecycle Callbacks
    // ============================================

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
        if (departmentCode != null) {
            departmentCode = departmentCode.toUpperCase();
        }
        if (departmentTypeId != null) {
            departmentTypeId = departmentTypeId.toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (departmentCode != null) {
            departmentCode = departmentCode.toUpperCase();
        }
        if (departmentTypeId != null) {
            departmentTypeId = departmentTypeId.toUpperCase();
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
