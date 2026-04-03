package com.example.erp.finance.gl.service;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.finance.gl.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GlJournalManualService — Manual journal entry adapter.
 *
 * Enforces manual-specific constraints:
 * - journalTypeIdFk is always MANUAL
 * - Source fields are forced to NULL
 * - Entity dimension fields (customer, supplier, costCenter) are forced to NULL
 *
 * Delegates actual create/update logic to the shared GlJournalService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GlJournalManualService {

    private final GlJournalService journalService;

    private static final String TYPE_MANUAL = "MANUAL";

    // ==================== CREATE ====================

    @Transactional
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_JOURNAL_CREATE, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<GlJournalHdrResponse> create(GlJournalManualCreateRequest request) {

        GlJournalHdrCreateRequest fullRequest = GlJournalHdrCreateRequest.builder()
                .journalDate(request.getJournalDate())
                .journalTypeIdFk(TYPE_MANUAL)
                .description(request.getDescription())
                .sourceModuleIdFk(null)
                .sourceDocTypeId(null)
                .sourceDocIdFk(null)
                .sourcePostingIdFk(null)
                .lines(mapLines(request.getLines()))
                .build();

        log.info("Creating manual journal");
        return journalService.create(fullRequest);
    }

    // ==================== UPDATE ====================

    @Transactional
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_JOURNAL_UPDATE, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<GlJournalHdrResponse> update(Long journalId, GlJournalManualUpdateRequest request) {

        GlJournalHdrUpdateRequest fullRequest = GlJournalHdrUpdateRequest.builder()
                .journalDate(request.getJournalDate())
                .journalTypeIdFk(TYPE_MANUAL)
                .description(request.getDescription())
                .sourceModuleIdFk(null)
                .sourceDocTypeId(null)
                .sourceDocIdFk(null)
                .sourcePostingIdFk(null)
                .lines(mapLines(request.getLines()))
                .build();

        log.info("Updating manual journal: journalId={}", journalId);
        return journalService.update(journalId, fullRequest);
    }

    // ── Line Mapping ────────────────────────────────────────

    private List<GlJournalLineRequest> mapLines(List<GlJournalManualLineRequest> manualLines) {
        return manualLines.stream()
                .map(line -> GlJournalLineRequest.builder()
                        .accountIdFk(line.getAccountIdFk())
                        .debitAmount(line.getDebitAmount())
                        .creditAmount(line.getCreditAmount())
                        .customerIdFk(null)
                        .supplierIdFk(null)
                        .costCenterIdFk(null)
                        .description(line.getDescription())
                        .build())
                .collect(Collectors.toList());
    }
}
