package com.example.erp.finance.gl.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountsChartResponse {

    private Long accountChartPk;
    private String accountChartNo;
    private String accountChartName;
    private String accountType;
    private Long accountChartFk;
    private String parentAccountName;
    private String parentAccountNo;
    private Integer level;
    private Boolean isActive;
    private Long organizationFk;
    private Long organizationSubFk;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
}
