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
@Schema(description = "Posting Master response - استجابة مستند الترحيل")
public class AccPostingMstResponse {

    @Schema(description = "Posting ID - معرف الترحيل", example = "1")
    private Long postingId;

    @Schema(description = "Branch ID - معرف الفرع", example = "1")
    private Long branchIdFk;

    @Schema(description = "Company ID - معرف الشركة", example = "1")
    private Long companyIdFk;

    @Schema(description = "Currency Code - رمز العملة", example = "SAR")
    private String currencyCode;

    @Schema(description = "Document Date - تاريخ المستند", example = "2024-07-01")
    private LocalDate docDate;

    @Schema(description = "Error Message - رسالة الخطأ")
    private String errorMessage;

    @Schema(description = "Linked Journal ID - معرف القيد المرتبط", example = "10")
    private Long finJournalIdFk;

    @Schema(description = "Reversal Posting ID - معرف ترحيل العكس", example = "5")
    private Long reversalPostingIdFk;

    @Schema(description = "Source Document ID - معرف المستند المصدر", example = "100")
    private Long sourceDocId;

    @Schema(description = "Source Document Number - رقم المستند المصدر", example = "INV-2024-001")
    private String sourceDocNo;

    @Schema(description = "Source Document Type - نوع المستند المصدر", example = "INVOICE")
    private String sourceDocType;

    @Schema(description = "Source Module - الوحدة المصدرية", example = "SALES")
    private String sourceModule;

    @Schema(description = "Status - الحالة", example = "READY_FOR_GL")
    private String status;

    @Schema(description = "Total Amount - المبلغ الإجمالي", example = "10000.00")
    private BigDecimal totalAmount;

    @Schema(description = "Number of details - عدد التفاصيل", example = "3")
    private Integer detailCount;

    @Schema(description = "Posting Details - تفاصيل الترحيل")
    private List<AccPostingDtlResponse> details;

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
