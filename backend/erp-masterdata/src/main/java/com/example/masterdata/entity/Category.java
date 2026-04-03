package com.example.masterdata.entity;

import com.example.erp.common.converter.BooleanNumberConverter;
import com.example.erp.common.domain.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Category Entity - الفئة
 * 
 * Placeholder entity for FK constraint testing
 * Will be fully implemented later
 */
@Entity
@Table(name = "MD_CATEGORY",
       indexes = {
           @Index(name = "IDX_CATEGORY_ACTIVITY_FK", columnList = "ACTIVITY_ID_FK"),
           @Index(name = "IDX_CATEGORY_ACTIVE", columnList = "IS_ACTIVE")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Category extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "category_seq")
    @SequenceGenerator(name = "category_seq", sequenceName = "MD_CATEGORY_SEQ", allocationSize = 1)
    @Column(name = "ID_PK")
    private Long id;

    @NotBlank
    @Column(name = "CODE", length = 50, nullable = false)
    private String code;

    @NotBlank
    @Column(name = "NAME", length = 200, nullable = false)
    private String name;

    @Column(name = "ACTIVITY_ID_FK")
    private Long activityId;

    @NotNull
    @Column(name = "IS_ACTIVE", nullable = false)
    @Convert(converter = BooleanNumberConverter.class)
    @Builder.Default
    private Boolean isActive = true;
}
