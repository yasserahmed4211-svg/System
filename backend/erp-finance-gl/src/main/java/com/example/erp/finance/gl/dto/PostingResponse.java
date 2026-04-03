package com.example.erp.finance.gl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Response DTO returned by the Posting Engine after generating a journal entry.
 * Wraps the generated GlJournalHdrResponse plus the source rule metadata.
 *
 * @architecture DTO — public API response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Posting Engine execution response — نتيجة تنفيذ محرك الترحيل")
public class PostingResponse {

    @Schema(description = "Rule ID that was applied - معرف القاعدة المطبقة")
    private Long ruleId;

    @Schema(description = "Company ID - معرف الشركة")
    private Long companyIdFk;

    @Schema(description = "Source Module - الوحدة المصدرية")
    private String sourceModule;

    @Schema(description = "Source Document Type - نوع المستند المصدر")
    private String sourceDocType;

    @Schema(description = "Generated journal entry - القيد المحاسبي المولّد")
    private GlJournalHdrResponse journal;
}
