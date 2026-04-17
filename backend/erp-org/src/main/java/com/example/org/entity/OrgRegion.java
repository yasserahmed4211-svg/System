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
 * Region — وحدة تجميع جغرافية اختيارية تتبع الكيان القانوني
 *
 * @author ERP Team
 */
@Entity
@Table(name = "ORG_REGION",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_ORG_REGION_CODE", columnNames = {"REGION_CODE"})
    },
    indexes = {
        @Index(name = "IDX_ORG_REGION_LEGAL_ENTITY", columnList = "LEGAL_ENTITY_ID_FK"),
        @Index(name = "IDX_ORG_REGION_ACTIVE", columnList = "IS_ACTIVE")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OrgRegion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "region_seq")
    @SequenceGenerator(name = "region_seq", sequenceName = "ORG_REGION_SEQ", allocationSize = 1)
    @Column(name = "ID_PK")
    private Long id;

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Column(name = "REGION_CODE", length = 20, nullable = false)
    private String regionCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "REGION_NAME_AR", length = 200, nullable = false)
    private String regionNameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "REGION_NAME_EN", length = 200, nullable = false)
    private String regionNameEn;

    @Size(max = 500, message = "{validation.size}")
    @Column(name = "DESCRIPTION_AR", length = 500)
    private String descriptionAr;

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
        foreignKey = @ForeignKey(name = "FK_ORG_REGION_LEGAL_ENTITY"))
    private OrgLegalEntity legalEntity;

    @OneToMany(mappedBy = "region", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrgBranch> branches = new ArrayList<>();

    // ============================================
    // Computed Counts
    // ============================================

    @Formula("(SELECT COUNT(*) FROM ORG_BRANCH b WHERE b.REGION_ID_FK = ID_PK)")
    private Integer branchCount;

    // ============================================
    // Lifecycle Callbacks
    // ============================================

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
        if (regionCode != null) {
            regionCode = regionCode.toUpperCase();
        }
        if (statusId != null) {
            statusId = statusId.toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (regionCode != null) {
            regionCode = regionCode.toUpperCase();
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
