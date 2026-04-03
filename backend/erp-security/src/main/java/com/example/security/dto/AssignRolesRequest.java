package com.example.security.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

/**
 * طلب ربط مستخدم بأدوار
 * يُستخدم في POST /api/users/{userId}/roles
 */
@Data
public class AssignRolesRequest {
    
    /** قائمة أسماء الأدوار المراد ربطها بالمستخدم (استبدال كامل) */
    @NotEmpty(message = "{validation.not_empty}")
    private Set<String> roleNames;
}
