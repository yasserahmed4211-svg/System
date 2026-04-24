package com.example.org.controller;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.example.org.dto.DepartmentCreateRequest;
import com.example.org.dto.DepartmentResponse;
import com.example.org.dto.DepartmentUpdateRequest;
import com.example.org.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Department REST Controller — standalone endpoint per API-DP-02 / API-DP-03
 *
 * Maps POST /api/organization/departments and PUT /api/organization/departments/{id}
 * as required by test specification G7.spec.ts (TC-ORG-001-27, TC-ORG-001-29).
 *
 * Delegates to existing BranchService department methods — no new business logic.
 */
@RestController
@RequestMapping("/api/organization/departments")
@RequiredArgsConstructor
@Tag(name = "Department Management", description = "إدارة الأقسام - Department CRUD Operations")
public class DepartmentController {

    private final BranchService branchService;
    private final OperationCode operationCode;

    // [API-DP-02] POST /api/organization/departments
    @PostMapping
    @Operation(summary = "Create Department", description = "إنشاء قسم جديد مرتبط بفرع")
    public ResponseEntity<ApiResponse<DepartmentResponse>> create(
            @Valid @RequestBody DepartmentCreateRequest request) {
        ServiceResult<DepartmentResponse> result = branchService.createDepartment(request.getBranchId(), request);
        return operationCode.craftResponse(result);
    }

    // [API-DP-02 read-back] GET /api/organization/departments/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Get Department by ID", description = "جلب تفاصيل القسم")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getById(@PathVariable Long id) {
        ServiceResult<DepartmentResponse> result = branchService.getDepartmentById(id);
        return operationCode.craftResponse(result);
    }

    // [API-DP-03] PUT /api/organization/departments/{id}
    @PutMapping("/{id}")
    @Operation(summary = "Update Department", description = "تحديث قسم موجود")
    public ResponseEntity<ApiResponse<DepartmentResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentUpdateRequest request) {
        ServiceResult<DepartmentResponse> result = branchService.updateDepartment(id, request);
        return operationCode.craftResponse(result);
    }
}
