package com.example.org.service;

import com.erp.common.search.*;
import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.example.org.dto.*;
import com.example.org.entity.OrgLegalEntity;
import com.example.org.exception.OrgErrorCodes;
import com.example.org.mapper.LegalEntityMapper;
import com.example.org.repository.OrgLegalEntityRepository;
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
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegalEntityService {

    private final OrgLegalEntityRepository legalEntityRepository;
    private final LegalEntityMapper legalEntityMapper;
    private final OrgCodeGenerationService codeGenerationService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "legalEntityCode", "legalEntityNameAr", "legalEntityNameEn",
            "countryId", "functionalCurrencyId", "isActive", "createdAt", "updatedAt"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^https?://[\\w.-]+(:\\d+)?(/.*)?$"
    );

    // ==================== CRUD Operations ====================

    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).LEGAL_ENTITY_CREATE)")
    public ServiceResult<LegalEntityResponse> create(LegalEntityCreateRequest request) {
        log.info("Creating legal entity: {}", request.getLegalEntityNameEn());

        // RULE-LE-03: Validate email
        validateEmail(request.getEmail());
        // RULE-LE-04: Validate URL
        validateUrl(request.getWebsite());
        // RULE-LE-05: Validate fiscal year month
        validateFiscalMonth(request.getFiscalYearStartMonth());

        OrgLegalEntity entity = legalEntityMapper.toEntity(request);

        // RULE-LE-01: Auto-generate code via centralized sequence service (B4.1)
        entity.setLegalEntityCode(codeGenerationService.generateLegalEntityCode());

        OrgLegalEntity saved = legalEntityRepository.save(entity);

        log.info("Legal entity created with ID: {}", saved.getId());
        return ServiceResult.success(legalEntityMapper.toResponse(saved), Status.CREATED);
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).LEGAL_ENTITY_UPDATE)")
    public ServiceResult<LegalEntityResponse> update(Long id, LegalEntityUpdateRequest request) {
        log.info("Updating legal entity ID: {}", id);

        OrgLegalEntity entity = legalEntityRepository.findById(id)
                .orElseThrow(() -> new LocalizedException(
                        Status.NOT_FOUND,
                        OrgErrorCodes.LEGAL_ENTITY_NOT_FOUND,
                        id
                ));

        // RULE-LE-06 / B4.4: Currency lock — prevent change after first financial transaction
        if (request.getFunctionalCurrencyId() != null
                && !entity.getFunctionalCurrencyId().equals(request.getFunctionalCurrencyId())) {
            if (legalEntityRepository.hasFinancialTransactions(id) > 0) {
                throw new LocalizedException(
                        Status.BUSINESS_RULE_VIOLATION,
                        OrgErrorCodes.LEGAL_ENTITY_CURRENCY_LOCKED
                );
            }
        }

        // RULE-LE-03: Validate email
        validateEmail(request.getEmail());
        // RULE-LE-04: Validate URL
        validateUrl(request.getWebsite());
        // RULE-LE-05: Validate fiscal year month
        validateFiscalMonth(request.getFiscalYearStartMonth());

        legalEntityMapper.updateEntityFromRequest(entity, request);

        OrgLegalEntity updated = legalEntityRepository.save(entity);

        log.info("Legal entity updated: {}", id);
        return ServiceResult.success(legalEntityMapper.toResponse(updated), Status.UPDATED);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).LEGAL_ENTITY_VIEW)")
    public ServiceResult<LegalEntityResponse> getById(Long id) {
        log.debug("Getting legal entity by ID: {}", id);

        OrgLegalEntity entity = legalEntityRepository.findById(id)
                .orElseThrow(() -> new LocalizedException(
                        Status.NOT_FOUND,
                        OrgErrorCodes.LEGAL_ENTITY_NOT_FOUND,
                        id
                ));

        return ServiceResult.success(legalEntityMapper.toResponse(entity));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).LEGAL_ENTITY_VIEW)")
    public ServiceResult<Page<LegalEntityResponse>> list(
            String legalEntityCode, String legalEntityNameAr, Long countryFk,
            String statusId, int page, int pageSize) {
        log.debug("Listing legal entities");

        Pageable pageable = PageRequest.of(page, Math.min(pageSize, 100), Sort.by("id").ascending());

        Specification<OrgLegalEntity> spec = (root, query, cb) -> cb.conjunction();
        if (legalEntityCode != null && !legalEntityCode.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.like(
                    cb.upper(root.get("legalEntityCode")),
                    "%" + legalEntityCode.trim().toUpperCase() + "%"));
        }
        if (legalEntityNameAr != null && !legalEntityNameAr.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.like(
                    root.get("legalEntityNameAr"),
                    "%" + legalEntityNameAr.trim() + "%"));
        }
        if (countryFk != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("countryId"), countryFk));
        }
        if (statusId != null && !statusId.isBlank()) {
            boolean activeFilter = "ACTIVE".equalsIgnoreCase(statusId.trim());
            spec = spec.and((root, q, cb) -> cb.equal(root.get("isActive"), activeFilter));
        }

        Page<OrgLegalEntity> result = legalEntityRepository.findAll(spec, pageable);
        return ServiceResult.success(result.map(legalEntityMapper::toResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).LEGAL_ENTITY_VIEW)")
    public ServiceResult<Page<LegalEntityResponse>> search(SearchRequest searchRequest) {
        log.debug("Searching legal entities");

        AllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        Specification<OrgLegalEntity> spec = SpecBuilder.build(searchRequest, allowedFields, converter);
        Pageable pageable = PageableBuilder.from(searchRequest, ALLOWED_SORT_FIELDS);

        Page<OrgLegalEntity> page = legalEntityRepository.findAll(spec, pageable);

        return ServiceResult.success(page.map(legalEntityMapper::toResponse));
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).LEGAL_ENTITY_UPDATE)")
    public ServiceResult<LegalEntityResponse> deactivate(Long id) {
        return toggleActive(id, false);
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).LEGAL_ENTITY_UPDATE)")
    public ServiceResult<LegalEntityResponse> toggleActive(Long id, Boolean active) {
        log.info("Toggling legal entity ID: {} to active={}", id, active);

        OrgLegalEntity entity = legalEntityRepository.findById(id)
                .orElseThrow(() -> new LocalizedException(
                        Status.NOT_FOUND,
                        OrgErrorCodes.LEGAL_ENTITY_NOT_FOUND,
                        id
                ));

        // B3.2: Re-activation is NOT supported (no INACTIVE → ACTIVE transition)
        if (Boolean.TRUE.equals(active)) {
            throw new LocalizedException(
                    Status.BUSINESS_RULE_VIOLATION,
                    OrgErrorCodes.LEGAL_ENTITY_REACTIVATION_NOT_ALLOWED
            );
        }

        // B3.4: Cannot deactivate if already inactive
        if (!Boolean.TRUE.equals(entity.getIsActive())) {
            throw new LocalizedException(
                    Status.BUSINESS_RULE_VIOLATION,
                    OrgErrorCodes.LEGAL_ENTITY_ALREADY_INACTIVE
            );
        }

        // RULE-LE-07: Cannot deactivate if has active branches
        long activeBranches = legalEntityRepository.countActiveBranches(id);
        if (activeBranches > 0) {
            throw new LocalizedException(
                    Status.CONFLICT,
                    OrgErrorCodes.LEGAL_ENTITY_HAS_ACTIVE_BRANCHES,
                    activeBranches
            );
        }

        // RULE-LE-08: Cannot deactivate last active entity
        long activeCount = legalEntityRepository.countAllActive();
        if (activeCount <= 1) {
            throw new LocalizedException(
                    Status.CONFLICT,
                    OrgErrorCodes.LEGAL_ENTITY_LAST_ACTIVE
            );
        }

        entity.deactivate();

        OrgLegalEntity updated = legalEntityRepository.save(entity);

        log.info("Legal entity {} deactivated", id);
        return ServiceResult.success(legalEntityMapper.toResponse(updated), Status.UPDATED);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).LEGAL_ENTITY_VIEW)")
    public ServiceResult<LegalEntityUsageResponse> getUsage(Long id) {
        log.debug("Getting usage for legal entity ID: {}", id);

        OrgLegalEntity entity = legalEntityRepository.findById(id)
                .orElseThrow(() -> new LocalizedException(
                        Status.NOT_FOUND,
                        OrgErrorCodes.LEGAL_ENTITY_NOT_FOUND,
                        id
                ));

        long activeBranches = legalEntityRepository.countActiveBranches(id);
        long totalRegions = entity.getRegionCount() != null ? entity.getRegionCount() : 0;
        long totalBranches = entity.getBranchCount() != null ? entity.getBranchCount() : 0;
        boolean hasFinancialTransactions = legalEntityRepository.hasFinancialTransactions(id) > 0;

        return ServiceResult.success(
            legalEntityMapper.toUsageResponse(
                entity,
                totalRegions,
                totalBranches,
                activeBranches,
                hasFinancialTransactions
            )
        );
    }

    // ==================== Validation Helpers ====================

    private void validateEmail(String email) {
        if (email != null && !email.isBlank() && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new LocalizedException(
                    Status.VALIDATION_ERROR,
                    OrgErrorCodes.LEGAL_ENTITY_INVALID_EMAIL
            );
        }
    }

    private void validateUrl(String url) {
        if (url != null && !url.isBlank() && !URL_PATTERN.matcher(url).matches()) {
            throw new LocalizedException(
                    Status.VALIDATION_ERROR,
                    OrgErrorCodes.LEGAL_ENTITY_INVALID_URL
            );
        }
    }

    private void validateFiscalMonth(Integer month) {
        if (month != null && (month < 1 || month > 12)) {
            throw new LocalizedException(
                    Status.VALIDATION_ERROR,
                    OrgErrorCodes.LEGAL_ENTITY_INVALID_MONTH
            );
        }
    }

}
