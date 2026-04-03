package com.example.erp.finance.gl.service;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.erp.common.search.*;
import com.example.erp.finance.gl.dto.*;
import com.example.erp.finance.gl.entity.AccountsChart;
import com.example.erp.finance.gl.entity.GlJournalHdr;
import com.example.erp.finance.gl.entity.GlJournalLine;
import com.example.erp.finance.gl.exception.GlErrorCodes;
import com.example.erp.finance.gl.mapper.GlJournalMapper;
import com.example.erp.finance.gl.repository.AccPostingMstRepository;
import com.example.erp.finance.gl.repository.AccountsChartRepository;
import com.example.erp.finance.gl.repository.GlJournalHdrRepository;
import com.example.erp.finance.gl.entity.AccPostingMst;
import com.example.masterdata.api.LookupValidationApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class GlJournalService {

    private final GlJournalHdrRepository journalHdrRepository;
    private final AccountsChartRepository accountsChartRepository;
    private final AccPostingMstRepository postingMstRepository;
    private final GlJournalMapper journalMapper;
    private final LookupValidationApi lookupValidationApi;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "journalNo", "journalDate", "journalTypeIdFk",
            "statusIdFk", "sourceModuleIdFk", "totalDebit", "totalCredit",
            "activeFl", "createdAt", "updatedAt"
    );

    private static final Set<String> ALLOWED_SEARCH_FIELDS = Set.of(
            "journalNo", "journalDate", "journalTypeIdFk", "statusIdFk",
            "sourceModuleIdFk", "activeFl"
    );

    // Lookup keys
    private static final String LK_JOURNAL_TYPE = "GL_JOURNAL_TYPE";
    private static final String LK_JOURNAL_STATUS = "GL_JOURNAL_STATUS";
    private static final String LK_SOURCE_MODULE = "SOURCE_MODULE";

    // Status constants
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_POSTED = "POSTED";
    private static final String STATUS_REVERSED = "REVERSED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final String TYPE_AUTOMATIC = "AUTOMATIC";
    private static final String TYPE_REVERSAL = "REVERSAL";

    // Posting status constants (for lifecycle sync)
    private static final String POSTING_STATUS_JOURNAL_CREATED = "JOURNAL_CREATED";
    private static final String POSTING_STATUS_READY_FOR_POST = "READY_FOR_POST";
    private static final String POSTING_STATUS_POSTED = "POSTED";
    private static final String POSTING_STATUS_REVERSED = "REVERSED";
    private static final String POSTING_STATUS_CANCELLED = "CANCELLED";

    // ==================== CREATE ====================

    @Transactional
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_JOURNAL_CREATE, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<GlJournalHdrResponse> create(GlJournalHdrCreateRequest request) {

        // Validate journal type
        lookupValidationApi.validateOrThrow(LK_JOURNAL_TYPE, request.getJournalTypeIdFk());

        // Validate source module if provided
        if (request.getSourceModuleIdFk() != null && !request.getSourceModuleIdFk().isBlank()) {
            lookupValidationApi.validateOrThrow(LK_SOURCE_MODULE, request.getSourceModuleIdFk());
        }

        // AUTOMATIC journal requires sourcePostingIdFk
        if (TYPE_AUTOMATIC.equalsIgnoreCase(request.getJournalTypeIdFk())
                && request.getSourcePostingIdFk() == null) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_JOURNAL_AUTOMATIC_REQUIRES_SOURCE);
        }

        // Validate lines
        validateLines(request.getLines());

        // Generate journal number
        String journalNo = journalHdrRepository.generateJournalNo();

        GlJournalHdr entity = journalMapper.toEntity(request, journalNo);

        // Validate balance
        if (!entity.isBalanced()) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_JOURNAL_NOT_BALANCED);
        }

        GlJournalHdr saved = journalHdrRepository.save(entity);

        log.info("Journal created: journalId={}, journalNo={}, type={}",
                saved.getId(), saved.getJournalNo(), saved.getJournalTypeIdFk());

        return ServiceResult.success(journalMapper.toResponse(saved), Status.CREATED);
    }

    // ==================== UPDATE ====================

    @Transactional
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_JOURNAL_UPDATE, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<GlJournalHdrResponse> update(Long journalId, GlJournalHdrUpdateRequest request) {

        GlJournalHdr entity = journalHdrRepository.findByIdWithLines(journalId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND,
                        GlErrorCodes.GL_JOURNAL_NOT_FOUND, journalId));

        // Only DRAFT journals can be updated
        if (!STATUS_DRAFT.equals(entity.getStatusIdFk())) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_JOURNAL_NOT_DRAFT);
        }

        // Journal type cannot be changed after creation
        if (!entity.getJournalTypeIdFk().equalsIgnoreCase(request.getJournalTypeIdFk())) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_JOURNAL_TYPE_IMMUTABLE);
        }

        // AUTOMATIC journals: only description + journalDate are editable (lines stay unchanged)
        if (TYPE_AUTOMATIC.equalsIgnoreCase(entity.getJournalTypeIdFk())) {
            entity.setDescription(request.getDescription());
            entity.setJournalDate(request.getJournalDate());
            GlJournalHdr updated = journalHdrRepository.save(entity);
            log.info("Automatic journal header updated: journalId={}", journalId);
            return ServiceResult.success(journalMapper.toResponse(updated), Status.UPDATED);
        }

        // Validate journal type
        lookupValidationApi.validateOrThrow(LK_JOURNAL_TYPE, request.getJournalTypeIdFk());

        // Validate source module if provided
        if (request.getSourceModuleIdFk() != null && !request.getSourceModuleIdFk().isBlank()) {
            lookupValidationApi.validateOrThrow(LK_SOURCE_MODULE, request.getSourceModuleIdFk());
        }

        // Validate lines
        validateLines(request.getLines());

        journalMapper.updateEntity(entity, request);

        // Validate balance
        if (!entity.isBalanced()) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_JOURNAL_NOT_BALANCED);
        }

        GlJournalHdr updated = journalHdrRepository.save(entity);

        log.info("Journal updated: journalId={}", journalId);
        return ServiceResult.success(journalMapper.toResponse(updated), Status.UPDATED);
    }

    // ==================== GET BY ID ====================

    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_JOURNAL_VIEW, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<GlJournalHdrResponse> getById(Long journalId) {
        GlJournalHdr entity = journalHdrRepository.findByIdWithLines(journalId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND,
                        GlErrorCodes.GL_JOURNAL_NOT_FOUND, journalId));
        return ServiceResult.success(journalMapper.toResponse(entity));
    }

    // ==================== SEARCH ====================

    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_JOURNAL_VIEW, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<Page<GlJournalHdrResponse>> search(SearchRequest searchRequest) {

        Specification<GlJournalHdr> spec = SpecBuilder.build(
                searchRequest,
                new SetAllowedFields(ALLOWED_SEARCH_FIELDS),
                DefaultFieldValueConverter.INSTANCE
        );

        Pageable pageable = PageableBuilder.from(searchRequest, ALLOWED_SORT_FIELDS);

        Page<GlJournalHdr> journals = journalHdrRepository.findAll(spec, pageable);
        return ServiceResult.success(journals.map(journalMapper::toListResponse));
    }

    // ==================== TOGGLE ACTIVE ====================

    @Transactional
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_JOURNAL_DELETE, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<GlJournalHdrResponse> toggleActive(Long journalId, Boolean active) {

        GlJournalHdr entity = journalHdrRepository.findById(journalId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND,
                        GlErrorCodes.GL_JOURNAL_NOT_FOUND, journalId));

        // Cannot deactivate POSTED journals
        if (STATUS_POSTED.equals(entity.getStatusIdFk())) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_JOURNAL_POSTED_IMMUTABLE);
        }

        if (Boolean.FALSE.equals(active)) {
            entity.deactivate();
        } else {
            entity.activate();
        }

        GlJournalHdr saved = journalHdrRepository.save(entity);

        log.info("Journal toggled active={}: journalId={}", active, journalId);

        return ServiceResult.success(journalMapper.toResponse(saved), Status.UPDATED);
    }

    // ==================== APPROVE ====================

    @Transactional
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_JOURNAL_APPROVE, T(com.example.security.constants.SecurityPermissions).GL_JOURNAL_UPDATE, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<GlJournalHdrResponse> approve(Long journalId) {

        GlJournalHdr entity = journalHdrRepository.findByIdWithLines(journalId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND,
                        GlErrorCodes.GL_JOURNAL_NOT_FOUND, journalId));

        if (!STATUS_DRAFT.equals(entity.getStatusIdFk())) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_JOURNAL_NOT_DRAFT);
        }

        // Validate balance before approval
        if (!entity.isBalanced()) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_JOURNAL_NOT_BALANCED);
        }

        entity.setStatusIdFk(STATUS_APPROVED);
        GlJournalHdr saved = journalHdrRepository.save(entity);

        // ── Sync posting lifecycle: JOURNAL_CREATED → READY_FOR_POST ──
        syncPostingStatusOnApprove(entity.getSourcePostingIdFk());

        log.info("Journal approved: journalId={}", journalId);
        return ServiceResult.success(journalMapper.toResponse(saved), Status.UPDATED);
    }

    // ==================== POST ====================

    @Transactional
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_JOURNAL_POST, T(com.example.security.constants.SecurityPermissions).GL_JOURNAL_UPDATE, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<GlJournalHdrResponse> post(Long journalId) {

        GlJournalHdr entity = journalHdrRepository.findByIdWithLines(journalId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND,
                        GlErrorCodes.GL_JOURNAL_NOT_FOUND, journalId));

        if (!STATUS_APPROVED.equals(entity.getStatusIdFk())) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_JOURNAL_NOT_APPROVED);
        }

        // Validate balance before posting
        if (!entity.isBalanced()) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_JOURNAL_NOT_BALANCED);
        }

        entity.setStatusIdFk(STATUS_POSTED);
        GlJournalHdr saved = journalHdrRepository.save(entity);

        // ── Sync posting lifecycle: JOURNAL_CREATED → POSTED ──
        syncPostingStatusOnPost(entity.getSourcePostingIdFk());

        log.info("Journal posted: journalId={}", journalId);
        return ServiceResult.success(journalMapper.toResponse(saved), Status.UPDATED);
    }

    // ==================== REVERSE ====================

    @Transactional
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_JOURNAL_REVERSE, T(com.example.security.constants.SecurityPermissions).GL_JOURNAL_UPDATE, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<GlJournalHdrResponse> reverse(Long journalId) {

        GlJournalHdr original = journalHdrRepository.findByIdWithLines(journalId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND,
                        GlErrorCodes.GL_JOURNAL_NOT_FOUND, journalId));

        if (!STATUS_POSTED.equals(original.getStatusIdFk())) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_JOURNAL_REVERSE_REQUIRES_POSTED);
        }

        // Mark original as REVERSED
        original.setStatusIdFk(STATUS_REVERSED);
        journalHdrRepository.save(original);

        // ── Sync posting lifecycle: POSTED → REVERSED ──
        syncPostingStatusOnReverse(original.getSourcePostingIdFk());

        // Create reversal journal
        String reversalJournalNo = journalHdrRepository.generateJournalNo();
        GlJournalHdr reversal = GlJournalHdr.builder()
                .journalNo(reversalJournalNo)
                .journalDate(original.getJournalDate())
                .journalTypeIdFk(TYPE_REVERSAL)
                .statusIdFk(STATUS_POSTED)
                .description("Reversal of " + original.getJournalNo())
                .sourceModuleIdFk(original.getSourceModuleIdFk())
                .sourceDocTypeId(original.getSourceDocTypeId())
                .sourceDocIdFk(original.getSourceDocIdFk())
                .sourcePostingIdFk(original.getSourcePostingIdFk())
                .totalDebit(BigDecimal.ZERO)
                .totalCredit(BigDecimal.ZERO)
                .activeFl(Boolean.TRUE)
                .build();

        int lineNo = 1;
        for (GlJournalLine origLine : original.getLines()) {
            GlJournalLine reversalLine = GlJournalLine.builder()
                    .lineNo(lineNo++)
                    .accountIdFk(origLine.getAccountIdFk())
                    // Swap debit ↔ credit
                    .debitAmount(origLine.getCreditAmount())
                    .creditAmount(origLine.getDebitAmount())
                    .customerIdFk(origLine.getCustomerIdFk())
                    .supplierIdFk(origLine.getSupplierIdFk())
                    .costCenterIdFk(origLine.getCostCenterIdFk())
                    .description(origLine.getDescription())
                    .build();
            reversal.addLine(reversalLine);
        }
        reversal.recalculateTotals();

        GlJournalHdr savedReversal = journalHdrRepository.save(reversal);

        log.info("Journal reversed: originalId={}, reversalId={}, reversalNo={}",
                journalId, savedReversal.getId(), savedReversal.getJournalNo());

        return ServiceResult.success(journalMapper.toResponse(savedReversal), Status.CREATED);
    }

    // ==================== CANCEL ====================

    @Transactional
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_JOURNAL_CANCEL, T(com.example.security.constants.SecurityPermissions).GL_JOURNAL_UPDATE, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<GlJournalHdrResponse> cancel(Long journalId) {

        GlJournalHdr entity = journalHdrRepository.findById(journalId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND,
                        GlErrorCodes.GL_JOURNAL_NOT_FOUND, journalId));

        // Cannot cancel POSTED journals (use reverse instead)
        if (STATUS_POSTED.equals(entity.getStatusIdFk())) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_JOURNAL_POSTED_IMMUTABLE);
        }

        // Cannot cancel already cancelled/reversed journals
        if (STATUS_CANCELLED.equals(entity.getStatusIdFk()) || STATUS_REVERSED.equals(entity.getStatusIdFk())) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_JOURNAL_ALREADY_FINALIZED);
        }

        entity.setStatusIdFk(STATUS_CANCELLED);
        GlJournalHdr saved = journalHdrRepository.save(entity);

        // ── Sync posting lifecycle: JOURNAL_CREATED/READY_FOR_POST → CANCELLED ──
        syncPostingStatusOnCancel(entity.getSourcePostingIdFk());

        log.info("Journal cancelled: journalId={}", journalId);
        return ServiceResult.success(journalMapper.toResponse(saved), Status.UPDATED);
    }

    // ==================== POSTING LIFECYCLE SYNC ====================

    /**
     * When a journal is APPROVED, update the linked ACC_POSTING_MST status from JOURNAL_CREATED to READY_FOR_POST.
     * Only applies to AUTOMATIC journals that have a sourcePostingIdFk.
     */
    private void syncPostingStatusOnApprove(Long sourcePostingIdFk) {
        if (sourcePostingIdFk == null) return;
        postingMstRepository.findById(sourcePostingIdFk).ifPresent(posting -> {
            if (POSTING_STATUS_JOURNAL_CREATED.equals(posting.getStatus())) {
                posting.setStatus(POSTING_STATUS_READY_FOR_POST);
                postingMstRepository.save(posting);
                log.info("Posting lifecycle sync: postingId={} → READY_FOR_POST (journal approved)", sourcePostingIdFk);
            }
        });
    }

    /**
     * When a journal is POSTED, update the linked ACC_POSTING_MST status to POSTED.
     * Only applies to AUTOMATIC journals that have a sourcePostingIdFk.
     */
    private void syncPostingStatusOnPost(Long sourcePostingIdFk) {
        if (sourcePostingIdFk == null) return;
        postingMstRepository.findById(sourcePostingIdFk).ifPresent(posting -> {
            if (POSTING_STATUS_READY_FOR_POST.equals(posting.getStatus())
                    || POSTING_STATUS_JOURNAL_CREATED.equals(posting.getStatus())) {
                posting.setStatus(POSTING_STATUS_POSTED);
                postingMstRepository.save(posting);
                log.info("Posting lifecycle sync: postingId={} → POSTED (journal posted)", sourcePostingIdFk);
            }
        });
    }

    /**
     * When a journal is REVERSED, update the linked ACC_POSTING_MST status to REVERSED.
     * Only applies to AUTOMATIC journals that have a sourcePostingIdFk.
     */
    private void syncPostingStatusOnReverse(Long sourcePostingIdFk) {
        if (sourcePostingIdFk == null) return;
        postingMstRepository.findById(sourcePostingIdFk).ifPresent(posting -> {
            if (POSTING_STATUS_POSTED.equals(posting.getStatus())) {
                posting.setStatus(POSTING_STATUS_REVERSED);
                postingMstRepository.save(posting);
                log.info("Posting lifecycle sync: postingId={} → REVERSED (journal reversed)", sourcePostingIdFk);
            }
        });
    }

    /**
     * When a journal is CANCELLED, update the linked ACC_POSTING_MST status to CANCELLED.
     * Only applies when posting is in JOURNAL_CREATED or READY_FOR_POST state.
     */
    private void syncPostingStatusOnCancel(Long sourcePostingIdFk) {
        if (sourcePostingIdFk == null) return;
        postingMstRepository.findById(sourcePostingIdFk).ifPresent(posting -> {
            if (POSTING_STATUS_JOURNAL_CREATED.equals(posting.getStatus())
                    || POSTING_STATUS_READY_FOR_POST.equals(posting.getStatus())) {
                posting.setStatus(POSTING_STATUS_CANCELLED);
                postingMstRepository.save(posting);
                log.info("Posting lifecycle sync: postingId={} → CANCELLED (journal cancelled)", sourcePostingIdFk);
            }
        });
    }

    // ==================== LINE VALIDATION ====================

    private void validateLines(List<GlJournalLineRequest> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_JOURNAL_NO_LINES);
        }

        // Collect account IDs for batch validation
        Set<Long> accountIds = lines.stream()
                .map(GlJournalLineRequest::getAccountIdFk)
                .collect(Collectors.toSet());

        List<AccountsChart> accounts = accountsChartRepository.findAllById(accountIds);
        Map<Long, AccountsChart> accountMap = accounts.stream()
                .collect(Collectors.toMap(AccountsChart::getAccountChartPk, a -> a));

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (GlJournalLineRequest line : lines) {
            // XOR check: exactly one of debitAmount or creditAmount
            boolean hasDebit = line.getDebitAmount() != null && line.getDebitAmount().compareTo(BigDecimal.ZERO) > 0;
            boolean hasCredit = line.getCreditAmount() != null && line.getCreditAmount().compareTo(BigDecimal.ZERO) > 0;

            if (hasDebit == hasCredit) {
                throw new LocalizedException(Status.BAD_REQUEST,
                        GlErrorCodes.GL_JOURNAL_LINE_XOR);
            }

            if (hasDebit) totalDebit = totalDebit.add(line.getDebitAmount());
            if (hasCredit) totalCredit = totalCredit.add(line.getCreditAmount());

            // Account must exist
            AccountsChart account = accountMap.get(line.getAccountIdFk());
            if (account == null) {
                throw new LocalizedException(Status.NOT_FOUND,
                        GlErrorCodes.GL_ACCOUNT_NOT_FOUND, line.getAccountIdFk());
            }

            // Account must be active
            if (!Boolean.TRUE.equals(account.getIsActive())) {
                throw new LocalizedException(Status.BAD_REQUEST,
                        GlErrorCodes.GL_INACTIVE_ACCOUNT, account.getAccountChartNo());
            }

            // Account must be leaf
            if (accountsChartRepository.hasChildren(account.getAccountChartPk())) {
                throw new LocalizedException(Status.BAD_REQUEST,
                        GlErrorCodes.GL_ACCOUNT_NOT_LEAF, account.getAccountChartNo());
            }
        }

        // Balance check
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_JOURNAL_NOT_BALANCED);
        }
    }

}
