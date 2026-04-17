package com.example.org.service;

import com.erp.common.search.*;
import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.example.org.dto.*;
import com.example.org.entity.OrgBranch;
import com.example.org.entity.OrgDepartment;
import com.example.org.entity.OrgLegalEntity;
import com.example.org.entity.OrgRegion;
import com.example.org.exception.OrgErrorCodes;
import com.example.org.mapper.BranchMapper;
import com.example.org.mapper.DepartmentMapper;
import com.example.org.repository.OrgBranchRepository;
import com.example.org.repository.OrgDepartmentRepository;
import com.example.org.repository.OrgLegalEntityRepository;
import com.example.org.repository.OrgRegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchService {

    private final OrgBranchRepository branchRepository;
    private final OrgLegalEntityRepository legalEntityRepository;
    private final OrgRegionRepository regionRepository;
    private final OrgDepartmentRepository departmentRepository;
    private final BranchMapper branchMapper;
    private final DepartmentMapper departmentMapper;
    private final OrgCodeGenerationService codeGenerationService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "branchCode", "branchNameAr", "branchNameEn",
            "branchTypeId", "isHeadquarter", "statusId", "isActive",
            "createdAt", "updatedAt"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // ==================== Branch CRUD ====================

    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).BRANCH_CREATE)")
    public ServiceResult<BranchResponse> create(BranchCreateRequest request) {
        log.info("Creating branch: {}", request.getBranchNameEn());

        // RULE-BR-03: Verify legal entity is active
        OrgLegalEntity legalEntity = legalEntityRepository.findById(request.getLegalEntityId())
                .orElseThrow(() -> new LocalizedException(
                        Status.NOT_FOUND,
                        OrgErrorCodes.LEGAL_ENTITY_NOT_FOUND,
                        request.getLegalEntityId()
                ));

        if (!Boolean.TRUE.equals(legalEntity.getIsActive())) {
            throw new LocalizedException(
                    Status.BUSINESS_RULE_VIOLATION,
                    OrgErrorCodes.BRANCH_INACTIVE_LEGAL_ENTITY
            );
        }

        // RULE-BR-04: Verify region belongs to same legal entity (if region provided)
        OrgRegion region = null;
        if (request.getRegionId() != null) {
            region = regionRepository.findById(request.getRegionId())
                    .orElseThrow(() -> new LocalizedException(
                            Status.NOT_FOUND,
                            OrgErrorCodes.REGION_NOT_FOUND,
                            request.getRegionId()
                    ));
            if (!region.getLegalEntity().getId().equals(request.getLegalEntityId())) {
                throw new LocalizedException(
                        Status.BUSINESS_RULE_VIOLATION,
                        OrgErrorCodes.BRANCH_REGION_MISMATCH
                );
            }
        }

        // RULE-BR-05: Only one HQ per legal entity
        if (Boolean.TRUE.equals(request.getIsHeadquarter())) {
            long existingHQ = branchRepository.countActiveHeadquarters(request.getLegalEntityId());
            if (existingHQ > 0) {
                throw new LocalizedException(
                        Status.CONFLICT,
                        OrgErrorCodes.BRANCH_HQ_EXISTS
                );
            }
        }

        // RULE-BR-06: Validate email
        validateEmail(request.getEmail());

        // RULE-BR-08: Atomic save — branch + departments (single transaction)
        OrgBranch entity = branchMapper.toEntity(request);

        // RULE-BR-01: Auto-generate branch code via centralized sequence service (B4.1)
        entity.setBranchCode(codeGenerationService.generateBranchCode());
        entity.setLegalEntity(legalEntity);
        entity.setRegion(region);

        // Save branch first to get ID
        OrgBranch savedBranch = branchRepository.save(entity);

        // Create inline departments if provided
        if (request.getDepartments() != null && !request.getDepartments().isEmpty()) {
            List<OrgDepartment> departments = new ArrayList<>();
            for (DepartmentInlineRequest deptReq : request.getDepartments()) {
                OrgDepartment dept = departmentMapper.toEntityFromInline(deptReq);
                dept.setDepartmentCode(codeGenerationService.generateDepartmentCode());
                dept.setBranch(savedBranch);
                departments.add(dept);
            }
            try {
                departmentRepository.saveAll(departments);
            } catch (Exception e) {
                log.error("Partial save failed — could not save departments for branch ID: {}", savedBranch.getId(), e);
                throw new LocalizedException(
                        Status.BUSINESS_RULE_VIOLATION,
                        OrgErrorCodes.PARTIAL_SAVE_FAILED
                );
            }
            savedBranch.setDepartments(departments);
        }

        log.info("Branch created with ID: {}", savedBranch.getId());
        return ServiceResult.success(branchMapper.toResponse(savedBranch), Status.CREATED);
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).BRANCH_UPDATE)")
    public ServiceResult<BranchResponse> update(Long id, BranchUpdateRequest request) {
        log.info("Updating branch ID: {}", id);

        OrgBranch entity = branchRepository.findById(id)
                .orElseThrow(() -> new LocalizedException(
                        Status.NOT_FOUND,
                        OrgErrorCodes.BRANCH_NOT_FOUND,
                        id
                ));

        // RULE-BR-07: legalEntityFk is NOT editable after save

        // RULE-BR-04: Verify region belongs to same legal entity (if region changed)
        if (request.getRegionId() != null) {
            OrgRegion region = regionRepository.findById(request.getRegionId())
                    .orElseThrow(() -> new LocalizedException(
                            Status.NOT_FOUND,
                            OrgErrorCodes.REGION_NOT_FOUND,
                            request.getRegionId()
                    ));
            if (!region.getLegalEntity().getId().equals(entity.getLegalEntity().getId())) {
                throw new LocalizedException(
                        Status.BUSINESS_RULE_VIOLATION,
                        OrgErrorCodes.BRANCH_REGION_MISMATCH
                );
            }
            entity.setRegion(region);
        } else {
            entity.setRegion(null);
        }

        // RULE-BR-05: Only one HQ per legal entity
        if (Boolean.TRUE.equals(request.getIsHeadquarter())) {
            long existingHQ = branchRepository.countActiveHeadquartersExcluding(
                    entity.getLegalEntity().getId(), id);
            if (existingHQ > 0) {
                throw new LocalizedException(
                        Status.CONFLICT,
                        OrgErrorCodes.BRANCH_HQ_EXISTS
                );
            }
        }

        // RULE-BR-06: Validate email
        validateEmail(request.getEmail());

        branchMapper.updateEntityFromRequest(entity, request);

        // RULE-BR-08: Atomic update of departments
        if (request.getDepartments() != null) {
            syncDepartments(entity, request.getDepartments());
        }

        OrgBranch updated = branchRepository.save(entity);

        log.info("Branch updated: {}", id);
        return ServiceResult.success(branchMapper.toResponse(updated), Status.UPDATED);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).BRANCH_VIEW)")
    public ServiceResult<BranchResponse> getById(Long id) {
        log.debug("Getting branch by ID: {}", id);

        OrgBranch entity = branchRepository.findById(id)
                .orElseThrow(() -> new LocalizedException(
                        Status.NOT_FOUND,
                        OrgErrorCodes.BRANCH_NOT_FOUND,
                        id
                ));

        return ServiceResult.success(branchMapper.toResponse(entity));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).BRANCH_VIEW)")
    public ServiceResult<Page<BranchResponse>> list(
            String branchCode, String branchNameAr, Long legalEntityFk,
            Long regionFk, String branchTypeId, String statusId,
            int page, int pageSize) {
        log.debug("Listing branches");

        Pageable pageable = PageRequest.of(page, Math.min(pageSize, 100), Sort.by("id").ascending());

        Specification<OrgBranch> spec = (root, query, cb) -> cb.conjunction();
        if (branchCode != null && !branchCode.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.like(
                    cb.upper(root.get("branchCode")),
                    "%" + branchCode.trim().toUpperCase() + "%"));
        }
        if (branchNameAr != null && !branchNameAr.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.like(
                    root.get("branchNameAr"),
                    "%" + branchNameAr.trim() + "%"));
        }
        if (legalEntityFk != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("legalEntity").get("id"), legalEntityFk));
        }
        if (regionFk != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("region").get("id"), regionFk));
        }
        if (branchTypeId != null && !branchTypeId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(
                    root.get("branchTypeId"), branchTypeId.trim().toUpperCase()));
        }
        if (statusId != null && !statusId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(
                    root.get("statusId"), statusId.trim().toUpperCase()));
        }

        Page<OrgBranch> result = branchRepository.findAll(spec, pageable);
        return ServiceResult.success(result.map(branchMapper::toResponseWithoutDepartments));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).BRANCH_VIEW)")
    public ServiceResult<Page<BranchResponse>> search(SearchRequest searchRequest) {
        log.debug("Searching branches");

        AllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);
        FieldValueConverter converter = DefaultFieldValueConverter.INSTANCE;

        Specification<OrgBranch> spec = SpecBuilder.build(searchRequest, allowedFields, converter);
        Pageable pageable = PageableBuilder.from(searchRequest, ALLOWED_SORT_FIELDS);

        Page<OrgBranch> page = branchRepository.findAll(spec, pageable);

        return ServiceResult.success(page.map(branchMapper::toResponseWithoutDepartments));
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).BRANCH_UPDATE)")
    public ServiceResult<BranchResponse> deactivate(Long id) {
        return toggleActive(id, false);
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).BRANCH_UPDATE)")
    public ServiceResult<BranchResponse> toggleActive(Long id, Boolean active) {
        log.info("Toggling branch ID: {} to active={}", id, active);

        OrgBranch entity = branchRepository.findById(id)
                .orElseThrow(() -> new LocalizedException(
                        Status.NOT_FOUND,
                        OrgErrorCodes.BRANCH_NOT_FOUND,
                        id
                ));

        // B3.2: Re-activation is NOT supported (no INACTIVE → ACTIVE transition)
        if (Boolean.TRUE.equals(active)) {
            throw new LocalizedException(
                    Status.BUSINESS_RULE_VIOLATION,
                    OrgErrorCodes.BRANCH_REACTIVATION_NOT_ALLOWED
            );
        }

        // B3.4: Cannot deactivate if already inactive
        if (!Boolean.TRUE.equals(entity.getIsActive())) {
            throw new LocalizedException(
                    Status.BUSINESS_RULE_VIOLATION,
                    OrgErrorCodes.BRANCH_ALREADY_INACTIVE
            );
        }

        // RULE-BR-10: Cannot deactivate last active HQ
        if (Boolean.TRUE.equals(entity.getIsHeadquarter())) {
            long otherActiveHQ = branchRepository.countActiveHeadquartersExcluding(
                    entity.getLegalEntity().getId(), id);
            if (otherActiveHQ == 0) {
                throw new LocalizedException(
                        Status.CONFLICT,
                        OrgErrorCodes.BRANCH_LAST_ACTIVE_HQ
                );
            }
        }

        // RULE-BR-11: Cannot deactivate with active users
        // TODO: Check user module integration when available

        // RULE-BR-12: Cannot deactivate with open financial transactions
        // TODO: Check financial module integration when available

        entity.deactivate();

        OrgBranch updated = branchRepository.save(entity);

        log.info("Branch {} deactivated", id);
        return ServiceResult.success(branchMapper.toResponse(updated), Status.UPDATED);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).BRANCH_VIEW)")
    public ServiceResult<BranchUsageResponse> getUsage(Long id) {
        log.debug("Getting usage for branch ID: {}", id);

        OrgBranch entity = branchRepository.findById(id)
                .orElseThrow(() -> new LocalizedException(
                        Status.NOT_FOUND,
                        OrgErrorCodes.BRANCH_NOT_FOUND,
                        id
                ));

        long activeDepartments = branchRepository.countActiveDepartments(id);

        return ServiceResult.success(branchMapper.toUsageResponse(entity, activeDepartments));
    }

    // ==================== Department Operations ====================

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).BRANCH_VIEW)")
    public ServiceResult<List<DepartmentResponse>> getDepartmentsByBranch(Long branchId) {
        log.debug("Getting departments for branch ID: {}", branchId);

        branchRepository.findById(branchId)
                .orElseThrow(() -> new LocalizedException(
                        Status.NOT_FOUND,
                        OrgErrorCodes.BRANCH_NOT_FOUND,
                        branchId
                ));

        List<OrgDepartment> departments = departmentRepository.findByBranchId(branchId);
        return ServiceResult.success(departments.stream()
                .map(departmentMapper::toResponse)
                .toList());
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).DEPARTMENT_UPDATE)")
    public ServiceResult<DepartmentResponse> deactivateDepartment(Long id) {
        return toggleDepartmentActive(id, false);
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).DEPARTMENT_CREATE)")
    public ServiceResult<DepartmentResponse> createDepartment(Long branchId, DepartmentCreateRequest request) {
        log.info("Creating department for branch ID: {}", branchId);

        OrgBranch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new LocalizedException(
                        Status.NOT_FOUND,
                        OrgErrorCodes.BRANCH_NOT_FOUND,
                        branchId
                ));

        // B3.4: Cannot create department under inactive branch
        if (!Boolean.TRUE.equals(branch.getIsActive())) {
            throw new LocalizedException(
                    Status.BUSINESS_RULE_VIOLATION,
                    OrgErrorCodes.BRANCH_INACTIVE_FOR_DEPARTMENT
            );
        }

        OrgDepartment entity = departmentMapper.toEntity(request);
        // RULE-DP-01: Auto-generate code via centralized sequence service (B4.1)
        entity.setDepartmentCode(codeGenerationService.generateDepartmentCode());
        // RULE-DP-03: Link to branch
        entity.setBranch(branch);

        OrgDepartment saved = departmentRepository.save(entity);

        log.info("Department created with ID: {}", saved.getId());
        return ServiceResult.success(departmentMapper.toResponse(saved), Status.CREATED);
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).DEPARTMENT_UPDATE)")
    public ServiceResult<DepartmentResponse> updateDepartment(Long id, DepartmentUpdateRequest request) {
        log.info("Updating department ID: {}", id);

        OrgDepartment entity = departmentRepository.findById(id)
                .orElseThrow(() -> new LocalizedException(
                        Status.NOT_FOUND,
                        OrgErrorCodes.DEPARTMENT_NOT_FOUND,
                        id
                ));

        departmentMapper.updateEntityFromRequest(entity, request);

        OrgDepartment updated = departmentRepository.save(entity);

        log.info("Department updated: {}", id);
        return ServiceResult.success(departmentMapper.toResponse(updated), Status.UPDATED);
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).DEPARTMENT_UPDATE)")
    public ServiceResult<DepartmentResponse> toggleDepartmentActive(Long id, Boolean active) {
        log.info("Toggling department ID: {} to active={}", id, active);

        OrgDepartment entity = departmentRepository.findById(id)
                .orElseThrow(() -> new LocalizedException(
                        Status.NOT_FOUND,
                        OrgErrorCodes.DEPARTMENT_NOT_FOUND,
                        id
                ));

        // B3.2: Re-activation is NOT supported (no INACTIVE → ACTIVE transition)
        if (Boolean.TRUE.equals(active)) {
            throw new LocalizedException(
                    Status.BUSINESS_RULE_VIOLATION,
                    OrgErrorCodes.DEPARTMENT_REACTIVATION_NOT_ALLOWED
            );
        }

        // B3.4: Cannot deactivate if already inactive
        if (!Boolean.TRUE.equals(entity.getIsActive())) {
            throw new LocalizedException(
                    Status.BUSINESS_RULE_VIOLATION,
                    OrgErrorCodes.DEPARTMENT_ALREADY_INACTIVE
            );
        }

        // RULE-DP-04: Cannot deactivate with active users
        // TODO: Check user module integration when available

        // RULE-DP-05: Cannot deactivate with linked records
        // TODO: Check operational records integration when available

        entity.deactivate();

        OrgDepartment updated = departmentRepository.save(entity);

        log.info("Department {} deactivated", id);
        return ServiceResult.success(departmentMapper.toResponse(updated), Status.UPDATED);
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).DEPARTMENT_DELETE)")
    public void deleteDepartment(Long id) {
        log.info("Delete requested for department ID: {}", id);

        // B3.2: Hard deletion is NOT permitted for any entity
        throw new LocalizedException(
                Status.BUSINESS_RULE_VIOLATION,
                OrgErrorCodes.DEPARTMENT_HARD_DELETE_NOT_ALLOWED
        );
    }

    // ==================== Helpers ====================

    private void syncDepartments(OrgBranch branch, List<DepartmentInlineRequest> departmentRequests) {
        // Get existing departments
        List<OrgDepartment> existingDepartments = departmentRepository.findByBranchId(branch.getId());

        // Process each inline request
        List<OrgDepartment> toSave = new ArrayList<>();
        List<Long> processedIds = new ArrayList<>();

        for (DepartmentInlineRequest deptReq : departmentRequests) {
            if (deptReq.getId() != null) {
                // Update existing
                OrgDepartment existing = existingDepartments.stream()
                        .filter(d -> d.getId().equals(deptReq.getId()))
                        .findFirst()
                        .orElseThrow(() -> new LocalizedException(
                                Status.NOT_FOUND,
                                OrgErrorCodes.DEPARTMENT_NOT_FOUND,
                                deptReq.getId()
                        ));
                existing.setDepartmentNameAr(deptReq.getDepartmentNameAr());
                existing.setDepartmentNameEn(deptReq.getDepartmentNameEn());
                existing.setDepartmentTypeId(deptReq.getDepartmentTypeId());
                toSave.add(existing);
                processedIds.add(existing.getId());
            } else {
                // Create new
                OrgDepartment newDept = departmentMapper.toEntityFromInline(deptReq);
                newDept.setDepartmentCode(codeGenerationService.generateDepartmentCode());
                newDept.setBranch(branch);
                toSave.add(newDept);
            }
        }

        // Departments not in the request list are left untouched (no auto-delete)
        departmentRepository.saveAll(toSave);
    }

    private void validateEmail(String email) {
        if (email != null && !email.isBlank() && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new LocalizedException(
                    Status.VALIDATION_ERROR,
                    OrgErrorCodes.BRANCH_INVALID_EMAIL
            );
        }
    }
}
