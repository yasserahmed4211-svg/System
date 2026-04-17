package com.example.org.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for updating a legal entity - طلب تحديث كيان قانوني")
public class LegalEntityUpdateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Arabic name - الاسم بالعربية", example = "شركة النور", required = true)
    private String legalEntityNameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "English name - الاسم بالإنجليزية", example = "Al Noor Company", required = true)
    private String legalEntityNameEn;

    @NotNull(message = "{validation.required}")
    @Schema(description = "Country FK - الدولة", example = "1", required = true)
    private Long countryId;

    @NotNull(message = "{validation.required}")
    @Schema(description = "Functional currency FK - العملة الوظيفية", example = "1", required = true)
    private Long functionalCurrencyId;

    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Tax number - الرقم الضريبي")
    private String taxNumber;

    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Commercial registration number - رقم السجل التجاري")
    private String commercialRegNumber;

    @Min(value = 1, message = "{validation.min}")
    @Max(value = 12, message = "{validation.max}")
    @Schema(description = "Fiscal year start month - شهر بداية السنة المالية")
    private Integer fiscalYearStartMonth;

    @Size(max = 250, message = "{validation.size}")
    @Schema(description = "Address line 1 - العنوان 1")
    private String addressLine1;

    @Size(max = 250, message = "{validation.size}")
    @Schema(description = "Address line 2 - العنوان 2")
    private String addressLine2;

    @Size(max = 100, message = "{validation.size}")
    @Schema(description = "City name - المدينة")
    private String cityName;

    @Size(max = 30, message = "{validation.size}")
    @Schema(description = "Phone - الهاتف")
    private String phone;

    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Email - البريد الإلكتروني")
    private String email;

    @Size(max = 500, message = "{validation.size}")
    @Schema(description = "Website - الموقع الإلكتروني")
    private String website;
}
