package com.example.erp.finance.gl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Request DTO for executing an automatic posting via the Posting Engine.
 * <p>
 * The engine looks up the active AccRuleHdr by (companyIdFk + sourceModule + sourceDocType),
 * processes each AccRuleLine ordered by priority, and generates a balanced GlJournalHdr
 * with type=AUTOMATIC and status=DRAFT.
 * <p>
 * entityMap provides dynamic entity references (e.g., CUSTOMER→123, BANK→456) used to
 * resolve customerIdFk/supplierIdFk on generated journal lines based on each rule line's entityType.
 *
 * @architecture DTO — public API for inter-module posting calls
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Posting Engine execution request — طلب تنفيذ محرك الترحيل")
public class PostingRequest {

    @NotNull(message = "{validation.required}")
    @Schema(description = "Company ID - معرف الشركة", example = "1")
    private Long companyIdFk;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Source Module - الوحدة المصدرية (e.g. SALES, PURCHASE, RENTAL)", example = "SALES")
    private String sourceModule;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Source Document Type - نوع المستند المصدر (e.g. INVOICE, RECEIPT)", example = "INVOICE")
    private String sourceDocType;

    @NotNull(message = "{validation.required}")
    @Positive(message = "{validation.positive}")
    @Schema(description = "Total Amount - المبلغ الإجمالي للعملية المصدرية", example = "10000.00")
    private BigDecimal totalAmount;

    @NotNull(message = "{validation.required}")
    @Schema(description = "Journal Date - تاريخ القيد", example = "2024-07-01")
    private LocalDate journalDate;

    @Size(max = 500, message = "{validation.size}")
    @Schema(description = "Description - الوصف", example = "Invoice #INV-2024-001 posting")
    private String description;

    @Schema(description = "Source Document ID FK - معرف المستند المصدر", example = "100")
    private Long sourceDocIdFk;

    /**
     * Dynamic entity references used to resolve customerIdFk/supplierIdFk on journal lines.
     * <p>Key = ENTITY_TYPE code (CUSTOMER, VENDOR, BANK, etc.),
     * Value = Entity primary key (e.g. customer PK, bank account PK)
     * <p>Example: {"CUSTOMER": 123, "BANK": 456}
     */
    @Schema(description = "Entity map — خريطة الكيانات (ENTITY_TYPE → PK)", example = "{\"CUSTOMER\": 123, \"BANK\": 456}")
    private Map<String, Long> entityMap;
}
