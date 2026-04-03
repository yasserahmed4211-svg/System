package com.example.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePermissionRequest {
    // اسم الصلاحية (مثال: PERM_ROLE_VIEW)
    @NotBlank(message = "{validation.required}") 
    private String name;
}
