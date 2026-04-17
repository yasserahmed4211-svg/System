package com.example.org.controller;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.example.org.dto.*;
import com.example.org.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Branch REST Controller
 *
 * Unified controller for Branch AND Department management.
 * Departments are inline entities managed under their parent branch.
 *
 * Architecture Rules:
 * - Thin controllers — delegate all logic to service
 * - Never expose entities — use DTOs only
 * - No repository injection
 */
@RestController
@RequestMapping("/api/organization/branches")
@RequiredArgsConstructor
@Tag(name = "Branch Management", description = "إدارة الفروع والأقسام - Branch & Department CRUD Operations")
public class BranchController {

    private final BranchService branchService;
    private final OperationCode operationCode;

    // ==================== Branch Endpoints ====================

    @GetMapping
    @Operation(summary = "List Branches", description = "استرجاع قائمة الفروع مع الفلترة والترقيم")
    public ResponseEntity<ApiResponse<Page<BranchResponse>>> list(
            @RequestParam(required = false) String branchCode,
            @RequestParam(required = false) String branchNameAr,
            @RequestParam(required = false) Long legalEntityFk,
            @RequestParam(required = false) Long regionFk,
            @RequestParam(required = false) String branchTypeId,
            @RequestParam(required = false) String statusId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        ServiceResult<Page<BranchResponse>> result =
                branchService.list(branchCode, branchNameAr, legalEntityFk, regionFk, branchTypeId, statusId, page, pageSize);
        return operationCode.craftResponse(result);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate Branch", description = "إيقاف تفعيل الفرع")
    public ResponseEntity<ApiResponse<BranchResponse>> deactivate(@PathVariable Long id) {
        ServiceResult<BranchResponse> result = branchService.deactivate(id);
        return operationCode.craftResponse(result);
    }

    @PostMapping
    @Operation(summary = "Create Branch", description = "إنشاء فرع جديد مع أقسامه")
    public ResponseEntity<ApiResponse<BranchResponse>> create(
            @Valid @RequestBody BranchCreateRequest request) {
        ServiceResult<BranchResponse> result = branchService.create(request);
        return operationCode.craftResponse(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Branch", description = "تحديث فرع موجود مع أقسامه")
    public ResponseEntity<ApiResponse<BranchResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody BranchUpdateRequest request) {
        ServiceResult<BranchResponse> result = branchService.update(id, request);
        return operationCode.craftResponse(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Branch by ID", description = "جلب تفاصيل الفرع مع أقسامه")
    public ResponseEntity<ApiResponse<BranchResponse>> getById(@PathVariable Long id) {
        ServiceResult<BranchResponse> result = branchService.getById(id);
        return operationCode.craftResponse(result);
    }

    @PostMapping("/search")
    @Operation(summary = "Search Branches", description = "البحث في الفروع مع الفلترة والترتيب والصفحات")
    public ResponseEntity<ApiResponse<Page<BranchResponse>>> search(
            @RequestBody BranchSearchRequest searchRequest) {
        ServiceResult<Page<BranchResponse>> result = branchService.search(searchRequest.toCommonSearchRequest());
        return operationCode.craftResponse(result);
    }

    @PutMapping("/{id}/toggle-active")
    @Operation(summary = "Toggle Branch Active Status", description = "تبديل حالة نشاط الفرع")
    public ResponseEntity<ApiResponse<BranchResponse>> toggleActive(
            @PathVariable Long id,
            @Valid @RequestBody ToggleActiveRequest request) {
        ServiceResult<BranchResponse> result = branchService.toggleActive(id, request.getActive());
        return operationCode.craftResponse(result);
    }

    @GetMapping("/{id}/usage")
    @Operation(summary = "Get Branch Usage", description = "جلب معلومات استخدام الفرع")
    public ResponseEntity<ApiResponse<BranchUsageResponse>> getUsage(@PathVariable Long id) {
        ServiceResult<BranchUsageResponse> result = branchService.getUsage(id);
        return operationCode.craftResponse(result);
    }

    // ==================== Department Endpoints (under Branch) ====================

    @GetMapping("/{id}/departments")
    @Operation(summary = "Get Departments by Branch", description = "استرجاع أقسام الفرع")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getDepartmentsByBranch(@PathVariable Long id) {
        ServiceResult<List<DepartmentResponse>> result = branchService.getDepartmentsByBranch(id);
        return operationCode.craftResponse(result);
    }

    @PatchMapping("/departments/{id}/deactivate")
    @Operation(summary = "Deactivate Department", description = "إيقاف تفعيل القسم")
    public ResponseEntity<ApiResponse<DepartmentResponse>> deactivateDepartment(@PathVariable Long id) {
        ServiceResult<DepartmentResponse> result = branchService.deactivateDepartment(id);
        return operationCode.craftResponse(result);
    }

    @PostMapping("/{branchId}/departments")
    @Operation(summary = "Create Department", description = "إنشاء قسم جديد داخل فرع")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @PathVariable Long branchId,
            @Valid @RequestBody DepartmentCreateRequest request) {
        ServiceResult<DepartmentResponse> result = branchService.createDepartment(branchId, request);
        return operationCode.craftResponse(result);
    }

    @PutMapping("/departments/{id}")
    @Operation(summary = "Update Department", description = "تحديث قسم موجود")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentUpdateRequest request) {
        ServiceResult<DepartmentResponse> result = branchService.updateDepartment(id, request);
        return operationCode.craftResponse(result);
    }

    @PutMapping("/departments/{id}/toggle-active")
    @Operation(summary = "Toggle Department Active Status", description = "تبديل حالة نشاط القسم")
    public ResponseEntity<ApiResponse<DepartmentResponse>> toggleDepartmentActive(
            @PathVariable Long id,
            @Valid @RequestBody ToggleActiveRequest request) {
        ServiceResult<DepartmentResponse> result = branchService.toggleDepartmentActive(id, request.getActive());
        return operationCode.craftResponse(result);
    }

    @DeleteMapping("/departments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete Department (Blocked)", description = "حذف القسم غير مسموح — استخدم تبديل حالة النشاط بدلاً من ذلك")
    public void deleteDepartment(@PathVariable Long id) {
        branchService.deleteDepartment(id);
    }
}
