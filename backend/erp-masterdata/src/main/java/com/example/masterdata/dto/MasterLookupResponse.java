package com.example.masterdata.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for Master Lookup
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
@Schema(description = "Master lookup response - استجابة نوع القائمة المرجعية")
public class MasterLookupResponse {

    @Schema(description = "Master lookup ID - معرف القائمة المرجعية", example = "1")
    private Long id;

    @Schema(description = "Lookup key (UPPERCASE) - مفتاح القائمة المرجعية", example = "COLOR")
    private String lookupKey;

    @Schema(description = "Lookup name (Arabic) - اسم القائمة المرجعية", example = "اللون")
    private String lookupName;

    @Schema(description = "Lookup name (English) - اسم القائمة المرجعية بالإنجليزية", example = "Color")
    private String lookupNameEn;

    @Schema(description = "Description - وصف", example = "Product color classification")
    private String description;

    @Schema(description = "Active status - حالة النشاط", example = "true")
    private Boolean isActive;

    @Schema(description = "Number of lookup details - عدد القيم المرجعية", example = "10")
    private Integer detailCount;

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
