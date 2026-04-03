package com.example.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to sync (replace) all page assignments for a Role
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to sync all page assignments for a role (replace mode)")
public class SyncRolePagesRequest {

    @NotNull(message = "{validation.required}")
    @Valid
    @Schema(description = "List of page assignments (replaces all existing assignments)")
    private List<PageAssignmentDto> assignments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Single page assignment with CRUD permissions")
    public static class PageAssignmentDto {

        @NotNull(message = "{validation.required}")
        @Schema(description = "Page code", example = "USER", required = true)
        private String pageCode;

        @NotNull(message = "{validation.required}")
        @Schema(
            description = "CRUD permissions (VIEW is always added). Use: CREATE, UPDATE, DELETE",
            example = "[\"CREATE\", \"UPDATE\"]"
        )
        private List<String> permissions;
    }
}
