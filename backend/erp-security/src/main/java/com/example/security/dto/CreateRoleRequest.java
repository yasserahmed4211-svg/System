package com.example.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new Role
 * 
 * Governance: BE-REQ-ROLEACCESS-001
 * Contract: role-access.contract.md - POST /api/roles
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new role")
public class CreateRoleRequest {

    @NotBlank(message = "{validation.required}")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "{validation.pattern}")
    @Schema(description = "Unique role code (uppercase)", example = "ADMIN", required = true)
    private String roleCode;

    @NotBlank(message = "{validation.required}")
    @Schema(description = "Role display name", example = "System Administrator", required = true)
    private String roleName;

    @Schema(description = "Role description", example = "Full system access role")
    private String description;

    @Schema(description = "Active status", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean active = true;
}
