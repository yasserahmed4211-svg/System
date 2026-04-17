package com.example.masterdata.service;

import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SpecBuilder;
import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.example.masterdata.dto.*;
import com.example.masterdata.entity.MdLookupDetail;
import com.example.masterdata.entity.MdMasterLookup;
import com.example.masterdata.exception.MasterDataErrorCodes;
import com.example.masterdata.mapper.LookupDetailMapper;
import com.example.masterdata.repository.LookupDetailRepository;
import com.example.masterdata.repository.MasterLookupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for Lookup Detail business logic
 * 
 * Architecture Rules:
 * - Rule 5.1: Business logic container
 * - Rule 5.2: Transaction management
 * - Rule 5.4: Return DTOs, not entities
 * 
 * @author ERP Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LookupDetailService {

    private final LookupDetailRepository lookupDetailRepository;
    private final MasterLookupRepository masterLookupRepository;
    private final LookupDetailMapper lookupDetailMapper;

    /**
     * Allowed sort fields for search
     * Rule 17.3: Sort field whitelist
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id", "code", "nameAr", "nameEn", "sortOrder", 
        "isActive", "createdAt", "updatedAt"
    );

    /**
     * Create new lookup detail
     * 
     * Business Rules:
     * - Code must be unique within same master lookup
     * - Master lookup must exist
     * 
     * @param request Create request
     * @return Created lookup detail
     */
    @org.springframework.cache.annotation.CacheEvict(cacheNames = "lookupValues", allEntries = true)
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).MASTER_LOOKUP_CREATE)")
    public ServiceResult<LookupDetailResponse> create(LookupDetailCreateRequest request) {
        log.info("Creating lookup detail with code: {} for master lookup ID: {}", 
                 request.getCode(), request.getMasterLookupId());

        // Validate master lookup exists
        MdMasterLookup masterLookup = masterLookupRepository.findById(request.getMasterLookupId())
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND,
                MasterDataErrorCodes.MASTER_LOOKUP_NOT_FOUND,
                request.getMasterLookupId()
            ));

        // Validate unique code within master lookup
        if (lookupDetailRepository.existsByMasterLookupIdAndCode(
                request.getMasterLookupId(), request.getCode())) {
            throw new LocalizedException(Status.ALREADY_EXISTS, MasterDataErrorCodes.LOOKUP_DETAIL_CODE_DUPLICATE, request.getCode());
        }

        // Create entity (mapper sets parent FK — compile-time safety)
        MdLookupDetail entity = lookupDetailMapper.toEntity(request, masterLookup);

        // Save
        MdLookupDetail saved = lookupDetailRepository.save(entity);
        
        log.info("Lookup detail created with ID: {}", saved.getId());
        return ServiceResult.success(lookupDetailMapper.toResponse(saved), Status.CREATED);
    }

    /**
     * Update existing lookup detail
     * 
     * Business Rules:
     * - masterLookupId is immutable and cannot be changed
     * - code is immutable and cannot be changed
     * - Only nameAr, nameEn, extraValue, and sortOrder can be updated
     * 
     * @param id Lookup detail ID
     * @param request Update request
     * @return Updated lookup detail
     */
    @org.springframework.cache.annotation.CacheEvict(cacheNames = "lookupValues", allEntries = true)
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).MASTER_LOOKUP_UPDATE)")
    public ServiceResult<LookupDetailResponse> update(Long id, LookupDetailUpdateRequest request) {
        log.info("Updating lookup detail ID: {}", id);

        // Find entity
        MdLookupDetail entity = lookupDetailRepository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND,
                MasterDataErrorCodes.LOOKUP_DETAIL_NOT_FOUND,
                id
            ));

        // Update entity (masterLookupId and code are NOT updated - immutable per contract)
        lookupDetailMapper.updateEntityFromRequest(entity, request);

        // Save
        MdLookupDetail updated = lookupDetailRepository.save(entity);
        
        log.info("Lookup detail updated: {}", id);
        return ServiceResult.success(lookupDetailMapper.toResponse(updated), Status.UPDATED);
    }

    /**
     * Get lookup detail by ID
     * 
     * @param id Lookup detail ID
     * @return Lookup detail response
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).MASTER_LOOKUP_VIEW)")
    public ServiceResult<LookupDetailResponse> getById(Long id) {
        log.debug("Getting lookup detail by ID: {}", id);

        MdLookupDetail entity = lookupDetailRepository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND,
                MasterDataErrorCodes.LOOKUP_DETAIL_NOT_FOUND,
                id
            ));

        return ServiceResult.success(lookupDetailMapper.toResponse(entity));
    }

    /**
     * Search lookup details with filtering, sorting, and pagination
     * 
     * Best Practices for Master-Detail:
     * 1. Uses explicit JOIN instead of implicit path navigation
     * 2. Uses repository method directly when no dynamic filters exist
     * 3. Uses Specification with explicit Join for dynamic filters
     * 
     * Rule 10.7: Standard CRUD operations with search
     * 
     * @param masterLookupId Master lookup ID to filter details (required for parent-child relationship)
     * @param searchRequest Search criteria (excluding masterLookupId filter)
     * @return Page of lookup details
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).MASTER_LOOKUP_VIEW)")
    public ServiceResult<Page<LookupDetailResponse>> search(Long masterLookupId, SearchRequest searchRequest) {
        log.debug("Searching lookup details for masterLookupId: {} with filters: {}", masterLookupId, searchRequest);

        // Validate required parent ID
        if (masterLookupId == null) {
            log.warn("masterLookupId is null - returning empty results");
            return ServiceResult.success(Page.empty());
        }

        // Build pageable with default sort by sortOrder ASC
        if (searchRequest == null) {
            searchRequest = new SearchRequest();
        }
        if (searchRequest.getSortBy() == null) {
            searchRequest.setSortBy("sortOrder");
            searchRequest.setSortDir("ASC");
        }
        Pageable pageable = PageableBuilder.from(searchRequest, ALLOWED_SORT_FIELDS);

        // Check if there are additional dynamic filters
        boolean hasAdditionalFilters = searchRequest.getFilters() != null && !searchRequest.getFilters().isEmpty();
        
        if (!hasAdditionalFilters) {
            // Best Practice: Use repository method with explicit JOIN when no dynamic filters
            Page<MdLookupDetail> page = lookupDetailRepository.searchByMasterLookupId(
                masterLookupId, pageable);
            return ServiceResult.success(page.map(lookupDetailMapper::toResponse));
        }

        // Dynamic filters exist - use Specification with explicit Join
        Specification<MdLookupDetail> spec = buildMasterDetailSpecification(masterLookupId);
        
        // Build additional specification from other filters
        com.erp.common.search.AllowedFields allowedFields = 
            new com.erp.common.search.SetAllowedFields(ALLOWED_SORT_FIELDS);
        com.erp.common.search.FieldValueConverter converter = 
            com.erp.common.search.DefaultFieldValueConverter.INSTANCE;
        
        Specification<MdLookupDetail> additionalSpec = SpecBuilder.build(searchRequest, allowedFields, converter);
        
        if (additionalSpec != null) {
            spec = spec.and(additionalSpec);
        }

        // Execute search
        Page<MdLookupDetail> page = lookupDetailRepository.findAll(spec, pageable);
        return ServiceResult.success(page.map(lookupDetailMapper::toResponse));
    }

    /**
     * Build Specification for master-detail relationship with explicit JOIN
     * Best Practice: Use explicit Join instead of implicit path navigation
     * 
     * @param masterLookupId Parent master lookup ID
     * @return Specification with masterLookup filter
     */
    private Specification<MdLookupDetail> buildMasterDetailSpecification(Long masterLookupId) {
        return (root, query, cb) -> {
            // Explicit JOIN on masterLookup relationship
            Join<MdLookupDetail, MdMasterLookup> masterLookupJoin = root.join("masterLookup", JoinType.INNER);
            
            return cb.equal(masterLookupJoin.get("id"), masterLookupId);
        };
    }

    /**
     * Get lookup detail options by master lookup key
     * Used for dropdown options in UI
     * 
     * @param lookupKey Master lookup key (e.g., COLOR, UOM)
     * @param activeOnly Filter by active status
     * @return List of lookup detail options
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).MASTER_LOOKUP_VIEW)")
    public ServiceResult<List<LookupDetailOptionResponse>> getOptionsByLookupKey(String lookupKey, Boolean activeOnly) {
        log.debug("Getting lookup options for key: {}, activeOnly: {}", lookupKey, activeOnly);

        Boolean active = activeOnly != null && activeOnly;

        List<MdLookupDetail> details = lookupDetailRepository.findByMasterLookupKeyAndActive(
            lookupKey.toUpperCase(), active);

        return ServiceResult.success(details.stream()
            .map(lookupDetailMapper::toOptionResponse)
            .collect(Collectors.toList()));
    }

    /**
     * Toggle active status of lookup detail
     * 
     * Rule 19.5: Unified toggle-active endpoint
     * 
     * @param id Lookup detail ID
     * @param active Target active status
     * @return Updated lookup detail
     */
    @org.springframework.cache.annotation.CacheEvict(cacheNames = "lookupValues", allEntries = true)
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).MASTER_LOOKUP_UPDATE)")
    public ServiceResult<LookupDetailResponse> toggleActive(Long id, Boolean active) {
        log.info("Toggling lookup detail ID: {} to active={}", id, active);

        MdLookupDetail entity = lookupDetailRepository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND,
                MasterDataErrorCodes.LOOKUP_DETAIL_NOT_FOUND,
                id
            ));

        if (Boolean.TRUE.equals(active)) {
            entity.activate();
        } else {
            entity.deactivate();
        }

        MdLookupDetail updated = lookupDetailRepository.save(entity);
        
        log.info("Lookup detail {} toggled to active={}", id, active);
        return ServiceResult.success(lookupDetailMapper.toResponse(updated), Status.UPDATED);
    }

    /**
     * Delete lookup detail
     * 
     * Business Rule: Cannot delete if referenced by any active entity
     * Returns HTTP 409 CONFLICT if deletion fails due to FK constraint
     * 
     * @param id Lookup detail ID
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).MASTER_LOOKUP_DELETE)")
    public void delete(Long id) {
        log.info("Deleting lookup detail ID: {}", id);

        MdLookupDetail entity = lookupDetailRepository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND,
                MasterDataErrorCodes.LOOKUP_DETAIL_NOT_FOUND,
                id
            ));

        // Delete - DataIntegrityViolationException handled by GlobalExceptionHandler
        lookupDetailRepository.delete(entity);
        log.info("Lookup detail deleted: {}", id);
    }

    /**
     * Get usage information for lookup detail
     * 
     * Shows where the lookup detail is being used and whether it can be deleted
     * 
     * @param id Lookup detail ID
     * @return Usage information
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).MASTER_LOOKUP_VIEW)")
    public ServiceResult<LookupDetailUsageResponse> getUsage(Long id) {
        log.debug("Getting usage for lookup detail ID: {}", id);

        MdLookupDetail entity = lookupDetailRepository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND,
                MasterDataErrorCodes.LOOKUP_DETAIL_NOT_FOUND,
                id
            ));

        return ServiceResult.success(lookupDetailMapper.toUsageResponse(entity));
    }
}
