package com.example.masterdata.dto;

import com.example.erp.common.dto.BaseSearchContractRequest;
import com.erp.common.search.SearchRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * API Contract Request DTO for Lookup Detail search.
 *
 * Converts frontend format to backend SearchRequest format.
 * Frontend sends: { filters: [{ field, operator, value }], sorts, page, size }
 * Backend expects: SearchFilter with Op enum
 *
 * Architecture Rules:
 * - Rule 7.1: DTOs for API contract
 * - Rule 7.3: Clear DTO naming
 *
 * @author ERP Team
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LookupDetailSearchRequest extends BaseSearchContractRequest {

    /** masterLookupId filter field name */
    private static final String MASTER_LOOKUP_ID_FIELD = "masterLookupId";

    /**
     * Extract masterLookupId from filters (required for parent-child relationship)
     */
    public Long getMasterLookupId() {
        List<ContractFilter> allFilters = getFilters();
        if (allFilters == null) {
            return null;
        }
        return allFilters.stream()
            .filter(f -> MASTER_LOOKUP_ID_FIELD.equalsIgnoreCase(f.getField()))
            .findFirst()
            .map(f -> {
                Object value = f.getValue();
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                } else if (value instanceof String) {
                    try {
                        return Long.parseLong((String) value);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
                return null;
            })
            .orElse(null);
    }

    /**
     * Map contract request into common-utils SearchRequest used by the service layer.
     * Excludes masterLookupId from filters (handled separately via getMasterLookupId)
     */
    @Override
    public SearchRequest toCommonSearchRequest() {
        return toCommonSearchRequest(Set.of(MASTER_LOOKUP_ID_FIELD));
    }
}
