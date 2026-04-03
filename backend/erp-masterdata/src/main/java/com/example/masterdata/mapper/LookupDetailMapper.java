package com.example.masterdata.mapper;

import com.example.masterdata.dto.*;
import com.example.masterdata.entity.MdLookupDetail;
import com.example.masterdata.entity.MdMasterLookup;
import org.springframework.stereotype.Component;

/**
 * Mapper for Lookup Detail Entity <-> DTOs
 * 
 * Architecture Rules:
 * - Rule 7.2: One mapper per entity
 * - Rule 7.2: Centralize mapping in dedicated mapper classes
 * 
 * @author ERP Team
 */
@Component
public class LookupDetailMapper {

    /**
     * Convert Create Request to Entity with explicit parent FK
     * 
     * Architecture: Parent entity is a required parameter — enforces FK
     * at compile-time rather than relying on the caller to remember
     * calling setMasterLookup() afterwards.
     * 
     * @param request       Create request
     * @param masterLookup  Parent master lookup entity (FK)
     * @return LookupDetail entity with parent relationship set
     */
    public MdLookupDetail toEntity(LookupDetailCreateRequest request, MdMasterLookup masterLookup) {
        if (request == null) {
            return null;
        }

        return MdLookupDetail.builder()
                .code(request.getCode())
                .nameAr(request.getNameAr())
                .nameEn(request.getNameEn())
                .extraValue(request.getExtraValue())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
            .isActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE)
                .masterLookup(masterLookup)
                .build();
    }

    /**
     * Update entity from Update Request
     * Note: masterLookupId and code are immutable and should NOT be updated
     * 
     * @param entity Existing entity
     * @param request Update request
     */
    public void updateEntityFromRequest(MdLookupDetail entity, LookupDetailUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }

        // Note: code is NOT updated - it is immutable per contract
        // Note: masterLookupId is NOT updated - it is immutable per contract
        entity.setNameAr(request.getNameAr());
        entity.setNameEn(request.getNameEn());
        entity.setExtraValue(request.getExtraValue());
        if (request.getSortOrder() != null) {
            entity.setSortOrder(request.getSortOrder());
        }
    }

    /**
     * Convert Entity to Response DTO
     * 
     * @param entity Lookup detail entity
     * @return Response DTO
     */
    public LookupDetailResponse toResponse(MdLookupDetail entity) {
        if (entity == null) {
            return null;
        }

        MdMasterLookup masterLookup = entity.getMasterLookup();

        return LookupDetailResponse.builder()
                .id(entity.getId())
                .masterLookupId(masterLookup != null ? masterLookup.getId() : null)
                .masterLookupKey(masterLookup != null ? masterLookup.getLookupKey() : null)
                .masterLookupName(masterLookup != null ? masterLookup.getLookupName() : null)
                .code(entity.getCode())
                .nameAr(entity.getNameAr())
                .nameEn(entity.getNameEn())
                .extraValue(entity.getExtraValue())
                .sortOrder(entity.getSortOrder())
                .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    /**
     * Convert Entity to Option Response DTO (for dropdowns)
     * 
     * @param entity Lookup detail entity
     * @return Option response DTO
     */
    public LookupDetailOptionResponse toOptionResponse(MdLookupDetail entity) {
        if (entity == null) {
            return null;
        }

        return LookupDetailOptionResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .nameAr(entity.getNameAr())
                .nameEn(entity.getNameEn())
                .extraValue(entity.getExtraValue())
                .sortOrder(entity.getSortOrder())
                .build();
    }

    /**
     * Convert Entity to Usage Response DTO
     * 
     * @param entity Lookup detail entity
     * @param activityReferencesCount References in Activity
     * @return Usage response DTO
     */
    public LookupDetailUsageResponse toUsageResponse(
            MdLookupDetail entity,
            long activityReferencesCount) {
        
        if (entity == null) {
            return null;
        }

        long totalReferences = activityReferencesCount;
        boolean canBeDeleted = totalReferences == 0;

        String reason = null;
        if (!canBeDeleted) {
            StringBuilder reasonBuilder = new StringBuilder();
            if (activityReferencesCount > 0) {
                reasonBuilder.append(String.format("Referenced by %d activities", activityReferencesCount));
            }
            reason = reasonBuilder.toString();
        }

        return LookupDetailUsageResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .activityReferencesCount(activityReferencesCount)
                .totalReferencesCount(totalReferences)
                .canBeDeleted(canBeDeleted)
                .reason(reason)
                .build();
    }
}
