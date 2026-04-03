package com.example.masterdata.mapper;

import com.example.masterdata.dto.*;
import com.example.masterdata.entity.MdMasterLookup;
import org.springframework.stereotype.Component;

/**
 * Mapper for Master Lookup Entity <-> DTOs
 * 
 * Architecture Rules:
 * - Rule 7.2: One mapper per entity
 * - Rule 7.2: Centralize mapping in dedicated mapper classes
 * 
 * @author ERP Team
 */
@Component
public class MasterLookupMapper {

    /**
     * Convert Create Request to Entity
     * 
     * @param request Create request
     * @return MasterLookup entity
     */
    public MdMasterLookup toEntity(MasterLookupCreateRequest request) {
        if (request == null) {
            return null;
        }

        return MdMasterLookup.builder()
                .lookupKey(request.getLookupKey())
                .lookupName(request.getLookupName())
                .lookupNameEn(request.getLookupNameEn())
                .description(request.getDescription())
            .isActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE)
                .build();
    }

    /**
     * Update entity from Update Request
     * 
     * Note: lookupKey is immutable and should not be updated
     * 
     * @param entity Existing entity
     * @param request Update request
     */
    public void updateEntityFromRequest(MdMasterLookup entity, MasterLookupUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }

        // Note: lookupKey is NOT updated - it is immutable per contract
        entity.setLookupName(request.getLookupName());
        entity.setLookupNameEn(request.getLookupNameEn());
        entity.setDescription(request.getDescription());
    }

    /**
     * Convert Entity to Response DTO
     * 
     * @param entity Master lookup entity
     * @return Response DTO
     */
    public MasterLookupResponse toResponse(MdMasterLookup entity) {
        if (entity == null) {
            return null;
        }

        return MasterLookupResponse.builder()
                .id(entity.getId())
                .lookupKey(entity.getLookupKey())
                .lookupName(entity.getLookupName())
                .lookupNameEn(entity.getLookupNameEn())
                .description(entity.getDescription())
            .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .detailCount(entity.getDetailCount())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    /**
     * Convert Entity to Usage Response DTO
     * 
     * @param entity Master lookup entity
     * @param totalDetailsCount Total lookup details count
     * @param activeDetailsCount Active lookup details count
     * @return Usage response DTO
     */
    public MasterLookupUsageResponse toUsageResponse(
            MdMasterLookup entity,
            long totalDetailsCount,
            long activeDetailsCount) {
        
        if (entity == null) {
            return null;
        }

        boolean canDelete = totalDetailsCount == 0;
        boolean canDeactivate = activeDetailsCount == 0;

        return MasterLookupUsageResponse.builder()
                .masterLookupId(entity.getId())
                .lookupKey(entity.getLookupKey())
                .totalDetails(totalDetailsCount)
                .activeDetails(activeDetailsCount)
                .canDelete(canDelete)
                .canDeactivate(canDeactivate)
                .build();
    }
}
