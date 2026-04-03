package com.example.masterdata.service;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.example.masterdata.api.LookupValidationApi;
import com.example.masterdata.dto.LookupValueResponse;
import com.example.masterdata.exception.MasterDataErrorCodes;
import com.example.masterdata.repository.MasterLookupRepository;
import com.example.masterdata.repository.projection.LookupValueProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for generic lookup consumption.
 *
 * <p>Provides read-only access to lookup values for all ERP modules
 * with aggressive caching for performance.
 *
 * <p>All data is fetched using a single native JOIN query
 * ({@code findLookupValuesByKey}) that returns both the master lookup
 * status and all active detail rows in one round-trip.</p>
 *
 * <h3>Architecture Rules:</h3>
 * <ul>
 *   <li>Rule 2: Implements {@link LookupValidationApi} for cross-module consumption</li>
 *   <li>Rule 5.4: Return DTOs, not entities</li>
 *   <li>Rule 7: Clear public API per module</li>
 *   <li>Rule 23: Cache read-heavy / stable reference data</li>
 *   <li>Rule 25: No N+1 — single JOIN query for fetch &amp; validation</li>
 * </ul>
 *
 * @author ERP Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LookupConsumptionService implements LookupValidationApi {

    private final MasterLookupRepository masterLookupRepository;

    // ── Public API (LookupValidationApi) ─────────────────────────

    /**
     * Fetch all active lookup values for a lookup code.
     *
     * <p>Uses a single native JOIN query for optimal performance.
     * Results are cached per lookup code.</p>
     *
     * @param lookupCode Master lookup key (case-insensitive)
     * @return ServiceResult with list of values, or NOT_FOUND on error
     */
    @Override
    @Cacheable(cacheNames = "lookupValues", key = "#lookupCode?.toUpperCase()")
    public ServiceResult<List<LookupValueResponse>> fetchLookupValues(String lookupCode) {
        String key = normalize(lookupCode);
        log.debug("Fetching lookup values for key='{}'", key);

        List<LookupValueProjection> rows = masterLookupRepository.findLookupValuesByKey(key, 1);

        if (rows.isEmpty()) {
            log.warn("Lookup not found: key='{}'", key);
            return ServiceResult.notFound("Lookup code does not exist: " + lookupCode);
        }

        LookupValueProjection first = rows.get(0);
        if (first.getMasterIsActive() == null || first.getMasterIsActive() != 1) {
            log.warn("Lookup is inactive: key='{}'", key);
            return ServiceResult.notFound("Lookup code is inactive: " + lookupCode);
        }

        List<LookupValueResponse> values = rows.stream()
                .filter(r -> r.getCode() != null)
                .map(this::toResponse)
                .toList();

        log.debug("Returning {} active values for key='{}'", values.size(), key);
        return ServiceResult.success(values, Status.SUCCESS);
    }

    /**
     * Validate whether a lookup detail code exists and is active
     * under the given master lookup key.
     *
     * <p>Executes a single COUNT query with JOIN — no N+1.</p>
     *
     * @param lookupCode Master lookup key (case-insensitive)
     * @param value      Detail code to validate (e.g., "DEBIT", "TOTAL")
     * @return true if valid and active
     */
    @Override
    public boolean isValid(String lookupCode, String value) {
        if (lookupCode == null || value == null || value.isBlank()) {
            return false;
        }
        String key = normalize(lookupCode);
        String code = value.trim().toUpperCase();
        log.debug("Validating lookup: key='{}', code='{}'", key, code);

        return masterLookupRepository.countActiveByKeyAndCode(key, code) > 0;
    }

    /**
     * Validate or throw {@link LocalizedException} with LOOKUP_VALUE_INVALID.
     *
     * @param lookupCode Master lookup key
     * @param value      Detail code to validate
     */
    @Override
    public void validateOrThrow(String lookupCode, String value) {
        if (!isValid(lookupCode, value)) {
            throw new LocalizedException(
                    Status.BAD_REQUEST,
                    MasterDataErrorCodes.LOOKUP_VALUE_INVALID,
                    lookupCode,
                    value
            );
        }
    }

    // ── Private helpers ──────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Queries the lookup projection directly to find the SORT_ORDER for
     * a specific detail code under the given master lookup key.</p>
     */
    @Override
    public Optional<Integer> getSortOrder(String lookupCode, String detailCode) {
        if (lookupCode == null || detailCode == null) {
            return Optional.empty();
        }
        String key = normalize(lookupCode);
        String normalizedCode = detailCode.trim().toUpperCase();

        List<LookupValueProjection> rows = masterLookupRepository.findLookupValuesByKey(key, 1);
        return rows.stream()
                .filter(r -> r.getCode() != null && r.getCode().equalsIgnoreCase(normalizedCode))
                .findFirst()
                .map(LookupValueProjection::getSortOrder);
    }

    // ── Private helpers (internal) ───────────────────────────────

    private LookupValueResponse toResponse(LookupValueProjection row) {
        return LookupValueResponse.builder()
                .code(row.getCode())
                .label(row.getNameAr())
                .labelEn(row.getNameEn())
                .build();
    }

    private String normalize(String lookupCode) {
        return lookupCode == null ? null : lookupCode.trim().toUpperCase();
    }
}
