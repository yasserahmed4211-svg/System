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
@Schema(description = "Department response - استجابة القسم")
public class DepartmentResponse {

    @Schema(description = "ID", example = "1")
    private Long id;

    @Schema(description = "Department code", example = "DEP-001")
    private String departmentCode;

    @Schema(description = "Arabic name", example = "قسم المبيعات")
    private String departmentNameAr;

    @Schema(description = "English name", example = "Sales Department")
    private String departmentNameEn;

    @Schema(description = "Department type ID", example = "SALES")
    private String departmentTypeId;

    @Schema(description = "Active status", example = "true")
    private Boolean isActive;

    @Schema(description = "Branch ID", example = "1")
    private Long branchId;

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
