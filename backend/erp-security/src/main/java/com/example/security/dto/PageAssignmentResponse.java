package com.example.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for a single Page Assignment
 * 
 * Governance: BE-REQ-ROLEACCESS-001
 * Contract: role-access.contract.md
 * 
 * Used in:
 * - GET /api/roles/{roleId}/pages (as part of RolePagesMatrixResponse)
 * - POST /api/roles/{roleId}/pages (as direct response)
 * - PUT /api/roles/{roleId}/pages (as part of RolePagesMatrixResponse)
 * 
 * Note: VIEW permission is implicit and NOT included in permissions array.
 * Only CRUD permissions (CREATE, UPDATE, DELETE) are returned.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Page assignment with permission flags")
public class PageAssignmentResponse {

    @Schema(description = "Page code", example = "USER")
    private String pageCode;

    @Schema(description = "Page name (English)", example = "User Management")
    private String pageName;

    @Schema(description = "Page name (Arabic)", example = "إدارة المستخدمين")
    private String pageNameAr;

    @Schema(description = "CRUD permissions (VIEW is implicit, only CREATE/UPDATE/DELETE returned)", 
            example = "[\"CREATE\", \"UPDATE\"]")
    private List<String> permissions;
}
