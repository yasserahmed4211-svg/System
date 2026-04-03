package com.example.erp.finance.gl.service;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.example.erp.finance.gl.dto.GlJournalHdrResponse;
import com.example.erp.finance.gl.dto.GlJournalLineResponse;
import com.example.erp.finance.gl.dto.JournalPreviewResponse;
import com.example.erp.finance.gl.dto.PostingResponse;
import com.example.erp.finance.gl.entity.AccPostingDtl;
import com.example.erp.finance.gl.entity.AccPostingMst;
import com.example.erp.finance.gl.entity.GlJournalHdr;
import com.example.erp.finance.gl.repository.AccPostingMstRepository;
import com.example.erp.finance.gl.repository.GlJournalHdrRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JournalGenerationService.
 * Tests the bridge between ACC_POSTING_MST and GL_JOURNAL_HDR.
 */
@ExtendWith(MockitoExtension.class)
class JournalGenerationServiceTest {

    @Mock
    private AccPostingMstRepository postingMstRepository;

    @Mock
    private GlJournalHdrRepository journalHdrRepository;

    @Mock
    private PostingEngineService postingEngineService;

    @InjectMocks
    private JournalGenerationService journalGenerationService;

    private AccPostingMst readyPosting;
    private AccPostingDtl testDetail;
    private PostingResponse engineResponse;

    @BeforeEach
    void setUp() {
        testDetail = AccPostingDtl.builder()
                .postingDtlId(10L)
                .lineNo(1)
                .amount(new BigDecimal("10000.00"))
                .businessSide("REVENUE")
                .sign(1)
                .description("Sales revenue")
                .customerIdFk(200L)
                .build();

        readyPosting = AccPostingMst.builder()
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
                .details(new ArrayList<>(List.of(testDetail)))
                .build();

        GlJournalHdrResponse journalResponse = GlJournalHdrResponse.builder()
                .id(50L)
                .journalNo("JRN-000050")
                .journalDate(LocalDate.of(2024, 7, 1))
                .journalTypeIdFk("AUTOMATIC")
                .statusIdFk("DRAFT")
                .build();

        engineResponse = PostingResponse.builder()
                .ruleId(10L)
                .companyIdFk(100L)
                .sourceModule("SALES")
                .sourceDocType("INVOICE")
                .journal(journalResponse)
                .build();
    }

    // ==================== Successful Generation ====================

