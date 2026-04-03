package com.example.masterdata.dto;

import com.example.erp.common.dto.BaseSearchContractRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * API Contract Request DTO for Master Lookup search.
 *
 * Matches api-contracts/master-lookup.contract.md:
 * - filters: [{ field, operator, value }]
 * - sorts:   [{ field, direction }]
 * - page, size
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
public class MasterLookupSearchRequest extends BaseSearchContractRequest {
}
