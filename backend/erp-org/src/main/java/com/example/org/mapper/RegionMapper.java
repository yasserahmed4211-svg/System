package com.example.org.mapper;

import com.example.org.dto.*;
import com.example.org.entity.OrgRegion;
import org.springframework.stereotype.Component;

@Component
public class RegionMapper {

    public OrgRegion toEntity(RegionCreateRequest request) {
        if (request == null) return null;

        return OrgRegion.builder()
                .regionNameAr(request.getRegionNameAr())
                .regionNameEn(request.getRegionNameEn())
                .descriptionAr(request.getDescriptionAr())
                .build();
    }

    public void updateEntityFromRequest(OrgRegion entity, RegionUpdateRequest request) {
        if (entity == null || request == null) return;

        entity.setRegionNameAr(request.getRegionNameAr());
        entity.setRegionNameEn(request.getRegionNameEn());
        entity.setDescriptionAr(request.getDescriptionAr());
    }

    public RegionResponse toResponse(OrgRegion entity) {
        if (entity == null) return null;

        return RegionResponse.builder()
                .id(entity.getId())
                .regionCode(entity.getRegionCode())
                .regionNameAr(entity.getRegionNameAr())
                .regionNameEn(entity.getRegionNameEn())
                .descriptionAr(entity.getDescriptionAr())
                .statusId(entity.getStatusId())
                .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .legalEntityId(entity.getLegalEntity() != null ? entity.getLegalEntity().getId() : null)
                .legalEntityDisplay(entity.getLegalEntity() != null ? entity.getLegalEntity().getLegalEntityNameAr() : null)
                .branchCount(entity.getBranchCount())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public RegionUsageResponse toUsageResponse(OrgRegion entity, long activeBranches) {
        if (entity == null) return null;

        boolean canDeactivate = activeBranches == 0;
        String reason = canDeactivate ? null : "المنطقة تحتوي على فروع نشطة";

        return RegionUsageResponse.builder()
                .regionId(entity.getId())
                .regionCode(entity.getRegionCode())
                .activeBranches(activeBranches)
                .canDeactivate(canDeactivate)
                .reason(reason)
                .build();
    }

    public RegionOptionResponse toOptionResponse(OrgRegion entity) {
        if (entity == null) return null;

        return RegionOptionResponse.builder()
                .id(entity.getId())
                .regionCode(entity.getRegionCode())
                .regionNameAr(entity.getRegionNameAr())
                .regionNameEn(entity.getRegionNameEn())
                .build();
    }
}
