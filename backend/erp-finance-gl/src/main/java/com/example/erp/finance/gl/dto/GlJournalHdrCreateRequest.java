package com.example.erp.finance.gl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Create GL Journal request - طلب إنشاء قيد يومية")
public class GlJournalHdrCreateRequest {

    @NotNull(message = "{validation.required}")
    @Schema(description = "Journal Date - تاريخ القيد", example = "2024-01-15")
    private LocalDate journalDate;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Journal Type - نوع القيد", example = "MANUAL")
    private String journalTypeIdFk;

    @Size(max = 500, message = "{validation.size}")
    @Schema(description = "Description - الوصف", example = "Monthly payroll entries")
    private String description;

    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Source Module - الوحدة المصدر", example = "GL")
    private String sourceModuleIdFk;

    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Source Doc Type - نوع المستند المصدر", example = "INVOICE")
    private String sourceDocTypeId;

    @Schema(description = "Source Doc ID FK - معرف المستند المصدر", example = "100")
    private Long sourceDocIdFk;

    @Schema(description = "Source Posting ID FK - معرف الترحيل المصدر", example = "50")
    private Long sourcePostingIdFk;

    @NotEmpty(message = "{validation.not_empty}")
    @Valid
    @Schema(description = "Journal Lines - سطور القيد")
    private List<GlJournalLineRequest> lines;
}
