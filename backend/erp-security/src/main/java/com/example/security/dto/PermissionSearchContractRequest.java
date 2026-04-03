package com.example.security.dto;

import com.example.erp.common.dto.BaseSearchContractRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * API Contract Request DTO for Permission search.
 *
 * Matches the standard search contract:
 * <pre>{ filters: [{ field, operator, value }], sorts: [{ field, direction }], page, size }</pre>
 *
 * Allowed filter fields:
 * - name (EQUALS, CONTAINS, STARTS_WITH)
 * - module (EQUALS)
 *
 * @author ERP Team
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PermissionSearchContractRequest extends BaseSearchContractRequest {
}
