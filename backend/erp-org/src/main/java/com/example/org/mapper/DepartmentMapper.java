package com.example.org.mapper;

import com.example.org.dto.*;
import com.example.org.entity.OrgDepartment;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    public OrgDepartment toEntity(DepartmentCreateRequest request) {
        if (request == null) return null;

        return OrgDepartment.builder()
                .departmentNameAr(request.getDepartmentNameAr())
                .departmentNameEn(request.getDepartmentNameEn())
                .departmentTypeId(request.getDepartmentTypeId())
                .build();
    }

    public OrgDepartment toEntityFromInline(DepartmentInlineRequest request) {
        if (request == null) return null;

        return OrgDepartment.builder()
                .departmentNameAr(request.getDepartmentNameAr())
                .departmentNameEn(request.getDepartmentNameEn())
                .departmentTypeId(request.getDepartmentTypeId())
                .build();
    }

    public void updateEntityFromRequest(OrgDepartment entity, DepartmentUpdateRequest request) {
        if (entity == null || request == null) return;

        entity.setDepartmentNameAr(request.getDepartmentNameAr());
        entity.setDepartmentNameEn(request.getDepartmentNameEn());
        entity.setDepartmentTypeId(request.getDepartmentTypeId());
    }

    public DepartmentResponse toResponse(OrgDepartment entity) {
        if (entity == null) return null;

        return DepartmentResponse.builder()
                .id(entity.getId())
                .departmentCode(entity.getDepartmentCode())
                .departmentNameAr(entity.getDepartmentNameAr())
                .departmentNameEn(entity.getDepartmentNameEn())
                .departmentTypeId(entity.getDepartmentTypeId())
                .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .branchId(entity.getBranch() != null ? entity.getBranch().getId() : null)
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
