package com.example.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for Role Pages Matrix
 * 
 * Governance: BE-REQ-ROLEACCESS-001
 * Contract: role-access.contract.md - GET/PUT /api/roles/{roleId}/pages
 * 
 * Returns the complete page assignments for a role with permission arrays.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role pages matrix response with all page assignments")
public class RolePagesMatrixResponse {

    @Schema(description = "Role ID", example = "1")
    private Long roleId;

    @Schema(description = "Role display name", example = "System Administrator")
    private String roleName;

    @Schema(description = "List of page assignments with permissions")
    private List<PageAssignmentResponse> assignments;
}
