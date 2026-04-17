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
@Schema(description = "Region usage information - معلومات استخدام المنطقة")
public class RegionUsageResponse {

    @Schema(description = "Region ID", example = "1")
    private Long regionId;

    @Schema(description = "Region code", example = "RGN-001")
    private String regionCode;

    @Schema(description = "Active branches count", example = "3")
    private Long activeBranches;

    @Schema(description = "Can be deactivated", example = "false")
    private Boolean canDeactivate;

    @Schema(description = "Reason if cannot deactivate")
    private String reason;
}
