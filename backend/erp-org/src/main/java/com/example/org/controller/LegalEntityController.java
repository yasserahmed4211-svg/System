package com.example.org.controller;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.example.org.dto.*;
import com.example.org.service.LegalEntityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Legal Entity REST Controller
 *
 * Architecture Rules:
 * - Thin controllers — delegate all logic to service
 * - Never expose entities — use DTOs only
 * - No repository injection
 */
@RestController
@RequestMapping("/api/organization/legal-entities")
@RequiredArgsConstructor
@Tag(name = "Legal Entity Management", description = "إدارة الكيانات القانونية - Legal Entity CRUD Operations")
public class LegalEntityController {

    private final LegalEntityService legalEntityService;
    private final OperationCode operationCode;

    @GetMapping
    @Operation(summary = "List Legal Entities", description = "استرجاع قائمة الكيانات القانونية مع الفلترة والترقيم")
    public ResponseEntity<ApiResponse<Page<LegalEntityResponse>>> list(
            @RequestParam(required = false) String legalEntityCode,
            @RequestParam(required = false) String legalEntityNameAr,
            @RequestParam(required = false) Long countryFk,
            @RequestParam(required = false) String statusId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        ServiceResult<Page<LegalEntityResponse>> result =
                legalEntityService.list(legalEntityCode, legalEntityNameAr, countryFk, statusId, page, pageSize);
        return operationCode.craftResponse(result);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate Legal Entity", description = "إيقاف تفعيل الكيان القانوني")
    public ResponseEntity<ApiResponse<LegalEntityResponse>> deactivate(@PathVariable Long id) {
        ServiceResult<LegalEntityResponse> result = legalEntityService.deactivate(id);
        return operationCode.craftResponse(result);
    }

    @PostMapping
    @Operation(summary = "Create Legal Entity", description = "إنشاء كيان قانوني جديد")
    public ResponseEntity<ApiResponse<LegalEntityResponse>> create(
            @Valid @RequestBody LegalEntityCreateRequest request) {
        ServiceResult<LegalEntityResponse> result = legalEntityService.create(request);
        return operationCode.craftResponse(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Legal Entity", description = "تحديث كيان قانوني موجود")
    public ResponseEntity<ApiResponse<LegalEntityResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody LegalEntityUpdateRequest request) {
        ServiceResult<LegalEntityResponse> result = legalEntityService.update(id, request);
        return operationCode.craftResponse(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Legal Entity by ID", description = "جلب تفاصيل الكيان القانوني")
    public ResponseEntity<ApiResponse<LegalEntityResponse>> getById(@PathVariable Long id) {
        ServiceResult<LegalEntityResponse> result = legalEntityService.getById(id);
        return operationCode.craftResponse(result);
    }

    @PostMapping("/search")
    @Operation(summary = "Search Legal Entities", description = "البحث في الكيانات القانونية مع الفلترة والترتيب والصفحات")
    public ResponseEntity<ApiResponse<Page<LegalEntityResponse>>> search(
            @RequestBody LegalEntitySearchRequest searchRequest) {
        ServiceResult<Page<LegalEntityResponse>> result = legalEntityService.search(searchRequest.toCommonSearchRequest());
        return operationCode.craftResponse(result);
    }

    @PutMapping("/{id}/toggle-active")
    @Operation(summary = "Toggle Legal Entity Active Status", description = "تبديل حالة نشاط الكيان القانوني")
    public ResponseEntity<ApiResponse<LegalEntityResponse>> toggleActive(
            @PathVariable Long id,
            @Valid @RequestBody ToggleActiveRequest request) {
        ServiceResult<LegalEntityResponse> result = legalEntityService.toggleActive(id, request.getActive());
        return operationCode.craftResponse(result);
    }

    @GetMapping("/{id}/usage")
    @Operation(summary = "Get Legal Entity Usage", description = "جلب معلومات استخدام الكيان القانوني")
    public ResponseEntity<ApiResponse<LegalEntityUsageResponse>> getUsage(@PathVariable Long id) {
        ServiceResult<LegalEntityUsageResponse> result = legalEntityService.getUsage(id);
        return operationCode.craftResponse(result);
    }
}
