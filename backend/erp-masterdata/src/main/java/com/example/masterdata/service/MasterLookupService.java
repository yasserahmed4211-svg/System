package com.example.masterdata.service;

import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SpecBuilder;
import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.example.masterdata.dto.*;
import com.example.masterdata.entity.MdMasterLookup;
import com.example.masterdata.exception.MasterDataErrorCodes;
import com.example.masterdata.mapper.MasterLookupMapper;
import com.example.masterdata.repository.MasterLookupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Service for Master Lookup business logic
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
public class MasterLookupService {

    private final MasterLookupRepository masterLookupRepository;
    private final MasterLookupMapper masterLookupMapper;

    /**
     * Allowed sort fields for search
     * Rule 17.3: Sort field whitelist
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id", "lookupKey", "lookupName", "lookupNameEn", 
        "isActive", "createdAt", "updatedAt"
    );

    /**
     * Create new master lookup
     * 
     * Business Rules:
     * - Lookup key must be unique
     * - Lookup key is converted to uppercase
     * 
     * @param request Create request
     * @return Created master lookup
     */
    @org.springframework.cache.annotation.CacheEvict(cacheNames = "lookupValues", allEntries = true)
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).MASTER_LOOKUP_CREATE)")
    public ServiceResult<MasterLookupResponse> create(MasterLookupCreateRequest request) {
        log.info("Creating master lookup with key: {}", request.getLookupKey());

        // Validate unique lookup key (entity @PrePersist normalises to uppercase)
        if (masterLookupRepository.existsByLookupKey(request.getLookupKey().toUpperCase())) {
            throw new LocalizedException(
                Status.ALREADY_EXISTS,
                MasterDataErrorCodes.MASTER_LOOKUP_KEY_DUPLICATE,
                request.getLookupKey()
            );
        }

        // Create entity
        MdMasterLookup entity = masterLookupMapper.toEntity(request);

        // Save
        MdMasterLookup saved = masterLookupRepository.save(entity);
        
        log.info("Master lookup created with ID: {}", saved.getId());
        return ServiceResult.success(masterLookupMapper.toResponse(saved), Status.CREATED);
    }

    /**
     * Update existing master lookup
     * 
     * Business Rules:
     * - lookupKey is immutable and cannot be changed
     * - Only lookupName, lookupNameEn, and description can be updated
     * 
     * @param id Master lookup ID
     * @param request Update request
     * @return Updated master lookup
     */
    @org.springframework.cache.annotation.CacheEvict(cacheNames = "lookupValues", allEntries = true)
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).MASTER_LOOKUP_UPDATE)")
    public ServiceResult<MasterLookupResponse> update(Long id, MasterLookupUpdateRequest request) {
        log.info("Updating master lookup ID: {}", id);

        // Find entity
        MdMasterLookup entity = masterLookupRepository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND,
                MasterDataErrorCodes.MASTER_LOOKUP_NOT_FOUND,
                id
            ));

        // Update entity (lookupKey is NOT updated - immutable per contract)
        masterLookupMapper.updateEntityFromRequest(entity, request);

        // Explicitly set audit fields before save so they are reflected in the
        // returned DTO immediately (JPA @PreUpdate fires at flush time which
        // may be after toResponse() is called).
        entity.setUpdatedAt(java.time.Instant.now());
        entity.setUpdatedBy(com.example.erp.common.util.SecurityContextHelper.getUsernameOrSystem());

        // Save
        MdMasterLookup updated = masterLookupRepository.save(entity);
        
        log.info("Master lookup updated: {}", id);
        return ServiceResult.success(masterLookupMapper.toResponse(updated), Status.UPDATED);
    }

    /**
     * Get master lookup by ID
     * 
     * @param id Master lookup ID
     * @return Master lookup response
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).MASTER_LOOKUP_VIEW)")
    public ServiceResult<MasterLookupResponse> getById(Long id) {
        log.debug("Getting master lookup by ID: {}", id);

        MdMasterLookup entity = masterLookupRepository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND,
                MasterDataErrorCodes.MASTER_LOOKUP_NOT_FOUND,
                id
            ));

        return ServiceResult.success(masterLookupMapper.toResponse(entity));
    }

    /**
     * Search master lookups with filtering, sorting, and pagination
     * 
     * Uses common-utils SearchRequest for dynamic filtering
     * Rule 10.7: Standard CRUD operations with search
     * 
     * @param searchRequest Search criteria
     * @return Page of master lookups
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).MASTER_LOOKUP_VIEW)")
    public ServiceResult<Page<MasterLookupResponse>> search(SearchRequest searchRequest) {
        log.debug("Searching master lookups with filters: {}", searchRequest);

        // Build specification using AllowedFields and FieldValueConverter
        com.erp.common.search.AllowedFields allowedFields = 
            new com.erp.common.search.SetAllowedFields(ALLOWED_SORT_FIELDS);
        com.erp.common.search.FieldValueConverter converter = 
            com.erp.common.search.DefaultFieldValueConverter.INSTANCE;
        
        Specification<MdMasterLookup> spec = SpecBuilder.build(searchRequest, allowedFields, converter);

        // Build pageable
        Pageable pageable = PageableBuilder.from(searchRequest, ALLOWED_SORT_FIELDS);

        // Execute search
        Page<MdMasterLookup> page = masterLookupRepository.findAll(spec, pageable);

        return ServiceResult.success(page.map(masterLookupMapper::toResponse));
    }

    /**
     * Toggle active status of master lookup
     * 
     * Business Rule: Cannot deactivate if there are active lookup details
     * Rule 19.5: Unified toggle-active endpoint
     * 
     * @param id Master lookup ID
     * @param active Target active status
     * @return Updated master lookup
     */
    @org.springframework.cache.annotation.CacheEvict(cacheNames = "lookupValues", allEntries = true)
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).MASTER_LOOKUP_UPDATE)")
    public ServiceResult<MasterLookupResponse> toggleActive(Long id, Boolean active) {
        log.info("Toggling master lookup ID: {} to active={}", id, active);

        MdMasterLookup entity = masterLookupRepository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND,
                MasterDataErrorCodes.MASTER_LOOKUP_NOT_FOUND,
                id
            ));

        // Business rule: Cannot deactivate if there are active lookup details
        if (Boolean.FALSE.equals(active)) {
            long activeDetailsCount = masterLookupRepository.countActiveLookupDetails(id);
            if (activeDetailsCount > 0) {
                throw new LocalizedException(
                    Status.CONFLICT,
                    MasterDataErrorCodes.MASTER_LOOKUP_ACTIVE_DETAILS_EXIST,
                    activeDetailsCount
                );
            }
            entity.deactivate();
        } else {
            entity.activate();
        }

        MdMasterLookup updated = masterLookupRepository.save(entity);
        
        log.info("Master lookup {} toggled to active={}", id, active);
        return ServiceResult.success(masterLookupMapper.toResponse(updated), Status.UPDATED);
    }

    /**
     * Delete master lookup
     * 
     * Business Rule: Cannot delete if it has any lookup details
     * Returns HTTP 409 CONFLICT if deletion fails due to FK constraint
     * 
     * @param id Master lookup ID
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).MASTER_LOOKUP_DELETE)")
    public void delete(Long id) {
        log.info("Deleting master lookup ID: {}", id);

        MdMasterLookup entity = masterLookupRepository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND,
                MasterDataErrorCodes.MASTER_LOOKUP_NOT_FOUND,
                id
            ));

        // Check for lookup details
        long detailsCount = masterLookupRepository.countLookupDetails(id);
        if (detailsCount > 0) {
            throw new LocalizedException(
                Status.CONFLICT,
                MasterDataErrorCodes.MASTER_LOOKUP_DETAILS_EXIST,
                detailsCount
            );
        }

        // Delete - DataIntegrityViolationException handled by GlobalExceptionHandler
        masterLookupRepository.delete(entity);
        log.info("Master lookup deleted: {}", id);
    }

    /**
     * Get usage information for master lookup
     * 
     * Shows where the master lookup is being used and whether it can be deleted/deactivated
     * 
     * @param id Master lookup ID
     * @return Usage information
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).MASTER_LOOKUP_VIEW)")
    public ServiceResult<MasterLookupUsageResponse> getUsage(Long id) {
        log.debug("Getting usage for master lookup ID: {}", id);

        MdMasterLookup entity = masterLookupRepository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND,
                MasterDataErrorCodes.MASTER_LOOKUP_NOT_FOUND,
                id
            ));

        long totalDetailsCount = masterLookupRepository.countLookupDetails(id);
        long activeDetailsCount = masterLookupRepository.countActiveLookupDetails(id);

        return ServiceResult.success(masterLookupMapper.toUsageResponse(entity, totalDetailsCount, activeDetailsCount));
    }
}
