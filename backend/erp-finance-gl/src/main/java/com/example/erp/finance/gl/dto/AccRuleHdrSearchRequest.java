package com.example.erp.finance.gl.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccRuleHdrSearchRequest {

    private Long companyIdFk;
    private String sourceModule;
    private String sourceDocType;
    private Boolean isActive;
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 20;
}
