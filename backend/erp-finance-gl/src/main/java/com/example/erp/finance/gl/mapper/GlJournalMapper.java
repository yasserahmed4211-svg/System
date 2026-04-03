package com.example.erp.finance.gl.mapper;

import com.example.erp.finance.gl.dto.*;
import com.example.erp.finance.gl.entity.GlJournalHdr;
import com.example.erp.finance.gl.entity.GlJournalLine;
import com.example.erp.finance.gl.entity.AccountsChart;
import com.example.erp.finance.gl.repository.AccountsChartRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GlJournalMapper {

    private final AccountsChartRepository accountsChartRepository;

    // ── Entity Creation ─────────────────────────────────────

    public GlJournalHdr toEntity(GlJournalHdrCreateRequest request, String journalNo) {
        GlJournalHdr hdr = GlJournalHdr.builder()
                .journalNo(journalNo)
                .journalDate(request.getJournalDate())
                .journalTypeIdFk(request.getJournalTypeIdFk())
                .statusIdFk("DRAFT")
                .description(request.getDescription())
                .sourceModuleIdFk(request.getSourceModuleIdFk())
                .sourceDocTypeId(request.getSourceDocTypeId())
                .sourceDocIdFk(request.getSourceDocIdFk())
                .sourcePostingIdFk(request.getSourcePostingIdFk())
                .totalDebit(BigDecimal.ZERO)
                .totalCredit(BigDecimal.ZERO)
                .activeFl(Boolean.TRUE)
                .build();

        if (request.getLines() != null) {
            AtomicInteger lineNo = new AtomicInteger(1);
            request.getLines().forEach(lineReq -> {
                GlJournalLine line = toLineEntity(lineReq, lineNo.getAndIncrement());
                hdr.addLine(line);
            });
        }

        hdr.recalculateTotals();
        return hdr;
    }

    // ── Entity Update ───────────────────────────────────────

    public void updateEntity(GlJournalHdr entity, GlJournalHdrUpdateRequest request) {
        entity.setJournalDate(request.getJournalDate());
        entity.setJournalTypeIdFk(request.getJournalTypeIdFk());
        entity.setDescription(request.getDescription());
        entity.setSourceModuleIdFk(request.getSourceModuleIdFk());
        entity.setSourceDocTypeId(request.getSourceDocTypeId());
        entity.setSourceDocIdFk(request.getSourceDocIdFk());
        entity.setSourcePostingIdFk(request.getSourcePostingIdFk());

        // Replace lines
        AtomicInteger lineNo = new AtomicInteger(1);
        List<GlJournalLine> newLines = request.getLines().stream()
                .map(lineReq -> toLineEntity(lineReq, lineNo.getAndIncrement()))
                .collect(Collectors.toList());
        entity.replaceLines(newLines);
        entity.recalculateTotals();
    }

    // ── Response Mapping (Detail) ───────────────────────────

    public GlJournalHdrResponse toResponse(GlJournalHdr entity) {
        List<GlJournalLineResponse> lineResponses = Collections.emptyList();
        if (entity.getLines() != null && !entity.getLines().isEmpty()) {
            List<Long> accountIds = entity.getLines().stream()
                    .map(GlJournalLine::getAccountIdFk)
                    .distinct()
                    .collect(Collectors.toList());

            Map<Long, AccountsChart> accountMap = accountsChartRepository
                    .findAllById(accountIds).stream()
                    .collect(Collectors.toMap(AccountsChart::getAccountChartPk, a -> a));

            lineResponses = entity.getLines().stream()
                    .map(line -> toLineResponse(line, accountMap))
                    .collect(Collectors.toList());
        }

        return GlJournalHdrResponse.builder()
                .id(entity.getId())
                .journalNo(entity.getJournalNo())
                .journalDate(entity.getJournalDate())
                .journalTypeIdFk(entity.getJournalTypeIdFk())
                .statusIdFk(entity.getStatusIdFk())
                .description(entity.getDescription())
                .sourceModuleIdFk(entity.getSourceModuleIdFk())
                .sourceDocTypeId(entity.getSourceDocTypeId())
                .sourceDocIdFk(entity.getSourceDocIdFk())
                .sourcePostingIdFk(entity.getSourcePostingIdFk())
                .totalDebit(entity.getTotalDebit())
                .totalCredit(entity.getTotalCredit())
                .activeFl(Boolean.TRUE.equals(entity.getActiveFl()))
                .lineCount(lineResponses.size())
                .lines(lineResponses)
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    // ── Response Mapping (List – no lines detail) ───────────

    public GlJournalHdrResponse toListResponse(GlJournalHdr entity) {
        int lineCount = 0;
        if (entity.getLines() != null && Hibernate.isInitialized(entity.getLines())) {
            lineCount = entity.getLines().size();
        }

        return GlJournalHdrResponse.builder()
                .id(entity.getId())
                .journalNo(entity.getJournalNo())
                .journalDate(entity.getJournalDate())
                .journalTypeIdFk(entity.getJournalTypeIdFk())
                .statusIdFk(entity.getStatusIdFk())
                .description(entity.getDescription())
                .sourceModuleIdFk(entity.getSourceModuleIdFk())
                .totalDebit(entity.getTotalDebit())
                .totalCredit(entity.getTotalCredit())
                .activeFl(Boolean.TRUE.equals(entity.getActiveFl()))
                .lineCount(lineCount)
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    // ── Line Helpers ────────────────────────────────────────

    private GlJournalLine toLineEntity(GlJournalLineRequest request, int lineNo) {
        return GlJournalLine.builder()
                .lineNo(lineNo)
                .accountIdFk(request.getAccountIdFk())
                .debitAmount(request.getDebitAmount())
                .creditAmount(request.getCreditAmount())
                .customerIdFk(request.getCustomerIdFk())
                .supplierIdFk(request.getSupplierIdFk())
                .costCenterIdFk(request.getCostCenterIdFk())
                .description(request.getDescription())
                .build();
    }

    private GlJournalLineResponse toLineResponse(GlJournalLine line, Map<Long, AccountsChart> accountMap) {
        AccountsChart account = accountMap.get(line.getAccountIdFk());
        return GlJournalLineResponse.builder()
                .id(line.getId())
                .journalIdFk(line.getJournalHdr() != null ? line.getJournalHdr().getId() : null)
                .lineNo(line.getLineNo())
                .accountIdFk(line.getAccountIdFk())
                .accountCode(account != null ? account.getAccountChartNo() : null)
                .accountName(account != null ? account.getAccountChartName() : null)
                .debitAmount(line.getDebitAmount())
                .creditAmount(line.getCreditAmount())
                .customerIdFk(line.getCustomerIdFk())
                .supplierIdFk(line.getSupplierIdFk())
                .costCenterIdFk(line.getCostCenterIdFk())
                .description(line.getDescription())
                .build();
    }
}
