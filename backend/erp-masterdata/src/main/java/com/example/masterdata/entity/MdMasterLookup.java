package com.example.masterdata.entity;

import com.example.erp.common.converter.BooleanNumberConverter;
import com.example.erp.common.domain.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.List;

/**
 * Master Lookup Entity - نوع القائمة المرجعية
 * 
 * Represents a master lookup type that contains multiple lookup detail values.
 * Examples: COLOR, GRADE, UOM, COUNTRY, CITY, PAYMENT_TERM
 * 
 * Architecture Rules:
 * - Rule 6.1: Entities are module-internal
 * - Rule 9: Database standards (Oracle naming)
 * - Rule 9.2: Primary key naming (_PK suffix)
 * 
 * @author ERP Team
 */
@Entity
@Table(name = "MD_MASTER_LOOKUP",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_MD_MASTER_LOOKUP_KEY", columnNames = {"LOOKUP_KEY"})
    },
    indexes = {
        @Index(name = "IDX_MD_MASTER_LOOKUP_ACTIVE", columnList = "IS_ACTIVE"),
        @Index(name = "IDX_MD_MASTER_LOOKUP_KEY", columnList = "LOOKUP_KEY")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MdMasterLookup extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "master_lookup_seq")
    @SequenceGenerator(name = "master_lookup_seq", sequenceName = "MD_MASTER_LOOKUP_SEQ", allocationSize = 1)
    @Column(name = "ID_PK")
    private Long id;

    /**
     * Lookup Key - مفتاح القائمة المرجعية
     * Unique identifier for the lookup type (e.g., COLOR, GRADE, UOM)
     * Must be UPPERCASE
     */
    @NotBlank(message = "Lookup key is required")
    @Size(max = 50, message = "Lookup key must not exceed 50 characters")
    @Column(name = "LOOKUP_KEY", length = 50, nullable = false)
    private String lookupKey;

    /**
     * Lookup Name (Arabic) - اسم القائمة المرجعية
     */
    @NotBlank(message = "Lookup name is required")
    @Size(max = 200, message = "Lookup name must not exceed 200 characters")
    @Column(name = "LOOKUP_NAME", length = 200, nullable = false)
    private String lookupName;

    /**
     * Lookup Name (English) - اسم القائمة المرجعية بالإنجليزية
     */
    @Size(max = 200, message = "Lookup name EN must not exceed 200 characters")
    @Column(name = "LOOKUP_NAME_EN", length = 200)
    private String lookupNameEn;

    /**
     * Description - وصف
     */
    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    /**
     * Active status - نشط / غير نشط
     * 1 = Active, 0 = Inactive
     */
    @Column(name = "IS_ACTIVE", nullable = false)
    @Builder.Default
    @Convert(converter = BooleanNumberConverter.class)
    private Boolean isActive = Boolean.TRUE;

    /**
     * Lookup Details - القيم المرجعية
     * One-to-many relationship with lookup details
     */
    @OneToMany(mappedBy = "masterLookup", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MdLookupDetail> lookupDetails = new ArrayList<>();

    /**
     * Computed detail count via SQL subquery — avoids loading the entire collection.
     * Used by mappers to display count without triggering N+1 queries.
     */
    @Formula("(SELECT COUNT(*) FROM MD_LOOKUP_DETAIL ld WHERE ld.MASTER_LOOKUP_ID_FK = ID_PK)")
    private int detailCountFormula;

    // ============================================
    // Lifecycle Callbacks (business rules only — audit handled by AuditEntityListener)
    // ============================================

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
        if (lookupKey != null) {
            lookupKey = lookupKey.toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (lookupKey != null) {
            lookupKey = lookupKey.toUpperCase();
        }
    }

    // ============================================
    // Helper Methods
    // ============================================

    /**
     * Activate the master lookup
     */
    public void activate() {
        this.isActive = Boolean.TRUE;
    }

    /**
     * Deactivate the master lookup
     */
    public void deactivate() {
        this.isActive = Boolean.FALSE;
    }

    /**
     * Get count of lookup details (uses @Formula, no lazy loading)
     */
    public int getDetailCount() {
        return detailCountFormula;
    }
}
