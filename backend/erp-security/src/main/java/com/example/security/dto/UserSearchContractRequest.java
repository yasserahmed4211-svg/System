package com.example.security.dto;

import com.example.erp.common.dto.BaseSearchContractRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * API Contract Request DTO for User search.
 *
 * Matches the standard search contract:
 * <pre>{ filters: [{ field, operator, value }], sorts: [{ field, direction }], page, size }</pre>
 *
 * Allowed filter fields:
 * - username (EQUALS, CONTAINS, STARTS_WITH)
 * - enabled (EQUALS)
 * - createdAt (EQUALS)
 *
 * @author ERP Team
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserSearchContractRequest extends BaseSearchContractRequest {
}
