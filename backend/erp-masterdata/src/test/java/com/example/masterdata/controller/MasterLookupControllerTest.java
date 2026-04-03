package com.example.masterdata.controller;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.example.masterdata.dto.*;
import com.example.masterdata.service.LookupDetailService;
import com.example.masterdata.service.MasterLookupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for MasterLookupController
 *
 * Covers:
 * - CRUD operations (POST, GET, PUT, DELETE)
 * - JWT authentication guard verification (@PreAuthorize on service methods)
 * - Validation rules (@Valid – missing / over-length fields)
 * - StandardResponse (ApiResponse) structure
 *
 * NOTE: Spring Boot 4.x removed @WebMvcTest and @MockBean.
 * Tests use MockMvcBuilders.standaloneSetup() for HTTP slice testing.
 * JWT filter-chain authentication is an integration test concern.
 *
 * @author ERP Team
 */
@ExtendWith(MockitoExtension.class)
class MasterLookupControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private MasterLookupService masterLookupService;

    @Mock
    private LookupDetailService lookupDetailService;

    @Mock
    private OperationCode operationCode;

    @InjectMocks
    private MasterLookupController controller;

    private MasterLookupResponse sampleResponse;
    private MasterLookupCreateRequest validCreateRequest;

    @BeforeEach
    void setUp() {
        // Build MockMvc with standaloneSetup so no Spring context or security filters
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setValidator(validator)
                .build();

        sampleResponse = MasterLookupResponse.builder()
                .id(1L)
                .lookupKey("COLOR")
                .lookupName("اللون")
                .lookupNameEn("Color")
                .isActive(true)
                .createdAt(Instant.now())
                .build();

        validCreateRequest = MasterLookupCreateRequest.builder()
                .lookupKey("COLOR")
                .lookupName("اللون")
                .lookupNameEn("Color")
                .isActive(true)
                .build();

        // Default: delegate craftResponse to a minimal implementation
        lenient().when(operationCode.craftResponse(any(ServiceResult.class)))
                .thenAnswer(invocation -> {
                    ServiceResult<?> result = invocation.getArgument(0);
                    if (result.isSuccess()) {
                        return org.springframework.http.ResponseEntity.ok(
                                new ApiResponse<>(true, "OK", result.getData(), null));
                    }
                    return org.springframework.http.ResponseEntity.badRequest()
                            .body(new ApiResponse<>(false, result.getMessage(), null, null));
                });
    }

    // =========================================================================
    // StandardResponse structure tests
    // =========================================================================

    /**
     * Verify all successful responses follow the ApiResponse envelope:
     * { success: true, message: ..., data: {...}, timestamp: ... }
     */
    @Test
    void getById_standardResponseStructure() throws Exception {
        when(masterLookupService.getById(1L))
                .thenReturn(ServiceResult.success(sampleResponse));

        mockMvc.perform(get("/api/masterdata/master-lookups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.lookupKey").value("COLOR"));
    }

    // =========================================================================
    // JWT / Security guard verification
    // =========================================================================

    /**
     * JWT authentication is enforced by the JwtAuthenticationFilter (Spring Security filter chain).
     * In unit tests (standaloneSetup), the filter chain is not loaded.
     * We verify the authorization annotations exist on the service layer methods.
     *
     * Integration: A full @SpringBootTest would be required to verify HTTP 401/403 responses.
     */
    @Test
    void masterLookupService_createMethod_hasPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method createMethod = MasterLookupService.class.getDeclaredMethod("create", MasterLookupCreateRequest.class);
        org.springframework.security.access.prepost.PreAuthorize annotation =
                createMethod.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        assertNotNull(annotation, "create() must be protected by @PreAuthorize");
        assertTrue(annotation.value().contains("MASTER_LOOKUP_CREATE"),
                "create() permission must contain MASTER_LOOKUP_CREATE");
    }

    @Test
    void masterLookupService_updateMethod_hasPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method updateMethod = MasterLookupService.class.getDeclaredMethod(
                "update", Long.class, MasterLookupUpdateRequest.class);
        org.springframework.security.access.prepost.PreAuthorize annotation =
                updateMethod.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        assertNotNull(annotation, "update() must be protected by @PreAuthorize");
        assertTrue(annotation.value().contains("MASTER_LOOKUP_UPDATE"),
                "update() permission must contain MASTER_LOOKUP_UPDATE");
    }

    // =========================================================================
    // CREATE – happy path
    // =========================================================================

    @Test
    void create_validRequest_returns201() throws Exception {
        when(masterLookupService.create(any()))
                .thenReturn(ServiceResult.success(sampleResponse, Status.CREATED));
        when(operationCode.craftResponse(any(ServiceResult.class)))
                .thenReturn(ResponseEntity.status(201)
                        .body(new ApiResponse<>(true, "Created", sampleResponse, null)));

        mockMvc.perform(post("/api/masterdata/master-lookups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lookupKey").value("COLOR"));
    }

    // =========================================================================
    // CREATE – validation rules
    // =========================================================================

    /**
     * lookupKey is @NotBlank – missing value must return HTTP 400.
     */
    @Test
    void create_missingLookupKey_returns400() throws Exception {
        MasterLookupCreateRequest badRequest = MasterLookupCreateRequest.builder()
                .lookupName("اللون")
                // lookupKey intentionally omitted
                .build();

        mockMvc.perform(post("/api/masterdata/master-lookups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * lookupKey @Size(max=50) – exceeding the limit must return HTTP 400.
     */
    @Test
    void create_lookupKeyTooLong_returns400() throws Exception {
        MasterLookupCreateRequest badRequest = MasterLookupCreateRequest.builder()
                .lookupKey("A".repeat(51))   // exceeds max=50
                .lookupName("اللون")
                .build();

        mockMvc.perform(post("/api/masterdata/master-lookups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * lookupName is @NotBlank – blank value must return HTTP 400.
     */
    @Test
    void create_blankLookupName_returns400() throws Exception {
        MasterLookupCreateRequest badRequest = MasterLookupCreateRequest.builder()
                .lookupKey("COLOR")
                .lookupName("")   // blank
                .build();

        mockMvc.perform(post("/api/masterdata/master-lookups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    @Test
    void update_validRequest_returns200() throws Exception {
        MasterLookupUpdateRequest updateRequest = MasterLookupUpdateRequest.builder()
                .lookupName("اللون المحدّث")
                .lookupNameEn("Updated Color")
                .build();

        when(masterLookupService.update(eq(1L), any()))
                .thenReturn(ServiceResult.success(sampleResponse, Status.UPDATED));

        mockMvc.perform(put("/api/masterdata/master-lookups/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void update_missingLookupName_returns400() throws Exception {
        MasterLookupUpdateRequest badRequest = MasterLookupUpdateRequest.builder()
                // lookupName omitted – @NotBlank violation
                .build();

        mockMvc.perform(put("/api/masterdata/master-lookups/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // GET BY ID
    // =========================================================================

    @Test
    void getById_notFound_returnsClientError() {
        when(masterLookupService.getById(999L))
                .thenThrow(new LocalizedException(Status.NOT_FOUND, "MASTER_LOOKUP_NOT_FOUND", 999L));

        // standaloneSetup has no GlobalExceptionHandler — Spring Test 7 throws
        // the wrapped ServletException directly from perform()
        assertThrows(jakarta.servlet.ServletException.class,
                () -> mockMvc.perform(get("/api/masterdata/master-lookups/999")));
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    @Test
    void delete_existingRecord_returns204() throws Exception {
        doNothing().when(masterLookupService).delete(1L);

        mockMvc.perform(delete("/api/masterdata/master-lookups/1"))
                .andExpect(status().isNoContent());
    }

    // =========================================================================
    // TOGGLE ACTIVE
    // =========================================================================

    @Test
    void toggleActive_validRequest_returns200() throws Exception {
        ToggleActiveRequest toggleRequest = ToggleActiveRequest.builder()
                .active(false)
                .build();

        when(masterLookupService.toggleActive(eq(1L), eq(false)))
                .thenReturn(ServiceResult.success(sampleResponse, Status.UPDATED));

        mockMvc.perform(put("/api/masterdata/master-lookups/1/toggle-active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toggleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void toggleActive_missingActive_returns400() throws Exception {
        // active field is @NotNull – null value should fail validation
        String jsonBody = "{\"active\": null}";

        mockMvc.perform(put("/api/masterdata/master-lookups/1/toggle-active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // SEARCH (pagination)
    // =========================================================================

    @Test
    void search_validRequest_returnsPaginatedResult() throws Exception {
        Page<MasterLookupResponse> page = new PageImpl<>(
                List.of(sampleResponse), PageRequest.of(0, 20), 1);

        when(masterLookupService.search(any()))
                .thenReturn(ServiceResult.success(page));

        String searchBody = "{\"filters\":[], \"sorts\":[], \"page\":0, \"size\":20}";

        mockMvc.perform(post("/api/masterdata/master-lookups/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // GET USAGE
    // =========================================================================

    @Test
    void getUsage_returns200WithUsageInfo() throws Exception {
        MasterLookupUsageResponse usage = MasterLookupUsageResponse.builder()
                .masterLookupId(1L)
                .lookupKey("COLOR")
                .totalDetails(5L)
                .activeDetails(3L)
                .canDelete(false)
                .canDeactivate(false)
                .build();

        when(masterLookupService.getUsage(1L))
                .thenReturn(ServiceResult.success(usage));

        mockMvc.perform(get("/api/masterdata/master-lookups/1/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lookupKey").value("COLOR"))
                .andExpect(jsonPath("$.data.totalDetails").value(5));
    }
}
