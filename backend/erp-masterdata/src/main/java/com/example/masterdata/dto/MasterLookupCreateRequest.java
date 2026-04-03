package com.example.masterdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating Master Lookup
 * 
 * Architecture Rules:
 * - Rule 7.1: DTOs for API contract
 * - Rule 7.3: Clear DTO naming (CreateRequest suffix)
 * 
 * @author ERP Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for creating master lookup - طلب إنشاء نوع قائمة مرجعية")
public class MasterLookupCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Lookup key (UPPERCASE) - مفتاح القائمة المرجعية", 
            example = "COLOR", required = true)
    private String lookupKey;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Lookup name (Arabic) - اسم القائمة المرجعية", 
            example = "اللون", required = true)
    private String lookupName;

    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Lookup name (English) - اسم القائمة المرجعية بالإنجليزية", 
            example = "Color")
    private String lookupNameEn;

    @Size(max = 500, message = "{validation.size}")
    @Schema(description = "Description - وصف", 
            example = "Product color classification")
    private String description;

    @Schema(description = "Active status - حالة النشاط", 
            example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean isActive = true;
}
