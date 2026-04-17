package com.example.org.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Region response - استجابة المنطقة")
public class RegionResponse {

    @Schema(description = "ID", example = "1")
    private Long id;

    @Schema(description = "Region code", example = "RGN-001")
    private String regionCode;

    @Schema(description = "Arabic name", example = "المنطقة الوسطى")
    private String regionNameAr;

    @Schema(description = "English name", example = "Central Region")
    private String regionNameEn;

    @Schema(description = "Arabic description")
    private String descriptionAr;

    @Schema(description = "Status ID", example = "ACTIVE")
    private String statusId;

    @Schema(description = "Active status", example = "true")
    private Boolean isActive;

    @Schema(description = "Legal entity ID", example = "1")
    private Long legalEntityId;

    @Schema(description = "Legal entity display name")
    private String legalEntityDisplay;

    @Schema(description = "Branch count", example = "3")
    private Integer branchCount;

    @Schema(description = "Creation timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Created by user")
    private String createdBy;

    @Schema(description = "Last update timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "Last updated by user")
    private String updatedBy;
}
