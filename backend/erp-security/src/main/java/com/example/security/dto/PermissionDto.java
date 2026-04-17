package com.example.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Permission Response DTO
 * 
 * Complies with Rule 4.2 & 7.1: Never expose entities, use DTOs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Permission information")
public class PermissionDto {

    @Schema(description = "Permission ID", example = "1")
    private Long id;

    @Schema(description = "Tenant ID", example = "tenant-001")
    private String tenantId;

    @Schema(description = "Permission name", example = "PERM_USER_VIEW")
    private String name;

    @Schema(description = "Permission description", example = "View user management page")
    private String description;

    @Schema(description = "ID of the page this permission belongs to (null for system permissions)", example = "5")
    private Long pageId;

    @Schema(description = "Code of the page this permission belongs to (null for system permissions)", example = "USER")
    private String pageCode;

    @Schema(description = "Permission type: VIEW, CREATE, UPDATE, DELETE", example = "VIEW")
    private String permissionType;
}
