package com.example.masterdata.controller;

import com.erp.common.search.SearchRequest;
import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.example.masterdata.dto.*;
import com.example.masterdata.service.LookupDetailService;
import com.example.masterdata.service.MasterLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Master Lookup REST Controller
 * 
 * Unified controller for Master Lookup AND Lookup Detail management.
 * Both are part of ONE functional screen in the frontend.
 * 
 * Architecture Rules:
 * - Rule 4.1: Thin controllers
 * - Rule 4.2: Never expose entities
 * - Rule 4.3: No repository injection
 * - Rule 10.7: Standard CRUD operations
 * 
 * Authorization: All operations use PERM_MASTER_LOOKUP_* permissions
 * 
 * @author ERP Team
 */
@RestController
@RequestMapping("/api/masterdata/master-lookups")
@RequiredArgsConstructor
@Tag(name = "Master Lookup Management", description = "إدارة القوائم المرجعية وقيمها - Master Data Lookup Types & Values")
public class MasterLookupController {

    private final MasterLookupService masterLookupService;
    private final LookupDetailService lookupDetailService;
    private final OperationCode operationCode;

    /**
     * Create new master lookup
     * 
     * @param request Master lookup create request
     * @return Created master lookup
     */
    @PostMapping
    @Operation(summary = "Create Master Lookup", description = "إنشاء نوع قائمة مرجعية جديد")
    public ResponseEntity<ApiResponse<MasterLookupResponse>> create(@Valid @RequestBody MasterLookupCreateRequest request) {
        ServiceResult<MasterLookupResponse> result = masterLookupService.create(request);
        return operationCode.craftResponse(result);
    }

    /**
     * Update existing master lookup
     * 
     * @param id Master lookup ID
     * @param request Master lookup update request
     * @return Updated master lookup
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update Master Lookup", description = "تحديث نوع قائمة مرجعية موجود")
    public ResponseEntity<ApiResponse<MasterLookupResponse>> update(
        @PathVariable Long id,
        @Valid @RequestBody MasterLookupUpdateRequest request
    ) {
        ServiceResult<MasterLookupResponse> result = masterLookupService.update(id, request);
        return operationCode.craftResponse(result);
    }

    /**
     * Get master lookup by ID
     * 
     * @param id Master lookup ID
     * @return Master lookup details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Master Lookup by ID", description = "جلب تفاصيل نوع القائمة المرجعية")
    public ResponseEntity<ApiResponse<MasterLookupResponse>> getById(@PathVariable Long id) {
        ServiceResult<MasterLookupResponse> result = masterLookupService.getById(id);
        return operationCode.craftResponse(result);
    }

    /**
     * Search master lookups with filtering, sorting, and pagination
     * 
     * Uses common-utils SearchRequest for dynamic filtering
     * 
     * Allowed filters:
     * - lookupKey (EQUALS, CONTAINS, STARTS_WITH)
     * - lookupName (EQUALS, CONTAINS, STARTS_WITH)
     * - lookupNameEn (EQUALS, CONTAINS, STARTS_WITH)
     * - isActive (EQUALS)
     * 
     * Allowed sort fields:
     * - id, lookupKey, lookupName, lookupNameEn, isActive, createdAt, updatedAt
     * 
     * @param searchRequest Search criteria
     * @return Page of master lookups
     */
    @PostMapping("/search")
    @Operation(
        summary = "Search Master Lookups", 
        description = "البحث في أنواع القوائم المرجعية مع الفلترة والترتيب والصفحات"
    )
    public ResponseEntity<ApiResponse<Page<MasterLookupResponse>>> search(@RequestBody MasterLookupSearchRequest searchRequest) {
        ServiceResult<Page<MasterLookupResponse>> result = masterLookupService.search(searchRequest.toCommonSearchRequest());
        return operationCode.craftResponse(result);
    }

