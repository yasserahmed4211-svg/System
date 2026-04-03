package com.example.masterdata.dto;

import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.example.masterdata.entity.Activity;
import com.example.masterdata.exception.MasterDataErrorCodes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Activity Create Request DTO
 * 
 * Used for creating new activity
 * 
 * Architecture Rules:
 * - Rule 7.1: DTOs for API contract
 * - Rule 7.3: Clear DTO naming
 * 
 * @author ERP Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    private String code;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    private String name;

    @Size(max = 500, message = "{validation.size}")
    private String description;

    private Long defaultStockUomId;

    @NotNull(message = "{validation.required}")
    private Activity.ConversionType conversionType;

    @NotNull(message = "{validation.required}")
    private Boolean requiresActualWeight;

    @NotNull(message = "{validation.required}")
    private Boolean allowFraction;

    /**
     * Business validation
     * Per Governance Rule 24.2: Uses LocalizedException for business rule violations
     */
    public void validate() {
        // Rule: VARIABLE conversion requires actual weight
        if (conversionType == Activity.ConversionType.VARIABLE && !Boolean.TRUE.equals(requiresActualWeight)) {
            throw new LocalizedException(
                Status.BAD_REQUEST,
                MasterDataErrorCodes.ACTIVITY_VARIABLE_REQUIRES_WEIGHT
            );
        }
    }
}
