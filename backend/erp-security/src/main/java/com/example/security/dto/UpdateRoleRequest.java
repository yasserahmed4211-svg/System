package com.example.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing Role
 * 
 * Governance: BE-REQ-ROLEACCESS-001
 * Contract: role-access.contract.md - PUT /api/roles/{roleId}
 * 
 * Note: roleCode is immutable and not included in update requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an existing role")
public class UpdateRoleRequest {

    @NotBlank(message = "{validation.required}")
    @Schema(description = "Role display name", example = "System Administrator", required = true)
    private String roleName;

    @Schema(description = "Role description", example = "Full system access role")
    private String description;

    @Schema(description = "Active status", example = "true")
    private Boolean active;
}
