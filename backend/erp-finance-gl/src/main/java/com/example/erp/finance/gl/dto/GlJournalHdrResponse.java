package com.example.erp.finance.gl.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "GL Journal Header response - استجابة قيد اليومية")
public class GlJournalHdrResponse {

    @Schema(description = "Journal ID - معرف القيد", example = "1")
    private Long id;

    @Schema(description = "Journal Number - رقم القيد", example = "JRN-000001")
    private String journalNo;

    @Schema(description = "Journal Date - تاريخ القيد", example = "2024-01-15")
    private LocalDate journalDate;

    @Schema(description = "Journal Type - نوع القيد", example = "MANUAL")
    private String journalTypeIdFk;

    @Schema(description = "Status - الحالة", example = "DRAFT")
    private String statusIdFk;

    @Schema(description = "Description - الوصف", example = "Monthly entries")
    private String description;

    @Schema(description = "Source Module - الوحدة المصدر", example = "GL")
    private String sourceModuleIdFk;

    @Schema(description = "Source Doc Type - نوع المستند المصدر", example = "INVOICE")
    private String sourceDocTypeId;

    @Schema(description = "Source Doc ID - معرف المستند المصدر", example = "100")
    private Long sourceDocIdFk;

    @Schema(description = "Source Posting ID - معرف الترحيل المصدر", example = "50")
    private Long sourcePostingIdFk;

    @Schema(description = "Total Debit - إجمالي المدين", example = "5000.00")
    private BigDecimal totalDebit;

    @Schema(description = "Total Credit - إجمالي الدائن", example = "5000.00")
    private BigDecimal totalCredit;

    @Schema(description = "Active flag - فعال", example = "true")
    private Boolean activeFl;

    @Schema(description = "Number of lines - عدد السطور", example = "4")
    private Integer lineCount;

    @Schema(description = "Journal lines - سطور القيد")
    private List<GlJournalLineResponse> lines;

    @Schema(description = "Created at - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Created by - أنشئ بواسطة", example = "admin")
    private String createdBy;

    @Schema(description = "Updated at - تاريخ التعديل")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "Updated by - عدل بواسطة", example = "admin")
    private String updatedBy;
}
