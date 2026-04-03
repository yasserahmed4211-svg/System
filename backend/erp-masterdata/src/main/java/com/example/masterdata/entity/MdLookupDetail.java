package com.example.masterdata.entity;

import com.example.erp.common.converter.BooleanNumberConverter;
import com.example.erp.common.domain.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Lookup Detail Entity - قيمة القائمة المرجعية
 * 
 * Represents individual values within a master lookup type.
 * Examples: RED (under COLOR), METER (under UOM), EGYPT (under COUNTRY)
 * 
 * Architecture Rules:
 * - Rule 6.1: Entities are module-internal
 * - Rule 9: Database standards (Oracle naming)
 * - Rule 9.2: Foreign key naming (_FK suffix)
 * 
 * @author ERP Team
 */
@Entity
@Table(name = "MD_LOOKUP_DETAIL",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_MD_LOOKUP_DETAIL_CODE", 
            columnNames = {"MASTER_LOOKUP_ID_FK", "CODE"})
    },
    indexes = {
        @Index(name = "IDX_MD_LOOKUP_DETAIL_MASTER_FK", columnList = "MASTER_LOOKUP_ID_FK"),
        @Index(name = "IDX_MD_LOOKUP_DETAIL_ACTIVE", columnList = "IS_ACTIVE"),
        @Index(name = "IDX_MD_LOOKUP_DETAIL_CODE", columnList = "CODE"),
        @Index(name = "IDX_MD_LOOKUP_DETAIL_SORT", columnList = "SORT_ORDER")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MdLookupDetail extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lookup_detail_seq")
    @SequenceGenerator(name = "lookup_detail_seq", sequenceName = "MD_LOOKUP_DETAIL_SEQ", allocationSize = 1)
    @Column(name = "ID_PK")
    private Long id;

    /**
     * Master Lookup reference - مرجع القائمة الرئيسية
     * Foreign key to MD_MASTER_LOOKUP
     */
    @NotNull(message = "Master lookup is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MASTER_LOOKUP_ID_FK", nullable = false,
        foreignKey = @ForeignKey(name = "FK_MD_LOOKUP_DETAIL_MASTER"))
    private MdMasterLookup masterLookup;

    /**
     * Detail Code - كود القيمة
     * Unique within the same master lookup
     */
    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    @Column(name = "CODE", length = 50, nullable = false)
    private String code;

    /**
     * Name (Arabic) - اسم القيمة
     */
    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    @Column(name = "NAME_AR", length = 200, nullable = false)
    private String nameAr;

    /**
     * Name (English) - اسم القيمة بالإنجليزية
     */
    @Size(max = 200, message = "Name EN must not exceed 200 characters")
    @Column(name = "NAME_EN", length = 200)
    private String nameEn;

    /**
     * Extra Value - قيمة إضافية
     * Optional field for storing additional data
     */
    @Size(max = 255, message = "Extra value must not exceed 255 characters")
    @Column(name = "EXTRA_VALUE", length = 255)
    private String extraValue;

    /**
     * Sort Order - ترتيب العرض
     * Used for ordering lookup values in dropdowns
     */
    @Column(name = "SORT_ORDER")
    private Integer sortOrder;

    /**
     * Active status - نشط / غير نشط
     * 1 = Active, 0 = Inactive
     */
    @Column(name = "IS_ACTIVE", nullable = false)
    @Builder.Default
    @Convert(converter = BooleanNumberConverter.class)
    private Boolean isActive = Boolean.TRUE;

    // ============================================
    // Lifecycle Callbacks (business rules only — audit handled by AuditEntityListener)
    // ============================================

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }

    // ============================================
    // Helper Methods
    // ============================================

    /**
     * Activate the lookup detail
     */
    public void activate() {
        this.isActive = Boolean.TRUE;
    }

    /**
     * Deactivate the lookup detail
     */
    public void deactivate() {
        this.isActive = Boolean.FALSE;
    }

    /**
     * Get master lookup key
     */
    public String getMasterLookupKey() {
        return masterLookup != null ? masterLookup.getLookupKey() : null;
    }
}
