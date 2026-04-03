package com.example.erp.finance.gl.dto;

import com.example.erp.common.dto.BaseSearchContractRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * API Contract Request DTO for ACC_POSTING_MST search.
 *
 * Allowed filter fields:
 * - status (EQUALS)
 * - sourceModule (EQUALS)
 * - sourceDocType (EQUALS)
 * - companyIdFk (EQUALS)
 * - docDate (EQUALS)
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AccPostingSearchContractRequest extends BaseSearchContractRequest {
}
