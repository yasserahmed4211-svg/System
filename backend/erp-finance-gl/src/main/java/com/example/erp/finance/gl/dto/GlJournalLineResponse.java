package com.example.erp.finance.gl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "GL Journal Line response - استجابة سطر القيد")
public class GlJournalLineResponse {

    @Schema(description = "Line ID - معرف السطر", example = "1")
    private Long id;

    @Schema(description = "Journal Header ID FK - معرف رأس القيد", example = "10")
    private Long journalIdFk;

    @Schema(description = "Line Number - رقم السطر", example = "1")
    private Integer lineNo;

    @Schema(description = "Account ID FK - معرف الحساب", example = "100")
    private Long accountIdFk;

    @Schema(description = "Account Code - رقم الحساب", example = "1101")
    private String accountCode;

    @Schema(description = "Account Name - اسم الحساب", example = "Cash")
    private String accountName;

    @Schema(description = "Debit Amount - المبلغ المدين", example = "5000.00")
    private BigDecimal debitAmount;

    @Schema(description = "Credit Amount - المبلغ الدائن", example = "0.00")
    private BigDecimal creditAmount;

    @Schema(description = "Customer ID FK - معرف العميل", example = "1")
    private Long customerIdFk;

    @Schema(description = "Supplier ID FK - معرف المورد", example = "1")
    private Long supplierIdFk;

    @Schema(description = "Cost Center ID FK - معرف مركز التكلفة", example = "1")
    private Long costCenterIdFk;

    @Schema(description = "Line Description - وصف السطر", example = "Cash payment")
    private String description;
}
