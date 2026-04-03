package com.example.masterdata.dto;

import com.example.masterdata.entity.Activity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Activity Response DTO
 * 
 * Used for returning activity data to client
 * 
 * @author ERP Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Long defaultStockUomId;
    private Activity.ConversionType conversionType;
    private Boolean requiresActualWeight;
    private Boolean allowFraction;
    private Boolean isActive;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
}
