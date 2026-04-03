package com.example.erp.finance.gl.service;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.erp.common.search.*;
import com.example.erp.finance.gl.dto.*;
import com.example.erp.finance.gl.entity.AccRuleHdr;
import com.example.erp.finance.gl.entity.AccRuleLine;
import com.example.erp.finance.gl.entity.AccountsChart;
import com.example.erp.finance.gl.exception.GlErrorCodes;
import com.example.erp.finance.gl.mapper.AccRuleHdrMapper;
import com.example.erp.finance.gl.repository.AccRuleHdrRepository;
import com.example.erp.finance.gl.repository.AccountsChartRepository;
import com.example.erp.finance.gl.repository.GlJournalHdrRepository;
import com.example.masterdata.api.LookupValidationApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AccRuleHdrService {

    private final AccRuleHdrRepository accRuleHdrRepository;
    private final AccountsChartRepository accountsChartRepository;
    private final GlJournalHdrRepository journalHdrRepository;
    private final AccRuleHdrMapper accRuleHdrMapper;
    private final LookupValidationApi lookupValidationApi;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "ruleId", "companyIdFk", "sourceModule", "sourceDocType", "isActive", "createdAt", "updatedAt"
    );

    private static final String DEFAULT_SORT_FIELD = "ruleId";

    private static final Set<String> ALLOWED_SEARCH_FIELDS = Set.of(
        "companyIdFk", "sourceModule", "sourceDocType", "isActive"
    );

    // Lookup keys for dynamic validation from MD_MASTER_LOOKUP
    private static final String LK_SOURCE_MODULE       = "SOURCE_MODULE";
    private static final String LK_SOURCE_DOC_TYPE     = "SOURCE_DOC_TYPE";
    private static final String LK_ENTRY_SIDE          = "ENTRY_SIDE";
    private static final String LK_AMOUNT_SOURCE_TYPE  = "AMOUNT_SOURCE_TYPE";
    private static final String LK_ENTITY_TYPE         = "ENTITY_TYPE";

    // ==================== CREATE ====================

    @Transactional
    @CacheEvict(cacheNames = "accountingRules", allEntries = true)
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_RULE_CREATE, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<AccRuleHdrResponse> create(AccRuleHdrCreateRequest request) {

        // Validate header fields
        validateSourceModule(request.getSourceModule());
        validateSourceDocType(request.getSourceDocType());

        // R-004: Unique active rule per combination
        if (accRuleHdrRepository.existsActiveRuleForCombination(
                request.getCompanyIdFk(), request.getSourceModule().toUpperCase(),
                request.getSourceDocType().toUpperCase())) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                    GlErrorCodes.GL_DUPLICATE_ACTIVE_RULE,
                    request.getCompanyIdFk(), request.getSourceModule(), request.getSourceDocType());
        }

        // Validate lines
        validateLines(request.getLines());

        AccRuleHdr entity = accRuleHdrMapper.toEntity(request);
        AccRuleHdr saved = accRuleHdrRepository.save(entity);

        log.info("Accounting rule created: ruleId={}, company={}, module={}, docType={}",
                saved.getRuleId(), saved.getCompanyIdFk(), saved.getSourceModule(),
                saved.getSourceDocType());

        return ServiceResult.success(accRuleHdrMapper.toResponse(saved), Status.CREATED);
    }

    // ==================== UPDATE ====================

    @Transactional
    @CacheEvict(cacheNames = "accountingRules", allEntries = true)
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_RULE_UPDATE, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<AccRuleHdrResponse> update(Long ruleId, AccRuleHdrUpdateRequest request) {

        AccRuleHdr entity = accRuleHdrRepository.findByIdWithLines(ruleId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, GlErrorCodes.GL_RULE_NOT_FOUND, ruleId));

        // Validate header fields
        validateSourceModule(request.getSourceModule());
        validateSourceDocType(request.getSourceDocType());

        // R-004: Check for duplicate (exclude self)
        if (accRuleHdrRepository.existsActiveRuleForCombinationExcluding(
                request.getCompanyIdFk(), request.getSourceModule().toUpperCase(),
                request.getSourceDocType().toUpperCase(), ruleId)) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                    GlErrorCodes.GL_DUPLICATE_ACTIVE_RULE,
                    request.getCompanyIdFk(), request.getSourceModule(), request.getSourceDocType());
        }

        // R-019: Check if rule is used in completed postings (POSTED or REVERSED automatic journals)
        boolean ruleUsedInPostings = journalHdrRepository.existsBySourcePostingIdFkAndJournalTypeIdFkAndStatusIdFkIn(
                ruleId, "AUTOMATIC", List.of("POSTED", "REVERSED"));
        if (ruleUsedInPostings) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_RULE_IN_USE, ruleId);
        }

        // Validate lines
        validateLines(request.getLines());

        accRuleHdrMapper.updateEntity(entity, request);
        AccRuleHdr updated = accRuleHdrRepository.save(entity);

        log.info("Accounting rule updated: ruleId={}", ruleId);
        return ServiceResult.success(accRuleHdrMapper.toResponse(updated), Status.UPDATED);
    }

    // ==================== DEACTIVATE ====================

    @Transactional
    @CacheEvict(cacheNames = "accountingRules", allEntries = true)
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_RULE_DELETE, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<AccRuleHdrResponse> deactivate(Long ruleId) {

        AccRuleHdr entity = accRuleHdrRepository.findById(ruleId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, GlErrorCodes.GL_RULE_NOT_FOUND, ruleId));

        // R-020: Cannot deactivate if has pending postings (DRAFT or APPROVED automatic journals)
        boolean hasPendingPostings = journalHdrRepository.existsBySourcePostingIdFkAndJournalTypeIdFkAndStatusIdFkIn(
                ruleId, "AUTOMATIC", List.of("DRAFT", "APPROVED"));
        if (hasPendingPostings) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_RULE_HAS_PENDING_POSTINGS, ruleId);
        }

        // R-021: Soft delete
        entity.setIsActive(Boolean.FALSE);
        AccRuleHdr saved = accRuleHdrRepository.save(entity);

        // R-022: Audit trail
        log.info("Accounting rule deactivated: ruleId={}", ruleId);

        return ServiceResult.success(AccRuleHdrResponse.builder()
                .ruleId(saved.getRuleId())
                .isActive(Boolean.FALSE)
                .build(), Status.UPDATED);
    }

    // ==================== GET BY ID ====================

    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_RULE_VIEW, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<AccRuleHdrResponse> getById(Long ruleId) {
        AccRuleHdr entity = accRuleHdrRepository.findByIdWithLines(ruleId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, GlErrorCodes.GL_RULE_NOT_FOUND, ruleId));
        return ServiceResult.success(accRuleHdrMapper.toResponse(entity));
    }

    // ==================== SEARCH ====================

    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_RULE_VIEW, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<Page<AccRuleHdrResponse>> search(SearchRequest searchRequest) {

        SearchRequest normalizedSearchRequest = normalizeSearchRequest(searchRequest);

        Specification<AccRuleHdr> spec = SpecBuilder.build(
            normalizedSearchRequest,
            new SetAllowedFields(ALLOWED_SEARCH_FIELDS),
            DefaultFieldValueConverter.INSTANCE
        );

        Pageable pageable = PageableBuilder.from(normalizedSearchRequest, ALLOWED_SORT_FIELDS, DEFAULT_SORT_FIELD);

        Page<AccRuleHdr> rules = accRuleHdrRepository.findAll(spec, pageable);
        return ServiceResult.success(rules.map(accRuleHdrMapper::toListResponse));
    }

    private SearchRequest normalizeSearchRequest(SearchRequest searchRequest) {
        SearchRequest normalized = searchRequest != null ? searchRequest : new SearchRequest();

        String sortBy = normalized.getSortBy();
        if (sortBy == null || sortBy.isBlank() || "id".equalsIgnoreCase(sortBy)) {
            normalized.setSortBy(DEFAULT_SORT_FIELD);
        }

        if (normalized.getSortDir() == null || normalized.getSortDir().isBlank()) {
            normalized.setSortDir("DESC");
        }

        return normalized;
    }

    // ==================== LINE VALIDATION ====================

    private void validateLines(List<AccRuleLineRequest> lines) {
        // R-006: At least one line
        if (lines == null || lines.isEmpty()) {
            throw new LocalizedException(Status.BAD_REQUEST, GlErrorCodes.GL_RULE_NO_LINES);
        }

        // R-017: Must have both DEBIT and CREDIT sides
        boolean hasDebit = lines.stream().anyMatch(l -> "DEBIT".equalsIgnoreCase(l.getEntrySide()));
        boolean hasCredit = lines.stream().anyMatch(l -> "CREDIT".equalsIgnoreCase(l.getEntrySide()));
        if (!hasDebit || !hasCredit) {
            throw new LocalizedException(Status.BAD_REQUEST, GlErrorCodes.GL_RULE_MISSING_SIDES);
        }

        // Collect accountIds for batch validation
        Set<Long> accountIds = lines.stream()
                .map(AccRuleLineRequest::getAccountIdFk)
                .collect(Collectors.toSet());

        List<AccountsChart> accounts = accountsChartRepository.findAllById(accountIds);
        Map<Long, AccountsChart> accountMap = accounts.stream()
                .collect(Collectors.toMap(AccountsChart::getAccountChartPk, a -> a));

        Set<Integer> priorities = new HashSet<>();

        for (AccRuleLineRequest line : lines) {
            // R-009: entrySide must be a valid ENTRY_SIDE lookup value (e.g. DEBIT, CREDIT)
            lookupValidationApi.validateOrThrow(LK_ENTRY_SIDE, line.getEntrySide());

            // R-010: amountSourceType must be a valid AMOUNT_SOURCE_TYPE lookup value
            String amountType = line.getAmountSourceType().toUpperCase();
            lookupValidationApi.validateOrThrow(LK_AMOUNT_SOURCE_TYPE, amountType);

            // R-010c: entityType validation (optional field)
            if (line.getEntityType() != null && !line.getEntityType().isBlank()) {
                lookupValidationApi.validateOrThrow(LK_ENTITY_TYPE, line.getEntityType());
            }

            // R-011: TOTAL requires null amountSourceValue
            if ("TOTAL".equals(amountType) && line.getAmountSourceValue() != null) {
                throw new LocalizedException(Status.BAD_REQUEST,
                        GlErrorCodes.GL_RULE_AMOUNT_TOTAL_NO_VALUE);
            }

            // R-012: FIXED requires value > 0
            if ("FIXED".equals(amountType)) {
                if (line.getAmountSourceValue() == null || line.getAmountSourceValue().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new LocalizedException(Status.BAD_REQUEST,
                            GlErrorCodes.GL_RULE_AMOUNT_FIXED_POSITIVE);
                }
            }

            // R-013: PERCENT requires 0 < value < 1
            if ("PERCENT".equals(amountType)) {
                if (line.getAmountSourceValue() == null
                        || line.getAmountSourceValue().compareTo(BigDecimal.ZERO) <= 0
                        || line.getAmountSourceValue().compareTo(BigDecimal.ONE) >= 0) {
                    throw new LocalizedException(Status.BAD_REQUEST,
                            GlErrorCodes.GL_RULE_AMOUNT_PERCENT_RANGE);
                }
            }

            // R-014b: REMAINING requires null amountSourceValue (like TOTAL - computed at posting time)
            if ("REMAINING".equals(amountType) && line.getAmountSourceValue() != null) {
                throw new LocalizedException(Status.BAD_REQUEST,
                        GlErrorCodes.GL_RULE_AMOUNT_REMAINING_NO_VALUE);
            }

            // R-014: Priority must be positive and unique
            if (line.getPriority() == null || line.getPriority() <= 0) {
                throw new LocalizedException(Status.BAD_REQUEST,
                        GlErrorCodes.GL_RULE_INVALID_PRIORITY);
            }
            if (!priorities.add(line.getPriority())) {
                throw new LocalizedException(Status.BAD_REQUEST,
                        GlErrorCodes.GL_RULE_DUPLICATE_PRIORITY, line.getPriority());
            }

            // R-007: accountIdFk must reference an existing active account
            AccountsChart account = accountMap.get(line.getAccountIdFk());
            if (account == null) {
                throw new LocalizedException(Status.NOT_FOUND,
                        GlErrorCodes.GL_ACCOUNT_NOT_FOUND, line.getAccountIdFk());
            }

            // BP-010: Must be active
            if (!Boolean.TRUE.equals(account.getIsActive())) {
                throw new LocalizedException(Status.BAD_REQUEST,
                        GlErrorCodes.GL_INACTIVE_ACCOUNT, account.getAccountChartNo());
            }

            // R-008 / BP-011: Must be leaf account (no children)
            if (accountsChartRepository.hasChildren(account.getAccountChartPk())) {
                throw new LocalizedException(Status.BAD_REQUEST,
                        GlErrorCodes.GL_ACCOUNT_NOT_LEAF, account.getAccountChartNo());
            }
        }
    }

    private void validateSourceModule(String sourceModule) {
        lookupValidationApi.validateOrThrow(LK_SOURCE_MODULE, sourceModule);
    }

    private void validateSourceDocType(String sourceDocType) {
        lookupValidationApi.validateOrThrow(LK_SOURCE_DOC_TYPE, sourceDocType);
    }
}
