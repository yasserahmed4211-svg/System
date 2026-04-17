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
@Schema(description = "Request for updating a region - طلب تحديث منطقة")
public class RegionUpdateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Arabic name - الاسم بالعربية", example = "المنطقة الوسطى", required = true)
    private String regionNameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "English name - الاسم بالإنجليزية", example = "Central Region", required = true)
    private String regionNameEn;

    @Size(max = 500, message = "{validation.size}")
    @Schema(description = "Arabic description - الوصف بالعربية")
    private String descriptionAr;
}
