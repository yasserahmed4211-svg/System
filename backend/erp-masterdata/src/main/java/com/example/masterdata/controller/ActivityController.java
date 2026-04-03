package com.example.masterdata.controller;

import com.erp.common.search.SearchRequest;
import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.example.masterdata.dto.*;
import com.example.masterdata.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Activity REST Controller
 * 
 * Provides REST API endpoints for Activity management
 * 
 * Architecture Rules:
 * - Rule 4.1: Thin controllers
 * - Rule 4.2: Never expose entities
 * - Rule 4.3: No repository injection
 * - Rule 10.7: Standard CRUD operations
 * 
 * @author ERP Team
 */
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Tag(name = "Activity Management", description = "إدارة الأنشطة - Master Data Activities")
public class ActivityController {

    private final ActivityService activityService;
    private final OperationCode operationCode;

    /**
     * Create new activity
     * 
     * @param request Activity create request
     * @return Created activity
     */
    @PostMapping
    @Operation(summary = "Create Activity", description = "إنشاء نشاط جديد")
    public ResponseEntity<ApiResponse<ActivityResponse>> create(@Valid @RequestBody ActivityCreateRequest request) {
        ServiceResult<ActivityResponse> result = activityService.create(request);
        return operationCode.craftResponse(result);
    }

    /**
     * Update existing activity
     * 
     * @param id Activity ID
     * @param request Activity update request
     * @return Updated activity
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update Activity", description = "تحديث نشاط موجود")
    public ResponseEntity<ApiResponse<ActivityResponse>> update(
        @PathVariable Long id,
        @Valid @RequestBody ActivityUpdateRequest request
    ) {
        ServiceResult<ActivityResponse> result = activityService.update(id, request);
        return operationCode.craftResponse(result);
    }

    /**
     * Get activity by ID
     * 
     * @param id Activity ID
     * @return Activity details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Activity by ID", description = "جلب تفاصيل النشاط")
    public ResponseEntity<ApiResponse<ActivityResponse>> getById(@PathVariable Long id) {
        ServiceResult<ActivityResponse> result = activityService.getById(id);
        return operationCode.craftResponse(result);
    }

    /**
     * Search activities with filtering, sorting, and pagination
     * 
     * Uses common-utils SearchRequest for dynamic filtering
     * 
     * Allowed filters:
     * - code (EQUALS, CONTAINS, STARTS_WITH)
     * - name (EQUALS, CONTAINS, STARTS_WITH)
     * - conversionType (EQUALS, IN)
     * - isActive (EQUALS)
     * 
     * Allowed sort fields:
     * - id, code, name, conversionType, isActive, createdAt, updatedAt
     * 
     * @param searchRequest Search criteria
     * @return Page of activities
     */
    @PostMapping("/search")
    @Operation(
        summary = "Search Activities", 
        description = "البحث في الأنشطة مع الفلترة والترتيب والصفحات"
    )
    public ResponseEntity<ApiResponse<Page<ActivityResponse>>> search(@RequestBody SearchRequest searchRequest) {
        ServiceResult<Page<ActivityResponse>> result = activityService.search(searchRequest);
        return operationCode.craftResponse(result);
    }

    /**
     * Activate activity
     * 
     * @param id Activity ID
     * @return Updated activity
     */
    @PutMapping("/{id}/activate")
    @Operation(summary = "Activate Activity", description = "تفعيل النشاط")
    public ResponseEntity<ApiResponse<ActivityResponse>> activate(@PathVariable Long id) {
        ServiceResult<ActivityResponse> result = activityService.activate(id);
        return operationCode.craftResponse(result);
    }

    /**
     * Deactivate activity
     * 
     * Business Rule: Cannot deactivate if there are active categories
     * 
     * @param id Activity ID
     * @return Updated activity
     */
    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate Activity", description = "إلغاء تفعيل النشاط")
    public ResponseEntity<ApiResponse<ActivityResponse>> deactivate(@PathVariable Long id) {
        ServiceResult<ActivityResponse> result = activityService.deactivate(id);
        return operationCode.craftResponse(result);
    }

    /**
     * Delete activity
     * 
     * Business Rule: Cannot delete if referenced by categories (FK constraint)
     * Returns HTTP 409 CONFLICT if deletion fails due to FK constraint
     * 
     * @param id Activity ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete Activity", description = "حذف النشاط")
    public void delete(@PathVariable Long id) {
        activityService.delete(id);
    }

    /**
     * Get activity usage information
     * 
     * Shows where the activity is being used and whether it can be deleted/deactivated
     * 
     * @param id Activity ID
     * @return Usage information
     */
    @GetMapping("/{id}/usage")
    @Operation(summary = "Get Activity Usage", description = "جلب معلومات استخدام النشاط")
    public ResponseEntity<ApiResponse<ActivityUsageResponse>> getUsage(@PathVariable Long id) {
        ServiceResult<ActivityUsageResponse> result = activityService.getUsage(id);
        return operationCode.craftResponse(result);
    }
}
