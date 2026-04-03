package com.example.security.dto;

import com.example.erp.common.dto.BaseSearchContractRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * API Contract Request DTO for Page (UI Screen) search.
 *
 * Matches the standard search contract:
 * <pre>{ filters: [{ field, operator, value }], sorts: [{ field, direction }], page, size }</pre>
 *
 * Allowed filter fields:
 * - pageCode (EQUALS, CONTAINS, STARTS_WITH)
 * - nameAr (CONTAINS, STARTS_WITH)
 * - nameEn (CONTAINS, STARTS_WITH)
 * - module (EQUALS)
 * - active (EQUALS)
 *
 * @author ERP Team
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PageSearchContractRequest extends BaseSearchContractRequest {
}
