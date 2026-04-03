package com.example.erp.finance.gl.dto;

import lombok.*;

/**
 * Lightweight projection DTO for the eligible-parent LOV.
 * Contains only the fields needed for display and selection — no audit columns.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibleParentAccountDto {
    private Long accountChartPk;
    private String accountChartNo;
    private String accountChartName;
    private String accountType;
    private Boolean isActive;
}
