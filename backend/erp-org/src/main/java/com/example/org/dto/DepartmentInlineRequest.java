package com.example.org.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Inline department request (used within branch create/update) - طلب قسم مضمّن")
public class DepartmentInlineRequest {

    @Schema(description = "Department ID (null for new, existing ID for update) - معرف القسم", example = "1")
    private Long id;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Arabic name - الاسم بالعربية", example = "قسم المبيعات", required = true)
    private String departmentNameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "English name - الاسم بالإنجليزية", example = "Sales Department", required = true)
    private String departmentNameEn;

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Schema(description = "Department type ID - نوع القسم", example = "SALES", required = true)
    private String departmentTypeId;
}
