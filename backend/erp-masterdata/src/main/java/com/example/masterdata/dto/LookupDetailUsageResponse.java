package com.example.masterdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Usage information DTO for Lookup Detail
 * 
 * Shows where the lookup detail is being used
 * 
 * @author ERP Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lookup detail usage information - معلومات استخدام القيمة المرجعية")
public class LookupDetailUsageResponse {

    @Schema(description = "Lookup detail ID - معرف القيمة المرجعية", example = "1")
    private Long id;

    @Schema(description = "Detail code - كود القيمة", example = "RED")
    private String code;

        @Schema(description = "Total references count - إجمالي عدد المراجع", example = "0")
    private Long totalReferencesCount;

        @Schema(description = "Can be deleted - يمكن حذفه", example = "true")
    private Boolean canBeDeleted;

    @Schema(description = "Reason why it cannot be deleted", 
            example = "null")
    private String reason;
}
