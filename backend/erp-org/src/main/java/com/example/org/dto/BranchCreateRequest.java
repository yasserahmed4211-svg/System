package com.example.org.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for creating a branch - طلب إنشاء فرع")
public class BranchCreateRequest {

    @NotNull(message = "{validation.required}")
    @Schema(description = "Legal entity FK - الكيان القانوني", example = "1", required = true)
    private Long legalEntityId;

    @Schema(description = "Region FK - المنطقة (اختياري)", example = "1")
    private Long regionId;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Arabic name - الاسم بالعربية", example = "فرع الرياض", required = true)
    private String branchNameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "English name - الاسم بالإنجليزية", example = "Riyadh Branch", required = true)
    private String branchNameEn;

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Schema(description = "Branch type ID - نوع الفرع", example = "BRANCH", required = true)
    private String branchTypeId;

    @Schema(description = "Is headquarter - مقر رئيسي", example = "false")
    private Boolean isHeadquarter;

    @Size(max = 250, message = "{validation.size}")
    @Schema(description = "Address line 1")
    private String addressLine1;

    @Size(max = 250, message = "{validation.size}")
    @Schema(description = "Address line 2")
    private String addressLine2;

    @Size(max = 100, message = "{validation.size}")
    @Schema(description = "City name - المدينة")
    private String cityName;

    @Size(max = 30, message = "{validation.size}")
    @Schema(description = "Phone - الهاتف")
    private String phone;

    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Email - البريد الإلكتروني")
    private String email;

    @Valid
    @Schema(description = "Departments to create with the branch - الأقسام")
    private List<DepartmentInlineRequest> departments;
}
