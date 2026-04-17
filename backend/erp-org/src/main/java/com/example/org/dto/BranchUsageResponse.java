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
@Schema(description = "Branch usage information - معلومات استخدام الفرع")
public class BranchUsageResponse {

    @Schema(description = "Branch ID", example = "1")
    private Long branchId;

    @Schema(description = "Branch code", example = "BRN-001")
    private String branchCode;

    @Schema(description = "Active departments count", example = "3")
    private Long activeDepartments;

    @Schema(description = "Can be deactivated", example = "false")
    private Boolean canDeactivate;

    @Schema(description = "Reason if cannot deactivate")
    private String reason;
}
