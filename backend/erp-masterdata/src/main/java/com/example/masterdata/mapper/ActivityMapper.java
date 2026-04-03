package com.example.masterdata.mapper;

import com.example.masterdata.dto.ActivityCreateRequest;
import com.example.masterdata.dto.ActivityResponse;
import com.example.masterdata.dto.ActivityUpdateRequest;
import com.example.masterdata.entity.Activity;
import org.springframework.stereotype.Component;

/**
 * Activity Mapper
 * 
 * Maps between Activity entity and DTOs
 * 
 * Architecture Rules:
 * - Rule 7.2: One mapper per entity
 * 
 * @author ERP Team
 */
@Component
public class ActivityMapper {

    /**
     * Convert CreateRequest to Entity
     */
    public Activity toEntity(ActivityCreateRequest request) {
        Activity activity = Activity.builder()
            .code(request.getCode().toUpperCase())
            .name(request.getName())
            .description(request.getDescription())
            .defaultStockUomId(request.getDefaultStockUomId())
            .conversionType(request.getConversionType())
            .requiresActualWeight(request.getRequiresActualWeight())
            .allowFraction(request.getAllowFraction())
            .isActive(Boolean.TRUE) // New activities are active by default
            .build();

        return activity;
    }

    /**
     * Update entity from UpdateRequest
     */
    public void updateEntity(Activity activity, ActivityUpdateRequest request) {
        activity.setName(request.getName());
        activity.setDescription(request.getDescription());
        activity.setDefaultStockUomId(request.getDefaultStockUomId());
        activity.setConversionType(request.getConversionType());
        activity.setRequiresActualWeight(request.getRequiresActualWeight());
        activity.setAllowFraction(request.getAllowFraction());
    }

    /**
     * Convert Entity to Response DTO
     */
    public ActivityResponse toResponse(Activity activity) {
        return ActivityResponse.builder()
            .id(activity.getId())
            .code(activity.getCode())
            .name(activity.getName())
            .description(activity.getDescription())
            .defaultStockUomId(activity.getDefaultStockUomId())
            .conversionType(activity.getConversionType())
            .requiresActualWeight(activity.getRequiresActualWeight())
            .allowFraction(activity.getAllowFraction())
            .isActive(Boolean.TRUE.equals(activity.getIsActive()))
            .createdAt(activity.getCreatedAt())
            .createdBy(activity.getCreatedBy())
            .updatedAt(activity.getUpdatedAt())
            .updatedBy(activity.getUpdatedBy())
            .build();
    }
}
