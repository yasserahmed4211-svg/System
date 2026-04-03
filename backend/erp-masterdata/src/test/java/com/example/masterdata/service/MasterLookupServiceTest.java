package com.example.masterdata.service;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.exception.LocalizedException;
import com.example.masterdata.dto.*;
import com.example.masterdata.entity.MdMasterLookup;
import com.example.masterdata.mapper.MasterLookupMapper;
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
 * Unit tests for MasterLookupService
 * 
 * Architecture Rules:
 * - Rule 13.5: Unit tests are MANDATORY
 * - Tests cover happy path, edge cases, and error scenarios
 * 
 * @author ERP Team
 */
@ExtendWith(MockitoExtension.class)
class MasterLookupServiceTest {

    @Mock
    private MasterLookupRepository masterLookupRepository;

    @Mock
    private MasterLookupMapper masterLookupMapper;

    @InjectMocks
    private MasterLookupService masterLookupService;

    private static final String TEST_USERNAME = "testuser";

    private MdMasterLookup testEntity;
    private MasterLookupCreateRequest createRequest;
    private MasterLookupResponse response;

    @BeforeEach
    void setUp() {
        // Setup test data
        testEntity = MdMasterLookup.builder()
                .id(1L)
                .lookupKey("COLOR")
                .lookupName("اللون")
                .lookupNameEn("Color")
                .description("Product color options")
            .isActive(Boolean.TRUE)
                .createdAt(Instant.now())
                .createdBy(TEST_USERNAME)
                .build();

        createRequest = MasterLookupCreateRequest.builder()
                .lookupKey("COLOR")
                .lookupName("اللون")
                .lookupNameEn("Color")
                .description("Product color options")
                .isActive(true)
                .build();

        response = MasterLookupResponse.builder()
                .id(1L)
                .lookupKey("COLOR")
                .lookupName("اللون")
                .lookupNameEn("Color")
                .description("Product color options")
                .isActive(true)
                .build();
    }

    @Test
    void create_ShouldSucceed_WhenValidInput() {
        // Given
        when(masterLookupRepository.existsByLookupKey(anyString())).thenReturn(false);
        when(masterLookupMapper.toEntity(any())).thenReturn(testEntity);
        when(masterLookupRepository.save(any())).thenReturn(testEntity);
        when(masterLookupMapper.toResponse(any())).thenReturn(response);

        // When
        ServiceResult<MasterLookupResponse> result = masterLookupService.create(createRequest);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("COLOR", result.getData().getLookupKey());
        assertEquals("اللون", result.getData().getLookupName());
        verify(masterLookupRepository).save(any(MdMasterLookup.class));
    }

    @Test
    void create_ShouldThrowException_WhenDuplicateKey() {
        // Given
        when(masterLookupRepository.existsByLookupKey("COLOR")).thenReturn(true);

        // When & Then
        assertThrows(LocalizedException.class, () -> masterLookupService.create(createRequest));
        verify(masterLookupRepository, never()).save(any());
    }

    @Test
    void getById_ShouldReturnEntity_WhenExists() {
        // Given
        when(masterLookupRepository.findById(1L)).thenReturn(Optional.of(testEntity));
        when(masterLookupMapper.toResponse(testEntity)).thenReturn(response);

        // When
        ServiceResult<MasterLookupResponse> result = masterLookupService.getById(1L);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals("COLOR", result.getData().getLookupKey());
    }

    @Test
    void getById_ShouldThrowNotFoundException_WhenNotExists() {
        // Given
        when(masterLookupRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(LocalizedException.class, () -> masterLookupService.getById(999L));
    }

    @Test
    void deactivate_ShouldFail_WhenHasActiveDetails() {
        // Given
        when(masterLookupRepository.findById(1L)).thenReturn(Optional.of(testEntity));
        when(masterLookupRepository.countActiveLookupDetails(1L)).thenReturn(5L);

        // When & Then
        assertThrows(LocalizedException.class, () -> masterLookupService.toggleActive(1L, false));
        verify(masterLookupRepository, never()).save(any());
    }

    @Test
    void delete_ShouldFail_WhenHasDetails() {
        // Given
        when(masterLookupRepository.findById(1L)).thenReturn(Optional.of(testEntity));
        when(masterLookupRepository.countLookupDetails(1L)).thenReturn(10L);

        // When & Then
        assertThrows(LocalizedException.class, () -> masterLookupService.delete(1L));
        verify(masterLookupRepository, never()).delete((MdMasterLookup) any());
    }

    @Test
    void getUsage_ShouldReturnUsageInfo() {
        // Given
        when(masterLookupRepository.findById(1L)).thenReturn(Optional.of(testEntity));
        when(masterLookupRepository.countLookupDetails(1L)).thenReturn(10L);
        when(masterLookupRepository.countActiveLookupDetails(1L)).thenReturn(8L);

        MasterLookupUsageResponse usageResponse = MasterLookupUsageResponse.builder()
                .masterLookupId(1L)
                .lookupKey("COLOR")
                .totalDetails(10L)
                .activeDetails(8L)
                .canDelete(false)
                .canDeactivate(false)
                .build();

        when(masterLookupMapper.toUsageResponse(testEntity, 10L, 8L)).thenReturn(usageResponse);

        // When
        ServiceResult<MasterLookupUsageResponse> result = masterLookupService.getUsage(1L);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(10L, result.getData().getTotalDetails());
        assertEquals(8L, result.getData().getActiveDetails());
        assertFalse(result.getData().getCanDelete());
    }
}
