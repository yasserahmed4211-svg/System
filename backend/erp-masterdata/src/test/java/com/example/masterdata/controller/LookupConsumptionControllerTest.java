package com.example.masterdata.controller;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.example.masterdata.dto.LookupValueResponse;
import com.example.masterdata.service.LookupConsumptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for LookupConsumptionController
 *
 * Covers:
 * - GET lookup values by lookup code
 * - StandardResponse (ApiResponse) structure
 * - Not found scenario (inactive / missing lookup)
 *
 * NOTE: Spring Boot 4.x removed @WebMvcTest and @MockBean.
 * Tests use MockMvcBuilders.standaloneSetup() for HTTP slice testing.
 * JWT authentication is an integration test concern.
 *
 * @author ERP Team
 */
@ExtendWith(MockitoExtension.class)
class LookupConsumptionControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private LookupConsumptionService lookupConsumptionService;

    @Mock
    private OperationCode operationCode;

    @InjectMocks
    private LookupConsumptionController controller;

    private List<LookupValueResponse> sampleValues;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        sampleValues = List.of(
                LookupValueResponse.builder().code("DEBIT").label("مدين").labelEn("Debit").build(),
                LookupValueResponse.builder().code("CREDIT").label("دائن").labelEn("Credit").build()
        );

        // Default: craftResponse returns 200 with data
        lenient().when(operationCode.craftResponse(any(ServiceResult.class)))
                .thenAnswer(invocation -> {
                    ServiceResult<?> result = invocation.getArgument(0);
                    if (result.isSuccess()) {
                        return ResponseEntity.ok(
                                new ApiResponse<>(true, "OK", result.getData(), null));
                    }
                    return ResponseEntity.status(404)
                            .body(new ApiResponse<>(false, result.getMessage(), null, null));
                });
    }

    // =========================================================================
    // StandardResponse structure
    // =========================================================================

    @Test
    void getLookupValues_standardResponseStructure() throws Exception {
        when(lookupConsumptionService.fetchLookupValues("ENTRY_SIDE"))
                .thenReturn(ServiceResult.success(sampleValues));

        mockMvc.perform(get("/api/lookups/ENTRY_SIDE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].code").value("DEBIT"))
                .andExpect(jsonPath("$.data[1].code").value("CREDIT"));
    }

    // =========================================================================
    // Happy path – returns lookup values
    // =========================================================================

    @Test
    void getLookupValues_existingCode_returnsValues() throws Exception {
        when(lookupConsumptionService.fetchLookupValues("ACCOUNT_TYPE"))
                .thenReturn(ServiceResult.success(sampleValues));

        mockMvc.perform(get("/api/lookups/ACCOUNT_TYPE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // =========================================================================
    // Not found – inactive or missing lookup code
    // =========================================================================

    @Test
    void getLookupValues_nonExistentCode_returns404() throws Exception {
        when(lookupConsumptionService.fetchLookupValues("NONEXISTENT"))
                .thenReturn(ServiceResult.notFound("Lookup code does not exist: NONEXISTENT"));
        when(operationCode.craftResponse(any(ServiceResult.class)))
                .thenReturn(ResponseEntity.status(404)
                        .body(new ApiResponse<>(false, "Lookup code does not exist: NONEXISTENT", null, null)));

        mockMvc.perform(get("/api/lookups/NONEXISTENT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getLookupValues_inactiveLookup_returns404() throws Exception {
        when(lookupConsumptionService.fetchLookupValues("INACTIVE_CODE"))
                .thenReturn(ServiceResult.notFound("Lookup code is inactive: INACTIVE_CODE"));
        when(operationCode.craftResponse(any(ServiceResult.class)))
                .thenReturn(ResponseEntity.status(404)
                        .body(new ApiResponse<>(false, "Lookup code is inactive: INACTIVE_CODE", null, null)));

        mockMvc.perform(get("/api/lookups/INACTIVE_CODE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // Case – controller delegates raw path variable to service
    // =========================================================================

    @Test
    void getLookupValues_lowercaseCode_isDelegatedToService() throws Exception {
        when(lookupConsumptionService.fetchLookupValues("entry_side"))
                .thenReturn(ServiceResult.success(sampleValues));

        mockMvc.perform(get("/api/lookups/entry_side"))
                .andExpect(status().isOk());
    }
}
