package com.example.erp.finance.gl.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccRuleLineResponse {

    private Long ruleLineId;
    private Long accountIdFk;
    private String accountCode;
    private String accountName;
    private String entrySide;
    private Integer priority;
    private String amountSourceType;
    private BigDecimal amountSourceValue;
    private String paymentTypeCode;
    private String entityType;
}
