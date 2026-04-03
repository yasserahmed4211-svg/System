package com.example.erp.finance.gl.controller;

import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.example.erp.finance.gl.dto.AccPostingMstResponse;
import com.example.erp.finance.gl.dto.AccPostingSearchContractRequest;
import com.example.erp.finance.gl.dto.JournalPreviewResponse;
import com.example.erp.finance.gl.dto.PostingResponse;
import com.example.erp.finance.gl.service.AccPostingService;
import com.example.erp.finance.gl.service.JournalGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for ACC_POSTING_MST operations and Journal Generation bridge.
 *
 * @architecture Controller — thin layer, delegates to AccPostingService and JournalGenerationService
 */
@RestController
@RequestMapping("/api/gl/postings")
@RequiredArgsConstructor
@Tag(name = "GL Postings", description = "مستندات الترحيل وتوليد القيود")
public class AccPostingController {

    private final AccPostingService accPostingService;
    private final JournalGenerationService journalGenerationService;
    private final OperationCode operationCode;

    // ── Query Endpoints ─────────────────────────────────────

    @PostMapping("/search")
    @Operation(summary = "Search Postings",
            description = "Search posting records with dynamic filters, sorting, and pagination. "
                    + "Allowed filter fields: status, sourceModule, sourceDocType, companyIdFk, docDate. "
                    + "Allowed sort fields: postingId, companyIdFk, docDate, status, sourceModule, sourceDocType, totalAmount, createdAt, updatedAt.")
    public ResponseEntity<ApiResponse<Page<AccPostingMstResponse>>> search(
            @Valid @RequestBody AccPostingSearchContractRequest searchRequest) {
        return operationCode.craftResponse(accPostingService.search(searchRequest.toCommonSearchRequest()));
    }

    @GetMapping("/{postingId}")
    @Operation(summary = "Get Posting Details (Header + Details)",
            description = "عرض تفاصيل مستند الترحيل مع سطور التفاصيل")
    public ResponseEntity<ApiResponse<AccPostingMstResponse>> getById(@PathVariable Long postingId) {
        return operationCode.craftResponse(accPostingService.getById(postingId));
    }

    // ── Journal Generation Bridge ───────────────────────────

    @GetMapping("/{postingId}/preview-journal")
    @Operation(summary = "Preview Journal from Posting",
            description = "معاينة القيد المتوقع من مستند ترحيل بحالة READY_FOR_GL. "
                    + "يُحاكي محرك الترحيل دون حفظ أي بيانات. "
                    + "يُرجع سطور القيد المتوقعة مع التحقق من التوازن.")
    public ResponseEntity<ApiResponse<JournalPreviewResponse>> previewJournal(@PathVariable Long postingId) {
        return operationCode.craftResponse(journalGenerationService.previewJournalFromPosting(postingId));
    }

    @PostMapping("/{postingId}/generate-journal")
    @Operation(summary = "Generate Journal from Posting",
            description = "توليد قيد يومية آلي من مستند ترحيل بحالة READY_FOR_GL. "
                    + "يستخدم محرك الترحيل للبحث عن القاعدة المحاسبية المطابقة وإنشاء القيد. "
                    + "العملية متكررة-آمنة (idempotent): إذا تم توليد قيد سابقاً يتم إرجاع خطأ.")
    public ResponseEntity<ApiResponse<PostingResponse>> generateJournal(@PathVariable Long postingId) {
        return operationCode.craftResponse(journalGenerationService.createJournalFromPosting(postingId));
    }
}
