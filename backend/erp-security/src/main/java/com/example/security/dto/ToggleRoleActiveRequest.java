package com.example.security.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for toggling role active status.
 * 
 * Used by: PUT /api/roles/{roleId}/toggle-active
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToggleRoleActiveRequest {
    
    /**
     * The desired active status.
     * true = activate the role
     * false = deactivate the role
     */
    @NotNull(message = "{validation.required}")
    private Boolean active;
    
    /**
     * Getter for active field (needed for boolean property binding)
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}