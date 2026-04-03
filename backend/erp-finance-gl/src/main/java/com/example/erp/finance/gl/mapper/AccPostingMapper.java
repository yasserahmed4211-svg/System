package com.example.erp.finance.gl.mapper;

import com.example.erp.finance.gl.dto.AccPostingDtlResponse;
import com.example.erp.finance.gl.dto.AccPostingMstResponse;
import com.example.erp.finance.gl.entity.AccPostingDtl;
import com.example.erp.finance.gl.entity.AccPostingMst;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AccPostingMapper {

    // ── Response Mapping (Detail view — includes lines) ─────

    public AccPostingMstResponse toResponse(AccPostingMst entity) {
        if (entity == null) return null;
        List<AccPostingDtlResponse> detailResponses = Collections.emptyList();
        if (entity.getDetails() != null && Hibernate.isInitialized(entity.getDetails())) {
            detailResponses = entity.getDetails().stream()
                    .map(this::toDetailResponse)
                    .collect(Collectors.toList());
        }

        return AccPostingMstResponse.builder()
                .postingId(entity.getPostingId())
                .branchIdFk(entity.getBranchIdFk())
                .companyIdFk(entity.getCompanyIdFk())
                .currencyCode(entity.getCurrencyCode())
                .docDate(entity.getDocDate())
                .errorMessage(entity.getErrorMessage())
                .finJournalIdFk(entity.getFinJournalIdFk())
                .reversalPostingIdFk(entity.getReversalPostingIdFk())
                .sourceDocId(entity.getSourceDocId())
                .sourceDocNo(entity.getSourceDocNo())
                .sourceDocType(entity.getSourceDocType())
                .sourceModule(entity.getSourceModule())
                .status(entity.getStatus())
                .totalAmount(entity.getTotalAmount())
                .detailCount(detailResponses.size())
                .details(detailResponses)
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    // ── Response Mapping (List view — no lines) ─────────────

    public AccPostingMstResponse toListResponse(AccPostingMst entity) {
        if (entity == null) return null;

        return AccPostingMstResponse.builder()
                .postingId(entity.getPostingId())
                .companyIdFk(entity.getCompanyIdFk())
                .docDate(entity.getDocDate())
                .sourceDocNo(entity.getSourceDocNo())
                .sourceDocType(entity.getSourceDocType())
                .sourceModule(entity.getSourceModule())
                .status(entity.getStatus())
                .totalAmount(entity.getTotalAmount())
                .finJournalIdFk(entity.getFinJournalIdFk())
                .detailCount(entity.getDetailCount() != null ? entity.getDetailCount() : 0)
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    // ── Detail Line Mapping ─────────────────────────────────

    public AccPostingDtlResponse toDetailResponse(AccPostingDtl detail) {
        if (detail == null) return null;
        return AccPostingDtlResponse.builder()
                .postingDtlId(detail.getPostingDtlId())
                .lineNo(detail.getLineNo())
                .amount(detail.getAmount())
                .businessSide(detail.getBusinessSide())
                .sign(detail.getSign())
                .description(detail.getDescription())
                .customerIdFk(detail.getCustomerIdFk())
                .supplierIdFk(detail.getSupplierIdFk())
                .costCenterIdFk(detail.getCostCenterIdFk())
                .contractIdFk(detail.getContractIdFk())
                .itemIdFk(detail.getItemIdFk())
                .build();
    }
}
