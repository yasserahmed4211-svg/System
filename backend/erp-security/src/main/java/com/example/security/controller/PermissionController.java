package com.example.security.controller;

import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.example.security.dto.CreatePermissionRequest;
import com.example.security.dto.PermissionDto;
import com.example.security.dto.UpdatePermissionRequest;
import com.example.security.dto.PermissionSearchContractRequest;
import com.example.security.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Tag(name = "Permission Management", description = "APIs for managing system permissions")
public class PermissionController {

    private final PermissionService permissionService;
    private final OperationCode operationCode;

    @PostMapping
    @Operation(summary = "Create new permission", description = "Creates a new system permission")
    public ResponseEntity<ApiResponse<PermissionDto>> create(@RequestBody @Valid CreatePermissionRequest req) {
        return operationCode.craftResponse(permissionService.createPermission(req));
    }

    @PostMapping("/search")
    @Operation(
        summary = "Search permissions",
        description = "Search permissions with dynamic filters, sorting, and pagination. "
                + "Allowed filter fields: name, module. "
                + "Allowed sort fields: id, name, module, createdAt, updatedAt."
    )
    public ResponseEntity<ApiResponse<Page<PermissionDto>>> searchPermissions(@RequestBody PermissionSearchContractRequest searchRequest) {
        return operationCode.craftResponse(permissionService.searchPermissions(searchRequest.toCommonSearchRequest()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update permission", description = "Updates an existing system permission")
    public ResponseEntity<ApiResponse<PermissionDto>> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdatePermissionRequest req) {
        return operationCode.craftResponse(permissionService.updatePermission(id, req));
    }

}
