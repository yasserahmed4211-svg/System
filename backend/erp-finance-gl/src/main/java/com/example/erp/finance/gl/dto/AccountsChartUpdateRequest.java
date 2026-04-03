package com.example.erp.finance.gl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountsChartUpdateRequest {

    /**
     * Account number is auto-generated and CANNOT be manually overridden.
     * This field is NOT accepted by the API — it is ignored if supplied.
     */

    @NotBlank(message = "{validation.required}")
    @Size(max = 500, message = "{validation.size}")
    private String accountChartName;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    private String accountType;

    /**
     * Parent account PK (ACCOUNT_CHART_FK). Null to make this a root account.
     */
    private Long accountChartFk;

    /**
     * organizationFk is immutable after creation (R-008).
     * Included in update DTO for completeness but must match original.
     */
    @NotNull(message = "{validation.required}")
    private Long organizationFk;

    private Long organizationSubFk;

    private Boolean isActive;
}
