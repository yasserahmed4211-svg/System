package com.example.security.controller;

import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.example.security.dto.CreatePageRequest;
import com.example.security.dto.PageResponse;
import com.example.security.dto.PageSearchContractRequest;
import com.example.security.dto.UpdatePageRequest;
import com.example.security.service.PageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing Pages (UI screens)
 * 
 * Pages are the DETAIL in RBAC model (Role is MASTER)
 * Each Page auto-generates 4 CRUD permissions: VIEW, CREATE, UPDATE, DELETE
 * 
 * @see PageService
 */
@RestController
@RequestMapping("/api/pages")
@RequiredArgsConstructor
@Tag(name = "Page Management", description = "إدارة الصفحات (Pages/Screens)")
public class PageController {

    private final PageService pageService;
    private final OperationCode operationCode;

    /**
     * POST /api/pages
     * إنشاء صفحة جديدة مع توليد 4 صلاحيات تلقائياً
     */
    @PostMapping
    @Operation(
        summary = "Create new page",
        description = "Register a new page/screen and auto-generate 4 CRUD permissions (VIEW, CREATE, UPDATE, DELETE)"
    )
    public ResponseEntity<ApiResponse<PageResponse>> createPage(@Valid @RequestBody CreatePageRequest request) {
        return operationCode.craftResponse(pageService.createPage(request));
    }

    /**
     * POST /api/pages/search
     * Search pages with dynamic filters, sorting, and pagination
     */
    @PostMapping("/search")
    @Operation(
        summary = "Search pages",
        description = "Search pages with dynamic filters, sorting, and pagination. "
                + "Allowed filter fields: pageCode, nameAr, nameEn, module, active. "
                + "Allowed sort fields: id, pageCode, nameAr, nameEn, module, displayOrder, createdAt, updatedAt."
    )
    public ResponseEntity<ApiResponse<Page<PageResponse>>> searchPages(@RequestBody PageSearchContractRequest searchRequest) {
        return operationCode.craftResponse(pageService.searchPages(searchRequest.toCommonSearchRequest()));
    }

    /**
     * GET /api/pages/active
     * جلب جميع الصفحات النشطة (للاستخدام في Dropdowns)
     */
    @GetMapping("/active")
    @Operation(
        summary = "Get active pages",
        description = "Get all active pages for dropdowns (used in Role Access Control 'Add Page' dropdown)"
    )
    public ResponseEntity<ApiResponse<java.util.List<PageResponse>>> getActivePages() {
        return operationCode.craftResponse(pageService.getActivePages());
    }

    /**
     * GET /api/pages/{id}
     * جلب صفحة واحدة بالـ ID
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get page by ID",
        description = "Retrieve a single page by its ID"
    )
    public ResponseEntity<ApiResponse<PageResponse>> getPageById(@PathVariable Long id) {
        return operationCode.craftResponse(pageService.getPageById(id));
    }

    /**
     * PUT /api/pages/{id}
     * تعديل صفحة موجودة
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Update page",
        description = "Update an existing page (pageCode cannot be changed)"
    )
    public ResponseEntity<ApiResponse<PageResponse>> updatePage(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePageRequest request
    ) {
        return operationCode.craftResponse(pageService.updatePage(id, request));
    }

    /**
     * PUT /api/pages/{id}/deactivate
     * تعطيل صفحة (Soft Delete)
     */
    @PutMapping("/{id}/deactivate")
    @Operation(
        summary = "Deactivate page",
        description = "Deactivate a page (soft delete - sets active = false)"
    )
    public ResponseEntity<ApiResponse<PageResponse>> deactivatePage(@PathVariable Long id) {
        return operationCode.craftResponse(pageService.deactivatePage(id));
    }

    /**
     * PUT /api/pages/{id}/reactivate
     * إعادة تفعيل صفحة معطلة
     */
    @PutMapping("/{id}/reactivate")
    @Operation(
        summary = "Reactivate page",
        description = "Reactivate a previously deactivated page (sets active = true)"
    )
    public ResponseEntity<ApiResponse<PageResponse>> reactivatePage(@PathVariable Long id) {
        return operationCode.craftResponse(pageService.reactivatePage(id));
    }

}
