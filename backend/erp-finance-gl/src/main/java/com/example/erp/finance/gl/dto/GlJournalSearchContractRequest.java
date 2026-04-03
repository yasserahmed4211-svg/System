package com.example.erp.finance.gl.dto;

import com.example.erp.common.dto.BaseSearchContractRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * API Contract Request DTO for GL Journal search.
 *
 * Allowed filter fields:
 * - journalNo (EQUALS, CONTAINS)
 * - journalDate (EQUALS)
 * - journalTypeIdFk (EQUALS)
 * - statusIdFk (EQUALS)
 * - sourceModuleIdFk (EQUALS)
 * - activeFl (EQUALS)
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GlJournalSearchContractRequest extends BaseSearchContractRequest {
}
