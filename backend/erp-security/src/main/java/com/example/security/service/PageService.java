package com.example.security.service;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.example.erp.common.multitenancy.TenantHelper;
import com.example.erp.common.web.util.PageableValidator;
import com.erp.common.search.DefaultFieldValueConverter;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.example.security.constants.SecurityPermissions;
import com.example.security.domain.Page;
import com.example.security.domain.Permission;
import com.example.security.dto.*;
import com.example.security.exception.SecurityErrorCodes;
import com.example.security.repo.PageRepository;
import com.example.security.repo.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for managing Pages (UI Screens Registry)
 * 
 * ====================================================
 * FINAL AGREED RBAC DESIGN
 * ====================================================
 * 
 * Core responsibilities:
 * 1) Register and manage UI Pages (pageCode, route, icon, menu structure, active flag)
 * 2) Create Permission RECORDS in database (PERM_<CODE>_VIEW/CREATE/UPDATE/DELETE)
 * 3) Enforce uniqueness constraints (pageCode, route)
 * 4) Tenant isolation
 * 
 * IMPORTANT DISTINCTIONS:
 * -------------------------
 * - Permission RECORD: Database entry in PERMISSIONS table (definition only)
 * - Permission ASSIGNMENT: Linking Permission to a Role (happens ONLY in RoleAccessService)
 * 
 * PageService is NOT responsible for:
 * - Assigning permissions to Roles
 * - Managing Role-Permission relationships
 * - Determining which permissions are granted to users
 * 
 * VIEW Permission Rule:
 * - VIEW permission is created as a RECORD during page registration
 * - VIEW is ALWAYS assigned automatically when a page is added to a role (in RoleAccessService)
 * - VIEW is NOT selectable in UI (implicit and mandatory)
 * - VIEW controls: screen access, menu visibility, search/query access
 * 
 * CREATE/UPDATE/DELETE Permissions:
 * - Created as RECORDS during page registration
 * - Assigned to roles ONLY if explicitly selected by user (in RoleAccessService)
 * - User has full control over these permissions via UI
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PageService {

    private final PageRepository pageRepository;
    private final PermissionRepository permissionRepository;

    // Whitelist of allowed sort fields (Rule 17.3)
    private static final Set<String> ALLOWED_PAGE_SORT_FIELDS = Set.of(
        "id", "pageCode", "nameAr", "nameEn", "module", "displayOrder", "createdAt", "updatedAt"
    );

    // Whitelist of allowed search fields for dynamic filtering
    private static final Set<String> ALLOWED_PAGE_SEARCH_FIELDS = Set.of(
        "pageCode", "nameAr", "nameEn", "module", "active"
    );

    /**
     * Create a new Page and auto-create 4 permission RECORDS
     * 
     * IMPORTANT: This method creates:
     * 1) Page entity in SEC_PAGES table
     * 2) Permission RECORDS in PERMISSIONS table (definitions only)
     * 
     * This method does NOT:
     * - Assign permissions to any Role
     * - Grant access to any user
     * - Imply that CRUD permissions are granted automatically
     * 
     * Permission ASSIGNMENT to Roles happens via RoleAccessService.addPageToRole()
     * 
     * @param request CreatePageRequest with page details
     * @return PageResponse with generated permission keys (for reference only)
     */
    @Transactional
        @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).PAGE_CREATE)")
    public ServiceResult<PageResponse> createPage(CreatePageRequest request) {
        String tenantId = TenantHelper.requireTenant();
        
        // Normalize pageCode to uppercase
        String pageCode = request.getPageCode().toUpperCase().trim();
        log.info("Creating page '{}' for tenant: {}", pageCode, tenantId);

        // ENHANCED VALIDATION: Check pageCode format (must be alphanumeric + underscore only)
        if (!pageCode.matches("^[A-Z0-9_]+$")) {
            throw new LocalizedException(Status.BAD_REQUEST, SecurityErrorCodes.INVALID_PAGE_CODE_FORMAT, pageCode);
        }

        // ENHANCED VALIDATION: pageCode length (between 2 and 50 characters)
        if (pageCode.length() < 2 || pageCode.length() > 50) {
            throw new LocalizedException(Status.BAD_REQUEST, SecurityErrorCodes.INVALID_PAGE_CODE_LENGTH, pageCode.length());
        }

        // Check for duplicate pageCode
        if (pageRepository.existsByPageCodeAndTenantId(pageCode, tenantId)) {
            throw new LocalizedException(Status.ALREADY_EXISTS, SecurityErrorCodes.DUPLICATE_PAGE_CODE, pageCode);
        }

        // Validate route format
        String route = request.getRoute().trim();
        validateRouteFormat(route);

        // Check for duplicate route
        if (pageRepository.existsByRouteAndTenantId(route, tenantId)) {
            throw new LocalizedException(Status.ALREADY_EXISTS, SecurityErrorCodes.DUPLICATE_ROUTE, route);
        }

        // ENHANCED VALIDATION: Validate parent page exists if parentId is provided
        if (request.getParentId() != null) {
            pageRepository.findByIdAndTenantId(request.getParentId(), tenantId)
                    .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.PARENT_PAGE_NOT_FOUND, request.getParentId()));
        }

        // Create Page entity
        Page page = Page.builder()
                .tenantId(tenantId)
                .pageCode(pageCode)
                .nameAr(request.getNameAr())
                .nameEn(request.getNameEn())
                .route(route)
                .icon(request.getIcon())
                .module(request.getModule())
                .parentId(request.getParentId())
                .displayOrder(request.getDisplayOrder())
                .active(request.getActive() != null ? request.getActive() : true)
                .description(request.getDescription())
                .build();

        Page savedPage = pageRepository.save(page);
        log.info("Page created with ID: {}", savedPage.getId());

        // Create permission RECORDS linked to the page (definitions only, no role assignment)
        Map<String, String> permissionKeys = createPermissionRecords(savedPage, tenantId);
        log.info("Auto-generated {} permission records for page: {}", permissionKeys.size(), pageCode);

        return ServiceResult.success(toResponse(savedPage, permissionKeys), Status.CREATED);
    }

    /**
     * Update an existing Page
     * 
     * Note: pageCode cannot be changed (it's the stable identifier)
     * 
     * @param id Page ID
     * @param request UpdatePageRequest
     * @return Updated PageResponse
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).PAGE_UPDATE)")
    public ServiceResult<PageResponse> updatePage(Long id, UpdatePageRequest request) {
        String tenantId = TenantHelper.requireTenant();

        Page page = pageRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.PAGE_NOT_FOUND, id));

        log.info("Updating page ID: {} (code: {})", id, page.getPageCode());

        // Validate route format
        String route = request.getRoute().trim();
        validateRouteFormat(route);

        // Check route uniqueness (excluding this page)
        if (pageRepository.existsByRouteAndTenantIdAndIdNot(route, tenantId, id)) {
            throw new LocalizedException(Status.ALREADY_EXISTS, SecurityErrorCodes.DUPLICATE_ROUTE, route);
        }

        // ENHANCED VALIDATION: Validate parent page exists if parentId is provided
        if (request.getParentId() != null) {
            // Prevent self-reference
            if (request.getParentId().equals(id)) {
                throw new LocalizedException(Status.BAD_REQUEST, SecurityErrorCodes.INVALID_PARENT_PAGE);
            }
            
            pageRepository.findByIdAndTenantId(request.getParentId(), tenantId)
                    .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.PARENT_PAGE_NOT_FOUND, request.getParentId()));
        }

        // Update fields
        page.setNameAr(request.getNameAr());
        page.setNameEn(request.getNameEn());
        page.setRoute(route);
        page.setIcon(request.getIcon());
        page.setModule(request.getModule());
        page.setParentId(request.getParentId());
        page.setDisplayOrder(request.getDisplayOrder());
        page.setDescription(request.getDescription());

        Page updated = pageRepository.save(page);
        log.info("Page updated successfully: {}", id);

        // Get existing permission keys
        Map<String, String> permissionKeys = buildPermissionKeys(page.getPageCode());

        return ServiceResult.success(toResponse(updated, permissionKeys), Status.UPDATED);
    }

    /**
     * Get Page by ID
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).PAGE_VIEW)")
    public ServiceResult<PageResponse> getPageById(Long id) {
        String tenantId = TenantHelper.requireTenant();

        Page page = pageRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.PAGE_NOT_FOUND, id));

        Map<String, String> permissionKeys = buildPermissionKeys(page.getPageCode());
        return ServiceResult.success(toResponse(page, permissionKeys));
    }

    /**
     * List all Pages with pagination
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).PAGE_VIEW)")
    public ServiceResult<org.springframework.data.domain.Page<PageResponse>> listPages(
            String module, 
            Boolean active, 
            String search, 
            Pageable pageable
    ) {
        String tenantId = TenantHelper.requireTenant();
        
        // Validate sort fields (Rule 17.3)
        pageable = PageableValidator.validateSortFields(pageable, ALLOWED_PAGE_SORT_FIELDS);

        org.springframework.data.domain.Page<Page> pages;

        if (search != null && !search.trim().isEmpty()) {
            // Search by name or code
            pages = pageRepository.searchPages(tenantId, search.trim(), pageable);
        } else if (module != null && !module.trim().isEmpty()) {
            // Filter by module
            pages = pageRepository.findByTenantIdAndModule(tenantId, module, pageable);
        } else if (active != null) {
            // Filter by active status
            pages = pageRepository.findByTenantIdAndActive(tenantId, active, pageable);
        } else {
            // All pages
            pages = pageRepository.findByTenantId(tenantId, pageable);
        }

        return ServiceResult.success(pages.map(page -> {
            Map<String, String> permissionKeys = buildPermissionKeys(page.getPageCode());
            return toResponse(page, permissionKeys);
        }));
    }

    /**
     * POST /api/pages/search
     * Dynamic search for pages with filtering, sorting, and pagination
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).PAGE_VIEW)")
    public ServiceResult<org.springframework.data.domain.Page<PageResponse>> searchPages(SearchRequest request) {
        String tenantId = TenantHelper.requireTenant();

        // Build JPA Specification from filters
        Specification<Page> spec = SpecBuilder.build(
            request,
            new SetAllowedFields(ALLOWED_PAGE_SEARCH_FIELDS),
            DefaultFieldValueConverter.INSTANCE
        );

        // Add tenant filter (security requirement)
        if (spec != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tenantId"), tenantId));
        } else {
            spec = (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
        }

        // Build Pageable with validated sort fields
        Pageable pageable = PageableBuilder.from(request, ALLOWED_PAGE_SORT_FIELDS);

        org.springframework.data.domain.Page<Page> pages = pageRepository.findAll(spec, pageable);
        return ServiceResult.success(pages.map(page -> {
            Map<String, String> permissionKeys = buildPermissionKeys(page.getPageCode());
            return toResponse(page, permissionKeys);
        }));
    }

    /**
     * Get all active Pages (for dropdown in Role Access Control)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).PAGE_VIEW)")
    public ServiceResult<List<PageResponse>> getActivePages() {
        String tenantId = TenantHelper.requireTenant();

        List<Page> pages = pageRepository.findByTenantIdAndActiveOrderByDisplayOrder(tenantId, true);

        return ServiceResult.success(pages.stream()
                .map(page -> {
                    Map<String, String> permissionKeys = buildPermissionKeys(page.getPageCode());
                    return toResponse(page, permissionKeys);
                })
                .toList());
    }

    /**
     * Deactivate a Page (soft delete - RECOMMENDED)
     * 
     * Inactive pages:
     * - Will NOT appear in getActivePages() dropdown
     * - Will NOT appear in user menu even if user has VIEW permission
     * - Can be reactivated later if needed
     * 
     * @param id Page ID
     * @return Updated PageResponse with active=false
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).PAGE_UPDATE)")
    public ServiceResult<PageResponse> deactivatePage(Long id) {
        return setPageActive(id, false);
    }

    /**
     * Reactivate a previously deactivated Page
     * 
     * @param id Page ID
     * @return Updated PageResponse with active=true
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).PAGE_UPDATE)")
    public ServiceResult<PageResponse> reactivatePage(Long id) {
        return setPageActive(id, true);
    }

    private ServiceResult<PageResponse> setPageActive(Long id, boolean active) {
        String tenantId = TenantHelper.requireTenant();

        Page page = pageRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecurityErrorCodes.PAGE_NOT_FOUND, id));

        log.info("{} page ID: {} (code: {})", active ? "Reactivating" : "Deactivating", id, page.getPageCode());

        page.setActive(active);

        Page updated = pageRepository.save(page);

        Map<String, String> permissionKeys = buildPermissionKeys(page.getPageCode());
        return ServiceResult.success(toResponse(updated, permissionKeys), Status.UPDATED);
    }

    /**
     * Delete a Page (hard delete - NOT RECOMMENDED)
     * 
     * ⚠️ WARNING: Hard delete permanently removes the page.
    // ==============================
    // Helper Methods
    // ==============================

    /**
     * Create Permission RECORDS in database for a page
     * 
     * ====================================================
     * CRITICAL DISTINCTION
     * ====================================================
     * 
     * This method creates Permission RECORDS (definitions) in PERMISSIONS table:
     * - PERM_<CODE>_VIEW
     * - PERM_<CODE>_CREATE
     * - PERM_<CODE>_UPDATE
     * - PERM_<CODE>_DELETE
     * 
     * ⚠️ IMPORTANT: This method does NOT:
     * - Assign these permissions to any Role
     * - Grant access to any User
     * - Link permissions to Role-Permission join table
     * 
     * Permission ASSIGNMENT to Roles happens ONLY in RoleAccessService:
     * - RoleAccessService.addPageToRole() assigns VIEW + selected CRUD permissions to a Role
     * - VIEW is ALWAYS assigned (implicit, mandatory)
     * - CREATE/UPDATE/DELETE are assigned ONLY if user explicitly selects them
     * 
     * This separation ensures:
     * 1) Pages Registry manages UI screen definitions
     * 2) RoleAccessService manages access control (who can do what)
     * 
     * @param page The Page entity to link permissions to
     * @param tenantId Tenant ID for multi-tenancy isolation
     * @return Map of permission type -> permission key (e.g., "VIEW" -> "PERM_USER_VIEW")
     */
    private Map<String, String> createPermissionRecords(Page page, String tenantId) {
        Map<String, String> permissionKeys = new LinkedHashMap<>();
        String pageCode = page.getPageCode();

        for (PermissionType type : PermissionType.values()) {
            String permKey = type.buildPermissionKey(pageCode);
            permissionKeys.put(type.name(), permKey);

            // Check if permission already exists
            Optional<Permission> existing = permissionRepository.findByNameAndTenantId(permKey, tenantId);

            if (existing.isEmpty()) {
                // Create new permission RECORD linked to the page
                Permission newPerm = Permission.builder()
                        .name(permKey)
                        .tenantId(tenantId)
                        .page(page)                    // Link to Page via FK
                        .permissionType(type)          // Store type for efficient queries
                        .build();
                permissionRepository.save(newPerm);
                log.debug("Created permission record: {} linked to page ID: {}", permKey, page.getId());
            } else {
                // Update existing permission to link to page if not already linked
                Permission existingPerm = existing.get();
                if (existingPerm.getPage() == null) {
                    existingPerm.setPage(page);
                    existingPerm.setPermissionType(type);
                    permissionRepository.save(existingPerm);
                    log.debug("Updated permission record: {} to link to page ID: {}", permKey, page.getId());
                } else {
                    log.debug("Permission record already exists and linked: {}", permKey);
                }
            }
        }

        return permissionKeys;
    }

    /**
     * Build permission keys map for a given pageCode
     * 
     * Returns reference keys without creating or modifying any database records.
     * Used for displaying permission keys in UI responses.
     * 
     * @param pageCode Page code
     * @return Map of permission type -> permission key
     */
    private Map<String, String> buildPermissionKeys(String pageCode) {
        Map<String, String> keys = new LinkedHashMap<>();
        for (PermissionType type : PermissionType.values()) {
            keys.put(type.name(), type.buildPermissionKey(pageCode));
        }
        return keys;
    }

    /**
     * Convert Page entity to PageResponse DTO
     */
    private PageResponse toResponse(Page page, Map<String, String> permissionKeys) {
        return PageResponse.builder()
                .id(page.getId())
                .pageCode(page.getPageCode())
                .nameAr(page.getNameAr())
                .nameEn(page.getNameEn())
                .route(page.getRoute())
                .icon(page.getIcon())
                .module(page.getModule())
                .parentId(page.getParentId())
                .displayOrder(page.getDisplayOrder())
                .active(page.getActive())
                .description(page.getDescription())
                .permissionKeys(permissionKeys)
                .createdAt(page.getCreatedAt())
                .createdBy(page.getCreatedBy())
                .updatedAt(page.getUpdatedAt())
                .updatedBy(page.getUpdatedBy())
                .build();
    }

    private static final String ROUTE_PATTERN = "^/[a-zA-Z0-9/_-]+$";

    private void validateRouteFormat(String route) {
        if (!route.startsWith("/")) {
            throw new LocalizedException(Status.BAD_REQUEST, SecurityErrorCodes.INVALID_ROUTE_FORMAT, route);
        }
        if (!route.matches(ROUTE_PATTERN)) {
            throw new LocalizedException(Status.BAD_REQUEST, SecurityErrorCodes.INVALID_ROUTE_FORMAT, route);
        }
    }
}
