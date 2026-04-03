package com.example.erp.finance.gl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccRuleLineRequest {

    @NotNull(message = "{validation.required}")
    private Long accountIdFk;

    @NotBlank(message = "{validation.required}")
    @Size(max = 10, message = "{validation.size}")
    private String entrySide;

    @NotNull(message = "{validation.required}")
    private Integer priority;

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    private String amountSourceType;

    private BigDecimal amountSourceValue;

    @Size(max = 20, message = "{validation.size}")
    private String paymentTypeCode;

    @Size(max = 20, message = "{validation.size}")
    private String entityType;
}
