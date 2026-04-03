package com.example.erp.finance.gl.service;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.example.erp.finance.gl.dto.JournalPreviewResponse;
import com.example.erp.finance.gl.dto.PostingRequest;
import com.example.erp.finance.gl.dto.PostingResponse;
import com.example.erp.finance.gl.entity.AccPostingMst;
import com.example.erp.finance.gl.entity.GlJournalHdr;
import com.example.erp.finance.gl.exception.GlErrorCodes;
import com.example.erp.finance.gl.mapper.GlJournalMapper;
import com.example.erp.finance.gl.repository.AccPostingMstRepository;
import com.example.erp.finance.gl.repository.GlJournalHdrRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Bridge service for generating GL Journal entries from ACC_POSTING_MST records.
 * <p>
 * This is separate from PostingEngineService (which handles direct rule-based execution).
 * The flow:
 * <ol>
 *   <li>Load ACC_POSTING_MST + details</li>
 *   <li>Validate status = READY_FOR_GL</li>
 *   <li>Idempotency check: posting.finJournalIdFk must be null</li>
 *   <li>Construct PostingRequest from posting data</li>
 *   <li>Delegate to PostingEngineService.executeForPosting(request, postingId)</li>
 *   <li>Update posting: finJournalIdFk = journal.id, status = JOURNAL_CREATED</li>
 * </ol>
 * <p>
 * Posting lifecycle:
 * READY_FOR_GL → JOURNAL_CREATED (after journal creation) → POSTED (after journal POST in GlJournalService)
 *
 * @architecture Service — bridge between ACC_POSTING_MST and GL_JOURNAL_HDR
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class JournalGenerationService {

    private final AccPostingMstRepository postingMstRepository;
    private final GlJournalHdrRepository journalHdrRepository;
    private final PostingEngineService postingEngineService;
    private final GlJournalMapper journalMapper;

    private static final String STATUS_READY_FOR_GL = "READY_FOR_GL";
    private static final String STATUS_JOURNAL_CREATED = "JOURNAL_CREATED";
    private static final String STATUS_POSTED = "POSTED";
    private static final String STATUS_ERROR = "ERROR";

    /**
     * Generate a GL Journal from an ACC_POSTING_MST record.
     *
     * @param postingId the ACC_POSTING_MST.POSTING_ID
     * @return ServiceResult containing the generated journal wrapped in PostingResponse
     */
    @Transactional
    @PreAuthorize("hasAnyAuthority("
            + "T(com.example.security.constants.SecurityPermissions).GL_POSTING_CREATE, "
            + "T(com.example.security.constants.SecurityPermissions).GL_POSTING_UPDATE, "
            + "T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<PostingResponse> createJournalFromPosting(Long postingId) {

        log.info("JournalGeneration: starting for postingId={}", postingId);

        // ── 1. Load posting with details ────────────────────
        AccPostingMst posting = postingMstRepository.findByIdWithDetails(postingId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND,
                        GlErrorCodes.GL_POSTING_NOT_FOUND, postingId));

        // ── 2. Validate status ──────────────────────────────
        if (!STATUS_READY_FOR_GL.equals(posting.getStatus())) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_POSTING_NOT_READY, postingId, posting.getStatus());
        }

        // ── 3. Idempotency check ────────────────────────────
        if (posting.getFinJournalIdFk() != null) {
            throw new LocalizedException(Status.CONFLICT,
                    GlErrorCodes.GL_POSTING_JOURNAL_ALREADY_EXISTS,
                    postingId, posting.getFinJournalIdFk());
        }

        // ── 4. Validate details exist ───────────────────────
        if (posting.getDetails() == null || posting.getDetails().isEmpty()) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_POSTING_NO_DETAILS_FOR_JOURNAL, postingId);
        }

        // ── 5. Build PostingRequest from ACC_POSTING_MST ────
        PostingRequest request = PostingRequest.builder()
                .companyIdFk(posting.getCompanyIdFk())
                .sourceModule(posting.getSourceModule())
                .sourceDocType(posting.getSourceDocType())
                .totalAmount(posting.getTotalAmount())
                .journalDate(posting.getDocDate())
                .description(String.format("Posting %s: %s/%s - %s",
                        postingId, posting.getSourceModule(),
                        posting.getSourceDocType(),
                        posting.getSourceDocNo() != null ? posting.getSourceDocNo() : ""))
                .sourceDocIdFk(posting.getSourceDocId())
                .entityMap(buildEntityMap(posting))
                .build();

        // ── 6. Execute via PostingEngine ────────────────────
        ServiceResult<PostingResponse> result;
        try {
            result = postingEngineService.executeForPosting(request, postingId);
        } catch (LocalizedException e) {
            // Mark posting as ERROR with the error message
            posting.setStatus(STATUS_ERROR);
            posting.setErrorMessage(e.getMessageKey());
            postingMstRepository.save(posting);
            log.error("JournalGeneration: failed for postingId={}, error={}",
                    postingId, e.getMessageKey());
            throw e;
        }

        // ── 7. Link posting to generated journal ────────────
        PostingResponse response = result.getData();
        Long journalId = response.getJournal().getId();

        posting.setFinJournalIdFk(journalId);
        posting.setStatus(STATUS_JOURNAL_CREATED);
        posting.setErrorMessage(null);
        postingMstRepository.save(posting);

        log.info("JournalGeneration: completed — postingId={}, journalId={}, journalNo={}, postingStatus=JOURNAL_CREATED",
                postingId, journalId, response.getJournal().getJournalNo());

        return ServiceResult.success(response, Status.CREATED);
    }

    /**
     * Preview a journal that WOULD be generated from an ACC_POSTING_MST record.
     * Runs the full rule engine simulation without persisting anything.
     *
     * @param postingId the ACC_POSTING_MST.POSTING_ID
     * @return ServiceResult containing the journal preview
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority("
            + "T(com.example.security.constants.SecurityPermissions).GL_POSTING_CREATE, "
            + "T(com.example.security.constants.SecurityPermissions).GL_POSTING_UPDATE, "
            + "T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<JournalPreviewResponse> previewJournalFromPosting(Long postingId) {

        log.info("JournalGeneration: previewing for postingId={}", postingId);

        // ── 1. Load posting with details ────────────────────
        AccPostingMst posting = postingMstRepository.findByIdWithDetails(postingId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND,
                        GlErrorCodes.GL_POSTING_NOT_FOUND, postingId));

        // ── 2. Validate status (allow READY_FOR_GL only) ────
        if (!STATUS_READY_FOR_GL.equals(posting.getStatus())) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_POSTING_NOT_READY, postingId, posting.getStatus());
        }

        // ── 3. Idempotency: preview blocks if journal already generated ──
        if (posting.getFinJournalIdFk() != null) {
            throw new LocalizedException(Status.CONFLICT,
                    GlErrorCodes.GL_POSTING_JOURNAL_ALREADY_EXISTS,
                    postingId, posting.getFinJournalIdFk());
        }

        // ── 4. Validate details exist ───────────────────────
        if (posting.getDetails() == null || posting.getDetails().isEmpty()) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_POSTING_NO_DETAILS_FOR_JOURNAL, postingId);
        }

        // ── 5. Build PostingRequest from ACC_POSTING_MST ────
        PostingRequest request = PostingRequest.builder()
                .companyIdFk(posting.getCompanyIdFk())
                .sourceModule(posting.getSourceModule())
                .sourceDocType(posting.getSourceDocType())
                .totalAmount(posting.getTotalAmount())
                .journalDate(posting.getDocDate())
                .description(String.format("Posting %s: %s/%s - %s",
                        postingId, posting.getSourceModule(),
                        posting.getSourceDocType(),
                        posting.getSourceDocNo() != null ? posting.getSourceDocNo() : ""))
                .sourceDocIdFk(posting.getSourceDocId())
                .entityMap(buildEntityMap(posting))
                .build();

        // ── 6. Preview via PostingEngine (no DB write) ──────
        return postingEngineService.previewForPosting(request, postingId);
    }

    /**
     * Build entity map from posting details for PostingEngineService.
     * <p>
     * ARCHITECTURE NOTE: ACC_POSTING_DTL is NOT a source of accounting amounts.
     * The Rule Engine (AccRuleHdr/AccRuleLine) is the SOLE source of amounts, accounts, and entry sides.
     * Posting detail lines are used ONLY for:
     * <ul>
     *   <li>Entity extraction (customerIdFk, supplierIdFk) — passed to the engine for entity-aware rules</li>
     *   <li>Future extensibility (cost center, contract references, etc.)</li>
     * </ul>
     * The detail's amount, sign, and businessSide fields are descriptive metadata from the source module,
     * NOT inputs to the posting engine calculations.
     */
    private java.util.Map<String, Long> buildEntityMap(AccPostingMst posting) {
        java.util.Map<String, Long> entityMap = new java.util.HashMap<>();

        for (var detail : posting.getDetails()) {
            if (detail.getCustomerIdFk() != null && !entityMap.containsKey("CUSTOMER")) {
                entityMap.put("CUSTOMER", detail.getCustomerIdFk());
            }
            if (detail.getSupplierIdFk() != null && !entityMap.containsKey("VENDOR")) {
                entityMap.put("VENDOR", detail.getSupplierIdFk());
            }
        }

        return entityMap.isEmpty() ? Collections.emptyMap() : entityMap;
    }
}
