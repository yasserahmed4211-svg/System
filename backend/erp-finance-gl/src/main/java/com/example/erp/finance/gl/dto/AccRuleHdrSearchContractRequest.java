package com.example.erp.finance.gl.dto;

import com.example.erp.common.dto.BaseSearchContractRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * API Contract Request DTO for Accounting Rules search.
 *
 * Matches the standard search contract:
 * <pre>{ filters: [{ field, operator, value }], sorts: [{ field, direction }], page, size }</pre>
 *
 * Allowed filter fields:
 * - companyIdFk (EQUALS)
 * - sourceModule (EQUALS, CONTAINS)
 * - sourceDocType (EQUALS, CONTAINS)
 * - isActive (EQUALS)
 *
 * @author ERP Team
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AccRuleHdrSearchContractRequest extends BaseSearchContractRequest {
}
