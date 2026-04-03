package com.example.erp.finance.gl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Posting Detail response - استجابة تفصيل الترحيل")
public class AccPostingDtlResponse {

    @Schema(description = "Posting Detail ID - معرف تفصيل الترحيل", example = "1")
    private Long postingDtlId;

    @Schema(description = "Line Number - رقم السطر", example = "1")
    private Integer lineNo;

    @Schema(description = "Amount - المبلغ", example = "5000.00")
    private BigDecimal amount;

    @Schema(description = "Business Side - الجانب التجاري", example = "REVENUE")
    private String businessSide;

    @Schema(description = "Sign (1=Debit, -1=Credit) - الإشارة", example = "1")
    private Integer sign;

    @Schema(description = "Description - الوصف")
    private String description;

    @Schema(description = "Customer ID - معرف العميل", example = "100")
    private Long customerIdFk;

    @Schema(description = "Supplier ID - معرف المورد", example = "200")
    private Long supplierIdFk;

    @Schema(description = "Cost Center ID - معرف مركز التكلفة", example = "10")
    private Long costCenterIdFk;

    @Schema(description = "Contract ID - معرف العقد", example = "50")
    private Long contractIdFk;

    @Schema(description = "Item ID - معرف الصنف", example = "30")
    private Long itemIdFk;
}
