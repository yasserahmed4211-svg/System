package com.example.erp.finance.gl.controller;

import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.erp.common.search.SearchRequest;
import com.example.erp.finance.gl.dto.*;
import com.example.erp.finance.gl.service.GlJournalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gl/journals")
@RequiredArgsConstructor
@Tag(name = "GL Journals", description = "قيود اليومية")
public class GlJournalController {

    private final GlJournalService journalService;
    private final OperationCode operationCode;

    // ── Core CRUD ────────────────────────────────────────────

    @PostMapping("/search")
    @Operation(summary = "Search Journals",
            description = "Search journals with dynamic filters, sorting, and pagination. "
                    + "Allowed filter fields: journalNo, journalDate, journalTypeIdFk, statusIdFk, sourceModuleIdFk, activeFl. "
                    + "Allowed sort fields: id, journalNo, journalDate, journalTypeIdFk, statusIdFk, sourceModuleIdFk, totalDebit, totalCredit, activeFl, createdAt, updatedAt.")
    public ResponseEntity<ApiResponse<Page<GlJournalHdrResponse>>> search(
            @RequestBody GlJournalSearchContractRequest searchRequest) {
        return operationCode.craftResponse(journalService.search(searchRequest.toCommonSearchRequest()));
    }

    @GetMapping("/{journalId}")
    @Operation(summary = "Get Journal Details (Header + Lines)")
    public ResponseEntity<ApiResponse<GlJournalHdrResponse>> getById(@PathVariable Long journalId) {
        return operationCode.craftResponse(journalService.getById(journalId));
    }

    @PutMapping("/{journalId}")
    @Operation(summary = "Update Journal",
            description = "Updates a GL journal. For AUTOMATIC journals only description and date are updated.")
    public ResponseEntity<ApiResponse<GlJournalHdrResponse>> update(
            @PathVariable Long journalId,
            @Valid @RequestBody GlJournalHdrUpdateRequest request) {
        return operationCode.craftResponse(journalService.update(journalId, request));
    }

    @PutMapping("/{journalId}/toggle-active")
    @Operation(summary = "Toggle Journal Active Status")
    public ResponseEntity<ApiResponse<GlJournalHdrResponse>> toggleActive(
            @PathVariable Long journalId,
            @Valid @RequestBody com.example.masterdata.dto.ToggleActiveRequest request) {
        return operationCode.craftResponse(journalService.toggleActive(journalId, request.getActive()));
    }

    // ── State Transitions ───────────────────────────────────

    @PatchMapping("/{journalId}/approve")
    @Operation(summary = "Approve Journal")
    public ResponseEntity<ApiResponse<GlJournalHdrResponse>> approve(@PathVariable Long journalId) {
        return operationCode.craftResponse(journalService.approve(journalId));
    }

    @PatchMapping("/{journalId}/post")
    @Operation(summary = "Post Journal")
    public ResponseEntity<ApiResponse<GlJournalHdrResponse>> post(@PathVariable Long journalId) {
        return operationCode.craftResponse(journalService.post(journalId));
    }

    @PatchMapping("/{journalId}/reverse")
    @Operation(summary = "Reverse Journal (creates reversal journal)")
    public ResponseEntity<ApiResponse<GlJournalHdrResponse>> reverse(@PathVariable Long journalId) {
        return operationCode.craftResponse(journalService.reverse(journalId));
    }

    @PatchMapping("/{journalId}/cancel")
    @Operation(summary = "Cancel Journal")
    public ResponseEntity<ApiResponse<GlJournalHdrResponse>> cancel(@PathVariable Long journalId) {
        return operationCode.craftResponse(journalService.cancel(journalId));
    }
}
