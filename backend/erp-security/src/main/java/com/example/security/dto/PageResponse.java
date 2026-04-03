package com.example.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for Page entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Page response data")
public class PageResponse {

    @Schema(description = "Page ID", example = "1")
    private Long id;

    @Schema(description = "Page code (uppercase)", example = "USER")
    private String pageCode;

    @Schema(description = "Arabic name", example = "إدارة المستخدمين")
    private String nameAr;

    @Schema(description = "English name", example = "User Management")
    private String nameEn;

    @Schema(description = "Route path", example = "/admin/users")
    private String route;

    @Schema(description = "Icon", example = "pi pi-users")
    private String icon;

    @Schema(description = "Module", example = "SECURITY")
    private String module;

    @Schema(description = "Parent page ID")
    private Long parentId;

    @Schema(description = "Display order")
    private Integer displayOrder;

    @Schema(description = "Active status")
    private Boolean active;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Auto-generated permission keys (VIEW, CREATE, UPDATE, DELETE)")
    private Map<String, String> permissionKeys;

    @Schema(description = "Created timestamp")
    private Instant createdAt;

    @Schema(description = "Created by username")
    private String createdBy;

    @Schema(description = "Updated timestamp")
    private Instant updatedAt;

    @Schema(description = "Updated by username")
    private String updatedBy;
}
