package com.example.security.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

/**
 * طلب ربط مستخدم بأدوار
 * يُستخدم في POST /api/users/{userId}/roles
 */
@Data
public class AssignRolesRequest {
    
    /** قائمة أسماء الأدوار المراد ربطها بالمستخدم (استبدال كامل) */
    @NotNull(message = "{validation.not_null}")
    private Set<String> roleNames;
}