    /**
     * Toggle active status of master lookup
     * 
     * Business Rule: Cannot deactivate if there are active lookup details
     * Rule 19.5: Single toggle endpoint (replaces separate activate/deactivate)
     * 
     * @param id Master lookup ID
     * @param request Toggle active request with target status
     * @return Updated master lookup
     */
    @PutMapping("/{id}/toggle-active")
    @Operation(summary = "Toggle Master Lookup Active Status", description = "تبديل حالة نشاط نوع القائمة المرجعية")
    public ResponseEntity<ApiResponse<MasterLookupResponse>> toggleActive(
        @PathVariable Long id,
        @Valid @RequestBody ToggleActiveRequest request
    ) {
        ServiceResult<MasterLookupResponse> result = masterLookupService.toggleActive(id, request.getActive());
        return operationCode.craftResponse(result);
    }

    /**
     * Delete master lookup
     * 
     * Business Rule: Cannot delete if it has lookup details (FK constraint)
     * Returns HTTP 409 CONFLICT if deletion fails due to FK constraint
     * 
     * @param id Master lookup ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete Master Lookup", description = "حذف نوع القائمة المرجعية")
    public void delete(@PathVariable Long id) {
        masterLookupService.delete(id);
    }

    /**
     * Get master lookup usage information
     * 
     * Shows where the master lookup is being used and whether it can be deleted/deactivated
     * 
     * @param id Master lookup ID
     * @return Usage information
     */
    @GetMapping("/{id}/usage")
    @Operation(summary = "Get Master Lookup Usage", description = "جلب معلومات استخدام نوع القائمة المرجعية")
    public ResponseEntity<ApiResponse<MasterLookupUsageResponse>> getUsage(@PathVariable Long id) {
        ServiceResult<MasterLookupUsageResponse> result = masterLookupService.getUsage(id);
        return operationCode.craftResponse(result);
    }

    // ================================================================================
    // LOOKUP DETAIL ENDPOINTS (Unified under Master Lookup)
    // All use PERM_MASTER_LOOKUP_* permissions
    // ================================================================================

    /**
     * Create new lookup detail for a master lookup
     * 
     * @param request Lookup detail create request
     * @return Created lookup detail
     */
    @PostMapping("/details")
    @Operation(summary = "Create Lookup Detail", description = "إنشاء قيمة مرجعية جديدة")
    public ResponseEntity<ApiResponse<LookupDetailResponse>> createDetail(@Valid @RequestBody LookupDetailCreateRequest request) {
        ServiceResult<LookupDetailResponse> result = lookupDetailService.create (request);
        return operationCode.craftResponse(result);
    }

    /**
     * Update existing lookup detail
     * 
     * @param id Lookup detail ID
     * @param request Lookup detail update request
     * @return Updated lookup detail
     */
    @PutMapping("/details/{id}")
    @Operation(summary = "Update Lookup Detail", description = "تحديث قيمة مرجعية موجودة")
    public ResponseEntity<ApiResponse<LookupDetailResponse>> updateDetail(
        @PathVariable Long id,
        @Valid @RequestBody LookupDetailUpdateRequest request
    ) {
        ServiceResult<LookupDetailResponse> result = lookupDetailService.update(id, request);
        return operationCode.craftResponse(result);
    }

    /**
     * Get lookup detail by ID
     * 
     * @param id Lookup detail ID
     * @return Lookup detail details
     */
    @GetMapping("/details/{id}")
    @Operation(summary = "Get Lookup Detail by ID", description = "جلب تفاصيل القيمة المرجعية")
    public ResponseEntity<ApiResponse<LookupDetailResponse>> getDetailById(@PathVariable Long id) {
        ServiceResult<LookupDetailResponse> result = lookupDetailService.getById(id);
        return operationCode.craftResponse(result);
    }

