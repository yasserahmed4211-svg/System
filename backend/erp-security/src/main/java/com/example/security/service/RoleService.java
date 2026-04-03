package com.example.security.service;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.example.erp.common.web.util.PageableValidator;
import com.erp.common.search.DefaultFieldValueConverter;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.example.security.constants.SecurityPermissions;
import com.example.security.domain.Permission;
import com.example.security.domain.Role;
import com.example.security.dto.CreateRoleRequest;
import com.example.security.dto.RoleDto;
import com.example.security.dto.UpdateRoleRequest;
import com.example.security.exception.SecurityErrorCodes;
import com.example.security.mapper.RoleMapper;
import com.example.erp.common.multitenancy.TenantContext;
import com.example.erp.common.multitenancy.TenantHelper;
import com.example.security.repo.PermissionRepository;
import com.example.security.repo.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for Role CRUD operations
 * 
 * Governance: BE-REQ-ROLEACCESS-001
 * Contract: role-access.contract.md
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;

    // Whitelist of allowed sort fields (Rule 17.3)
    private static final Set<String> ALLOWED_ROLE_SORT_FIELDS = Set.of(
        "id", "roleName", "name", "active", "createdAt", "updatedAt"
    );

    // Whitelist of allowed search fields for dynamic filtering
    private static final Set<String> ALLOWED_ROLE_SEARCH_FIELDS = Set.of(
        "roleName", "active"
    );

    // Client-facing sort alias (API contract uses "name" in some clients) -> entity field
    private static final Map<String, String> ROLE_SORT_ALIASES = Map.of(
        "name", "roleName"
    );

    /**
     * POST /api/roles
     * Create a new role
     * 
     * Contract: role-access.contract.md - Endpoint 3
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ROLE_CREATE)")
    public ServiceResult<RoleDto> createRole(CreateRoleRequest req) {
        String tenant = TenantHelper.requireTenant();

        // Persisted role identifier in current schema is ROLES.NAME
        String storedRoleName = req.getRoleCode().toUpperCase().trim();

        // Validate uniqueness of stored role name
        roleRepo.findByRoleNameAndTenantId(storedRoleName, tenant).ifPresent(r -> {
            throw new LocalizedException(Status.ALREADY_EXISTS, SecurityErrorCodes.DUPLICATE_ROLE_CODE, storedRoleName);
        });

        Role role = Role.builder()
                .tenantId(tenant)
            .roleName(storedRoleName)
                .permissions(new HashSet<>())
                .build();

        Role saved = roleRepo.save(role);
        log.info("Created role '{}' for tenant {}", saved.getRoleName(), tenant);
        return ServiceResult.success(RoleMapper.toDto(saved), Status.CREATED);
    }

    /**
     * GET /api/roles
     * List all roles with optional filters
     * 
     * Contract: role-access.contract.md - Endpoint 1
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ROLE_VIEW)")
    public ServiceResult<Page<RoleDto>> listRoles(String search, Boolean active, Pageable pageable) {
        // Validate sort fields (Rule 17.3)
        pageable = PageableValidator.validateSortFields(pageable, ALLOWED_ROLE_SORT_FIELDS, ROLE_SORT_ALIASES);
        
        Page<Role> roles = roleRepo.findByFilters(TenantHelper.requireTenant(), search, active, pageable);
        return ServiceResult.success(roles.map(RoleMapper::toDto));
    }

    /**
     * POST /api/roles/search
     * Dynamic search for roles with filtering, sorting, and pagination
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ROLE_VIEW)")
    public ServiceResult<Page<RoleDto>> searchRoles(SearchRequest request) {
        String tenant = TenantHelper.requireTenant();

        // Build JPA Specification from filters
        Specification<Role> spec = SpecBuilder.build(
            request,
            new SetAllowedFields(ALLOWED_ROLE_SEARCH_FIELDS),
            DefaultFieldValueConverter.INSTANCE
        );

        // Add tenant filter (security requirement)
        if (spec != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tenantId"), tenant));
        } else {
            spec = (root, query, cb) -> cb.equal(root.get("tenantId"), tenant);
        }

        // Build Pageable with validated sort fields
        Pageable pageable = PageableBuilder.from(request, ALLOWED_ROLE_SORT_FIELDS);

        Page<Role> roles = roleRepo.findAll(spec, pageable);
        return ServiceResult.success(roles.map(RoleMapper::toDto));
    }

    /**
     * GET /api/roles/{roleId}
     * Get role by ID
     * 
     * Contract: role-access.contract.md - Endpoint 2
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ROLE_VIEW)")
    public ServiceResult<RoleDto> getRoleById(Long id) {
        String tenant = TenantHelper.requireTenant();
        Role role = roleRepo.findByIdAndTenantId(id, tenant)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.ROLE_NOT_FOUND, id));
        return ServiceResult.success(RoleMapper.toDto(role));
    }

    /**
     * PUT /api/roles/{roleId}
     * Update role (roleCode is immutable)
     * 
     * Contract: role-access.contract.md - Endpoint 4
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ROLE_UPDATE)")
    public ServiceResult<RoleDto> updateRole(Long id, UpdateRoleRequest req) {
        String tenant = TenantHelper.requireTenant();
        
        Role role = roleRepo.findByIdAndTenantId(id, tenant)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.ROLE_NOT_FOUND, id));
        
        // Check if new roleName conflicts with another role
        roleRepo.findByRoleNameAndTenantId(req.getRoleName(), tenant).ifPresent(r -> {
            if (!r.getId().equals(id)) {
                throw new LocalizedException(Status.ALREADY_EXISTS, SecurityErrorCodes.DUPLICATE_ROLE_NAME, req.getRoleName());
            }
        });
        
        // Update fields (roleCode is immutable)
        role.setRoleName(req.getRoleName());
        
        Role saved = roleRepo.save(role);
        log.info("Updated role '{}' (id: {})", saved.getRoleName(), saved.getId());
        return ServiceResult.success(RoleMapper.toDto(saved), Status.UPDATED);
    }

    /**
     * DELETE /api/roles/{roleId}
     * Delete role
     * 
     * Business Prevention:
     * - Cannot delete role that is assigned to users
     * - Pre-check via hasUserAssignments() query
     * 
     * Contract: role-access.contract.md - Endpoint 5
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ROLE_DELETE)")
    public void deleteRole(Long id) {
        String tenant = TenantHelper.requireTenant();
        
        // Check if role exists
        Role role = roleRepo.findByIdAndTenantId(id, tenant)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.ROLE_NOT_FOUND, id));
        
        // Business Prevention: Check if role has user assignments (409 Conflict)
        if (roleRepo.hasUserAssignments(id, tenant)) {
            throw new LocalizedException(Status.CONFLICT, SecurityErrorCodes.ROLE_IN_USE);
        }
        
        roleRepo.delete(role);
        log.info("Deleted role '{}' (id: {})", role.getRoleName(), id);
    }

    /**
     * PATCH /api/roles/{roleId}/toggle-active
     * Toggle role active status
     * 
     * NOTE: Currently the 'active' field is @Transient (not persisted).
     * This method returns the role with the requested active status in memory only.
     * After DB migration (adding IS_ACTIVE column), remove this note.
     * 
     * Contract: role-access.contract.md - Endpoint 11
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ROLE_UPDATE)")
    public ServiceResult<RoleDto> toggleRoleActive(Long id, boolean active) {
        String tenant = TenantHelper.requireTenant();
        
        Role role = roleRepo.findByIdAndTenantId(id, tenant)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.ROLE_NOT_FOUND, id));
        
        // Set the active status
        // NOTE: Currently @Transient - won't persist until DB migration is done
        role.setActive(active);
        
        // Save the role (active field won't persist while @Transient)
        Role saved = roleRepo.save(role);
        
        // Manually set active on the saved entity for the response
        // (since @Transient fields are not persisted/retrieved)
        saved.setActive(active);
        
        log.info("Toggled role '{}' (id: {}) active status to: {}", 
            saved.getRoleName(), saved.getId(), active);
        
        return ServiceResult.success(RoleMapper.toDto(saved), Status.UPDATED);
    }

}
