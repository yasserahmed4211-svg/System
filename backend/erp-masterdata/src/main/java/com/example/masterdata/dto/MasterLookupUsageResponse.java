package com.example.masterdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Usage information DTO for Master Lookup
 * 
 * Shows where the master lookup is being used
 * 
 * @author ERP Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Master lookup usage information - معلومات استخدام القائمة المرجعية")
public class MasterLookupUsageResponse {

    @Schema(description = "Master lookup ID - معرف القائمة المرجعية", example = "1")
    private Long masterLookupId;

    @Schema(description = "Lookup key - مفتاح القائمة المرجعية", example = "COLOR")
    private String lookupKey;

    @Schema(description = "Total lookup details count - إجمالي عدد القيم المرجعية", example = "10")
    private Long totalDetails;

    @Schema(description = "Active lookup details count - عدد القيم المرجعية النشطة", example = "8")
    private Long activeDetails;

    @Schema(description = "Can be deleted - يمكن حذفه", example = "false")
    private Boolean canDelete;

    @Schema(description = "Can be deactivated - يمكن إلغاء تفعيله", example = "false")
    private Boolean canDeactivate;
}