    /**
     * Search lookup details with filtering, sorting, and pagination
     * 
     * Allowed filters:
     * - masterLookupId (EQUALS) - REQUIRED for proper parent-child filtering
     * - code (EQUALS, CONTAINS, STARTS_WITH)
     * - nameAr (EQUALS, CONTAINS, STARTS_WITH)
     * - nameEn (EQUALS, CONTAINS, STARTS_WITH)
     * - isActive (EQUALS)
     * 
     * Allowed sort fields:
     * - id, code, nameAr, nameEn, sortOrder, isActive, createdAt, updatedAt
     * 
     * Default sort: sortOrder ASC
     * 
     * @param searchRequest Search criteria
     * @return Page of lookup details
     */
    @PostMapping("/details/search")
    @Operation(
        summary = "Search Lookup Details", 
        description = "البحث في القيم المرجعية مع الفلترة والترتيب والصفحات"
    )
    public ResponseEntity<ApiResponse<Page<LookupDetailResponse>>> searchDetails(@RequestBody LookupDetailSearchRequest searchRequest) {
        ServiceResult<Page<LookupDetailResponse>> result = lookupDetailService.search(
            searchRequest.getMasterLookupId(),
            searchRequest.toCommonSearchRequest()
        );
        return operationCode.craftResponse(result);
    }

    /**
     * Get lookup detail options by master lookup key
     * Used for dropdown options in UI
     * 
     * @param lookupKey Master lookup key (e.g., COLOR, UOM, COUNTRY)
     * @param active Filter by active status (default: true)
     * @return List of lookup detail options
     */
    @GetMapping("/details/options/{lookupKey}")
    @Operation(
        summary = "Get Lookup Options by Key", 
        description = "جلب خيارات القائمة المنسدلة حسب مفتاح القائمة المرجعية"
    )
    public ResponseEntity<ApiResponse<List<LookupDetailOptionResponse>>> getDetailOptions(
        @PathVariable 
        @Parameter(description = "Master lookup key (e.g., COLOR, UOM, COUNTRY)", example = "COLOR")
        String lookupKey,
        
        @RequestParam(required = false, defaultValue = "true")
        @Parameter(description = "Filter by active status", example = "true")
        Boolean active
    ) {
        ServiceResult<List<LookupDetailOptionResponse>> result = lookupDetailService.getOptionsByLookupKey(lookupKey, active);
        return operationCode.craftResponse(result);
    }

    /**
     * Toggle active status of lookup detail
     * 
     * Rule 19.5: Single toggle endpoint (replaces separate activate/deactivate)
     * 
     * @param id Lookup detail ID
     * @param request Toggle active request with target status
     * @return Updated lookup detail
     */
    @PutMapping("/details/{id}/toggle-active")
    @Operation(summary = "Toggle Lookup Detail Active Status", description = "تبديل حالة نشاط القيمة المرجعية")
    public ResponseEntity<ApiResponse<LookupDetailResponse>> toggleDetailActive(
        @PathVariable Long id,
        @Valid @RequestBody ToggleActiveRequest request
    ) {
        ServiceResult<LookupDetailResponse> result = lookupDetailService.toggleActive(id, request.getActive());
        return operationCode.craftResponse(result);
    }

    /**
     * Delete lookup detail
     * 
     * Business Rule: Cannot delete if referenced by any entity (FK constraint)
     * Returns HTTP 409 CONFLICT if deletion fails due to FK constraint
     * 
     * @param id Lookup detail ID
     */
    @DeleteMapping("/details/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete Lookup Detail", description = "حذف القيمة المرجعية")
    public void deleteDetail(@PathVariable Long id) {
        lookupDetailService.delete(id);
    }

    /**
     * Get lookup detail usage information
     * 
     * Shows where the lookup detail is being used and whether it can be deleted
     * 
     * @param id Lookup detail ID
     * @return Usage information
     */
    @GetMapping("/details/{id}/usage")
    @Operation(summary = "Get Lookup Detail Usage", description = "جلب معلومات استخدام القيمة المرجعية")
    public ResponseEntity<ApiResponse<LookupDetailUsageResponse>> getDetailUsage(@PathVariable Long id) {
        ServiceResult<LookupDetailUsageResponse> result = lookupDetailService.getUsage(id);
        return operationCode.craftResponse(result);
    }
}
