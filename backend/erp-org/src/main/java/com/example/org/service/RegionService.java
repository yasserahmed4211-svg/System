package com.example.org.service;

import com.erp.common.search.*;
import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.example.org.dto.*;
import com.example.org.entity.OrgLegalEntity;
import com.example.org.entity.OrgRegion;
import com.example.org.exception.OrgErrorCodes;
import com.example.org.mapper.RegionMapper;
import com.example.org.repository.OrgLegalEntityRepository;
import com.example.org.repository.OrgRegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionService {

    private final OrgRegionRepository regionRepository;
    private final OrgLegalEntityRepository legalEntityRepository;
    private final RegionMapper regionMapper;
    private final OrgCodeGenerationService codeGenerationService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "regionCode", "regionNameAr", "regionNameEn",
            "statusId", "isActive", "createdAt", "updatedAt"
    );

    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).REGION_CREATE)")
    public ServiceResult<RegionResponse> create(RegionCreateRequest request) {
        log.info("Creating region: {}", request.getRegionNameEn());

        // RULE-RG-02: Verify legal entity is active
        OrgLegalEntity legalEntity = legalEntityRepository.findById(request.getLegalEntityId())
                .orElseThrow(() -> new LocalizedException(
                        Status.NOT_FOUND,
                        OrgErrorCodes.LEGAL_ENTITY_NOT_FOUND,
                        request.getLegalEntityId()
                ));

        if (!Boolean.TRUE.equals(legalEntity.getIsActive())) {
            throw new LocalizedException(
                    Status.BUSINESS_RULE_VIOLATION,
                    OrgErrorCodes.REGION_INACTIVE_LEGAL_ENTITY
            );
        }

        OrgRegion entity = regionMapper.toEntity(request);

        // RULE-RG-01: Auto-generate code via centralized sequence service (B4.1)
        entity.setRegionCode(codeGenerationService.generateRegionCode());
        // Set FK relationship (service responsibility)
        entity.setLegalEntity(legalEntity);

        OrgRegion saved = regionRepository.save(entity);

        log.info("Region created with ID: {}", saved.getId());
        return ServiceResult.success(regionMapper.toResponse(saved), Status.CREATED);
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).REGION_UPDATE)")
    public ServiceResult<RegionResponse> update(Long id, RegionUpdateRequest request) {
        log.info("Updating region ID: {}", id);

        OrgRegion entity = regionRepository.findById(id)
                .orElseThrow(() -> new LocalizedException(
                        Status.NOT_FOUND,
                        OrgErrorCodes.REGION_NOT_FOUND,
                        id
                ));

        // RULE-RG-04: legalEntityFk is NOT editable — mapper does not update it
        regionMapper.updateEntityFromRequest(entity, request);

        OrgRegion updated = regionRepository.save(entity);

        log.info("Region updated: {}", id);
        return ServiceResult.success(regionMapper.toResponse(updated), Status.UPDATED);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).REGION_VIEW)")
    public ServiceResult<RegionResponse> getById(Long id) {
        log.debug("Getting region by ID: {}", id);

        OrgRegion entity = regionRepository.findById(id)
                .orElseThrow(() -> new LocalizedException(
                        Status.NOT_FOUND,
                        OrgErrorCodes.REGION_NOT_FOUND,
                        id
                ));

        return ServiceResult.success(regionMapper.toResponse(entity));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).REGION_VIEW)")
    public ServiceResult<Page<RegionResponse>> list(
            String regionCode, String regionNameAr, Long legalEntityFk,
            String statusId, int page, int pageSize) {
        log.debug("Listing regions");

        Pageable pageable = PageRequest.of(page, Math.min(pageSize, 100), Sort.by("id").ascending());

        Specification<OrgRegion> spec = (root, query, cb) -> cb.conjunction();
        if (regionCode != null && !regionCode.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.like(
                    cb.upper(root.get("regionCode")),
                    "%" + regionCode.trim().toUpperCase() + "%"));
        }
        if (regionNameAr != null && !regionNameAr.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.like(
                    root.get("regionNameAr"),
                    "%" + regionNameAr.trim() + "%"));
        }
        if (legalEntityFk != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("legalEntity").get("id"), legalEntityFk));
        }
        if (statusId != null && !statusId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("statusId"), statusId.trim().toUpperCase()));
        }

        Page<OrgRegion> result = regionRepository.findAll(spec, pageable);
        return ServiceResult.success(result.map(regionMapper::toResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).REGION_VIEW)")
    public ServiceResult<Page<RegionResponse>> search(SearchRequest searchRequest) {
        log.debug("Searching regions");

        AllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        Specification<OrgRegion> spec = SpecBuilder.build(searchRequest, allowedFields, converter);
        Pageable pageable = PageableBuilder.from(searchRequest, ALLOWED_SORT_FIELDS);

        Page<OrgRegion> page = regionRepository.findAll(spec, pageable);

        return ServiceResult.success(page.map(regionMapper::toResponse));
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).REGION_UPDATE)")
    public ServiceResult<RegionResponse> deactivate(Long id) {
        return toggleActive(id, false);
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).REGION_UPDATE)")
    public ServiceResult<RegionResponse> toggleActive(Long id, Boolean active) {
        log.info("Toggling region ID: {} to active={}", id, active);

        OrgRegion entity = regionRepository.findById(id)
                .orElseThrow(() -> new LocalizedException(
                        Status.NOT_FOUND,
                        OrgErrorCodes.REGION_NOT_FOUND,
                        id
                ));

        // B3.2: Re-activation is NOT supported (no INACTIVE → ACTIVE transition)
        if (Boolean.TRUE.equals(active)) {
            throw new LocalizedException(
                    Status.BUSINESS_RULE_VIOLATION,
                    OrgErrorCodes.REGION_REACTIVATION_NOT_ALLOWED
            );
        }

        // B3.4: Cannot deactivate if already inactive
        if (!Boolean.TRUE.equals(entity.getIsActive())) {
            throw new LocalizedException(
                    Status.BUSINESS_RULE_VIOLATION,
                    OrgErrorCodes.REGION_ALREADY_INACTIVE
            );
        }

        // RULE-RG-05: Cannot deactivate if has active branches
        long activeBranches = regionRepository.countActiveBranches(id);
        if (activeBranches > 0) {
            throw new LocalizedException(
                    Status.CONFLICT,
                    OrgErrorCodes.REGION_HAS_ACTIVE_BRANCHES,
                    activeBranches
            );
        }

        entity.deactivate();

        OrgRegion updated = regionRepository.save(entity);

        log.info("Region {} deactivated", id);
        return ServiceResult.success(regionMapper.toResponse(updated), Status.UPDATED);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).REGION_VIEW)")
    public ServiceResult<RegionUsageResponse> getUsage(Long id) {
        log.debug("Getting usage for region ID: {}", id);

        OrgRegion entity = regionRepository.findById(id)
                .orElseThrow(() -> new LocalizedException(
                        Status.NOT_FOUND,
                        OrgErrorCodes.REGION_NOT_FOUND,
                        id
                ));

        long activeBranches = regionRepository.countActiveBranches(id);

        return ServiceResult.success(regionMapper.toUsageResponse(entity, activeBranches));
    }

}
