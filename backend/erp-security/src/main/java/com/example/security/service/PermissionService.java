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
import com.example.security.dto.CreatePermissionRequest;
import com.example.security.dto.PermissionDto;
import com.example.security.exception.SecurityErrorCodes;
import com.example.security.mapper.PermissionMapper;
import com.example.erp.common.multitenancy.TenantContext;
import com.example.erp.common.multitenancy.TenantHelper;
import com.example.security.repo.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permRepo;

    // Whitelist of allowed sort fields (Rule 17.3)
    private static final Set<String> ALLOWED_PERMISSION_SORT_FIELDS = Set.of(
        "id", "name", "module", "createdAt", "updatedAt"
    );

    // Whitelist of allowed search fields for dynamic filtering
    private static final Set<String> ALLOWED_PERMISSION_SEARCH_FIELDS = Set.of(
        "name", "module"
    );

    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).PERMISSION_CREATE)")
    // @CacheEvict(cacheNames = {"permissionByName", "permissionsList"}, allEntries = true)
    public ServiceResult<PermissionDto> createPermission(CreatePermissionRequest req) {
        String tenant = TenantHelper.requireTenant();
        // تحقق من عدم التكرار داخل نفس الـtenant
        permRepo.findByNameAndTenantId(req.getName(), tenant).ifPresent(p -> {
            throw new LocalizedException(Status.ALREADY_EXISTS, SecurityErrorCodes.PERMISSION_ALREADY_EXISTS, req.getName());
        });
        Permission p = Permission.builder()
                .tenantId(tenant)
                .name(req.getName())
                .build();
        Permission saved = permRepo.save(p);
        return ServiceResult.success(PermissionMapper.toDto(saved), Status.CREATED);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).PERMISSION_VIEW)")
    // Rule 16.4: Do NOT cache Pageable results
    // Pagination results change too frequently and create too many cache keys
    public ServiceResult<Page<PermissionDto>> listPermissions(Pageable pageable) {
        // Validate sort fields (Rule 17.3)
        pageable = PageableValidator.validateSortFields(pageable, ALLOWED_PERMISSION_SORT_FIELDS);
        
        Page<Permission> permissions = permRepo.findAllByTenantId(TenantHelper.requireTenant(), pageable);
        return ServiceResult.success(permissions.map(PermissionMapper::toDto));
    }

    /**
     * POST /api/permissions/search
     * Dynamic search for permissions with filtering, sorting, and pagination
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).PERMISSION_VIEW)")
    public ServiceResult<Page<PermissionDto>> searchPermissions(SearchRequest request) {
        String tenant = TenantHelper.requireTenant();

        // Build JPA Specification from filters
        Specification<Permission> spec = SpecBuilder.build(
            request,
            new SetAllowedFields(ALLOWED_PERMISSION_SEARCH_FIELDS),
            DefaultFieldValueConverter.INSTANCE
        );

        // Add tenant filter (security requirement)
        if (spec != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tenantId"), tenant));
        } else {
            spec = (root, query, cb) -> cb.equal(root.get("tenantId"), tenant);
        }

        // Build Pageable with validated sort fields
        Pageable pageable = PageableBuilder.from(request, ALLOWED_PERMISSION_SORT_FIELDS);

        Page<Permission> permissions = permRepo.findAll(spec, pageable);
        return ServiceResult.success(permissions.map(PermissionMapper::toDto));
    }

    // tenant checking delegated to TenantHelper
}
