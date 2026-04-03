package com.example.masterdata.service;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.exception.LocalizedException;
import com.example.erp.common.exception.LocalizedException;
import com.example.masterdata.dto.ActivityCreateRequest;
import com.example.masterdata.dto.ActivityResponse;
import com.example.masterdata.dto.ActivityUpdateRequest;
import com.example.masterdata.dto.ActivityUsageResponse;
import com.example.masterdata.entity.Activity;
import com.example.masterdata.mapper.ActivityMapper;
import com.example.masterdata.repository.ActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Activity Service Tests
 * 
 * Tests service layer business logic with mocked repository
 * 
 * Testing Standards:
 * - Use JUnit 5
 * - Use @ExtendWith(MockitoExtension.class)
 * - Mock dependencies
 * - Test success cases and failure cases
 * 
 * @author ERP Team
 */
@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityMapper activityMapper;

    @InjectMocks
    private ActivityService activityService;

    private Activity testActivity;
    private ActivityCreateRequest createRequest;
    private ActivityUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {

        // Setup test activity entity
        testActivity = Activity.builder()
            .id(1L)
            .code("TEST_ACT")
            .name("Test Activity")
            .description("Test Description")
            .conversionType(Activity.ConversionType.FIXED)
            .requiresActualWeight(false)
            .allowFraction(true)
            .isActive(Boolean.TRUE)
            .build();

        // Setup create request
        createRequest = ActivityCreateRequest.builder()
            .code("NEW_ACT")
            .name("New Activity")
            .description("New Description")
            .conversionType(Activity.ConversionType.FIXED)
            .requiresActualWeight(false)
            .allowFraction(true)
            .build();

        // Setup update request
        updateRequest = ActivityUpdateRequest.builder()
            .name("Updated Name")
            .description("Updated Description")
            .conversionType(Activity.ConversionType.FIXED)
            .requiresActualWeight(false)
            .allowFraction(false)
            .build();

        lenient().when(activityMapper.toEntity(any(ActivityCreateRequest.class))).thenReturn(testActivity);
        lenient().doNothing().when(activityMapper).updateEntity(any(Activity.class), any(ActivityUpdateRequest.class));
        lenient().when(activityMapper.toResponse(any(Activity.class))).thenAnswer(invocation -> {
            Activity activity = invocation.getArgument(0, Activity.class);
            return ActivityResponse.builder()
                .id(activity != null ? activity.getId() : null)
                .code(activity != null ? activity.getCode() : null)
                .build();
        });
    }

    @Test
    void createActivity_success() {
        // Given
        when(activityRepository.existsByCode(anyString())).thenReturn(false);
        when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);

        // When
        ServiceResult<ActivityResponse> result = activityService.create(createRequest);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("TEST_ACT", result.getData().getCode());
        verify(activityRepository).existsByCode(anyString());
        verify(activityRepository).save(any(Activity.class));
    }

    @Test
    void createActivity_duplicateCode_throwsException() {
        // Given
        when(activityRepository.existsByCode(anyString())).thenReturn(true);

        // When & Then
        assertThrows(LocalizedException.class, () -> {
            activityService.create(createRequest);
        });
        verify(activityRepository, never()).save(any());
    }

    @Test
    void createActivity_variableConversion_requiresWeight() {
        // Given
        createRequest.setConversionType(Activity.ConversionType.VARIABLE);
        createRequest.setRequiresActualWeight(false); // Invalid combination

        // When & Then
        assertThrows(LocalizedException.class, () -> {
            activityService.create(createRequest);
        });
    }

    @Test
    void createActivity_variableConversion_withWeight_success() {
        // Given
        createRequest.setConversionType(Activity.ConversionType.VARIABLE);
        createRequest.setRequiresActualWeight(true); // Valid combination
        when(activityRepository.existsByCode(anyString())).thenReturn(false);
        when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);

        // When
        ServiceResult<ActivityResponse> result = activityService.create(createRequest);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        verify(activityRepository).save(any(Activity.class));
    }

    @Test
    void updateActivity_success() {
        // Given
        when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
        when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);

        // When
        ServiceResult<ActivityResponse> result = activityService.update(1L, updateRequest);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        verify(activityRepository).findById(1L);
        verify(activityRepository).save(any(Activity.class));
    }

    @Test
    void updateActivity_notFound_throwsException() {
        // Given
        when(activityRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(LocalizedException.class, () -> {
            activityService.update(999L, updateRequest);
        });
    }

    @Test
    void updateActivity_changeConversionType_variableRequiresWeight() {
        // Given
        updateRequest.setConversionType(Activity.ConversionType.VARIABLE);
        updateRequest.setRequiresActualWeight(false); // Invalid

        // When & Then
        assertThrows(LocalizedException.class, () -> {
            activityService.update(1L, updateRequest);
        });
    }

    @Test
    void getById_found() {
        // Given
        when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));

        // When
        ServiceResult<ActivityResponse> result = activityService.getById(1L);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("TEST_ACT", result.getData().getCode());
    }

    @Test
    void getById_notFound_throwsException() {
        // Given
        when(activityRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(LocalizedException.class, () -> {
            activityService.getById(999L);
        });
    }

    @Test
    void activate_success() {
        // Given
        testActivity.setIsActive(Boolean.FALSE);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
        when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);

        // When
        activityService.activate(1L);

        // Then
        verify(activityRepository).findById(1L);
        verify(activityRepository).save(argThat(activity -> Boolean.TRUE.equals(activity.getIsActive())));
    }

    @Test
    void deactivate_withActiveCategories_shouldFail() {
        // Given
        when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
        when(activityRepository.countActiveCategoriesByActivityId(1L)).thenReturn(5L);

        // When & Then
        assertThrows(LocalizedException.class, () -> {
            activityService.deactivate(1L);
        });
        verify(activityRepository, never()).save(any());
    }

    @Test
    void deactivate_noActiveCategories_success() {
        // Given
        when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
        when(activityRepository.countActiveCategoriesByActivityId(1L)).thenReturn(0L);
        when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);

        // When
        activityService.deactivate(1L);

        // Then
        verify(activityRepository).save(argThat(activity -> Boolean.FALSE.equals(activity.getIsActive())));
    }

    @Test
    void deleteActivity_withCategories_shouldFail() {
        // Given – service pre-checks category count; if > 0 it throws LocalizedException
        when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
        when(activityRepository.countCategoriesByActivityId(1L)).thenReturn(3L);

        // When & Then
        assertThrows(LocalizedException.class, () -> {
            activityService.delete(1L);
        });
        verify(activityRepository, never()).delete(any(Activity.class));
    }

    @Test
    void deleteActivity_withoutReferences_shouldSucceed() {
        // Given
        when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
        doNothing().when(activityRepository).delete(any(Activity.class));

        // When
        activityService.delete(1L);

        // Then
        verify(activityRepository).delete(testActivity);
    }

    @Test
    void getUsage_shouldReturnCategoryCount() {
        // Given
        when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
        when(activityRepository.countCategoriesByActivityId(1L)).thenReturn(10L);
        when(activityRepository.countActiveCategoriesByActivityId(1L)).thenReturn(7L);

        // When
        ServiceResult<ActivityUsageResponse> result = activityService.getUsage(1L);

        // Then
        assertTrue(result.isSuccess());
        ActivityUsageResponse usage = result.getData();
        assertNotNull(usage);
        assertEquals(1L, usage.getActivityId());
        assertEquals(10L, usage.getTotalCategories());
        assertEquals(7L, usage.getActiveCategories());
        assertFalse(usage.isCanDelete());
        assertFalse(usage.isCanDeactivate());
    }

    @Test
    void getUsage_noCategories_canDeleteAndDeactivate() {
        // Given
        when(activityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
        when(activityRepository.countCategoriesByActivityId(1L)).thenReturn(0L);
        when(activityRepository.countActiveCategoriesByActivityId(1L)).thenReturn(0L);

        // When
        ServiceResult<ActivityUsageResponse> result = activityService.getUsage(1L);

        // Then
        assertTrue(result.isSuccess());
        assertTrue(result.getData().isCanDelete());
        assertTrue(result.getData().isCanDeactivate());
    }

    @Test
    void search_withFilters_success() {
        // Given - Rule 10.7 MANDATORY search endpoint test
        com.erp.common.search.SearchRequest searchRequest = 
            new com.erp.common.search.SearchRequest();
        searchRequest.setPage(0);
        searchRequest.setSize(20);
        
        org.springframework.data.domain.Page<Activity> mockPage = 
            new org.springframework.data.domain.PageImpl<>(
                java.util.List.of(testActivity),
                org.springframework.data.domain.PageRequest.of(0, 20),
                1
            );
        
        when(activityRepository.findAll(
            org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<com.example.masterdata.entity.Activity>>any(),
            any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(mockPage);

        // When
        ServiceResult<org.springframework.data.domain.Page<com.example.masterdata.dto.ActivityResponse>> result = 
            activityService.search(searchRequest);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getTotalElements());
        assertEquals("TEST_ACT", result.getData().getContent().get(0).getCode());
        verify(activityRepository).findAll(
            org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<com.example.masterdata.entity.Activity>>any(),
            any(org.springframework.data.domain.Pageable.class)
        );
    }
}
