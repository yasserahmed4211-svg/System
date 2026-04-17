package com.example.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePermissionRequest {

    @NotBlank(message = "{validation.required}")
    private String name;
}
