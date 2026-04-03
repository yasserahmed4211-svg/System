package com.example.erp.finance.gl.controller;

import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.erp.common.search.SearchRequest;
import com.example.erp.finance.gl.dto.*;
import com.example.erp.finance.gl.service.AccRuleHdrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gl/rules")
@RequiredArgsConstructor
@Tag(name = "Accounting Rules", description = "القواعد المحاسبية")
public class AccRuleHdrController {

    private final AccRuleHdrService accRuleHdrService;
    private final OperationCode operationCode;

    @PostMapping("/search")
    @Operation(summary = "Search Accounting Rules",
            description = "Search accounting rules with dynamic filters, sorting, and pagination. "
                    + "Allowed filter fields: companyIdFk, sourceModule, sourceDocType, isActive. "
                    + "Allowed sort fields: ruleId, companyIdFk, sourceModule, sourceDocType, isActive, createdAt, updatedAt.")
    public ResponseEntity<ApiResponse<Page<AccRuleHdrResponse>>> search(@RequestBody AccRuleHdrSearchContractRequest searchRequest) {
        return operationCode.craftResponse(accRuleHdrService.search(searchRequest.toCommonSearchRequest()));
    }

    @GetMapping("/{ruleId}")
    @Operation(summary = "Get Rule Details (Header + Lines)")
    public ResponseEntity<ApiResponse<AccRuleHdrResponse>> getById(@PathVariable Long ruleId) {
        return operationCode.craftResponse(accRuleHdrService.getById(ruleId));
    }

    @PostMapping
    @Operation(summary = "Create Rule (Header + Lines)")
    public ResponseEntity<ApiResponse<AccRuleHdrResponse>> create(@Valid @RequestBody AccRuleHdrCreateRequest request) {
        return operationCode.craftResponse(accRuleHdrService.create(request));
    }

    @PutMapping("/{ruleId}")
    @Operation(summary = "Update Rule (Header + Lines)")
    public ResponseEntity<ApiResponse<AccRuleHdrResponse>> update(
            @PathVariable Long ruleId,
            @Valid @RequestBody AccRuleHdrUpdateRequest request) {
        return operationCode.craftResponse(accRuleHdrService.update(ruleId, request));
    }

    @PatchMapping("/{ruleId}/deactivate")
    @Operation(summary = "Deactivate Rule")
    public ResponseEntity<ApiResponse<AccRuleHdrResponse>> deactivate(@PathVariable Long ruleId) {
        return operationCode.craftResponse(accRuleHdrService.deactivate(ruleId));
    }
}
