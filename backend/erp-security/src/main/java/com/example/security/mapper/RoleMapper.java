package com.example.security.mapper;

import com.example.security.domain.Role;
import com.example.security.dto.RoleDto;
import lombok.experimental.UtilityClass;

/**
 * Mapper for Role entity to RoleDto
 * 
 * Governance: BE-REQ-ROLEACCESS-001
 * Contract: role-access.contract.md
 * 
 * Complies with Rule 7.2: Centralize mapping in dedicated mapper classes
 */
@UtilityClass
public class RoleMapper {

    /**
     * Convert Role entity to RoleDto (safe for JSON serialization)
     * 
     * @param entity Role entity
     * @return RoleDto without lazy-loaded collections
     */
    public static RoleDto toDto(Role entity) {
        if (entity == null) {
            return null;
        }

        return RoleDto.builder()
                .id(entity.getId())
            .roleCode(entity.getRoleName())
                .roleName(entity.getRoleName())
            .description(entity.getDescription())
            .active(entity.getActive())
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
