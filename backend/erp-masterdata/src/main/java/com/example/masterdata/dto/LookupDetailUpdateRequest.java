package com.example.masterdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating Lookup Detail
 * 
 * Architecture Rules:
 * - Rule 7.1: DTOs for API contract
 * - Rule 7.3: Clear DTO naming (UpdateRequest suffix)
 * 
 * @author ERP Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for updating lookup detail - طلب تحديث قيمة مرجعية")
public class LookupDetailUpdateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Name (Arabic) - اسم القيمة", 
            example = "أحمر", required = true)
    private String nameAr;

    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Name (English) - اسم القيمة بالإنجليزية", 
            example = "Red")
    private String nameEn;

    @Size(max = 255, message = "{validation.size}")
    @Schema(description = "Extra value (optional) - قيمة إضافية", 
            example = "#FF0000")
    private String extraValue;

    @Schema(description = "Sort order - ترتيب العرض", 
            example = "1")
    private Integer sortOrder;
}
