package com.example.masterdata.service;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.exception.LocalizedException;
import com.example.masterdata.dto.*;
import com.example.masterdata.entity.MdLookupDetail;
import com.example.masterdata.entity.MdMasterLookup;
import com.example.masterdata.mapper.LookupDetailMapper;
import com.example.masterdata.repository.LookupDetailRepository;
import com.example.masterdata.repository.MasterLookupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LookupDetailService
 * 
 * Architecture Rules:
 * - Rule 13.5: Unit tests are MANDATORY
 * - Tests cover happy path, edge cases, and error scenarios
 * 
 * @author ERP Team
 */
@ExtendWith(MockitoExtension.class)
class LookupDetailServiceTest {

    @Mock
    private LookupDetailRepository lookupDetailRepository;

    @Mock
    private MasterLookupRepository masterLookupRepository;

    @Mock
    private LookupDetailMapper lookupDetailMapper;

    @InjectMocks
    private LookupDetailService lookupDetailService;

    private static final String TEST_USERNAME = "testuser";

    private MdMasterLookup masterLookup;
    private MdLookupDetail testEntity;
    private LookupDetailCreateRequest createRequest;
    private LookupDetailResponse response;

    @BeforeEach
    void setUp() {
        // Setup master lookup
        masterLookup = MdMasterLookup.builder()
                .id(1L)
                .lookupKey("COLOR")
                .lookupName("اللون")
            .isActive(Boolean.TRUE)
                .build();

        // Setup test data
        testEntity = MdLookupDetail.builder()
                .id(1L)
                .masterLookup(masterLookup)
                .code("RED")
                .nameAr("أحمر")
                .nameEn("Red")
                .extraValue("#FF0000")
                .sortOrder(1)
            .isActive(Boolean.TRUE)
                .createdAt(Instant.now())
                .createdBy(TEST_USERNAME)
                .build();

        createRequest = LookupDetailCreateRequest.builder()
                .masterLookupId(1L)
                .code("RED")
                .nameAr("أحمر")
                .nameEn("Red")
                .extraValue("#FF0000")
                .sortOrder(1)
                .isActive(true)
                .build();

        response = LookupDetailResponse.builder()
                .id(1L)
                .masterLookupId(1L)
                .masterLookupKey("COLOR")
                .code("RED")
                .nameAr("أحمر")
                .nameEn("Red")
                .extraValue("#FF0000")
                .sortOrder(1)
                .isActive(true)
                .build();
    }

    @Test
    void create_ShouldSucceed_WhenValidInput() {
        // Given
        when(masterLookupRepository.findById(1L)).thenReturn(Optional.of(masterLookup));
        when(lookupDetailRepository.existsByMasterLookupIdAndCode(1L, "RED")).thenReturn(false);
        when(lookupDetailMapper.toEntity(any(), any())).thenReturn(testEntity);
        when(lookupDetailRepository.save(any())).thenReturn(testEntity);
        when(lookupDetailMapper.toResponse(any())).thenReturn(response);

        // When
        ServiceResult<LookupDetailResponse> result = lookupDetailService.create(createRequest);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("RED", result.getData().getCode());
        assertEquals("أحمر", result.getData().getNameAr());
        verify(lookupDetailRepository).save(any(MdLookupDetail.class));
    }

    @Test
    void create_ShouldThrowException_WhenMasterLookupNotFound() {
        // Given
        when(masterLookupRepository.findById(999L)).thenReturn(Optional.empty());

        LookupDetailCreateRequest invalidRequest = LookupDetailCreateRequest.builder()
                .masterLookupId(999L)
                .code("RED")
                .nameAr("أحمر")
                .build();

        // When & Then
        assertThrows(LocalizedException.class, () -> lookupDetailService.create(invalidRequest));
        verify(lookupDetailRepository, never()).save(any());
    }

    @Test
    void create_ShouldThrowException_WhenDuplicateCode() {
        // Given
        when(masterLookupRepository.findById(1L)).thenReturn(Optional.of(masterLookup));
        when(lookupDetailRepository.existsByMasterLookupIdAndCode(1L, "RED")).thenReturn(true);

        // When & Then
        assertThrows(LocalizedException.class, () -> lookupDetailService.create(createRequest));
        verify(lookupDetailRepository, never()).save(any());
    }

    @Test
    void getById_ShouldReturnEntity_WhenExists() {
        // Given
        when(lookupDetailRepository.findById(1L)).thenReturn(Optional.of(testEntity));
        when(lookupDetailMapper.toResponse(testEntity)).thenReturn(response);

        // When
        ServiceResult<LookupDetailResponse> result = lookupDetailService.getById(1L);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals("RED", result.getData().getCode());
    }

    @Test
    void getById_ShouldThrowNotFoundException_WhenNotExists() {
        // Given
        when(lookupDetailRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(LocalizedException.class, () -> lookupDetailService.getById(999L));
    }

    @Test
    void delete_ShouldFail_WhenReferencedByActivity() {
        // Given
        when(lookupDetailRepository.findById(1L)).thenReturn(Optional.of(testEntity));
        when(lookupDetailRepository.countActivityReferences(1L)).thenReturn(5L);

        // When & Then
        assertThrows(LocalizedException.class, () -> lookupDetailService.delete(1L));
        verify(lookupDetailRepository, never()).delete((MdLookupDetail) any());
    }

    @Test
    void getUsage_ShouldReturnUsageInfo() {
        // Given
        when(lookupDetailRepository.findById(1L)).thenReturn(Optional.of(testEntity));
        when(lookupDetailRepository.countActivityReferences(1L)).thenReturn(5L);

        LookupDetailUsageResponse usageResponse = LookupDetailUsageResponse.builder()
                .id(1L)
                .code("RED")
                .activityReferencesCount(5L)
                .totalReferencesCount(5L)
                .canBeDeleted(false)
                .build();

        when(lookupDetailMapper.toUsageResponse(testEntity, 5L)).thenReturn(usageResponse);

        // When
        ServiceResult<LookupDetailUsageResponse> result = lookupDetailService.getUsage(1L);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(5L, result.getData().getActivityReferencesCount());
        assertEquals(5L, result.getData().getTotalReferencesCount());
        assertFalse(result.getData().getCanBeDeleted());
    }
}
