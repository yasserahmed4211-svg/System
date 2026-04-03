package com.example.erp.finance.gl.service;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.exception.LocalizedException;
import com.example.erp.finance.gl.dto.AccPostingMstResponse;
import com.example.erp.finance.gl.entity.AccPostingMst;
import com.example.erp.finance.gl.mapper.AccPostingMapper;
import com.example.erp.finance.gl.repository.AccPostingMstRepository;
import com.erp.common.search.SearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AccPostingService.
 * Covers getById and search operations.
 */
@ExtendWith(MockitoExtension.class)
class AccPostingServiceTest {

    @Mock
    private AccPostingMstRepository postingMstRepository;

    @Mock
    private AccPostingMapper postingMapper;

    @InjectMocks
    private AccPostingService accPostingService;

    private AccPostingMst testEntity;
    private AccPostingMstResponse testResponse;

    @BeforeEach
    void setUp() {
        testEntity = AccPostingMst.builder()
                .postingId(1L)
                .companyIdFk(100L)
                .sourceModule("SALES")
                .sourceDocType("INVOICE")
                .sourceDocId(500L)
                .sourceDocNo("INV-2024-001")
                .docDate(LocalDate.of(2024, 7, 1))
                .totalAmount(new BigDecimal("10000.00"))
                .status("READY_FOR_GL")
                .currencyCode("SAR")
                .build();

        testResponse = AccPostingMstResponse.builder()
                .postingId(1L)
                .companyIdFk(100L)
                .sourceModule("SALES")
                .sourceDocType("INVOICE")
                .sourceDocId(500L)
                .sourceDocNo("INV-2024-001")
                .docDate(LocalDate.of(2024, 7, 1))
                .totalAmount(new BigDecimal("10000.00"))
                .status("READY_FOR_GL")
                .currencyCode("SAR")
                .detailCount(3)
                .build();
    }

    // ==================== getById ====================

    @Test
    void getById_ShouldReturnPosting_WhenExists() {
        // Given
        when(postingMstRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(testEntity));
        when(postingMapper.toResponse(testEntity)).thenReturn(testResponse);

        // When
        ServiceResult<AccPostingMstResponse> result = accPostingService.getById(1L);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getPostingId());
        assertEquals("SALES", result.getData().getSourceModule());
        assertEquals("INVOICE", result.getData().getSourceDocType());
        verify(postingMstRepository).findByIdWithDetails(1L);
        verify(postingMapper).toResponse(testEntity);
    }

    @Test
    void getById_ShouldThrowNotFoundException_WhenNotExists() {
        // Given
        when(postingMstRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

        // When & Then
        LocalizedException ex = assertThrows(LocalizedException.class,
                () -> accPostingService.getById(999L));
        assertEquals("GL_POSTING_NOT_FOUND", ex.getMessageKey());
        verify(postingMapper, never()).toResponse(any());
    }

    // ==================== search ====================

    @Test
    @SuppressWarnings("unchecked")
    void search_ShouldReturnPagedResults() {
        // Given
        Page<AccPostingMst> entityPage = new PageImpl<>(List.of(testEntity));
        when(postingMstRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(entityPage);
        when(postingMapper.toListResponse(testEntity)).thenReturn(testResponse);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setPage(0);
        searchRequest.setSize(20);

        // When
        ServiceResult<Page<AccPostingMstResponse>> result = accPostingService.search(searchRequest);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getTotalElements());
        assertEquals(1L, result.getData().getContent().get(0).getPostingId());
        verify(postingMstRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_ShouldReturnEmptyPage_WhenNoResults() {
        // Given
        Page<AccPostingMst> emptyPage = Page.empty();
        when(postingMstRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setPage(0);
        searchRequest.setSize(20);

        // When
        ServiceResult<Page<AccPostingMstResponse>> result = accPostingService.search(searchRequest);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(0, result.getData().getTotalElements());
        assertTrue(result.getData().getContent().isEmpty());
    }
}
