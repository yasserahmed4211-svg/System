package com.example.erp.finance.gl.controller;

import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.example.erp.finance.gl.dto.*;
import com.example.erp.finance.gl.service.GlJournalManualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gl/journals/manual")
@RequiredArgsConstructor
@Tag(name = "GL Manual Journals", description = "قيود يومية يدوية")
public class GlJournalManualController {

    private final GlJournalManualService manualService;
    private final OperationCode operationCode;

    @PostMapping
    @Operation(summary = "Create Manual Journal",
            description = "Creates a manual GL journal. Type is always MANUAL. "
                    + "Source and entity dimension fields are forced to NULL.")
    public ResponseEntity<ApiResponse<GlJournalHdrResponse>> create(
            @Valid @RequestBody GlJournalManualCreateRequest request) {
        return operationCode.craftResponse(manualService.create(request));
    }

    @PutMapping("/{journalId}")
    @Operation(summary = "Update Manual Journal",
            description = "Updates a manual GL journal. Only DRAFT journals with type MANUAL can be updated.")
    public ResponseEntity<ApiResponse<GlJournalHdrResponse>> update(
            @PathVariable Long journalId,
            @Valid @RequestBody GlJournalManualUpdateRequest request) {
        return operationCode.craftResponse(manualService.update(journalId, request));
    }
}