    @Test
    void createJournalFromPosting_ShouldSucceed_WhenPostingIsReady() {
        // Given
        when(postingMstRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(readyPosting));
        when(postingEngineService.executeForPosting(any(), eq(1L)))
                .thenReturn(ServiceResult.success(engineResponse, Status.CREATED));
        when(postingMstRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        ServiceResult<PostingResponse> result = journalGenerationService.createJournalFromPosting(1L);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(50L, result.getData().getJournal().getId());
        assertEquals("JRN-000050", result.getData().getJournal().getJournalNo());

        // Verify posting was updated
        verify(postingMstRepository).save(argThat(posting -> {
            assertEquals(50L, posting.getFinJournalIdFk());
            assertEquals("JOURNAL_CREATED", posting.getStatus());
            assertNull(posting.getErrorMessage());
            return true;
        }));
    }

    // ==================== Posting Not Found ====================

    @Test
    void createJournalFromPosting_ShouldThrowNotFound_WhenPostingDoesNotExist() {
        // Given
        when(postingMstRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

        // When & Then
        LocalizedException ex = assertThrows(LocalizedException.class,
                () -> journalGenerationService.createJournalFromPosting(999L));
        assertEquals("GL_POSTING_NOT_FOUND", ex.getMessageKey());
        verify(postingEngineService, never()).executeForPosting(any(), any());
    }

    // ==================== Posting Not Ready ====================

    @Test
    void createJournalFromPosting_ShouldReject_WhenStatusIsNotReady() {
        // Given
        readyPosting.setStatus("DRAFT");
        when(postingMstRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(readyPosting));

        // When & Then
        LocalizedException ex = assertThrows(LocalizedException.class,
                () -> journalGenerationService.createJournalFromPosting(1L));
        assertEquals("GL_POSTING_NOT_READY", ex.getMessageKey());
        verify(postingEngineService, never()).executeForPosting(any(), any());
        verify(postingMstRepository, never()).save(any());
    }

    // ==================== Already Generated (Idempotency) ====================

    @Test
    void createJournalFromPosting_ShouldReject_WhenJournalAlreadyLinked() {
        // Given
        readyPosting.setFinJournalIdFk(99L); // already has journal
        when(postingMstRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(readyPosting));

        // When & Then
        LocalizedException ex = assertThrows(LocalizedException.class,
                () -> journalGenerationService.createJournalFromPosting(1L));
        assertEquals("GL_POSTING_JOURNAL_ALREADY_EXISTS", ex.getMessageKey());
        verify(postingEngineService, never()).executeForPosting(any(), any());
    }

    // ==================== No Details ====================

    @Test
    void createJournalFromPosting_ShouldReject_WhenPostingHasNoDetails() {
        // Given
        readyPosting.setDetails(new ArrayList<>());
        when(postingMstRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(readyPosting));

        // When & Then
        LocalizedException ex = assertThrows(LocalizedException.class,
                () -> journalGenerationService.createJournalFromPosting(1L));
        assertEquals("GL_POSTING_NO_DETAILS_FOR_JOURNAL", ex.getMessageKey());
        verify(postingEngineService, never()).executeForPosting(any(), any());
    }

    @Test
    void createJournalFromPosting_ShouldReject_WhenDetailsAreNull() {
        // Given
        readyPosting.setDetails(null);
        when(postingMstRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(readyPosting));

        // When & Then
        LocalizedException ex = assertThrows(LocalizedException.class,
                () -> journalGenerationService.createJournalFromPosting(1L));
        assertEquals("GL_POSTING_NO_DETAILS_FOR_JOURNAL", ex.getMessageKey());
    }

    // ==================== Engine Failure → ERROR Status ====================

    @Test
    void createJournalFromPosting_ShouldSetErrorStatus_WhenEngineThrows() {
        // Given
        when(postingMstRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(readyPosting));
        when(postingEngineService.executeForPosting(any(), eq(1L)))
                .thenThrow(new LocalizedException(Status.BAD_REQUEST, "GL_POSTING_UNBALANCED"));
        when(postingMstRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When & Then
        LocalizedException ex = assertThrows(LocalizedException.class,
                () -> journalGenerationService.createJournalFromPosting(1L));
        assertEquals("GL_POSTING_UNBALANCED", ex.getMessageKey());

        // Verify posting was saved with ERROR status
        verify(postingMstRepository).save(argThat(posting -> {
            assertEquals("ERROR", posting.getStatus());
            assertEquals("GL_POSTING_UNBALANCED", posting.getErrorMessage());
            return true;
        }));
    }

    // ==================== Entity Map Building ====================

    @Test
    void createJournalFromPosting_ShouldBuildEntityMap_WithCustomerReference() {
        // Given
        when(postingMstRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(readyPosting));
        when(postingEngineService.executeForPosting(any(), eq(1L)))
                .thenReturn(ServiceResult.success(engineResponse, Status.CREATED));
        when(postingMstRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        journalGenerationService.createJournalFromPosting(1L);

        // Then — verify the request sent to the engine contains the posting data
        verify(postingEngineService).executeForPosting(argThat(request -> {
            assertEquals(100L, request.getCompanyIdFk());
            assertEquals("SALES", request.getSourceModule());
            assertEquals("INVOICE", request.getSourceDocType());
            assertEquals(new BigDecimal("10000.00"), request.getTotalAmount());
            assertEquals(LocalDate.of(2024, 7, 1), request.getJournalDate());
            return true;
        }), eq(1L));
    }

    @Test
    void createJournalFromPosting_ShouldBuildEntityMap_WithSupplierReference() {
        // Given — detail has supplierIdFk instead of customerIdFk
        testDetail.setCustomerIdFk(null);
        testDetail.setSupplierIdFk(300L);
        when(postingMstRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(readyPosting));
        when(postingEngineService.executeForPosting(any(), eq(1L)))
                .thenReturn(ServiceResult.success(engineResponse, Status.CREATED));
        when(postingMstRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        journalGenerationService.createJournalFromPosting(1L);

        // Then — verify VENDOR key in entityMap
        verify(postingEngineService).executeForPosting(argThat(request -> {
            assertNotNull(request.getEntityMap());
            assertEquals(300L, request.getEntityMap().get("VENDOR"));
            return true;
        }), eq(1L));
    }

    // ==================== Preview Journal Tests ====================

    @Test
    void previewJournalFromPosting_ShouldSucceed_WhenPostingIsReady() {
        // Given
        JournalPreviewResponse previewResponse = JournalPreviewResponse.builder()
                .ruleId(10L)
                .companyIdFk(100L)
                .sourceModule("SALES")
                .sourceDocType("INVOICE")
                .isBalanced(true)
                .totalDebit(new BigDecimal("10000.00"))
                .totalCredit(new BigDecimal("10000.00"))
                .lines(List.of(
                        GlJournalLineResponse.builder()
                                .lineNo(1).accountIdFk(1001L).accountCode("1101")
                                .accountName("Cash").debitAmount(new BigDecimal("10000.00")).build(),
                        GlJournalLineResponse.builder()
                                .lineNo(2).accountIdFk(4001L).accountCode("4101")
                                .accountName("Sales Revenue").creditAmount(new BigDecimal("10000.00")).build()
                ))
                .build();

        when(postingMstRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(readyPosting));
        when(postingEngineService.previewForPosting(any(), eq(1L)))
                .thenReturn(ServiceResult.success(previewResponse));

        // When
        ServiceResult<JournalPreviewResponse> result = journalGenerationService.previewJournalFromPosting(1L);

        // Then
        assertTrue(result.isSuccess());
        JournalPreviewResponse preview = result.getData();
        assertNotNull(preview);
        assertTrue(preview.getIsBalanced());
        assertEquals(new BigDecimal("10000.00"), preview.getTotalDebit());
        assertEquals(new BigDecimal("10000.00"), preview.getTotalCredit());
        assertEquals(2, preview.getLines().size());
        assertEquals("1101", preview.getLines().get(0).getAccountCode());
        assertEquals("Cash", preview.getLines().get(0).getAccountName());

        // Verify NO save was called — preview is read-only
        verify(postingMstRepository, never()).save(any());
    }

    @Test
    void previewJournalFromPosting_ShouldThrowNotFound_WhenPostingDoesNotExist() {
        // Given
        when(postingMstRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

        // When & Then
        LocalizedException ex = assertThrows(LocalizedException.class,
                () -> journalGenerationService.previewJournalFromPosting(999L));
        assertEquals("GL_POSTING_NOT_FOUND", ex.getMessageKey());
        verify(postingEngineService, never()).previewForPosting(any(), any());
    }

    @Test
    void previewJournalFromPosting_ShouldReject_WhenStatusIsDraft() {
        // Given
        readyPosting.setStatus("DRAFT");
        when(postingMstRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(readyPosting));

        // When & Then
        LocalizedException ex = assertThrows(LocalizedException.class,
                () -> journalGenerationService.previewJournalFromPosting(1L));
        assertEquals("GL_POSTING_NOT_READY", ex.getMessageKey());
        verify(postingEngineService, never()).previewForPosting(any(), any());
        verify(postingMstRepository, never()).save(any());
    }

    @Test
    void previewJournalFromPosting_ShouldReject_WhenJournalAlreadyLinked() {
        // Given
        readyPosting.setFinJournalIdFk(99L);
        when(postingMstRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(readyPosting));

        // When & Then
        LocalizedException ex = assertThrows(LocalizedException.class,
                () -> journalGenerationService.previewJournalFromPosting(1L));
        assertEquals("GL_POSTING_JOURNAL_ALREADY_EXISTS", ex.getMessageKey());
        verify(postingEngineService, never()).previewForPosting(any(), any());
    }

    @Test
    void previewJournalFromPosting_ShouldReject_WhenNoDetails() {
        // Given
        readyPosting.setDetails(new ArrayList<>());
        when(postingMstRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(readyPosting));

        // When & Then
        LocalizedException ex = assertThrows(LocalizedException.class,
                () -> journalGenerationService.previewJournalFromPosting(1L));
        assertEquals("GL_POSTING_NO_DETAILS_FOR_JOURNAL", ex.getMessageKey());
        verify(postingEngineService, never()).previewForPosting(any(), any());
    }

    @Test
    void previewJournalFromPosting_ShouldBuildCorrectRequest_WithEntityMap() {
        // Given
        JournalPreviewResponse previewResponse = JournalPreviewResponse.builder()
                .ruleId(10L).isBalanced(true)
                .totalDebit(new BigDecimal("10000.00"))
                .totalCredit(new BigDecimal("10000.00"))
                .lines(List.of()).build();

        when(postingMstRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(readyPosting));
        when(postingEngineService.previewForPosting(any(), eq(1L)))
                .thenReturn(ServiceResult.success(previewResponse));

        // When
        journalGenerationService.previewJournalFromPosting(1L);

        // Then — verify correct request was passed to engine
        verify(postingEngineService).previewForPosting(argThat(request -> {
            assertEquals(100L, request.getCompanyIdFk());
            assertEquals("SALES", request.getSourceModule());
            assertEquals("INVOICE", request.getSourceDocType());
            assertEquals(new BigDecimal("10000.00"), request.getTotalAmount());
            assertEquals(LocalDate.of(2024, 7, 1), request.getJournalDate());
            assertNotNull(request.getEntityMap());
            assertEquals(200L, request.getEntityMap().get("CUSTOMER"));
            return true;
        }), eq(1L));
    }
}
