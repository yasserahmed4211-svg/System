package com.example.masterdata.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for Lookup Detail
 * 
 * Architecture Rules:
 * - Rule 4.2: Never expose entities (use DTOs)
 * - Rule 7.1: DTOs for API contract
 * - Rule 7.3: Clear DTO naming (Response suffix)
 * 
 * @author ERP Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lookup detail response - استجابة قيمة مرجعية")
public class LookupDetailResponse {

    @Schema(description = "Lookup detail ID - معرف القيمة المرجعية", example = "1")
    private Long id;

    @Schema(description = "Master lookup ID - معرف القائمة المرجعية الرئيسية", example = "1")
    private Long masterLookupId;

    @Schema(description = "Master lookup key - مفتاح القائمة المرجعية الرئيسية", example = "COLOR")
    private String masterLookupKey;

    @Schema(description = "Master lookup name - اسم القائمة المرجعية الرئيسية", example = "اللون")
    private String masterLookupName;

    @Schema(description = "Detail code - كود القيمة", example = "RED")
    private String code;

    @Schema(description = "Name (Arabic) - اسم القيمة", example = "أحمر")
    private String nameAr;

    @Schema(description = "Name (English) - اسم القيمة بالإنجليزية", example = "Red")
    private String nameEn;

    @Schema(description = "Extra value - قيمة إضافية", example = "#FF0000")
    private String extraValue;

    @Schema(description = "Sort order - ترتيب العرض", example = "1")
    private Integer sortOrder;

    @Schema(description = "Active status - حالة النشاط", example = "true")
    private Boolean isActive;

    @Schema(description = "Creation timestamp - وقت الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Created by user - المستخدم المنشئ", example = "admin")
    private String createdBy;

    @Schema(description = "Last update timestamp - وقت آخر تحديث")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "Last updated by user - المستخدم المحدث", example = "admin")
    private String updatedBy;
}
