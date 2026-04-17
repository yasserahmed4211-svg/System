package com.example.security.mapper;

import com.example.security.domain.Permission;
import com.example.security.dto.PermissionDto;
import lombok.experimental.UtilityClass;

/**
 * Mapper for Permission entity to PermissionDto
 * 
 * Complies with Rule 7.2: Centralize mapping in dedicated mapper classes
 */
@UtilityClass
public class PermissionMapper {

    /**
     * Convert Permission entity to PermissionDto
     * 
     * @param entity Permission entity
     * @return PermissionDto safe for JSON serialization
     */
    public static PermissionDto toDto(Permission entity) {
        if (entity == null) {
            return null;
        }

        PermissionDto.PermissionDtoBuilder builder = PermissionDto.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .name(entity.getName())
                .description(null) // Permission entity does not have description field
                .permissionType(entity.getPermissionType() != null ? entity.getPermissionType().name() : null);

        if (entity.getPage() != null) {
            builder.pageId(entity.getPage().getId());
            builder.pageCode(entity.getPage().getPageCode());
        }

        return builder.build();
    }
}
