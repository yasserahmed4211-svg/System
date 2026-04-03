package com.example.erp.finance.gl.dto;

import lombok.*;

/**
 * Lightweight DTO for the account lookup endpoint.
 * Maps to the frontend LookupItem contract: { id, display, code, name, type }.
 *
 * @architecture DTO layer — public API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountLookupDto {
    /** Primary key — stored in the form as the selected value */
    private Long id;
    /** Formatted display label: "ACCOUNT_NO - ACCOUNT_NAME" */
    private String display;
    /** Account chart number */
    private String code;
    /** Account name */
    private String name;
    /** Account type name (e.g. ASSET, LIABILITY) */
    private String type;
    /** Whether the account is active */
    private Boolean isActive;
}
