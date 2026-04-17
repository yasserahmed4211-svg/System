package com.example.org.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Legal entity option for dropdown - خيار الكيان القانوني")
public class LegalEntityOptionResponse {

    @Schema(description = "ID", example = "1")
    private Long id;

    @Schema(description = "Code", example = "LE-001")
    private String legalEntityCode;

    @Schema(description = "Arabic name", example = "شركة النور")
    private String legalEntityNameAr;

    @Schema(description = "English name", example = "Al Noor Company")
    private String legalEntityNameEn;
}
