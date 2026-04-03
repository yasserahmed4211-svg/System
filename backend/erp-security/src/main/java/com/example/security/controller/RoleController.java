package com.example.security.controller;

import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.example.security.dto.*;
import com.example.security.service.RoleAccessService;
import com.example.security.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Role Access Control
 * 
 * Governance: BE-REQ-ROLEACCESS-001
 * Contract: role-access.contract.md
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Role Access Control", description = "إدارة الأدوار والصلاحيات (Role Access Control)")
public class RoleController {

    private final RoleService roleService;
    private final RoleAccessService roleAccessService;
    private final OperationCode operationCode;

    // ========================================
    // ROLE CRUD OPERATIONS
    // Contract: role-access.contract.md - Endpoints 1-5
    // ========================================

    /**
     * POST /api/roles
     * Create new role
     * 
     * Contract: role-access.contract.md - Endpoint 3
     */
    @PostMapping
    @Operation(summary = "Create new role", description = "Creates a new role with roleCode, roleName, description, and active status")
    public ResponseEntity<ApiResponse<RoleDto>> createRole(@RequestBody @Valid CreateRoleRequest req) {
        return operationCode.craftResponse(roleService.createRole(req));
    }

    /**
     * POST /api/roles/search
     * Search roles with dynamic filtering, sorting, and pagination
     * 
     * Contract: role-access.contract.md - Endpoint 1
     */
    @PostMapping("/search")
    @Operation(
        summary = "Search roles",
        description = "Search roles with dynamic filters, sorting, and pagination. "
                + "Allowed filter fields: roleName. "
                + "Allowed sort fields: id, roleName."
    )
    public ResponseEntity<ApiResponse<Page<RoleDto>>> searchRoles(@RequestBody RoleSearchContractRequest searchRequest) {
        return operationCode.craftResponse(roleService.searchRoles(searchRequest.toCommonSearchRequest()));
    }

    /**
     * GET /api/roles/{roleId}
     * Get role by ID
     * 
     * Contract: role-access.contract.md - Endpoint 2
     */
    @GetMapping("/{roleId}")
    @Operation(summary = "Get role by ID", description = "Retrieve a single role by its ID")
    public ResponseEntity<ApiResponse<RoleDto>> getRoleById(@PathVariable Long roleId) {
        return operationCode.craftResponse(roleService.getRoleById(roleId));
    }

    /**
     * PUT /api/roles/{roleId}
     * Update role (roleCode is immutable)
     * 
     * Contract: role-access.contract.md - Endpoint 4
     */
    @PutMapping("/{roleId}")
    @Operation(summary = "Update role", description = "Update an existing role's information (roleCode cannot be changed)")
    public ResponseEntity<ApiResponse<RoleDto>> updateRole(
            @PathVariable Long roleId,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        return operationCode.craftResponse(roleService.updateRole(roleId, request));
    }

    /**
     * DELETE /api/roles/{roleId}
     * Delete role
     * 
     * Contract: role-access.contract.md - Endpoint 5
     */
    @DeleteMapping("/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete role", description = "Delete a role permanently. Returns 409 if role has user assignments.")
    public void deleteRole(@PathVariable Long roleId) {
        roleService.deleteRole(roleId);
    }

    /**
     * PUT /api/roles/{roleId}/toggle-active
     * Toggle role active status
     * 
     * Contract: role-access.contract.md - Endpoint 11
     */
    @PutMapping("/{roleId}/toggle-active")
    @Operation(
        summary = "Toggle role active status",
        description = "Activate or deactivate a role. Pass {active: true} to activate, {active: false} to deactivate."
    )
    public ResponseEntity<ApiResponse<RoleDto>> toggleRoleActive(
            @PathVariable Long roleId,
            @RequestBody ToggleRoleActiveRequest request
    ) {
        return operationCode.craftResponse(roleService.toggleRoleActive(roleId, request.isActive()));
    }

    // ========================================
    // ROLE → PAGES PERMISSIONS (MASTER/DETAIL)
    // Contract: role-access.contract.md - Endpoints 6-10
    // ========================================

    /**
     * GET /api/roles/{roleId}/pages
     * Get role pages matrix
     * 
     * Contract: role-access.contract.md - Endpoint 6
     */
    @GetMapping("/{roleId}/pages")
    @Operation(
        summary = "Get role pages matrix",
        description = "Get list of assigned pages for this role with CRUD permissions. VIEW is implicit and not included."
    )
    public ResponseEntity<ApiResponse<RolePagesMatrixResponse>> getRolePages(@PathVariable Long roleId) {
        return operationCode.craftResponse(roleAccessService.getRolePages(roleId));
    }

    /**
     * POST /api/roles/{roleId}/pages
     * Add page to role
     * 
     * Contract: role-access.contract.md - Endpoint 7
     */
    @PostMapping("/{roleId}/pages")
    @Operation(
        summary = "Add page to role",
        description = "Add a page to a role with specific CRUD permissions. VIEW is ALWAYS added automatically. Permissions: CREATE, UPDATE, DELETE"
    )
    public ResponseEntity<ApiResponse<PageAssignmentResponse>> addPageToRole(
            @PathVariable Long roleId,
            @Valid @RequestBody AddPageToRoleRequest request
    ) {
        return operationCode.craftResponse(roleAccessService.addPageToRole(roleId, request));
    }

    /**
     * PUT /api/roles/{roleId}/pages
     * Bulk update role pages (FULL REPLACE)
     * 
     * Contract: role-access.contract.md - Endpoint 8
     */
    @PutMapping("/{roleId}/pages")
    @Operation(
        summary = "Bulk update role pages (replace mode)",
        description = "FULL REPLACE: Replace all page assignments for this role. VIEW is auto-added. Empty array removes all pages."
    )
    public ResponseEntity<ApiResponse<RolePagesMatrixResponse>> syncRolePages(
            @PathVariable Long roleId,
            @Valid @RequestBody SyncRolePagesRequest request
    ) {
        return operationCode.craftResponse(roleAccessService.syncRolePages(roleId, request));
    }

    /**
     * DELETE /api/roles/{roleId}/pages/{pageCode}
     * Remove page from role
     * 
     * Contract: role-access.contract.md - Endpoint 9
     */
    @DeleteMapping("/{roleId}/pages/{pageCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Remove page from role",
        description = "Remove a page from the role completely (removes VIEW + all CRUD permissions)"
    )
    public void removePageFromRole(
            @PathVariable Long roleId,
            @PathVariable String pageCode
    ) {
        roleAccessService.removePageFromRole(roleId, pageCode);
    }

    /**
     * POST /api/roles/{roleId}/copy-from/{sourceRoleId}
     * Copy permissions from another role
     * 
     * Contract: role-access.contract.md - Endpoint 10
     */
    @PostMapping("/{roleId}/copy-from/{sourceRoleId}")
    @Operation(
        summary = "Copy permissions from another role",
        description = "Copy all page permissions from source role. Replaces ALL existing assignments in target role."
    )
    public ResponseEntity<ApiResponse<CopyPermissionsResponse>> copyFromRole(
            @PathVariable Long roleId,
            @PathVariable Long sourceRoleId
    ) {
        return operationCode.craftResponse(roleAccessService.copyPermissionsFromRole(roleId, sourceRoleId));
    }
}
