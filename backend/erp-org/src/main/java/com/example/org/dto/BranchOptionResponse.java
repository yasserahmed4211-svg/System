package com.example.org.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Branch option for dropdown - خيار الفرع")
public class BranchOptionResponse {

    @Schema(description = "ID", example = "1")
    private Long id;

    @Schema(description = "Code", example = "BRN-001")
    private String branchCode;

    @Schema(description = "Arabic name", example = "فرع الرياض")
    private String branchNameAr;

    @Schema(description = "English name", example = "Riyadh Branch")
    private String branchNameEn;
}
