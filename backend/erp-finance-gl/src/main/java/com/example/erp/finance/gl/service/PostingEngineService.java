package com.example.erp.finance.gl.service;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.example.erp.finance.gl.dto.JournalPreviewResponse;
import com.example.erp.finance.gl.dto.PostingRequest;
import com.example.erp.finance.gl.dto.PostingResponse;
import com.example.erp.finance.gl.entity.AccRuleHdr;
import com.example.erp.finance.gl.entity.AccRuleLine;
import com.example.erp.finance.gl.entity.AccountsChart;
import com.example.erp.finance.gl.entity.GlJournalHdr;
import com.example.erp.finance.gl.entity.GlJournalLine;
import com.example.erp.finance.gl.exception.GlErrorCodes;
import com.example.erp.finance.gl.mapper.GlJournalMapper;
import com.example.erp.finance.gl.repository.AccRuleHdrRepository;
import com.example.erp.finance.gl.repository.AccountsChartRepository;
import com.example.erp.finance.gl.repository.GlJournalHdrRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dynamic, rule-driven Posting Engine.
 * <p>
 * Given a {@link PostingRequest}, this service:
 * <ol>
 *   <li>Finds the active {@link AccRuleHdr} by (companyIdFk + sourceModule + sourceDocType)</li>
 *   <li>Iterates over {@link AccRuleLine}s ordered by priority</li>
 *   <li>Calculates amounts dynamically:
 *       <ul>
 *           <li>TOTAL → totalAmount (full source amount)</li>
 *           <li>FIXED → amountSourceValue (literal fixed amount)</li>
 *           <li>PERCENT → totalAmount × amountSourceValue</li>
 *           <li>REMAINING → totalAmount − sum(all previously calculated amounts on the same side)</li>
 *       </ul>
 *   </li>
 *   <li>Resolves entityType (CUSTOMER, VENDOR, BANK, etc.) from the request's entityMap</li>
 *   <li>Creates a balanced {@link GlJournalHdr} with type=AUTOMATIC, status=DRAFT</li>
 *   <li>Validates DR == CR balance before saving</li>
 * </ol>
 *
 * @architecture Service — internal posting logic, called by PostingEngineController
 *              or programmatically by other modules via the public API
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PostingEngineService {

    private final AccRuleHdrRepository accRuleHdrRepository;
    private final AccountsChartRepository accountsChartRepository;
    private final GlJournalHdrRepository journalHdrRepository;
    private final GlJournalMapper journalMapper;

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String TYPE_AUTOMATIC = "AUTOMATIC";

    // Amount source type constants
    private static final String AMT_TOTAL = "TOTAL";
    private static final String AMT_FIXED = "FIXED";
    private static final String AMT_PERCENT = "PERCENT";
    private static final String AMT_REMAINING = "REMAINING";

    // ==================== EXECUTE POSTING ====================

    /**
     * Execute a dynamic posting based on accounting rules.
     *
     * @param request the posting request containing source context and totalAmount
     * @return ServiceResult containing the generated journal entry
     */
    @Transactional
    @PreAuthorize("hasAnyAuthority("
            + "T(com.example.security.constants.SecurityPermissions).GL_POSTING_UPDATE, "
            + "T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<PostingResponse> execute(PostingRequest request) {
        return executeInternal(request, null);
    }

    /**
     * Execute a dynamic posting and link the generated journal to a specific posting ID.
     * Used by JournalGenerationService when creating a journal from ACC_POSTING_MST.
     *
     * @param request        the posting request
     * @param sourcePostingId the ACC_POSTING_MST.POSTING_ID to store on the journal's sourcePostingIdFk
     * @return ServiceResult containing the generated journal entry
     */
    @Transactional
    public ServiceResult<PostingResponse> executeForPosting(PostingRequest request, Long sourcePostingId) {
        return executeInternal(request, sourcePostingId);
    }

    /**
     * Preview a journal that WOULD be generated from a posting, without saving to DB.
     * Runs the full rule engine logic (rule lookup, line processing, balance validation)
     * but returns a preview response instead of persisting.
     *
     * @param request        the posting request
     * @param sourcePostingId the ACC_POSTING_MST.POSTING_ID
     * @return ServiceResult containing the preview response
     */
    @Transactional(readOnly = true)
    public ServiceResult<JournalPreviewResponse> previewForPosting(PostingRequest request, Long sourcePostingId) {
        return previewInternal(request, sourcePostingId);
    }

    private ServiceResult<PostingResponse> executeInternal(PostingRequest request, Long sourcePostingId) {

        String module = request.getSourceModule().toUpperCase();
        String docType = request.getSourceDocType().toUpperCase();
        Long companyId = request.getCompanyIdFk();

        log.info("PostingEngine: executing for company={}, module={}, docType={}, amount={}",
                companyId, module, docType, request.getTotalAmount());

        // ── 1. Validate totalAmount ─────────────────────────
        if (request.getTotalAmount() == null || request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_POSTING_ZERO_AMOUNT);
        }

        // ── 2. Find active rule ─────────────────────────────
        AccRuleHdr rule = accRuleHdrRepository
                .findActiveRuleWithLines(companyId, module, docType)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND,
                        GlErrorCodes.GL_POSTING_RULE_NOT_FOUND, companyId, module, docType));

        List<AccRuleLine> ruleLines = rule.getLines();
        if (ruleLines == null || ruleLines.isEmpty()) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_POSTING_NO_RULE_LINES, rule.getRuleId());
        }

        // Sort by priority ascending
        ruleLines.sort(Comparator.comparingInt(AccRuleLine::getPriority));

        log.debug("PostingEngine: found ruleId={} with {} lines",
                rule.getRuleId(), ruleLines.size());

        // ── 3. Validate all referenced accounts upfront ─────
        Set<Long> accountIds = ruleLines.stream()
                .map(AccRuleLine::getAccountIdFk)
                .collect(Collectors.toSet());

        Map<Long, AccountsChart> accountMap = accountsChartRepository
                .findAllById(accountIds).stream()
                .collect(Collectors.toMap(AccountsChart::getAccountChartPk, a -> a));

        for (AccRuleLine rl : ruleLines) {
            AccountsChart account = accountMap.get(rl.getAccountIdFk());
            if (account == null || !Boolean.TRUE.equals(account.getIsActive())) {
                throw new LocalizedException(Status.BAD_REQUEST,
                        GlErrorCodes.GL_POSTING_ACCOUNT_INVALID, rl.getAccountIdFk());
            }
        }

        // ── 4. Process rule lines → journal lines ───────────
        BigDecimal totalAmount = request.getTotalAmount();
        Map<String, Long> entityMap = request.getEntityMap() != null
                ? request.getEntityMap() : Collections.emptyMap();

        // Track cumulative calculated amounts per side for REMAINING calculation
        BigDecimal cumulativeDebit = BigDecimal.ZERO;
        BigDecimal cumulativeCredit = BigDecimal.ZERO;

        List<GlJournalLine> journalLines = new ArrayList<>();
        int lineNo = 1;

        for (AccRuleLine rl : ruleLines) {
            String amountType = rl.getAmountSourceType().toUpperCase();
            String entrySide = rl.getEntrySide().toUpperCase();

            // Calculate amount based on amountSourceType
            BigDecimal lineAmount = calculateAmount(
                    amountType, rl.getAmountSourceValue(), totalAmount,
                    entrySide, cumulativeDebit, cumulativeCredit,
                    rl.getPriority());

            // Skip lines with zero amount (can happen with REMAINING)
            if (lineAmount.compareTo(BigDecimal.ZERO) == 0) {
                log.debug("PostingEngine: line priority={} amount=0, skipping", rl.getPriority());
                continue;
            }

            // Determine debit/credit
            BigDecimal debitAmount = null;
            BigDecimal creditAmount = null;

            if ("DEBIT".equals(entrySide)) {
                debitAmount = lineAmount;
                cumulativeDebit = cumulativeDebit.add(lineAmount);
            } else {
                creditAmount = lineAmount;
                cumulativeCredit = cumulativeCredit.add(lineAmount);
            }

            // Resolve entity references
            Long customerIdFk = resolveEntity(entityMap, rl.getEntityType(), "CUSTOMER");
            Long supplierIdFk = resolveEntity(entityMap, rl.getEntityType(), "VENDOR");

            GlJournalLine jLine = GlJournalLine.builder()
                    .lineNo(lineNo++)
                    .accountIdFk(rl.getAccountIdFk())
                    .debitAmount(debitAmount)
                    .creditAmount(creditAmount)
                    .customerIdFk(customerIdFk)
                    .supplierIdFk(supplierIdFk)
                    .description(buildLineDescription(rl, accountMap.get(rl.getAccountIdFk())))
                    .build();

            journalLines.add(jLine);

            log.debug("PostingEngine: line#{} priority={} account={} side={} type={} amount={}",
                    lineNo - 1, rl.getPriority(), rl.getAccountIdFk(),
                    entrySide, amountType, lineAmount);
        }

        // ── 5. Build journal header ─────────────────────────
        String journalNo = journalHdrRepository.generateJournalNo();

        GlJournalHdr journal = GlJournalHdr.builder()
                .journalNo(journalNo)
                .journalDate(request.getJournalDate())
                .journalTypeIdFk(TYPE_AUTOMATIC)
                .statusIdFk(STATUS_DRAFT)
                .description(request.getDescription() != null
                        ? request.getDescription()
                        : String.format("Auto-posting: %s/%s", module, docType))
                .sourceModuleIdFk(module)
                .sourceDocTypeId(docType)
                .sourceDocIdFk(request.getSourceDocIdFk())
                .sourcePostingIdFk(sourcePostingId)
                .totalDebit(BigDecimal.ZERO)
                .totalCredit(BigDecimal.ZERO)
                .activeFl(Boolean.TRUE)
                .build();

        journalLines.forEach(journal::addLine);
        journal.recalculateTotals();

        // ── 6. Validate balance ─────────────────────────────
        if (!journal.isBalanced()) {
            log.error("PostingEngine: UNBALANCED journal — DR={}, CR={}",
                    journal.getTotalDebit(), journal.getTotalCredit());
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_POSTING_UNBALANCED,
                    journal.getTotalDebit().toPlainString(),
                    journal.getTotalCredit().toPlainString());
        }

        // ── 7. Save ─────────────────────────────────────────
        GlJournalHdr saved = journalHdrRepository.save(journal);

        log.info("PostingEngine: journal created — journalId={}, journalNo={}, ruleId={}, " +
                        "DR={}, CR={}, lines={}",
                saved.getId(), saved.getJournalNo(), rule.getRuleId(),
                saved.getTotalDebit(), saved.getTotalCredit(), journalLines.size());

        // ── 8. Build response ───────────────────────────────
        PostingResponse response = PostingResponse.builder()
                .ruleId(rule.getRuleId())
                .companyIdFk(companyId)
                .sourceModule(module)
                .sourceDocType(docType)
                .journal(journalMapper.toResponse(saved))
                .build();

        return ServiceResult.success(response, Status.CREATED);
    }

    /**
     * Preview-only version of executeInternal.
     * Runs the full rule engine simulation (rule lookup, line processing, balance check)
     * but does NOT save to DB. Returns a JournalPreviewResponse with the preview lines.
     */
    private ServiceResult<JournalPreviewResponse> previewInternal(PostingRequest request, Long sourcePostingId) {

        String module = request.getSourceModule().toUpperCase();
        String docType = request.getSourceDocType().toUpperCase();
        Long companyId = request.getCompanyIdFk();

        log.info("PostingEngine: previewing for company={}, module={}, docType={}, amount={}",
                companyId, module, docType, request.getTotalAmount());

        // ── 1. Validate totalAmount ─────────────────────────
        if (request.getTotalAmount() == null || request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_POSTING_ZERO_AMOUNT);
        }

        // ── 2. Find active rule ─────────────────────────────
        AccRuleHdr rule = accRuleHdrRepository
                .findActiveRuleWithLines(companyId, module, docType)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND,
                        GlErrorCodes.GL_POSTING_RULE_NOT_FOUND, companyId, module, docType));

        List<AccRuleLine> ruleLines = rule.getLines();
        if (ruleLines == null || ruleLines.isEmpty()) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_POSTING_NO_RULE_LINES, rule.getRuleId());
        }

        ruleLines.sort(Comparator.comparingInt(AccRuleLine::getPriority));

        // ── 3. Validate all referenced accounts upfront ─────
        Set<Long> accountIds = ruleLines.stream()
                .map(AccRuleLine::getAccountIdFk)
                .collect(Collectors.toSet());

        Map<Long, AccountsChart> accountMap = accountsChartRepository
                .findAllById(accountIds).stream()
                .collect(Collectors.toMap(AccountsChart::getAccountChartPk, a -> a));

        for (AccRuleLine rl : ruleLines) {
            AccountsChart account = accountMap.get(rl.getAccountIdFk());
            if (account == null || !Boolean.TRUE.equals(account.getIsActive())) {
                throw new LocalizedException(Status.BAD_REQUEST,
                        GlErrorCodes.GL_POSTING_ACCOUNT_INVALID, rl.getAccountIdFk());
            }
        }

        // ── 4. Process rule lines → journal lines ───────────
        BigDecimal totalAmount = request.getTotalAmount();
        Map<String, Long> entityMap = request.getEntityMap() != null
                ? request.getEntityMap() : Collections.emptyMap();

        BigDecimal cumulativeDebit = BigDecimal.ZERO;
        BigDecimal cumulativeCredit = BigDecimal.ZERO;

        List<GlJournalLine> journalLines = new ArrayList<>();
        int lineNo = 1;

        for (AccRuleLine rl : ruleLines) {
            String amountType = rl.getAmountSourceType().toUpperCase();
            String entrySide = rl.getEntrySide().toUpperCase();

            BigDecimal lineAmount = calculateAmount(
                    amountType, rl.getAmountSourceValue(), totalAmount,
                    entrySide, cumulativeDebit, cumulativeCredit,
                    rl.getPriority());

            if (lineAmount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal debitAmount = null;
            BigDecimal creditAmount = null;

            if ("DEBIT".equals(entrySide)) {
                debitAmount = lineAmount;
                cumulativeDebit = cumulativeDebit.add(lineAmount);
            } else {
                creditAmount = lineAmount;
                cumulativeCredit = cumulativeCredit.add(lineAmount);
            }

            Long customerIdFk = resolveEntity(entityMap, rl.getEntityType(), "CUSTOMER");
            Long supplierIdFk = resolveEntity(entityMap, rl.getEntityType(), "VENDOR");

            GlJournalLine jLine = GlJournalLine.builder()
                    .lineNo(lineNo++)
                    .accountIdFk(rl.getAccountIdFk())
                    .debitAmount(debitAmount)
                    .creditAmount(creditAmount)
                    .customerIdFk(customerIdFk)
                    .supplierIdFk(supplierIdFk)
                    .description(buildLineDescription(rl, accountMap.get(rl.getAccountIdFk())))
                    .build();

            journalLines.add(jLine);
        }

        // ── 5. Build preview lines with account enrichment ──
        List<com.example.erp.finance.gl.dto.GlJournalLineResponse> previewLines = journalLines.stream()
                .map(line -> {
                    AccountsChart account = accountMap.get(line.getAccountIdFk());
                    return com.example.erp.finance.gl.dto.GlJournalLineResponse.builder()
                            .lineNo(line.getLineNo())
                            .accountIdFk(line.getAccountIdFk())
                            .accountCode(account != null ? account.getAccountChartNo() : null)
                            .accountName(account != null ? account.getAccountChartName() : null)
                            .debitAmount(line.getDebitAmount())
                            .creditAmount(line.getCreditAmount())
                            .customerIdFk(line.getCustomerIdFk())
                            .supplierIdFk(line.getSupplierIdFk())
                            .description(line.getDescription())
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalDebit = cumulativeDebit;
        BigDecimal totalCredit = cumulativeCredit;
        boolean balanced = totalDebit.compareTo(totalCredit) == 0;

        String description = request.getDescription() != null
                ? request.getDescription()
                : String.format("Auto-posting: %s/%s", module, docType);

        JournalPreviewResponse preview = JournalPreviewResponse.builder()
                .ruleId(rule.getRuleId())
                .companyIdFk(companyId)
                .sourceModule(module)
                .sourceDocType(docType)
                .isBalanced(balanced)
                .totalDebit(totalDebit)
                .totalCredit(totalCredit)
                .lines(previewLines)
                .description(description)
                .build();

        log.info("PostingEngine: preview complete — ruleId={}, DR={}, CR={}, balanced={}, lines={}",
                rule.getRuleId(), totalDebit, totalCredit, balanced, previewLines.size());

        return ServiceResult.success(preview);
    }

    // ==================== PRIVATE HELPERS ====================

    /**
     * Calculate the line amount based on the amount source type.
     */
    private BigDecimal calculateAmount(String amountType, BigDecimal sourceValue,
                                       BigDecimal totalAmount, String entrySide,
                                       BigDecimal cumulativeDebit, BigDecimal cumulativeCredit,
                                       int priority) {
        return switch (amountType) {
            case AMT_TOTAL -> totalAmount;

            case AMT_FIXED -> {
                if (sourceValue == null || sourceValue.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new LocalizedException(Status.BAD_REQUEST,
                            GlErrorCodes.GL_POSTING_NEGATIVE_AMOUNT, priority);
                }
                yield sourceValue;
            }

            case AMT_PERCENT -> {
                if (sourceValue == null) {
                    throw new LocalizedException(Status.BAD_REQUEST,
                            GlErrorCodes.GL_POSTING_NEGATIVE_AMOUNT, priority);
                }
                yield totalAmount.multiply(sourceValue).setScale(2, RoundingMode.HALF_UP);
            }

            case AMT_REMAINING -> {
                BigDecimal cumulative = "DEBIT".equals(entrySide) ? cumulativeDebit : cumulativeCredit;
                BigDecimal remaining = totalAmount.subtract(cumulative);
                if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                    log.warn("PostingEngine: REMAINING is negative at priority={}, " +
                            "totalAmount={}, cumulative={}", priority, totalAmount, cumulative);
                    throw new LocalizedException(Status.BAD_REQUEST,
                            GlErrorCodes.GL_POSTING_REMAINING_NEGATIVE, priority);
                }
                yield remaining;
            }

            default -> throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_POSTING_UNKNOWN_AMOUNT_TYPE, amountType);
        };
    }

    /**
     * Resolve entity FK (customerIdFk or supplierIdFk) from the entity map.
     * Returns null if the rule line's entityType != targetType or mapping is absent.
     */
    private Long resolveEntity(Map<String, Long> entityMap, String ruleEntityType, String targetType) {
        if (ruleEntityType == null || !ruleEntityType.equalsIgnoreCase(targetType)) {
            return null;
        }
        Long entityId = entityMap.get(ruleEntityType.toUpperCase());
        if (entityId == null) {
            log.debug("PostingEngine: entityType={} requested by rule but not provided in entityMap",
                    ruleEntityType);
        }
        return entityId;
    }

    /**
     * Build a descriptive text for the journal line.
     */
    private String buildLineDescription(AccRuleLine ruleLine, AccountsChart account) {
        String accountName = account != null ? account.getAccountChartName() : "N/A";
        return String.format("Rule line priority=%d, account=%s (%s), type=%s",
                ruleLine.getPriority(), ruleLine.getAccountIdFk(),
                accountName, ruleLine.getAmountSourceType());
    }

}
