package com.example.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for Copy Permissions operation
 * 
 * Governance: BE-REQ-ROLEACCESS-001
 * Contract: role-access.contract.md - POST /api/roles/{roleId}/copy-from/{sourceRoleId}
 * 
 * Returns the target role with copied assignments and source role info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response after copying permissions from another role")
public class CopyPermissionsResponse {

    @Schema(description = "Target role ID", example = "1")
    private Long roleId;

    @Schema(description = "Target role name", example = "New Accountant")
    private String roleName;

    @Schema(description = "Information about the source role")
    private SourceRoleInfo copiedFrom;

    @Schema(description = "List of copied page assignments")
    private List<PageAssignmentResponse> assignments;

    /**
     * Nested DTO for source role information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Source role information")
    public static class SourceRoleInfo {

        @Schema(description = "Source role ID", example = "2")
        private Long roleId;

        @Schema(description = "Source role name", example = "Senior Accountant")
        private String roleName;
    }
}
