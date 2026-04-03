package com.example.erp.finance.gl.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountsChartSearchRequest {

    private String accountChartNo;
    private String accountChartName;
    private String accountType;
    private Boolean isActive;
    private Long organizationFk;
    private Long parentAccountId;
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 20;
}
