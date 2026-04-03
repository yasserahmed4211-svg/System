package com.example.erp.finance.gl.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccRuleHdrResponse {

    private Long ruleId;
    private Long companyIdFk;
    private String companyName;
    private String sourceModule;
    private String sourceDocType;
    private Boolean isActive;
    private Integer lineCount;
    private List<AccRuleLineResponse> lines;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
}
