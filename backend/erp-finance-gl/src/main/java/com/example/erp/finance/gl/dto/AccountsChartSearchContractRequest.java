package com.example.erp.finance.gl.dto;

import com.example.erp.common.dto.BaseSearchContractRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * API Contract Request DTO for Chart of Accounts search.
 *
 * Matches the standard search contract:
 * <pre>{ filters: [{ field, operator, value }], sorts: [{ field, direction }], page, size }</pre>
 *
 * Allowed filter fields:
 * - accountChartNo (CONTAINS, STARTS_WITH, EQUALS)
 * - accountChartName (CONTAINS, STARTS_WITH, EQUALS)
 * - accountType (EQUALS)
 * - isActive (EQUALS)
 * - organizationFk (EQUALS)
 * - parent.accountChartPk (EQUALS)
 *
 * @author ERP Team
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AccountsChartSearchContractRequest extends BaseSearchContractRequest {
}
