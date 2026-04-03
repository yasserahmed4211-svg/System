package com.example.masterdata.service;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.exception.LocalizedException;
import com.erp.common.search.*;
import com.example.masterdata.dto.*;
import com.example.masterdata.entity.Activity;
import com.example.masterdata.exception.MasterDataErrorCodes;
import com.example.masterdata.mapper.ActivityMapper;
import com.example.masterdata.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import com.example.erp.common.domain.status.Status;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Activity Service
 * 
 * Contains all business logic for Activity management
 * 
 * Architecture Rules:
 * - Rule 5.1: Business logic in service layer
 * - Rule 5.2: Use @Transactional
 * - Rule 5.4: Return DTOs, not entities
 * - Rule 11: Security & Multi-tenancy
 * 
 * @author ERP Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityMapper activityMapper;

    // Whitelist of allowed sort fields (Rule 17.3)
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id", "code", "name", "conversionType", "isActive", "createdAt", "updatedAt"
    );

    /**
     * Create new activity
     * 
     * Business Rules:
     * - Code must be unique
     * - Code is automatically converted to uppercase
     * - VARIABLE conversion requires actual weight
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ACTIVITY_CREATE)")
    public ServiceResult<ActivityResponse> create(ActivityCreateRequest request) {
        log.debug("Creating activity with code: {}", request.getCode());

        // Validate request
        request.validate();

        String codeUpper = request.getCode().toUpperCase();

        // Check for duplicate code
        if (activityRepository.existsByCode(codeUpper)) {
            throw new LocalizedException(
                Status.ALREADY_EXISTS,
                MasterDataErrorCodes.ACTIVITY_CODE_ALREADY_EXISTS,
                codeUpper
            );
        }

        // Create entity
        Activity activity = activityMapper.toEntity(request);
        Activity saved = activityRepository.save(activity);

        log.info("Activity created successfully: id={}, code={}", saved.getId(), saved.getCode());
        return ServiceResult.success(activityMapper.toResponse(saved), Status.CREATED);
    }

    /**
     * Update existing activity
     * 
     * Business Rules:
     * - Code cannot be changed
     * - VARIABLE conversion requires actual weight
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ACTIVITY_UPDATE)")
    public ServiceResult<ActivityResponse> update(Long id, ActivityUpdateRequest request) {
        log.debug("Updating activity: id={}", id);

        // Validate request
        request.validate();

        // Find activity
        Activity activity = activityRepository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, MasterDataErrorCodes.ACTIVITY_NOT_FOUND, id));

        // Update entity
        activityMapper.updateEntity(activity, request);
        Activity updated = activityRepository.save(activity);

        log.info("Activity updated successfully: id={}, code={}", updated.getId(), updated.getCode());
        return ServiceResult.success(activityMapper.toResponse(updated), Status.UPDATED);
    }

    /**
     * Get activity by ID
     */
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ACTIVITY_VIEW)")
    public ServiceResult<ActivityResponse> getById(Long id) {
        Activity activity = activityRepository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, MasterDataErrorCodes.ACTIVITY_NOT_FOUND, id));

        return ServiceResult.success(activityMapper.toResponse(activity));
    }

    /**
     * Search activities with filtering, sorting, pagination
     * 
     * Uses common-utils SearchRequest
     */
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ACTIVITY_VIEW)")
    public ServiceResult<Page<ActivityResponse>> search(SearchRequest searchRequest) {
        // Build specification from search filters
        Specification<Activity> spec = SpecBuilder.build(
            searchRequest,
            new SetAllowedFields(ALLOWED_SORT_FIELDS),
            DefaultFieldValueConverter.INSTANCE
        );

        // Build pageable with validated sort fields
        Pageable pageable = PageableBuilder.from(searchRequest, ALLOWED_SORT_FIELDS);

        // Execute query
        Page<Activity> activities = activityRepository.findAll(spec, pageable);

        // Map to response DTOs
        return ServiceResult.success(activities.map(activityMapper::toResponse));
    }

    /**
     * Activate activity
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ACTIVITY_UPDATE)")
    public ServiceResult<ActivityResponse> activate(Long id) {
        log.debug("Activating activity: id={}", id);

        Activity activity = activityRepository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, MasterDataErrorCodes.ACTIVITY_NOT_FOUND, id));

        activity.setIsActive(Boolean.TRUE);
        Activity updated = activityRepository.save(activity);

        log.info("Activity activated successfully: id={}, code={}", updated.getId(), updated.getCode());
        return ServiceResult.success(activityMapper.toResponse(updated), Status.UPDATED);
    }

    /**
     * Deactivate activity
     * 
     * Business Prevention:
     * - Cannot deactivate if there are active categories
     * - Pre-check via countActiveCategoriesByActivityId() query
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ACTIVITY_UPDATE)")
    public ServiceResult<ActivityResponse> deactivate(Long id) {
        log.debug("Deactivating activity: id={}", id);

        Activity activity = activityRepository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, MasterDataErrorCodes.ACTIVITY_NOT_FOUND, id));

        // Business Prevention: Check for active categories
        long activeCategories = activityRepository.countActiveCategoriesByActivityId(id);
        if (activeCategories > 0) {
            throw new LocalizedException(
                Status.CONFLICT,
                MasterDataErrorCodes.ACTIVITY_HAS_ACTIVE_CATEGORIES,
                String.valueOf(activeCategories)
            );
        }

        activity.setIsActive(Boolean.FALSE);
        Activity updated = activityRepository.save(activity);

        log.info("Activity deactivated successfully: id={}, code={}", updated.getId(), updated.getCode());
        return ServiceResult.success(activityMapper.toResponse(updated), Status.UPDATED);
    }

    /**
     * Delete activity
     * 
     * Business Prevention:
     * - Cannot delete if referenced by categories (FK constraint)
     * - Pre-check via countCategoriesByActivityId() query
     * - DataIntegrityViolationException handled by GlobalExceptionHandler
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ACTIVITY_DELETE)")
    public void delete(Long id) {
        log.debug("Deleting activity: id={}", id);

        Activity activity = activityRepository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, MasterDataErrorCodes.ACTIVITY_NOT_FOUND, id));

        // Business Prevention: Check for category references
        long totalCategories = activityRepository.countCategoriesByActivityId(id);
        if (totalCategories > 0) {
            log.warn("Cannot delete activity: id={}, categories={}", id, totalCategories);
            throw new LocalizedException(
                Status.CONFLICT,
                MasterDataErrorCodes.ACTIVITY_CANNOT_DELETE_HAS_CATEGORIES,
                String.valueOf(totalCategories)
            );
        }

        // Delete - DataIntegrityViolationException handled by GlobalExceptionHandler
        activityRepository.delete(activity);
        log.info("Activity deleted successfully: id={}, code={}", id, activity.getCode());
    }

    /**
     * Get activity usage information
     * 
     * Shows where the activity is being used and whether it can be deleted/deactivated
     */
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).ACTIVITY_VIEW)")
    public ServiceResult<ActivityUsageResponse> getUsage(Long id) {
        Activity activity = activityRepository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, MasterDataErrorCodes.ACTIVITY_NOT_FOUND, id));

        long totalCategories = activityRepository.countCategoriesByActivityId(id);
        long activeCategories = activityRepository.countActiveCategoriesByActivityId(id);

        return ServiceResult.success(ActivityUsageResponse.from(
            activity.getId(),
            activity.getCode(),
            activity.getName(),
            totalCategories,
            activeCategories
        ));
    }
}
