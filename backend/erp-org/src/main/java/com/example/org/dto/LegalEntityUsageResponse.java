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
@Schema(description = "Legal entity usage information - معلومات استخدام الكيان القانوني")
public class LegalEntityUsageResponse {

    @Schema(description = "Legal entity ID", example = "1")
    private Long legalEntityId;

    @Schema(description = "Legal entity code", example = "LE-001")
    private String legalEntityCode;

    @Schema(description = "Total regions count", example = "3")
    private Long totalRegions;

    @Schema(description = "Total branches count", example = "5")
    private Long totalBranches;

    @Schema(description = "Active branches count", example = "4")
    private Long activeBranches;

    @Schema(description = "Can be deactivated", example = "false")
    private Boolean canDeactivate;

    @Schema(description = "Whether the legal entity already has financial transactions", example = "true")
    private Boolean hasFinancialTransactions;

    @Schema(description = "Reason if cannot deactivate")
    private String reason;
}
