package com.example.erp.finance.gl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "GL Journal Line request - طلب سطر قيد يومية")
public class GlJournalLineRequest {

    @NotNull(message = "{validation.required}")
    @Schema(description = "Account ID FK - معرف الحساب", example = "100")
    private Long accountIdFk;

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

    @Size(max = 500, message = "{validation.size}")
    @Schema(description = "Line Description - وصف السطر", example = "Cash payment")
    private String description;
}
