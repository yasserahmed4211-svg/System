package com.example.org.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Branch response - استجابة الفرع")
public class BranchResponse {

    @Schema(description = "ID", example = "1")
    private Long id;

    @Schema(description = "Branch code", example = "BRN-001")
    private String branchCode;

    @Schema(description = "Arabic name", example = "فرع الرياض")
    private String branchNameAr;

    @Schema(description = "English name", example = "Riyadh Branch")
    private String branchNameEn;

    @Schema(description = "Branch type ID", example = "BRANCH")
    private String branchTypeId;

    @Schema(description = "Is headquarter", example = "false")
    private Boolean isHeadquarter;

    @Schema(description = "Address line 1")
    private String addressLine1;

    @Schema(description = "Address line 2")
    private String addressLine2;

    @Schema(description = "City name")
    private String cityName;

    @Schema(description = "Phone")
    private String phone;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Status ID", example = "ACTIVE")
    private String statusId;

    @Schema(description = "Active status", example = "true")
    private Boolean isActive;

    @Schema(description = "Legal entity ID", example = "1")
    private Long legalEntityId;

    @Schema(description = "Legal entity display name")
    private String legalEntityDisplay;

    @Schema(description = "Region ID", example = "1")
    private Long regionId;

    @Schema(description = "Region display name")
    private String regionDisplay;

    @Schema(description = "Department count", example = "4")
    private Integer departmentCount;

    @Schema(description = "Departments - الأقسام")
    private List<DepartmentResponse> departments;

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
