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
@Schema(description = "Region option for dropdown - خيار المنطقة")
public class RegionOptionResponse {

    @Schema(description = "ID", example = "1")
    private Long id;

    @Schema(description = "Code", example = "RGN-001")
    private String regionCode;

    @Schema(description = "Arabic name", example = "المنطقة الوسطى")
    private String regionNameAr;

    @Schema(description = "English name", example = "Central Region")
    private String regionNameEn;
}
