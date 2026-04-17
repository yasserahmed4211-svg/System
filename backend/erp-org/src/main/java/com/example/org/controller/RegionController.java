package com.example.org.controller;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.example.org.dto.*;
import com.example.org.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Region REST Controller
 *
 * Architecture Rules:
 * - Thin controllers — delegate all logic to service
 * - Never expose entities — use DTOs only
 * - No repository injection
 */
@RestController
@RequestMapping("/api/organization/regions")
@RequiredArgsConstructor
@Tag(name = "Region Management", description = "إدارة المناطق - Region CRUD Operations")
public class RegionController {

    private final RegionService regionService;
    private final OperationCode operationCode;

    @GetMapping
    @Operation(summary = "List Regions", description = "استرجاع قائمة المناطق مع الفلترة والترقيم")
    public ResponseEntity<ApiResponse<Page<RegionResponse>>> list(
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) String regionNameAr,
            @RequestParam(required = false) Long legalEntityFk,
            @RequestParam(required = false) String statusId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        ServiceResult<Page<RegionResponse>> result =
                regionService.list(regionCode, regionNameAr, legalEntityFk, statusId, page, pageSize);
        return operationCode.craftResponse(result);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate Region", description = "إيقاف تفعيل المنطقة")
    public ResponseEntity<ApiResponse<RegionResponse>> deactivate(@PathVariable Long id) {
        ServiceResult<RegionResponse> result = regionService.deactivate(id);
        return operationCode.craftResponse(result);
    }

    @PostMapping
    @Operation(summary = "Create Region", description = "إنشاء منطقة جديدة")
    public ResponseEntity<ApiResponse<RegionResponse>> create(
            @Valid @RequestBody RegionCreateRequest request) {
        ServiceResult<RegionResponse> result = regionService.create(request);
        return operationCode.craftResponse(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Region", description = "تحديث منطقة موجودة")
    public ResponseEntity<ApiResponse<RegionResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody RegionUpdateRequest request) {
        ServiceResult<RegionResponse> result = regionService.update(id, request);
        return operationCode.craftResponse(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Region by ID", description = "جلب تفاصيل المنطقة")
    public ResponseEntity<ApiResponse<RegionResponse>> getById(@PathVariable Long id) {
        ServiceResult<RegionResponse> result = regionService.getById(id);
        return operationCode.craftResponse(result);
    }

    @PostMapping("/search")
    @Operation(summary = "Search Regions", description = "البحث في المناطق مع الفلترة والترتيب والصفحات")
    public ResponseEntity<ApiResponse<Page<RegionResponse>>> search(
            @RequestBody RegionSearchRequest searchRequest) {
        ServiceResult<Page<RegionResponse>> result = regionService.search(searchRequest.toCommonSearchRequest());
        return operationCode.craftResponse(result);
    }

    @PutMapping("/{id}/toggle-active")
    @Operation(summary = "Toggle Region Active Status", description = "تبديل حالة نشاط المنطقة")
    public ResponseEntity<ApiResponse<RegionResponse>> toggleActive(
            @PathVariable Long id,
            @Valid @RequestBody ToggleActiveRequest request) {
        ServiceResult<RegionResponse> result = regionService.toggleActive(id, request.getActive());
        return operationCode.craftResponse(result);
    }

    @GetMapping("/{id}/usage")
    @Operation(summary = "Get Region Usage", description = "جلب معلومات استخدام المنطقة")
    public ResponseEntity<ApiResponse<RegionUsageResponse>> getUsage(@PathVariable Long id) {
        ServiceResult<RegionUsageResponse> result = regionService.getUsage(id);
        return operationCode.craftResponse(result);
    }
}
