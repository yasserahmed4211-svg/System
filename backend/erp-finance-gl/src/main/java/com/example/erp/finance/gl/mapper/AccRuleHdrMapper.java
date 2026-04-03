package com.example.erp.finance.gl.mapper;

import com.example.erp.finance.gl.dto.*;
import com.example.erp.finance.gl.entity.AccRuleHdr;
import com.example.erp.finance.gl.entity.AccRuleLine;
import com.example.erp.finance.gl.entity.AccountsChart;
import com.example.erp.finance.gl.repository.AccountsChartRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AccRuleHdrMapper {

    private final AccountsChartRepository accountsChartRepository;

    public AccRuleHdr toEntity(AccRuleHdrCreateRequest request) {
        AccRuleHdr hdr = AccRuleHdr.builder()
                .companyIdFk(request.getCompanyIdFk())
                .sourceModule(request.getSourceModule().toUpperCase())
                .sourceDocType(request.getSourceDocType().toUpperCase())
                .isActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE)
                .build();

        if (request.getLines() != null) {
            request.getLines().forEach(lineReq -> {
                AccRuleLine line = toLineEntity(lineReq);
                hdr.addLine(line);
            });
        }
        return hdr;
    }

    public void updateEntity(AccRuleHdr entity, AccRuleHdrUpdateRequest request) {
        entity.setCompanyIdFk(request.getCompanyIdFk());
        entity.setSourceModule(request.getSourceModule().toUpperCase());
        entity.setSourceDocType(request.getSourceDocType().toUpperCase());
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }

        // Replace lines
        List<AccRuleLine> newLines = request.getLines().stream()
                .map(this::toLineEntity)
                .collect(Collectors.toList());
        entity.replaceLines(newLines);
    }

    public AccRuleHdrResponse toResponse(AccRuleHdr entity) {
        List<AccRuleLineResponse> lineResponses = Collections.emptyList();
        if (entity.getLines() != null && !entity.getLines().isEmpty()) {
            // Collect account IDs for batch lookup
            List<Long> accountIds = entity.getLines().stream()
                    .map(AccRuleLine::getAccountIdFk)
                    .distinct()
                    .collect(Collectors.toList());

            Map<Long, AccountsChart> accountMap = accountsChartRepository
                    .findAllById(accountIds).stream()
                    .collect(Collectors.toMap(AccountsChart::getAccountChartPk, a -> a));

            lineResponses = entity.getLines().stream()
                    .map(line -> toLineResponse(line, accountMap))
                    .collect(Collectors.toList());
        }

        return AccRuleHdrResponse.builder()
                .ruleId(entity.getRuleId())
                .companyIdFk(entity.getCompanyIdFk())
                .sourceModule(entity.getSourceModule())
                .sourceDocType(entity.getSourceDocType())
                .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .lineCount(lineResponses.size())
                .lines(lineResponses)
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public AccRuleHdrResponse toListResponse(AccRuleHdr entity) {
        int lineCount = 0;
        if (entity.getLines() != null && Hibernate.isInitialized(entity.getLines())) {
            lineCount = entity.getLines().size();
        }

        return AccRuleHdrResponse.builder()
                .ruleId(entity.getRuleId())
                .companyIdFk(entity.getCompanyIdFk())
                .sourceModule(entity.getSourceModule())
                .sourceDocType(entity.getSourceDocType())
                .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .lineCount(lineCount)
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    private AccRuleLine toLineEntity(AccRuleLineRequest request) {
        return AccRuleLine.builder()
                .accountIdFk(request.getAccountIdFk())
                .entrySide(request.getEntrySide().toUpperCase())
                .priority(request.getPriority())
                .amountSourceType(request.getAmountSourceType().toUpperCase())
                .amountSourceValue(request.getAmountSourceValue())
                .paymentTypeCode(request.getPaymentTypeCode())
                .entityType(request.getEntityType())
                .build();
    }

    private AccRuleLineResponse toLineResponse(AccRuleLine line, Map<Long, AccountsChart> accountMap) {
        AccountsChart account = accountMap.get(line.getAccountIdFk());
        return AccRuleLineResponse.builder()
                .ruleLineId(line.getRuleLineId())
                .accountIdFk(line.getAccountIdFk())
                .accountCode(account != null ? account.getAccountChartNo() : null)
                .accountName(account != null ? account.getAccountChartName() : null)
                .entrySide(line.getEntrySide())
                .priority(line.getPriority())
                .amountSourceType(line.getAmountSourceType())
                .amountSourceValue(line.getAmountSourceValue())
                .paymentTypeCode(line.getPaymentTypeCode())
                .entityType(line.getEntityType())
                .build();
    }
}
