package com.example.erp.finance.gl.controller;

import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.example.erp.finance.gl.dto.PostingRequest;
import com.example.erp.finance.gl.dto.PostingResponse;
import com.example.erp.finance.gl.service.PostingEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the dynamic Posting Engine.
 * <p>
 * This endpoint is restricted to SYSTEM_ADMIN only.
 * Normal posting flow should go through:
 * POST /api/gl/postings/{id}/generate-journal (AccPostingController)
 * which uses JournalGenerationService → PostingEngineService internally.
 * <p>
 * Direct use of this endpoint bypasses posting lifecycle management
 * (status tracking, idempotency, posting-journal linkage).
 *
 * @architecture Controller — INTERNAL/ADMIN endpoint, delegates to PostingEngineService
 */
@RestController
@RequestMapping("/api/gl/posting")
@RequiredArgsConstructor
@Tag(name = "GL Posting Engine", description = "محرك الترحيل الآلي — للاستخدام الداخلي فقط")
public class PostingEngineController {

    private final PostingEngineService postingEngineService;
    private final OperationCode operationCode;

    /**
     * Execute a dynamic posting directly via the rule engine.
     * <p>
     * WARNING: This endpoint bypasses the posting lifecycle (ACC_POSTING_MST).
     * Use POST /api/gl/postings/{id}/generate-journal for standard posting flow.
     * This endpoint is restricted to SYSTEM_ADMIN for internal/diagnostic use only.
     *
     * @param request posting parameters including totalAmount, entity references, etc.
     * @return the generated journal entry wrapped in a PostingResponse
     * @deprecated Use AccPostingController.generateJournal() for standard flow
     */
    @Deprecated
    @PostMapping("/execute")
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    @Operation(summary = "Execute Automatic Posting (ADMIN ONLY)",
            description = "⚠️ نقطة نهاية داخلية — للمسؤول فقط. "
                    + "تنفيذ ترحيل آلي مباشر عبر محرك القواعد. "
                    + "للاستخدام العادي استخدم: POST /api/gl/postings/{id}/generate-journal")
    public ResponseEntity<ApiResponse<PostingResponse>> execute(
            @Valid @RequestBody PostingRequest request) {
        return operationCode.craftResponse(postingEngineService.execute(request));
    }
}
