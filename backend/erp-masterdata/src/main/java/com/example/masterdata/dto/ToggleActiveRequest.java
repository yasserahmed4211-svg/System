package com.example.masterdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for toggling active status
 * 
 * Architecture Rules:
 * - Rule 19.5: Toggle endpoint request body
 * 
 * @author ERP Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for toggling active status - طلب تبديل حالة النشاط")
public class ToggleActiveRequest {

    @NotNull(message = "{validation.required}")
    @Schema(description = "Target active status - الحالة المطلوبة", example = "true", required = true)
    private Boolean active;
}
