package com.example.erp.finance.gl.service;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.erp.common.search.*;
import com.example.erp.finance.gl.dto.*;
import com.example.erp.finance.gl.entity.AccountsChart;
import com.example.erp.finance.gl.exception.GlErrorCodes;
import com.example.erp.finance.gl.mapper.AccountsChartMapper;
import com.example.erp.finance.gl.repository.AccountsChartRepository;
import com.example.erp.finance.gl.repository.AccRuleHdrRepository;
import com.example.erp.finance.gl.util.GlAccountTypeNormalizer;
import com.example.masterdata.api.LookupValidationApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer for Chart of Accounts management.
 * Integrates hierarchical auto-numbering and tree structure validation.
 *
 * <h3>Key Responsibilities:</h3>
 * <ul>
 *   <li>Auto-generate ACCOUNT_CHART_NO based on hierarchy</li>
 *   <li>Validate tree integrity (no circular refs, self-refs, descendant-as-parent)</li>
 *   <li>Prevent account type changes when children exist</li>
 *   <li>Soft-delete with child dependency checks</li>
 *   <li>Provide eligible parent accounts for LOV selection</li>
 * </ul>
 *
 * @author ERP Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AccountsChartService {

    private final AccountsChartRepository accountsChartRepository;
    private final AccRuleHdrRepository accRuleHdrRepository;
    private final AccountsChartMapper accountsChartMapper;
    private final AccountChartNumberGenerator numberGenerator;
    private final AccountChartTreeValidator treeValidator;
    private final LookupValidationApi lookupValidationApi;

    private static final String LK_GL_ACCOUNT_TYPE = "GL_ACCOUNT_TYPE";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "accountChartPk", "accountChartNo", "accountChartName", "accountType",
        "isActive", "organizationFk", "createdAt", "updatedAt"
    );

    private static final Set<String> ALLOWED_SEARCH_FIELDS = Set.of(
        "accountChartNo", "accountChartName", "accountType",
        "isActive", "organizationFk", "parent.accountChartPk"
    );

    // ==================== CREATE ====================

    @Transactional
    @CacheEvict(cacheNames = "accountsChart", allEntries = true)
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_ACCOUNT_CREATE, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<AccountsChartResponse> create(AccountsChartCreateRequest request) {

        String normalizedAccountType = GlAccountTypeNormalizer.normalize(request.getAccountType());

        // Validate account type against lookup
        lookupValidationApi.validateOrThrow(LK_GL_ACCOUNT_TYPE, normalizedAccountType);

        AccountsChart entity = accountsChartMapper.toEntity(request);
        entity.setAccountType(normalizedAccountType);

        // Determine parent and generate account number
        if (request.getAccountChartFk() != null) {
            // --- CHILD ACCOUNT ---
            AccountsChart parent = treeValidator.validateParentExists(request.getAccountChartFk());

            // Child account type must match parent account type
                treeValidator.validateTypeMatchesParent(
                    normalizedAccountType,
                    GlAccountTypeNormalizer.normalize(parent.getAccountType()));

            entity.setParent(parent);

            // Validate parent has a code (defensive check for legacy data)
            if (parent.getAccountChartNo() == null || parent.getAccountChartNo().isBlank()) {
                throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                        GlErrorCodes.GL_ACCOUNT_NO_GENERATION_FAILED,
                        "Parent account (PK=" + parent.getAccountChartPk() + ") has no account number");
            }

            // Auto-generate child account number
            String accountNo = numberGenerator.generateChildAccountNo(
                    parent.getAccountChartNo(), parent.getAccountChartPk(),
                    request.getOrganizationFk());
            entity.setAccountChartNo(accountNo);
        } else {
            // --- ROOT ACCOUNT ---
            String accountNo = numberGenerator.generateRootAccountNo(
                    request.getOrganizationFk(), normalizedAccountType);
            entity.setAccountChartNo(accountNo);
        }

        log.debug("Generated account number: {} for org={}", entity.getAccountChartNo(), entity.getOrganizationFk());

        // Final uniqueness check (defensive — constraint also enforces this)
        if (accountsChartRepository.existsByAccountChartNoAndOrganizationFk(
                entity.getAccountChartNo(), entity.getOrganizationFk())) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                    GlErrorCodes.GL_DUPLICATE_ACCOUNT_CODE, entity.getAccountChartNo());
        }

        AccountsChart saved = accountsChartRepository.save(entity);
        log.info("Account created: pk={}, code={}, org={}, parent={}",
                saved.getAccountChartPk(), saved.getAccountChartNo(),
                saved.getOrganizationFk(),
                saved.getParent() != null ? saved.getParent().getAccountChartPk() : "ROOT");
        return ServiceResult.success(accountsChartMapper.toResponse(saved), Status.CREATED);
    }

    // ==================== UPDATE ====================

    @Transactional
    @CacheEvict(cacheNames = "accountsChart", allEntries = true)
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_ACCOUNT_UPDATE, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<AccountsChartResponse> update(Long accountChartPk, AccountsChartUpdateRequest request) {

        AccountsChart entity = findAccountOrThrow(accountChartPk);
        String normalizedAccountType = GlAccountTypeNormalizer.normalize(request.getAccountType());

        // Validate account type
        lookupValidationApi.validateOrThrow(LK_GL_ACCOUNT_TYPE, normalizedAccountType);

        // organizationFk is immutable
        if (!entity.getOrganizationFk().equals(request.getOrganizationFk())) {
            throw new LocalizedException(Status.CONFLICT,
                    GlErrorCodes.GL_ACCOUNT_ORG_LOCKED, accountChartPk);
        }

        // Prevent account type change if children exist
        treeValidator.validateAccountTypeChange(
            accountChartPk,
            GlAccountTypeNormalizer.normalize(entity.getAccountType()),
            normalizedAccountType);

        // Parent validation
        if (request.getAccountChartFk() != null) {
            // Prevent self-reference
            treeValidator.validateNoSelfReference(accountChartPk, request.getAccountChartFk());

            AccountsChart newParent = treeValidator.validateParentExists(request.getAccountChartFk());

            // Prevent circular reference and descendant-as-parent
            treeValidator.validateNoCircularReference(entity, newParent);

            // Child type must match parent type
                treeValidator.validateTypeMatchesParent(
                    normalizedAccountType,
                    GlAccountTypeNormalizer.normalize(newParent.getAccountType()));

            entity.setParent(newParent);
        } else {
            entity.setParent(null);
        }

        // Note: accountChartNo is NOT updated — it is auto-generated and immutable
        accountsChartMapper.updateEntity(entity, request);
        entity.setAccountType(normalizedAccountType);
        AccountsChart updated = accountsChartRepository.save(entity);
        log.info("Account updated: pk={}, code={}", accountChartPk, updated.getAccountChartNo());
        return ServiceResult.success(accountsChartMapper.toResponse(updated), Status.UPDATED);
    }

    // ==================== DEACTIVATE (Soft Delete) ====================

    @Transactional
    @CacheEvict(cacheNames = "accountsChart", allEntries = true)
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_ACCOUNT_DELETE, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public void deactivate(Long accountChartPk) {
        AccountsChart entity = findAccountOrThrow(accountChartPk);

        // Cannot deactivate if has active children
        long activeChildren = accountsChartRepository.countActiveChildrenByParentPk(accountChartPk);
        if (activeChildren > 0) {
            throw new LocalizedException(Status.CONFLICT,
                    GlErrorCodes.GL_ACCOUNT_HAS_CHILDREN, accountChartPk, activeChildren);
        }

        // Cannot deactivate if used in active accounting rules
        if (accRuleHdrRepository.isAccountUsedInActiveRules(accountChartPk)) {
            throw new LocalizedException(Status.CONFLICT,
                    GlErrorCodes.GL_ACCOUNT_IN_ACTIVE_RULE, accountChartPk);
        }

        // Placeholder for balance check
        log.debug("Balance check for deactivation of account pk={} — placeholder (no balance table yet)", accountChartPk);

        entity.setIsActive(Boolean.FALSE);
        accountsChartRepository.save(entity);
        log.info("Account deactivated: pk={}", accountChartPk);
    }

    // ==================== GET BY ID ====================

    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_ACCOUNT_VIEW, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<AccountsChartResponse> getById(Long accountChartPk) {
        AccountsChart entity = accountsChartRepository.findByIdWithParent(accountChartPk)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, GlErrorCodes.GL_ACCOUNT_NOT_FOUND, accountChartPk));
        return ServiceResult.success(accountsChartMapper.toResponse(entity));
    }

    // ==================== SEARCH (paginated) ====================

    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_ACCOUNT_VIEW, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<Page<AccountsChartResponse>> search(SearchRequest searchRequest) {

        Specification<AccountsChart> spec = SpecBuilder.build(
            searchRequest,
            new SetAllowedFields(ALLOWED_SEARCH_FIELDS),
            DefaultFieldValueConverter.INSTANCE
        );

        Pageable pageable = PageableBuilder.from(searchRequest, ALLOWED_SORT_FIELDS);

        Page<AccountsChart> accounts = accountsChartRepository.findAll(spec, pageable);
        return ServiceResult.success(accounts.map(accountsChartMapper::toResponse));
    }

    // ==================== TREE VIEW ====================

    /**
     * Retrieves the full hierarchical tree for a given organization.
     * Uses recursive DTO building for unlimited depth support.
     * Optionally filters by account type.
     */
    @Cacheable(cacheNames = "accountsChart", key = "'tree:' + #organizationFk + ':' + #accountType")
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_ACCOUNT_VIEW, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<List<AccountsChartTreeNode>> getTree(Long organizationFk, String accountType) {

        String normalizedAccountType = GlAccountTypeNormalizer.normalize(accountType);

        // Single query to fetch ALL accounts (flat list) — avoids N+1
        List<AccountsChart> allAccounts;
        if (organizationFk != null) {
            allAccounts = accountsChartRepository.findAllForTreeByOrganization(organizationFk);
        } else {
            allAccounts = accountsChartRepository.findAllForTree();
        }

        // Filter by canonical account type in-memory to support mixed legacy data.
        if (normalizedAccountType != null && !normalizedAccountType.isBlank()) {
            allAccounts = allAccounts.stream()
                    .filter(a -> normalizedAccountType.equals(GlAccountTypeNormalizer.normalize(a.getAccountType())))
                    .collect(Collectors.toList());
        }

        // Build parent→children map in memory
        Map<Long, List<AccountsChart>> childrenMap = allAccounts.stream()
                .filter(a -> a.getParent() != null)
                .collect(Collectors.groupingBy(a -> a.getParent().getAccountChartPk()));

        // Filter roots and build tree from the in-memory map
        return ServiceResult.success(allAccounts.stream()
                .filter(a -> a.getParent() == null)
                .sorted(Comparator.comparing(AccountsChart::getAccountChartNo))
                .map(root -> buildTreeFromMap(root, childrenMap, 0))
                .collect(Collectors.toList()));
    }

    /**
     * Retrieves the subtree starting from a given account (useful for lazy-loading trees).
     */
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_ACCOUNT_VIEW, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<AccountsChartTreeNode> getSubTree(Long accountChartPk) {
        AccountsChart root = accountsChartRepository.findByIdWithChildren(accountChartPk)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, GlErrorCodes.GL_ACCOUNT_NOT_FOUND, accountChartPk));

        // Fetch all accounts for the same organization to build subtree without N+1
        List<AccountsChart> allAccounts = accountsChartRepository
                .findAllForTreeByOrganization(root.getOrganizationFk());

        Map<Long, List<AccountsChart>> childrenMap = allAccounts.stream()
                .filter(a -> a.getParent() != null)
                .collect(Collectors.groupingBy(a -> a.getParent().getAccountChartPk()));

        return ServiceResult.success(buildTreeFromMap(root, childrenMap, 0));
    }

    // ==================== ELIGIBLE PARENTS LOV ====================

    /**
     * Returns a paginated list of accounts eligible to be used as parents.
     * Excludes the given account and all its descendants (to prevent circular references).
     * Only returns active accounts for the given organization.
     *
     * @param excludeAccountPk the account PK to exclude (self + descendants), null for create mode
     * @param organizationFk   the organization to scope accounts
     * @param search           optional search string (matches accountChartNo or accountChartName)
     * @param page             page number (0-based)
     * @param size             page size
     * @return paginated list of eligible parent accounts
     */
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_ACCOUNT_VIEW, T(com.example.security.constants.SecurityPermissions).GL_ACCOUNT_CREATE, T(com.example.security.constants.SecurityPermissions).GL_ACCOUNT_UPDATE, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<Page<EligibleParentAccountDto>> getEligibleParents(
            Long excludeAccountPk, Long organizationFk, String search, int page, int size) {

        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);

        // Collect PKs to exclude: self + all descendants
        Set<Long> excludePks = new HashSet<>();
        if (excludeAccountPk != null) {
            excludePks.add(excludeAccountPk);
            collectDescendantPks(excludeAccountPk, excludePks);
        }

        // Use a sentinel value if excludePks is empty (JPA IN clause requires non-empty collection)
        if (excludePks.isEmpty()) {
            excludePks.add(-1L);
        }

        Page<AccountsChart> accounts;
        if (search != null && !search.isBlank()) {
            String searchPattern = "%" + search.trim() + "%";
            accounts = accountsChartRepository.findEligibleParents(organizationFk, excludePks, searchPattern, pageable);
        } else {
            accounts = accountsChartRepository.findEligibleParentsNoSearch(organizationFk, excludePks, pageable);
        }

        return ServiceResult.success(accounts.map(this::toEligibleParentDto));
    }

    private EligibleParentAccountDto toEligibleParentDto(AccountsChart entity) {
        return EligibleParentAccountDto.builder()
                .accountChartPk(entity.getAccountChartPk())
                .accountChartNo(entity.getAccountChartNo())
                .accountChartName(entity.getAccountChartName())
                .accountType(entity.getAccountType())
                .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .build();
    }

    /**
     * Collect all descendant PKs for the given account using a single DB query
     * and in-memory traversal (avoids N+1 recursive queries).
     */
    private void collectDescendantPks(Long parentPk, Set<Long> collector) {
        // Find the account to get its organization
        AccountsChart account = accountsChartRepository.findByAccountChartPk(parentPk).orElse(null);
        if (account == null) return;

        // Load all accounts for the organization in one query
        List<AccountsChart> allAccounts = accountsChartRepository.findAllForTreeByOrganization(account.getOrganizationFk());

        // Build parent→children map in memory
        Map<Long, List<AccountsChart>> childrenMap = allAccounts.stream()
                .filter(a -> a.getParent() != null)
                .collect(Collectors.groupingBy(a -> a.getParent().getAccountChartPk()));

        // BFS traversal in memory
        Queue<Long> queue = new LinkedList<>();
        queue.add(parentPk);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            List<AccountsChart> children = childrenMap.getOrDefault(current, Collections.emptyList());
            for (AccountsChart child : children) {
                collector.add(child.getAccountChartPk());
                queue.add(child.getAccountChartPk());
            }
        }
    }

    // ==================== HELPER METHODS ====================

    private AccountsChart findAccountOrThrow(Long pk) {
        return accountsChartRepository.findByAccountChartPk(pk)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, GlErrorCodes.GL_ACCOUNT_NOT_FOUND, pk));
    }

    /**
     * Build tree node from pre-loaded in-memory children map.
     * Zero additional DB queries — the entire tree is built from a single flat query.
     */
    private AccountsChartTreeNode buildTreeFromMap(AccountsChart account,
                                                    Map<Long, List<AccountsChart>> childrenMap,
                                                    int level) {
        List<AccountsChart> children = childrenMap.getOrDefault(
                account.getAccountChartPk(), Collections.emptyList());

        List<AccountsChartTreeNode> childNodes = children.stream()
                .sorted(Comparator.comparing(AccountsChart::getAccountChartNo))
                .map(child -> buildTreeFromMap(child, childrenMap, level + 1))
                .collect(Collectors.toList());

        return AccountsChartTreeNode.builder()
                .accountChartPk(account.getAccountChartPk())
                .accountChartNo(account.getAccountChartNo())
                .accountChartName(account.getAccountChartName())
            .accountType(GlAccountTypeNormalizer.normalize(account.getAccountType()))
                .level(level)
                .isActive(Boolean.TRUE.equals(account.getIsActive()))
                .isLeaf(childNodes.isEmpty())
                .parentPk(account.getParent() != null ? account.getParent().getAccountChartPk() : null)
                .organizationFk(account.getOrganizationFk())
                .childCount(childNodes.size())
                .children(childNodes)
                .build();
    }

    // ==================== LOOKUP (for erp-lookup-field) ====================

    /**
     * Paginated account lookup for the frontend lookup dialog.
     * Searches by account number or account name (case-insensitive).
     * Only returns active accounts.
     *
     * @param search  optional search term (matches accountChartNo or accountChartName)
     * @param page    zero-based page number
     * @param size    page size (capped at MAX_PAGE_SIZE)
     * @return Page of AccountLookupDto
     */
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_ACCOUNT_VIEW, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<Page<AccountLookupDto>> lookupAccounts(String search, int page, int size, boolean leafOnly) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.ASC, "accountChartNo"));

        Specification<AccountsChart> spec = (root, query, cb) -> cb.isTrue(root.get("isActive"));

        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("accountChartNo")), pattern),
                    cb.like(cb.lower(root.get("accountChartName")), pattern)
            ));
        }

        if (leafOnly) {
            spec = spec.and((root, query, cb) -> cb.isEmpty(root.get("children")));
        }

        return ServiceResult.success(accountsChartRepository.findAll(spec, pageable)
                .map(this::toAccountLookupDto));
    }

    /**
     * Single account lookup by PK — used by the frontend to resolve display label.
     *
     * @param accountChartPk primary key
     * @return AccountLookupDto
     */
    @PreAuthorize("hasAnyAuthority(T(com.example.security.constants.SecurityPermissions).GL_ACCOUNT_VIEW, T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
    public ServiceResult<AccountLookupDto> lookupAccountById(Long accountChartPk) {
        AccountsChart entity = accountsChartRepository.findByAccountChartPk(accountChartPk)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, GlErrorCodes.GL_ACCOUNT_NOT_FOUND, accountChartPk));
        return ServiceResult.success(toAccountLookupDto(entity));
    }

    private AccountLookupDto toAccountLookupDto(AccountsChart entity) {
        return AccountLookupDto.builder()
                .id(entity.getAccountChartPk())
                .display(entity.getAccountChartNo() + " - " + entity.getAccountChartName())
                .code(entity.getAccountChartNo())
                .name(entity.getAccountChartName())
            .type(GlAccountTypeNormalizer.normalize(entity.getAccountType()))
                .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .build();
    }
}
