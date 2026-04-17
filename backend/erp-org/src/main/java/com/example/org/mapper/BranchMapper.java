package com.example.org.mapper;

import com.example.org.dto.*;
import com.example.org.entity.OrgBranch;
import com.example.org.entity.OrgDepartment;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class BranchMapper {

    private final DepartmentMapper departmentMapper;

    public BranchMapper(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    public OrgBranch toEntity(BranchCreateRequest request) {
        if (request == null) return null;

        return OrgBranch.builder()
                .branchNameAr(request.getBranchNameAr())
                .branchNameEn(request.getBranchNameEn())
                .branchTypeId(request.getBranchTypeId())
                .isHeadquarter(request.getIsHeadquarter() != null ? request.getIsHeadquarter() : Boolean.FALSE)
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .cityName(request.getCityName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .build();
    }

    public void updateEntityFromRequest(OrgBranch entity, BranchUpdateRequest request) {
        if (entity == null || request == null) return;

        entity.setBranchNameAr(request.getBranchNameAr());
        entity.setBranchNameEn(request.getBranchNameEn());
        entity.setBranchTypeId(request.getBranchTypeId());
        if (request.getIsHeadquarter() != null) {
            entity.setIsHeadquarter(request.getIsHeadquarter());
        }
        entity.setAddressLine1(request.getAddressLine1());
        entity.setAddressLine2(request.getAddressLine2());
        entity.setCityName(request.getCityName());
        entity.setPhone(request.getPhone());
        entity.setEmail(request.getEmail());
    }

    public BranchResponse toResponse(OrgBranch entity) {
        if (entity == null) return null;

        List<DepartmentResponse> departmentResponses = null;
        if (entity.getDepartments() != null && !entity.getDepartments().isEmpty()) {
            departmentResponses = entity.getDepartments().stream()
                    .map(departmentMapper::toResponse)
                    .toList();
        }

        return BranchResponse.builder()
                .id(entity.getId())
                .branchCode(entity.getBranchCode())
                .branchNameAr(entity.getBranchNameAr())
                .branchNameEn(entity.getBranchNameEn())
                .branchTypeId(entity.getBranchTypeId())
                .isHeadquarter(Boolean.TRUE.equals(entity.getIsHeadquarter()))
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())
                .cityName(entity.getCityName())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .statusId(entity.getStatusId())
                .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .legalEntityId(entity.getLegalEntity() != null ? entity.getLegalEntity().getId() : null)
                .legalEntityDisplay(entity.getLegalEntity() != null ? entity.getLegalEntity().getLegalEntityNameAr() : null)
                .regionId(entity.getRegion() != null ? entity.getRegion().getId() : null)
                .regionDisplay(entity.getRegion() != null ? entity.getRegion().getRegionNameAr() : null)
                .departmentCount(entity.getDepartmentCount())
                .departments(departmentResponses)
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public BranchResponse toResponseWithoutDepartments(OrgBranch entity) {
        if (entity == null) return null;

        return BranchResponse.builder()
                .id(entity.getId())
                .branchCode(entity.getBranchCode())
                .branchNameAr(entity.getBranchNameAr())
                .branchNameEn(entity.getBranchNameEn())
                .branchTypeId(entity.getBranchTypeId())
                .isHeadquarter(Boolean.TRUE.equals(entity.getIsHeadquarter()))
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())
                .cityName(entity.getCityName())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .statusId(entity.getStatusId())
                .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .legalEntityId(entity.getLegalEntity() != null ? entity.getLegalEntity().getId() : null)
                .legalEntityDisplay(entity.getLegalEntity() != null ? entity.getLegalEntity().getLegalEntityNameAr() : null)
                .regionId(entity.getRegion() != null ? entity.getRegion().getId() : null)
                .regionDisplay(entity.getRegion() != null ? entity.getRegion().getRegionNameAr() : null)
                .departmentCount(entity.getDepartmentCount())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public BranchUsageResponse toUsageResponse(OrgBranch entity, long activeDepartments) {
        if (entity == null) return null;

        boolean canDeactivate = activeDepartments == 0;
        String reason = canDeactivate ? null : "الفرع يحتوي على أقسام نشطة";

        return BranchUsageResponse.builder()
                .branchId(entity.getId())
                .branchCode(entity.getBranchCode())
                .activeDepartments(activeDepartments)
                .canDeactivate(canDeactivate)
                .reason(reason)
                .build();
    }

    public BranchOptionResponse toOptionResponse(OrgBranch entity) {
        if (entity == null) return null;

        return BranchOptionResponse.builder()
                .id(entity.getId())
                .branchCode(entity.getBranchCode())
                .branchNameAr(entity.getBranchNameAr())
                .branchNameEn(entity.getBranchNameEn())
                .build();
    }
}
