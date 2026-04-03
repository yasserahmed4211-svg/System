package com.example.erp.finance.gl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
@Schema(description = "Create Manual GL Journal request - طلب إنشاء قيد يدوي")
public class GlJournalManualCreateRequest {

    @NotNull(message = "{validation.required}")
    @Schema(description = "Journal Date - تاريخ القيد", example = "2024-01-15")
    private LocalDate journalDate;

    @Size(max = 500, message = "{validation.size}")
    @Schema(description = "Description - الوصف", example = "Monthly payroll entries")
    private String description;

    @NotEmpty(message = "{validation.not_empty}")
    @Valid
    @Schema(description = "Journal Lines - سطور القيد")
    private List<GlJournalManualLineRequest> lines;
}
