package com.example.org.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Legal entity response - استجابة الكيان القانوني")
public class LegalEntityResponse {

    @Schema(description = "ID", example = "1")
    private Long id;

    @Schema(description = "Legal entity code - كود الكيان القانوني", example = "LE-001")
    private String legalEntityCode;

    @Schema(description = "Arabic name - الاسم بالعربية", example = "شركة النور")
    private String legalEntityNameAr;

    @Schema(description = "English name - الاسم بالإنجليزية", example = "Al Noor Company")
    private String legalEntityNameEn;

    @Schema(description = "Country FK", example = "1")
    private Long countryId;

    @Schema(description = "Functional currency FK", example = "1")
    private Long functionalCurrencyId;

    @Schema(description = "Tax number - الرقم الضريبي")
    private String taxNumber;

    @Schema(description = "Commercial registration number - رقم السجل التجاري")
    private String commercialRegNumber;

    @Schema(description = "Fiscal year start month - شهر بداية السنة المالية")
    private Integer fiscalYearStartMonth;

    @Schema(description = "Address line 1")
    private String addressLine1;

    @Schema(description = "Address line 2")
    private String addressLine2;

    @Schema(description = "City name - المدينة")
    private String cityName;

    @Schema(description = "Phone - الهاتف")
    private String phone;

    @Schema(description = "Email - البريد الإلكتروني")
    private String email;

    @Schema(description = "Website - الموقع الإلكتروني")
    private String website;

    @Schema(description = "Active status - حالة النشاط", example = "true")
    private Boolean isActive;

    @Schema(description = "Region count - عدد المناطق", example = "3")
    private Integer regionCount;

    @Schema(description = "Branch count - عدد الفروع", example = "5")
    private Integer branchCount;

    @Schema(description = "Creation timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Created by user")
    private String createdBy;

    @Schema(description = "Last update timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "Last updated by user")
    private String updatedBy;
}
