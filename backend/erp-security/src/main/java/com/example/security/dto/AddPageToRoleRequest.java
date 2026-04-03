package com.example.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to add a Page to a Role with specific CRUD permissions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to assign a page to a role with specific permissions")
public class AddPageToRoleRequest {

    @NotBlank(message = "{validation.required}")
    @Schema(description = "Page code to assign", example = "USER", required = true)
    private String pageCode;

    @NotNull(message = "{validation.required}")
    @Schema(
        description = "CRUD permissions to grant (VIEW is always added automatically). Use: CREATE, UPDATE, DELETE",
        example = "[\"CREATE\", \"UPDATE\", \"DELETE\"]"
    )
    private List<String> permissions;
}
