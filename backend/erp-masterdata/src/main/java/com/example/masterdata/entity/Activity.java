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
 * Activity Entity - النشاط
 * 
 * Represents a business activity in the Master Data module.
 * Activities are used to categorize and manage different types of business operations.
 * 
 * Architecture Rules:
 * - Rule 6.1: Entities are module-internal
 * - Rule 9: Database standards (Oracle naming)
 * 
 * @author ERP Team
 */
@Entity
@Table(name = "MD_ACTIVITY",
    uniqueConstraints = {
        @UniqueConstraint(name = "MD_ACTIVITY_UK", columnNames = {"CODE"})
    },
    indexes = {
        @Index(name = "IDX_MD_ACTIVITY_UOM_FK", columnList = "DEFAULT_STOCK_UOM_ID_FK"),
        @Index(name = "IDX_MD_ACTIVITY_ACTIVE", columnList = "IS_ACTIVE"),
        @Index(name = "IDX_MD_ACTIVITY_CODE", columnList = "CODE")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Activity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "activity_seq")
    @SequenceGenerator(name = "activity_seq", sequenceName = "MD_ACTIVITY_SEQ", allocationSize = 1)
    @Column(name = "ID_PK")
    private Long id;

    /**
     * Activity Code - كود النشاط
     * Must be unique and uppercase
     */
    @NotBlank(message = "Activity code is required")
    @Size(max = 50, message = "Activity code must not exceed 50 characters")
    @Column(name = "CODE", length = 50, nullable = false)
    private String code;

    /**
     * Activity Name - اسم النشاط
     */
    @NotBlank(message = "Activity name is required")
    @Size(max = 200, message = "Activity name must not exceed 200 characters")
    @Column(name = "NAME", length = 200, nullable = false)
    private String name;

    /**
     * Description - الوصف
     */
    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    /**
     * Default Stock UOM - وحدة المخزون الافتراضية
     * Foreign key to LOOKUP_DETAIL table
     */
    @Column(name = "DEFAULT_STOCK_UOM_ID_FK")
    private Long defaultStockUomId;

    /**
     * Conversion Type - نوع التحويل
     * Values: FIXED, VARIABLE
     */
    @NotNull(message = "Conversion type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "CONVERSION_TYPE", length = 20, nullable = false)
    private ConversionType conversionType;

    /**
     * Requires Actual Weight - يتطلب وزن فعلي
     * Must be true if conversion type is VARIABLE
     */
    @NotNull(message = "Requires actual weight flag is required")
    @Column(name = "REQUIRES_ACTUAL_WEIGHT", nullable = false)
    @Convert(converter = BooleanNumberConverter.class)
    private Boolean requiresActualWeight;

    /**
     * Allow Fraction - يسمح بالكسور
     */
    @NotNull(message = "Allow fraction flag is required")
    @Column(name = "ALLOW_FRACTION", nullable = false)
    @Convert(converter = BooleanNumberConverter.class)
    private Boolean allowFraction;

    /**
     * Active Status - حالة النشاط
     * 1 = Active, 0 = Inactive
     */
    @NotNull(message = "Active status is required")
    @Column(name = "IS_ACTIVE", nullable = false)
    @Convert(converter = BooleanNumberConverter.class)
    @Builder.Default
    private Boolean isActive = Boolean.TRUE;

    /**
     * Conversion Type Enum
     */
    public enum ConversionType {
        FIXED,
        VARIABLE
    }

    /**
     * Business validation before persist/update (RULE 24.8 safety net)
     */
    @PrePersist
    @PreUpdate
    public void validateBusinessRules() {
        // Rule: Code must be uppercase
        if (code != null) {
            code = code.toUpperCase();
        }

        // Rule: VARIABLE conversion requires actual weight
        if (conversionType == ConversionType.VARIABLE && !Boolean.TRUE.equals(requiresActualWeight)) {
            throw new IllegalStateException(
                "VARIABLE conversion type requires actual weight to be true"
            );
        }
    }
}
