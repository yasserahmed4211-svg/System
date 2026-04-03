package com.example.erp.finance.gl.service;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.erp.common.search.*;
import com.example.erp.finance.gl.dto.AccPostingMstResponse;
import com.example.erp.finance.gl.entity.AccPostingMst;
import com.example.erp.finance.gl.exception.GlErrorCodes;
import com.example.erp.finance.gl.mapper.AccPostingMapper;
import com.example.erp.finance.gl.repository.AccPostingMstRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AccPostingService {

    private final AccPostingMstRepository postingMstRepository;
    private final AccPostingMapper postingMapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "postingId", "companyIdFk", "docDate", "status",
            "sourceModule", "sourceDocType", "totalAmount",
            "createdAt", "updatedAt"
    );

    private static final Set<String> ALLOWED_SEARCH_FIELDS = Set.of(
            "status", "sourceModule", "sourceDocType",
            "companyIdFk", "docDate"
    );

    // ==================== GET BY ID ====================

    @PreAuthorize("hasAnyAuthority("
            + "T(com.example.security.constants.SecurityPermissions).GL_POSTING_VIEW, "
            + "T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<AccPostingMstResponse> getById(Long postingId) {
        log.debug("Fetching AccPostingMst ID: {}", postingId);
        AccPostingMst entity = postingMstRepository.findByIdWithDetails(postingId)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND,
                        GlErrorCodes.GL_POSTING_NOT_FOUND, postingId));
        return ServiceResult.success(postingMapper.toResponse(entity));
    }

    // ==================== SEARCH ====================

    @PreAuthorize("hasAnyAuthority("
            + "T(com.example.security.constants.SecurityPermissions).GL_POSTING_VIEW, "
            + "T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<Page<AccPostingMstResponse>> search(SearchRequest searchRequest) {
        log.debug("Searching AccPostingMst");

        Specification<AccPostingMst> spec = SpecBuilder.build(
                searchRequest,
                new SetAllowedFields(ALLOWED_SEARCH_FIELDS),
                DefaultFieldValueConverter.INSTANCE
        );

        Pageable pageable = PageableBuilder.from(searchRequest, ALLOWED_SORT_FIELDS);

        Page<AccPostingMst> postings = postingMstRepository.findAll(spec, pageable);
        return ServiceResult.success(postings.map(postingMapper::toListResponse));
    }
}
