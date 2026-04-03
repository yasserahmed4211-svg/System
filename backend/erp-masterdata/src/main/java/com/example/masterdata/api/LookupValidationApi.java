package com.example.masterdata.api;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.masterdata.dto.LookupValueResponse;

import java.util.List;
import java.util.Optional;

/**
 * Public API Interface for Lookup Consumption and Validation.
 *
 * <p>This interface defines the contract for cross-module lookup consumption.
 * Other ERP modules (e.g., erp-finance-gl, erp-security) MUST depend on this
 * interface rather than directly on the implementation class.</p>
 *
 * <h3>Architecture Rules Compliance:</h3>
 * <ul>
 *   <li><b>Rule 2:</b> Inter-module communication only via public interfaces and DTOs</li>
 *   <li><b>Rule 4:</b> No cross-module repository access</li>
 *   <li><b>Rule 7:</b> Clear public API per module</li>
 *   <li><b>Rule 15:</b> Hide internal implementation details</li>
 * </ul>
 *
 * <h3>Usage Example (from erp-finance-gl):</h3>
 * <pre>{@code
 * @Autowired
 * private LookupValidationApi lookupValidationApi; // ✅ Interface injection
 *
 * // Validate accountType value
 * lookupValidationApi.validateOrThrow("ACCOUNT_TYPE", accountType);
 *
 * // Fetch dropdown values
 * ServiceResult<List<LookupValueResponse>> result =
 *     lookupValidationApi.fetchLookupValues("ENTRY_SIDE");
 * }</pre>
 *
 * @author ERP Team
 * @since 1.0
 */
public interface LookupValidationApi {

    /**
     * Fetch all active lookup values for a given lookup code.
     *
     * <p>Returns only ACTIVE lookup details, ordered by sortOrder.
     * Response is cached for performance (24h TTL).</p>
     *
     * @param lookupCode Master lookup code (e.g., "ACCOUNT_TYPE", "ENTRY_SIDE", "SOURCE_MODULE").
     *                   Case-insensitive – will be uppercased internally.
     * @return ServiceResult containing the list of active lookup values,
     *         or NOT_FOUND status if lookup code does not exist or is inactive.
     */
    ServiceResult<List<LookupValueResponse>> fetchLookupValues(String lookupCode);

    /**
     * Validate whether a specific lookup value exists and is active.
     *
     * <p>Used by business services to validate incoming request field values
     * against registered lookup codes.</p>
     *
     * @param lookupCode Master lookup code (e.g., "ACCOUNT_TYPE")
     * @param value      Lookup detail code (e.g., "DEBIT", "TOTAL", "CASH")
     * @return {@code true} if the value exists, belongs to the given lookup, and is active;
     *         {@code false} otherwise.
     */
    boolean isValid(String lookupCode, String value);

    /**
     * Validate a lookup value or throw a LocalizedException.
     *
     * <p>Convenience method that throws {@code LocalizedException} with
     * error code {@code LOOKUP_VALUE_INVALID} if the value is not valid.</p>
     *
     * @param lookupCode Master lookup code
     * @param value      Lookup detail code to validate
     * @throws com.example.erp.common.exception.LocalizedException
     *         if value is invalid, with status BAD_REQUEST and error code LOOKUP_VALUE_INVALID
     */
    void validateOrThrow(String lookupCode, String value);

    /**
     * Get the SORT_ORDER for a specific lookup detail code.
     *
     * <p>Used by business services (e.g., account number generation) that need
     * the numeric sort order to derive prefixes or ordering.</p>
     *
     * @param lookupCode Master lookup key (e.g., "GL_ACCOUNT_TYPE")
     * @param detailCode Lookup detail code (e.g., "ASSET", "LIABILITY")
     * @return Optional containing the sort order if found, empty otherwise
     */
    Optional<Integer> getSortOrder(String lookupCode, String detailCode);
}
