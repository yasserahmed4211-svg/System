package com.example.masterdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple DTO for dropdown options
 * Used for dropdowns in UI
 * 
 * @author ERP Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lookup detail option for dropdown - خيار قائمة منسدلة")
public class LookupDetailOptionResponse {

    @Schema(description = "Lookup detail ID - معرف القيمة المرجعية", example = "1")
    private Long id;

    @Schema(description = "Detail code - كود القيمة", example = "RED")
    private String code;

    @Schema(description = "Name (Arabic) - اسم القيمة", example = "أحمر")
    private String nameAr;

    @Schema(description = "Name (English) - اسم القيمة بالإنجليزية", example = "Red")
    private String nameEn;

    @Schema(description = "Extra value - قيمة إضافية", example = "#FF0000")
    private String extraValue;

    @Schema(description = "Sort order - ترتيب العرض", example = "1")
    private Integer sortOrder;
}
