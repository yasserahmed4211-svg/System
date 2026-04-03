package com.example.masterdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight DTO for lookup consumption.
 * Used by all ERP modules for dropdown lists.
 *
 * @author ERP Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LookupValueResponse {

    /**
     * Lookup detail code (e.g. "DEBIT", "TOTAL", "CASH")
     */
    private String code;

    /**
     * Lookup detail label (Arabic name)
     */
    private String label;

    /**
     * Lookup detail label (English name)
     */
    private String labelEn;
}
