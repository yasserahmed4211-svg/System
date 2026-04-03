package com.example.security.service;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.example.erp.common.multitenancy.TenantHelper;
import com.example.security.constants.SecurityPermissions;
import com.example.security.domain.Page;
import com.example.security.domain.Permission;
import com.example.security.domain.Role;
import com.example.security.dto.*;
import com.example.security.exception.SecurityErrorCodes;
import com.example.security.repo.PageRepository;
import com.example.security.repo.PermissionRepository;
import com.example.security.repo.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing Role-Page assignments and permissions
 * 
 * Governance: BE-REQ-ROLEACCESS-001
 * Contract: role-access.contract.md
 * 
 * Core Rules:
 * 1. Role is MASTER, Pages are DETAIL
 * 2. VIEW permission is ALWAYS added when a Page is assigned to a Role
 * 3. VIEW is NOT selectable in UI and cannot be removed while Page is assigned
 * 4. CRUD permissions (CREATE, UPDATE, DELETE) are optional
 * 5. VIEW is implicit and NOT returned in API responses
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleAccessService {

    private final RoleRepository roleRepository;
    private final PageRepository pageRepository;
    private final PermissionRepository permissionRepository;

    /**
     * GET /api/roles/{roleId}/pages
     * Get all pages assigned to a role with their CRUD permission flags
     * 
     * Contract: role-access.contract.md - Endpoint 6
     * Returns: RolePagesMatrixResponse with assignments (VIEW excluded from permissions array)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ROLE_VIEW)")
    public ServiceResult<RolePagesMatrixResponse> getRolePages(Long roleId) {
        String tenantId = TenantHelper.requireTenant();

        // Fetch role with permissions
        Role role = roleRepository.findByIdWithPermissions(roleId, tenantId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.ROLE_NOT_FOUND, roleId));

        List<PageAssignmentResponse> assignments = buildPageAssignments(role, tenantId);

        return ServiceResult.success(RolePagesMatrixResponse.builder()
                .roleId(role.getId())
                .roleName(role.getRoleName())
                .assignments(assignments)
                .build());
    }

    /**
     * POST /api/roles/{roleId}/pages
     * Add a Page to a Role with specific CRUD permissions
     * VIEW permission is ALWAYS added automatically
     * 
     * Contract: role-access.contract.md - Endpoint 7
     * Returns: PageAssignmentResponse (201 Created)
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ROLE_UPDATE)")
    public ServiceResult<PageAssignmentResponse> addPageToRole(Long roleId, AddPageToRoleRequest request) {
        String tenantId = TenantHelper.requireTenant();

        // Normalize pageCode
        String pageCode = request.getPageCode().toUpperCase().trim();
        log.info("Adding page '{}' to role ID: {}", pageCode, roleId);

        // Fetch role with permissions
        Role role = roleRepository.findByIdWithPermissions(roleId, tenantId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.ROLE_NOT_FOUND, roleId));

        // Verify page exists
        Page page = pageRepository.findByPageCodeAndTenantId(pageCode, tenantId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.PAGE_NOT_FOUND_BY_CODE, pageCode));

        // Check for duplicate assignment (page already assigned to role)
        boolean hasViewForPage = role.getPermissions().stream()
                .anyMatch(p -> p.getName().equals(PermissionType.VIEW.buildPermissionKey(pageCode)));
        if (hasViewForPage) {
            throw new LocalizedException(Status.ALREADY_EXISTS, SecurityErrorCodes.PAGE_ALREADY_ASSIGNED_TO_ROLE, pageCode, role.getRoleName());
        }

        // Validate permission types
        List<String> validCrudPermissions = new ArrayList<>();
        for (String permType : request.getPermissions()) {
            String upperPermType = permType.toUpperCase().trim();
            if (!upperPermType.equals("CREATE") && !upperPermType.equals("UPDATE") && !upperPermType.equals("DELETE")) {
                throw new LocalizedException(Status.BAD_REQUEST, SecurityErrorCodes.INVALID_PERMISSION_TYPE, permType);
            }
            validCrudPermissions.add(upperPermType);
        }

        // Build permission keys: VIEW + requested CRUD
        Set<String> permKeysToAdd = new HashSet<>();
        permKeysToAdd.add(PermissionType.VIEW.buildPermissionKey(pageCode)); // ALWAYS add VIEW

        for (String permType : validCrudPermissions) {
            PermissionType type = PermissionType.valueOf(permType);
            permKeysToAdd.add(type.buildPermissionKey(pageCode));
        }

        // Fetch permissions by keys
        List<Permission> permissions = permissionRepository.findByNameInAndTenantId(
            new ArrayList<>(permKeysToAdd), 
            tenantId
        );

        if (permissions.size() != permKeysToAdd.size()) {
            throw new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.PERMISSIONS_NOT_FOUND);
        }

        // Add permissions to role
        role.getPermissions().addAll(permissions);
        roleRepository.save(role);

        log.info("Added page '{}' to role '{}' with permissions: {}", 
                pageCode, role.getRoleName(), validCrudPermissions);

        // Build response (VIEW excluded per contract)
        return ServiceResult.success(PageAssignmentResponse.builder()
                .pageCode(pageCode)
                .pageName(page.getNameEn())
                .pageNameAr(page.getNameAr())
                .permissions(validCrudPermissions)
                .build(), Status.CREATED);
    }

    /**
     * PUT /api/roles/{roleId}/pages
     * SYNC MODE: Replace all page assignments for a role (FULL REPLACE)
     * 
     * Contract: role-access.contract.md - Endpoint 8
     * Returns: RolePagesMatrixResponse
     * 
     * Rules:
     * - Every listed page MUST keep VIEW (auto-added)
     * - CRUD permissions must match exactly the request
     * - Empty assignments array removes all page access
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ROLE_UPDATE)")
    public ServiceResult<RolePagesMatrixResponse> syncRolePages(Long roleId, SyncRolePagesRequest request) {
        String tenantId = TenantHelper.requireTenant();

        log.info("Syncing pages for role ID: {} with {} assignments", roleId, request.getAssignments().size());

        // Fetch role with permissions
        Role role = roleRepository.findByIdWithPermissions(roleId, tenantId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.ROLE_NOT_FOUND, roleId));

        // Build target permission set
        Set<String> targetPermissionKeys = new HashSet<>();

        for (SyncRolePagesRequest.PageAssignmentDto assignment : request.getAssignments()) {
            String pageCode = assignment.getPageCode().toUpperCase().trim();

            // Verify page exists
            Page page = pageRepository.findByPageCodeAndTenantId(pageCode, tenantId)
                    .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.PAGE_NOT_FOUND_BY_CODE, pageCode));

            // ALWAYS add VIEW
            targetPermissionKeys.add(PermissionType.VIEW.buildPermissionKey(pageCode));

            // Validate and add requested CRUD permissions
            for (String permType : assignment.getPermissions()) {
                String upperPermType = permType.toUpperCase().trim();
                if (!upperPermType.equals("CREATE") && !upperPermType.equals("UPDATE") && !upperPermType.equals("DELETE")) {
                    throw new LocalizedException(Status.BAD_REQUEST, SecurityErrorCodes.INVALID_PERMISSION_TYPE, permType);
                }
                PermissionType type = PermissionType.valueOf(upperPermType);
                targetPermissionKeys.add(type.buildPermissionKey(pageCode));
            }
        }

        // Fetch all target permissions (if any)
        List<Permission> targetPermissions = new ArrayList<>();
        if (!targetPermissionKeys.isEmpty()) {
            targetPermissions = permissionRepository.findByNameInAndTenantId(
                new ArrayList<>(targetPermissionKeys),
                tenantId
            );

            if (targetPermissions.size() != targetPermissionKeys.size()) {
                throw new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.PERMISSIONS_NOT_FOUND);
            }
        }

        // Get current page-related permissions for this role (PERM_*_*)
        Set<Permission> currentPagePermissions = role.getPermissions().stream()
                .filter(p -> p.getName().startsWith("PERM_"))
                .collect(Collectors.toSet());

        // Remove all current page permissions
        role.getPermissions().removeAll(currentPagePermissions);

        // Add new target permissions
        role.getPermissions().addAll(targetPermissions);

        roleRepository.save(role);

        log.info("Synced role '{}': removed {} old page permissions, added {} new permissions",
                role.getRoleName(), currentPagePermissions.size(), targetPermissions.size());

        // Build response
        List<PageAssignmentResponse> assignments = buildPageAssignments(role, tenantId);
        
        return ServiceResult.success(RolePagesMatrixResponse.builder()
                .roleId(role.getId())
                .roleName(role.getRoleName())
                .assignments(assignments)
                .build(), Status.UPDATED);
    }

    /**
     * DELETE /api/roles/{roleId}/pages/{pageCode}
     * Remove a Page from a Role completely
     * Removes VIEW + CREATE + UPDATE + DELETE for that page
     * 
     * Contract: role-access.contract.md - Endpoint 9
     * Returns: void (204 No Content)
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ROLE_UPDATE)")
    public void removePageFromRole(Long roleId, String pageCode) {
        String tenantId = TenantHelper.requireTenant();

        // Normalize pageCode
        pageCode = pageCode.toUpperCase().trim();
        log.info("Removing page '{}' from role ID: {}", pageCode, roleId);

        // Fetch role with permissions
        Role role = roleRepository.findByIdWithPermissions(roleId, tenantId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.ROLE_NOT_FOUND, roleId));

        // Build permission keys to remove (all 4: VIEW + CRUD)
        Set<String> permKeysToRemove = new HashSet<>();
        for (PermissionType type : PermissionType.values()) {
            permKeysToRemove.add(type.buildPermissionKey(pageCode));
        }

        // Check if page is assigned (has VIEW permission)
        final String finalPageCode = pageCode;
        boolean hasViewForPage = role.getPermissions().stream()
                .anyMatch(p -> p.getName().equals(PermissionType.VIEW.buildPermissionKey(finalPageCode)));
        if (!hasViewForPage) {
            throw new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.PAGE_NOT_ASSIGNED_TO_ROLE, pageCode);
        }

        // Remove matching permissions
        int beforeSize = role.getPermissions().size();
        role.getPermissions().removeIf(p -> permKeysToRemove.contains(p.getName()));
        int afterSize = role.getPermissions().size();

        roleRepository.save(role);

        log.info("Removed page '{}' from role '{}' (removed {} permissions)",
                pageCode, role.getRoleName(), (beforeSize - afterSize));
    }

    /**
     * POST /api/roles/{roleId}/copy-from/{sourceRoleId}
     * Copy all pages and CRUD permissions from source role to target role
     * VIEW is auto-added for all pages
     * Replaces ALL existing assignments in target role
     * 
     * Contract: role-access.contract.md - Endpoint 10
     * Returns: CopyPermissionsResponse
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ROLE_UPDATE)")
    public ServiceResult<CopyPermissionsResponse> copyPermissionsFromRole(Long targetRoleId, Long sourceRoleId) {
        String tenantId = TenantHelper.requireTenant();

        log.info("Copying permissions from role ID: {} to role ID: {}", sourceRoleId, targetRoleId);

        if (targetRoleId.equals(sourceRoleId)) {
            throw new LocalizedException(Status.BAD_REQUEST, SecurityErrorCodes.INVALID_OPERATION, "Cannot copy permissions from a role to itself");
        }

        // Fetch both roles with permissions
        Role targetRole = roleRepository.findByIdWithPermissions(targetRoleId, tenantId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.ROLE_NOT_FOUND, targetRoleId));

        Role sourceRole = roleRepository.findByIdWithPermissions(sourceRoleId, tenantId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.ROLE_NOT_FOUND, sourceRoleId));

        // Get all page-related permissions from source role
        Set<Permission> sourcePagePermissions = sourceRole.getPermissions().stream()
                .filter(p -> p.getName().startsWith("PERM_"))
                .collect(Collectors.toSet());

        // Remove current page permissions from target role
        Set<Permission> currentTargetPagePermissions = targetRole.getPermissions().stream()
                .filter(p -> p.getName().startsWith("PERM_"))
                .collect(Collectors.toSet());

        targetRole.getPermissions().removeAll(currentTargetPagePermissions);

        // Add source permissions to target (even if empty - clears all permissions)
        targetRole.getPermissions().addAll(sourcePagePermissions);

        roleRepository.save(targetRole);

        log.info("Copied {} page permissions from role '{}' to role '{}' (replaced {} existing)",
                sourcePagePermissions.size(), sourceRole.getRoleName(), targetRole.getRoleName(),
                currentTargetPagePermissions.size());

        // Build response
        List<PageAssignmentResponse> assignments = buildPageAssignments(targetRole, tenantId);
        
        return ServiceResult.success(CopyPermissionsResponse.builder()
                .roleId(targetRole.getId())
                .roleName(targetRole.getRoleName())
                .copiedFrom(CopyPermissionsResponse.SourceRoleInfo.builder()
                        .roleId(sourceRole.getId())
                        .roleName(sourceRole.getRoleName())
                        .build())
                .assignments(assignments)
                .build());
    }

    /**
     * Helper method to build page assignments from role permissions
     * Excludes VIEW from permissions array per contract
     * 
     * OPTIMIZED: Uses PAGE_ID_FK foreign key for direct JOIN - no string parsing!
     * Performance: Single query with JOIN instead of regex parsing + batch load
     */
    private List<PageAssignmentResponse> buildPageAssignments(Role role, String tenantId) {
        // Group permissions by page - using the direct FK relationship
        Map<Long, Page> pageMap = new HashMap<>();
        Map<Long, EnumSet<PermissionType>> pagePermissionTypes = new HashMap<>();

        for (Permission perm : role.getPermissions()) {
            // Skip non-page permissions (system permissions)
            if (!perm.isPagePermission()) {
                continue;
            }

            Page page = perm.getPage();
            Long pageId = page.getId();
            PermissionType permType = perm.getPermissionType();

            // Store page reference (avoiding duplicate lookups)
            pageMap.putIfAbsent(pageId, page);

            // Track permissions for this page
            pagePermissionTypes
                .computeIfAbsent(pageId, k -> EnumSet.noneOf(PermissionType.class))
                .add(permType);
        }

        // Build response DTOs - only for pages that have VIEW permission (assigned)
        List<PageAssignmentResponse> result = new ArrayList<>(pageMap.size());
        
        for (Map.Entry<Long, Page> entry : pageMap.entrySet()) {
            Long pageId = entry.getKey();
            Page page = entry.getValue();
            EnumSet<PermissionType> permTypes = pagePermissionTypes.get(pageId);

            // Only include pages that have VIEW permission (means they are assigned)
            if (permTypes == null || !permTypes.contains(PermissionType.VIEW)) {
                continue;
            }

            // Build CRUD permissions list (VIEW excluded per contract)
            List<String> crudPermissions = new ArrayList<>(3);
            if (permTypes.contains(PermissionType.CREATE)) crudPermissions.add("CREATE");
            if (permTypes.contains(PermissionType.UPDATE)) crudPermissions.add("UPDATE");
            if (permTypes.contains(PermissionType.DELETE)) crudPermissions.add("DELETE");

            result.add(PageAssignmentResponse.builder()
                    .pageCode(page.getPageCode())
                    .pageName(page.getNameEn())
                    .pageNameAr(page.getNameAr())
                    .permissions(crudPermissions)
                    .build());
        }

        // Sort by page code for consistent ordering
        result.sort(Comparator.comparing(PageAssignmentResponse::getPageCode));
        
        return result;
    }
}
