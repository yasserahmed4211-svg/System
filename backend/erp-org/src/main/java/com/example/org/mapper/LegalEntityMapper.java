package com.example.org.mapper;

import com.example.org.dto.*;
import com.example.org.entity.OrgLegalEntity;
import org.springframework.stereotype.Component;

@Component
public class LegalEntityMapper {

    public OrgLegalEntity toEntity(LegalEntityCreateRequest request) {
        if (request == null) return null;

        return OrgLegalEntity.builder()
                .legalEntityNameAr(request.getLegalEntityNameAr())
                .legalEntityNameEn(request.getLegalEntityNameEn())
                .countryId(request.getCountryId())
                .functionalCurrencyId(request.getFunctionalCurrencyId())
                .taxNumber(request.getTaxNumber())
                .commercialRegNumber(request.getCommercialRegNumber())
                .fiscalYearStartMonth(request.getFiscalYearStartMonth())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .cityName(request.getCityName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .website(request.getWebsite())
                .build();
    }

    public void updateEntityFromRequest(OrgLegalEntity entity, LegalEntityUpdateRequest request) {
        if (entity == null || request == null) return;

        entity.setLegalEntityNameAr(request.getLegalEntityNameAr());
        entity.setLegalEntityNameEn(request.getLegalEntityNameEn());
        entity.setCountryId(request.getCountryId());
        entity.setFunctionalCurrencyId(request.getFunctionalCurrencyId());
        entity.setTaxNumber(request.getTaxNumber());
        entity.setCommercialRegNumber(request.getCommercialRegNumber());
        entity.setFiscalYearStartMonth(request.getFiscalYearStartMonth());
        entity.setAddressLine1(request.getAddressLine1());
        entity.setAddressLine2(request.getAddressLine2());
        entity.setCityName(request.getCityName());
        entity.setPhone(request.getPhone());
        entity.setEmail(request.getEmail());
        entity.setWebsite(request.getWebsite());
    }

    public LegalEntityResponse toResponse(OrgLegalEntity entity) {
        if (entity == null) return null;

        return LegalEntityResponse.builder()
                .id(entity.getId())
                .legalEntityCode(entity.getLegalEntityCode())
                .legalEntityNameAr(entity.getLegalEntityNameAr())
                .legalEntityNameEn(entity.getLegalEntityNameEn())
                .countryId(entity.getCountryId())
                .functionalCurrencyId(entity.getFunctionalCurrencyId())
                .taxNumber(entity.getTaxNumber())
                .commercialRegNumber(entity.getCommercialRegNumber())
                .fiscalYearStartMonth(entity.getFiscalYearStartMonth())
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())
                .cityName(entity.getCityName())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .website(entity.getWebsite())
                .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .regionCount(entity.getRegionCount())
                .branchCount(entity.getBranchCount())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public LegalEntityUsageResponse toUsageResponse(
            OrgLegalEntity entity,
            long totalRegions,
            long totalBranches,
            long activeBranches,
            boolean hasFinancialTransactions
    ) {
        if (entity == null) return null;

        boolean canDeactivate = activeBranches == 0;
        String reason = canDeactivate ? null : "الكيان القانوني يحتوي على فروع نشطة";

        return LegalEntityUsageResponse.builder()
                .legalEntityId(entity.getId())
                .legalEntityCode(entity.getLegalEntityCode())
                .totalRegions(totalRegions)
                .totalBranches(totalBranches)
                .activeBranches(activeBranches)
                .canDeactivate(canDeactivate)
                .hasFinancialTransactions(hasFinancialTransactions)
                .reason(reason)
                .build();
    }

    public LegalEntityOptionResponse toOptionResponse(OrgLegalEntity entity) {
        if (entity == null) return null;

        return LegalEntityOptionResponse.builder()
                .id(entity.getId())
                .legalEntityCode(entity.getLegalEntityCode())
                .legalEntityNameAr(entity.getLegalEntityNameAr())
                .legalEntityNameEn(entity.getLegalEntityNameEn())
                .build();
    }
}
