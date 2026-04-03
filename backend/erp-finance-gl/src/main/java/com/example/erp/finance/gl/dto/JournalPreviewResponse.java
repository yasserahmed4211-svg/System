package com.example.erp.finance.gl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Preview response for a journal that would be generated from a posting.
 * Contains the simulated journal lines with account details and balance info,
 * WITHOUT persisting anything to the database.
 *
 * @architecture DTO — public API response for preview-journal endpoint
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Journal preview response — معاينة القيد قبل التوليد")
public class JournalPreviewResponse {

    @Schema(description = "Rule ID that would be applied - معرف القاعدة المطبقة")
    private Long ruleId;

    @Schema(description = "Company ID - معرف الشركة")
    private Long companyIdFk;

    @Schema(description = "Source Module - الوحدة المصدرية")
    private String sourceModule;

    @Schema(description = "Source Document Type - نوع المستند المصدر")
    private String sourceDocType;

    @Schema(description = "Whether the preview journal is balanced (DR == CR) - هل القيد متوازن")
    private Boolean isBalanced;

    @Schema(description = "Total Debit - إجمالي المدين")
    private BigDecimal totalDebit;

    @Schema(description = "Total Credit - إجمالي الدائن")
    private BigDecimal totalCredit;

    @Schema(description = "Preview journal lines - سطور القيد المتوقعة")
    private List<GlJournalLineResponse> lines;

    @Schema(description = "Journal description - وصف القيد")
    private String description;
}
