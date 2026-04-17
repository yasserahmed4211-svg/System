package com.example.org.entity;

import com.example.erp.common.converter.BooleanNumberConverter;
import com.example.erp.common.domain.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.List;

/**
 * Legal Entity — الكيان القانوني المسجّل رسمياً — جذر الهيكل التنظيمي
 *
 * @author ERP Team
 */
@Entity
@Table(name = "ORG_LEGAL_ENTITY",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_ORG_LEGAL_ENTITY_CODE", columnNames = {"LEGAL_ENTITY_CODE"})
    },
    indexes = {
        @Index(name = "IDX_ORG_LEGAL_ENTITY_COUNTRY", columnList = "COUNTRY_ID_FK"),
        @Index(name = "IDX_ORG_LEGAL_ENTITY_ACTIVE", columnList = "IS_ACTIVE")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OrgLegalEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "legal_entity_seq")
    @SequenceGenerator(name = "legal_entity_seq", sequenceName = "ORG_LEGAL_ENTITY_SEQ", allocationSize = 1)
    @Column(name = "ID_PK")
    private Long id;

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Column(name = "LEGAL_ENTITY_CODE", length = 20, nullable = false)
    private String legalEntityCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "LEGAL_ENTITY_NAME_AR", length = 200, nullable = false)
    private String legalEntityNameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "LEGAL_ENTITY_NAME_EN", length = 200, nullable = false)
    private String legalEntityNameEn;

    @NotNull(message = "{validation.required}")
    @Column(name = "COUNTRY_ID_FK", nullable = false)
    private Long countryId;

    @NotNull(message = "{validation.required}")
    @Column(name = "FUNCTIONAL_CURRENCY_ID_FK", nullable = false)
    private Long functionalCurrencyId;

    @Size(max = 50, message = "{validation.size}")
    @Column(name = "TAX_NUMBER", length = 50)
    private String taxNumber;

    @Size(max = 50, message = "{validation.size}")
    @Column(name = "COMMERCIAL_REG_NUMBER", length = 50)
    private String commercialRegNumber;

    @Min(value = 1, message = "{validation.min}")
    @Max(value = 12, message = "{validation.max}")
    @Column(name = "FISCAL_YEAR_START_MONTH")
    private Integer fiscalYearStartMonth;

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

    @Size(max = 500, message = "{validation.size}")
    @Column(name = "WEBSITE", length = 500)
    private String website;

    @Column(name = "IS_ACTIVE", nullable = false)
    @Builder.Default
    @Convert(converter = BooleanNumberConverter.class)
    private Boolean isActive = Boolean.TRUE;

    // ============================================
    // Relationships
    // ============================================

    @OneToMany(mappedBy = "legalEntity", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrgRegion> regions = new ArrayList<>();

    @OneToMany(mappedBy = "legalEntity", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrgBranch> branches = new ArrayList<>();

    // ============================================
    // Computed Counts
    // ============================================

    @Formula("(SELECT COUNT(*) FROM ORG_REGION r WHERE r.LEGAL_ENTITY_ID_FK = ID_PK)")
    private Integer regionCount;

    @Formula("(SELECT COUNT(*) FROM ORG_BRANCH b WHERE b.LEGAL_ENTITY_ID_FK = ID_PK)")
    private Integer branchCount;

    // ============================================
    // Lifecycle Callbacks
    // ============================================

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
        if (legalEntityCode != null) {
            legalEntityCode = legalEntityCode.toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (legalEntityCode != null) {
            legalEntityCode = legalEntityCode.toUpperCase();
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
