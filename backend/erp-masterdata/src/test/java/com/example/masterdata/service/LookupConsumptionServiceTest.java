package com.example.masterdata.service;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.exception.LocalizedException;
import com.example.masterdata.dto.LookupValueResponse;
import com.example.masterdata.repository.MasterLookupRepository;
import com.example.masterdata.repository.projection.LookupValueProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LookupConsumptionService}.
 *
 * <p>Validates JOIN-based fetch, single-query validation,
 * and edge-case behaviour (null/empty/inactive).</p>
 */
@ExtendWith(MockitoExtension.class)
class LookupConsumptionServiceTest {

    @Mock
    private MasterLookupRepository masterLookupRepository;

    @InjectMocks
    private LookupConsumptionService service;

    // ── Shared mock projections ──────────────────────────────────

    private LookupValueProjection activeRow1;
    private LookupValueProjection activeRow2;

    @BeforeEach
    void setUp() {
        // The service uses @Lazy @Autowired self-injection for @Cacheable interception.
        // In unit tests, inject the service itself as 'self' so calls bypass the proxy.
        ReflectionTestUtils.setField(service, "self", service);
        activeRow1 = mockProjection("ENTRY_SIDE", 1, "DEBIT", "مدين", "Debit", 1);
        activeRow2 = mockProjection("ENTRY_SIDE", 1, "CREDIT", "دائن", "Credit", 2);
    }

    // ══════════════════════════════════════════════════════════════
    // fetchLookupValues
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("fetchLookupValues")
    class FetchLookupValuesTests {

        @Test
        @DisplayName("should return active details ordered by sortOrder")
        void shouldReturnActiveDetails() {
            when(masterLookupRepository.findLookupValuesByKey("ENTRY_SIDE", 1))
                    .thenReturn(List.of(activeRow1, activeRow2));

            ServiceResult<List<LookupValueResponse>> result =
                    service.fetchLookupValues("entry_side"); // lowercase → normalized

            assertTrue(result.isSuccess());
            List<LookupValueResponse> values = result.getData();
            assertEquals(2, values.size());
            assertEquals("DEBIT", values.get(0).getCode());
            assertEquals("مدين", values.get(0).getLabel());
            assertEquals("CREDIT", values.get(1).getCode());

            verify(masterLookupRepository).findLookupValuesByKey("ENTRY_SIDE", 1);
        }

        @Test
        @DisplayName("should filter null codes from LEFT JOIN results")
        void shouldFilterNullCodes() {
            LookupValueProjection noDetail = mockProjection("EMPTY", 1, null, null, null, null);

            when(masterLookupRepository.findLookupValuesByKey("EMPTY", 1))
                    .thenReturn(List.of(noDetail));

            ServiceResult<List<LookupValueResponse>> result = service.fetchLookupValues("EMPTY");

            assertTrue(result.isSuccess());
            assertTrue(result.getData().isEmpty());
        }

        @Test
        @DisplayName("should return NOT_FOUND when lookup key does not exist")
        void shouldReturnNotFoundForMissingKey() {
            when(masterLookupRepository.findLookupValuesByKey("NONEXISTENT", 1))
                    .thenReturn(Collections.emptyList());

            ServiceResult<List<LookupValueResponse>> result =
                    service.fetchLookupValues("NONEXISTENT");

            assertFalse(result.isSuccess());
        }

        @Test
        @DisplayName("should return NOT_FOUND when master lookup is inactive")
        void shouldReturnNotFoundForInactiveMaster() {
            LookupValueProjection inactive = mockProjection("FROZEN", 0, "X", "اكس", "X", 1);

            when(masterLookupRepository.findLookupValuesByKey("FROZEN", 1))
                    .thenReturn(List.of(inactive));

            ServiceResult<List<LookupValueResponse>> result = service.fetchLookupValues("FROZEN");

            assertFalse(result.isSuccess());
        }

        @Test
        @DisplayName("should return NOT_FOUND when masterIsActive is null")
        void shouldReturnNotFoundForNullActiveStatus() {
            LookupValueProjection nullStatus = mockProjection("NULL_STATUS", null, "A", "أ", "A", 1);

            when(masterLookupRepository.findLookupValuesByKey("NULL_STATUS", 1))
                    .thenReturn(List.of(nullStatus));

            ServiceResult<List<LookupValueResponse>> result =
                    service.fetchLookupValues("NULL_STATUS");

            assertFalse(result.isSuccess());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // isValid  (single COUNT query with JOIN)
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("isValid")
    class IsValidTests {

        @Test
        @DisplayName("should return true when code exists and is active")
        void shouldReturnTrueForValidCode() {
            when(masterLookupRepository.countActiveByKeyAndCode("ENTRY_SIDE", "DEBIT"))
                    .thenReturn(1);

            assertTrue(service.isValid("entry_side", "debit"));
        }

        @Test
        @DisplayName("should return false when code does not exist")
        void shouldReturnFalseForMissingCode() {
            when(masterLookupRepository.countActiveByKeyAndCode("ENTRY_SIDE", "UNKNOWN"))
                    .thenReturn(0);

            assertFalse(service.isValid("ENTRY_SIDE", "UNKNOWN"));
        }

        @Test
        @DisplayName("should return false when lookupCode is null")
        void shouldReturnFalseForNullLookupCode() {
            assertFalse(service.isValid(null, "DEBIT"));
            verifyNoInteractions(masterLookupRepository);
        }

        @Test
        @DisplayName("should return false when value is null")
        void shouldReturnFalseForNullValue() {
            assertFalse(service.isValid("ENTRY_SIDE", null));
            verifyNoInteractions(masterLookupRepository);
        }

        @Test
        @DisplayName("should return false when value is blank")
        void shouldReturnFalseForBlankValue() {
            assertFalse(service.isValid("ENTRY_SIDE", "  "));
            verifyNoInteractions(masterLookupRepository);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // validateOrThrow
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("validateOrThrow")
    class ValidateOrThrowTests {

        @Test
        @DisplayName("should not throw when value is valid")
        void shouldNotThrowForValidCode() {
            when(masterLookupRepository.countActiveByKeyAndCode("ENTRY_SIDE", "DEBIT"))
                    .thenReturn(1);

            assertDoesNotThrow(() -> service.validateOrThrow("ENTRY_SIDE", "DEBIT"));
        }

        @Test
        @DisplayName("should throw LocalizedException when value is invalid")
        void shouldThrowForInvalidCode() {
            when(masterLookupRepository.countActiveByKeyAndCode("ENTRY_SIDE", "BAD"))
                    .thenReturn(0);

            LocalizedException ex = assertThrows(LocalizedException.class,
                    () -> service.validateOrThrow("ENTRY_SIDE", "BAD"));

            assertEquals("LOOKUP_VALUE_INVALID", ex.getMessageKey());
        }
    }

    // ── Helper ───────────────────────────────────────────────────

    private static LookupValueProjection mockProjection(
            String lookupKey, Integer masterIsActive,
            String code, String nameAr, String nameEn, Integer sortOrder) {
        LookupValueProjection p = mock(LookupValueProjection.class);
        lenient().when(p.getLookupKey()).thenReturn(lookupKey);
        lenient().when(p.getMasterIsActive()).thenReturn(masterIsActive);
        lenient().when(p.getCode()).thenReturn(code);
        lenient().when(p.getNameAr()).thenReturn(nameAr);
        lenient().when(p.getNameEn()).thenReturn(nameEn);
        lenient().when(p.getSortOrder()).thenReturn(sortOrder);
        return p;
    }
}
