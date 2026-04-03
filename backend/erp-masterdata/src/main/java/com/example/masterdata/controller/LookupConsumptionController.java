package com.example.masterdata.controller;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.example.masterdata.dto.LookupValueResponse;
import com.example.masterdata.service.LookupConsumptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lookup Consumption REST Controller
 * 
 * Provides generic read-only access to lookup values
 * for consumption by all ERP modules
 * 
 * Architecture Rules:
 * - Rule 4.1: Thin controllers (NO business logic)
 * - Rule 4.2: Never expose entities
 * - Rule 10.7: Standard read operations
 * - CLEAN ARCHITECTURE: Controller translates ServiceResult → HTTP via OperationCode
 * 
 * @author ERP Team
 */
@RestController
@RequestMapping("/api/lookups")
@RequiredArgsConstructor
@Tag(name = "Lookup Consumption", description = "استهلاك القوائم المرجعية - Generic Lookup Access for All Modules")
public class LookupConsumptionController {

    private final LookupConsumptionService lookupConsumptionService;
    private final OperationCode operationCode;

    /**
     * Get lookup values by lookup code
     * 
     * Returns only ACTIVE lookup details ordered by sortOrder
     * Response is cached for optimal performance
     * 
     * This endpoint is used by all ERP modules to populate
     * dropdown lists and validate lookup values
     * 
     * Architecture: Uses OperationCode.craftResponse to translate
     * ServiceResult → ResponseEntity<ApiResponse> (ZERO business logic here)
     * 
     * @param lookupCode Master lookup code (e.g., "ACCOUNT_TYPE", "STATUS", "COUNTRY")
     * @return ResponseEntity with ApiResponse containing list of lookup values
     */
    @GetMapping("/{lookupCode}")
    @Operation(
        summary = "Get Lookup Values", 
        description = "جلب قيم القائمة المرجعية - للاستخدام في جميع الوحدات"
    )
    public ResponseEntity<ApiResponse<List<LookupValueResponse>>> getLookupValues(
        @PathVariable 
        @Parameter(
            description = "Master lookup code (e.g., ACCOUNT_TYPE, STATUS, COUNTRY)", 
            example = "ACCOUNT_TYPE"
        )
        String lookupCode
    ) {
        ServiceResult<List<LookupValueResponse>> result = lookupConsumptionService.fetchLookupValues(lookupCode);
        return operationCode.craftResponse(result);
    }
}
