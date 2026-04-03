package com.example.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing Page
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an existing page")
public class UpdatePageRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Schema(description = "Arabic name for the page", example = "إدارة المستخدمين")
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Schema(description = "English name for the page", example = "User Management")
    private String nameEn;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Angular route path", example = "/admin/users")
    private String route;

    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Icon class or name", example = "pi pi-users")
    private String icon;

    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Module grouping", example = "SECURITY")
    private String module;

    @Schema(description = "Parent page ID for hierarchical structure")
    private Long parentId;

    @Schema(description = "Display order for sorting", example = "10")
    private Integer displayOrder;

    @Size(max = 500, message = "{validation.size}")
    @Schema(description = "Optional description")
    private String description;
}
