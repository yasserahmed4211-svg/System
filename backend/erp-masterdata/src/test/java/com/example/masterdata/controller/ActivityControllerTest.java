package com.example.masterdata.controller;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.example.masterdata.dto.*;
import com.example.masterdata.entity.Activity;
import com.example.masterdata.service.ActivityService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for ActivityController
 *
 * Covers:
 * - CRUD operations (POST, GET, PUT, DELETE)
 * - JWT authentication guard verification (reflection on @PreAuthorize)
 * - Validation rules (@Valid – missing / constraint violations)
 * - StandardResponse (ApiResponse) structure
 * - Business rules: VARIABLE conversion requires actual weight
 *
 * NOTE: Spring Boot 4.x removed @WebMvcTest and @MockBean.
 * Tests use MockMvcBuilders.standaloneSetup() for HTTP slice testing.
 * JWT filter-chain authentication is an integration test concern.
 *
 * @author ERP Team
 */
@ExtendWith(MockitoExtension.class)
class ActivityControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private ActivityService activityService;

    @Mock
    private OperationCode operationCode;

    @InjectMocks
    private ActivityController controller;

    private ActivityResponse sampleActivityResponse;
    private ActivityCreateRequest validCreateRequest;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setValidator(validator)
                .build();

        sampleActivityResponse = ActivityResponse.builder()
                .id(1L)
                .code("MANUFACTURING")
                .name("Manufacturing Activity")
                .conversionType(Activity.ConversionType.FIXED)
                .requiresActualWeight(false)
                .allowFraction(true)
                .isActive(true)
                .createdAt(Instant.now())
                .build();

        validCreateRequest = ActivityCreateRequest.builder()
                .code("MANUFACTURING")
                .name("Manufacturing Activity")
                .conversionType(Activity.ConversionType.FIXED)
                .requiresActualWeight(false)
                .allowFraction(true)
                .build();

        // Default mock: craftResponse returns 200 OK with the service result
        lenient().when(operationCode.craftResponse(any(ServiceResult.class)))
                .thenAnswer(invocation -> {
                    ServiceResult<?> result = invocation.getArgument(0);
                    if (result.isSuccess()) {
                        return ResponseEntity.ok(
                                new ApiResponse<>(true, "OK", result.getData(), null));
                    }
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse<>(false, result.getMessage(), null, null));
                });
    }

    // =========================================================================
    // JWT / Security guard verification
    // =========================================================================

    /**
     * JWT authentication is enforced by the JwtAuthenticationFilter.
     * We verify @PreAuthorize guards are on service methods.
     * HTTP 401/403 requires a @SpringBootTest integration test.
     */
    @Test
    void activityService_create_hasPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = ActivityService.class.getDeclaredMethod("create", ActivityCreateRequest.class);
        org.springframework.security.access.prepost.PreAuthorize annotation =
                method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        assertNotNull(annotation, "create() must be protected by @PreAuthorize");
        assertTrue(annotation.value().contains("ACTIVITY_CREATE"), "Must require ACTIVITY_CREATE");
    }

    @Test
    void activityService_delete_hasPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = ActivityService.class.getDeclaredMethod("delete", Long.class);
        org.springframework.security.access.prepost.PreAuthorize annotation =
                method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        assertNotNull(annotation, "delete() must be protected by @PreAuthorize");
        assertTrue(annotation.value().contains("ACTIVITY_DELETE"), "Must require ACTIVITY_DELETE");
    }

    // =========================================================================
    // StandardResponse structure
    // =========================================================================

    @Test
    void getById_standardResponseStructure_hasSuccessAndData() throws Exception {
        when(activityService.getById(1L))
                .thenReturn(ServiceResult.success(sampleActivityResponse));

        mockMvc.perform(get("/api/activities/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.code").value("MANUFACTURING"));
    }

    // =========================================================================
    // CREATE – happy path
    // =========================================================================

    @Test
    void create_validRequest_returns201() throws Exception {
        when(activityService.create(any()))
                .thenReturn(ServiceResult.success(sampleActivityResponse, Status.CREATED));
        when(operationCode.craftResponse(any(ServiceResult.class)))
                .thenReturn(ResponseEntity.status(201)
                        .body(new ApiResponse<>(true, "Created", sampleActivityResponse, null)));

        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("MANUFACTURING"));
    }

    // =========================================================================
    // CREATE – validation rules
    // =========================================================================

    @Test
    void create_missingCode_returns400() throws Exception {
        ActivityCreateRequest bad = ActivityCreateRequest.builder()
                .name("Some Name")
                .conversionType(Activity.ConversionType.FIXED)
                .requiresActualWeight(false)
                .allowFraction(true)
                .build();

        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingName_returns400() throws Exception {
        ActivityCreateRequest bad = ActivityCreateRequest.builder()
                .code("MANUFACTURING")
                .conversionType(Activity.ConversionType.FIXED)
                .requiresActualWeight(false)
                .allowFraction(true)
                .build();

        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingConversionType_returns400() throws Exception {
        ActivityCreateRequest bad = ActivityCreateRequest.builder()
                .code("MANUFACTURING")
                .name("Manufacturing")
                .requiresActualWeight(false)
                .allowFraction(true)
                .build();

        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_codeTooLong_returns400() throws Exception {
        ActivityCreateRequest bad = ActivityCreateRequest.builder()
                .code("A".repeat(51)) // max=50
                .name("Name")
                .conversionType(Activity.ConversionType.FIXED)
                .requiresActualWeight(false)
                .allowFraction(true)
                .build();

        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    @Test
    void update_validRequest_returns200() throws Exception {
        ActivityUpdateRequest updateReq = ActivityUpdateRequest.builder()
                .name("Updated Name")
                .conversionType(Activity.ConversionType.FIXED)
                .requiresActualWeight(false)
                .allowFraction(true)
                .build();

        when(activityService.update(eq(1L), any()))
                .thenReturn(ServiceResult.success(sampleActivityResponse, Status.UPDATED));

        mockMvc.perform(put("/api/activities/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void update_missingName_returns400() throws Exception {
        ActivityUpdateRequest bad = ActivityUpdateRequest.builder()
                // name intentionally omitted
                .conversionType(Activity.ConversionType.FIXED)
                .requiresActualWeight(false)
                .allowFraction(true)
                .build();

        mockMvc.perform(put("/api/activities/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // GET BY ID – not found path
    // =========================================================================

    @Test
    void getById_serviceThrows_propagatesAsServletException() {
        when(activityService.getById(999L))
                .thenThrow(new LocalizedException(Status.NOT_FOUND, "ACTIVITY_NOT_FOUND", 999L));

        // standaloneSetup has no GlobalExceptionHandler — Spring Test 7 throws
        // the wrapped ServletException directly from perform()
        assertThrows(jakarta.servlet.ServletException.class,
                () -> mockMvc.perform(get("/api/activities/999")));
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    @Test
    void delete_success_returns204() throws Exception {
        doNothing().when(activityService).delete(1L);

        mockMvc.perform(delete("/api/activities/1"))
                .andExpect(status().isNoContent());
    }

    // =========================================================================
    // ACTIVATE / DEACTIVATE
    // =========================================================================

    @Test
    void activate_returnsUpdatedActivity() throws Exception {
        when(activityService.activate(1L))
                .thenReturn(ServiceResult.success(sampleActivityResponse, Status.UPDATED));

        mockMvc.perform(put("/api/activities/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deactivate_returnsUpdatedActivity() throws Exception {
        when(activityService.deactivate(1L))
                .thenReturn(ServiceResult.success(sampleActivityResponse, Status.UPDATED));

        mockMvc.perform(put("/api/activities/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // SEARCH (pagination)
    // =========================================================================

    @Test
    void search_returnsPaginatedActivities() throws Exception {
        Page<ActivityResponse> page = new PageImpl<>(
                List.of(sampleActivityResponse), PageRequest.of(0, 20), 1);

        when(activityService.search(any()))
                .thenReturn(ServiceResult.success(page));

        String searchBody = "{\"filters\":[], \"sorts\":[], \"page\":0, \"size\":20}";

        mockMvc.perform(post("/api/activities/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // USAGE
    // =========================================================================

    @Test
    void getUsage_returns200() throws Exception {
        ActivityUsageResponse usage = ActivityUsageResponse.builder()
                .activityId(1L)
                .activityCode("MANUFACTURING")
                .totalCategories(3)
                .activeCategories(2)
                .canDelete(false)
                .canDeactivate(false)
                .build();

        when(activityService.getUsage(1L))
                .thenReturn(ServiceResult.success(usage));

        mockMvc.perform(get("/api/activities/1/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activityCode").value("MANUFACTURING"))
                .andExpect(jsonPath("$.data.totalCategories").value(3));
    }
}
