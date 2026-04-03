package com.example.erp.finance.gl.service;

import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.example.erp.finance.gl.entity.AccountsChart;
import com.example.erp.finance.gl.exception.GlErrorCodes;
import com.example.erp.finance.gl.repository.AccountsChartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Validates the hierarchical tree structure of the Chart of Accounts.
 * Prevents structural corruption including:
 * <ul>
 *   <li>Self-reference (parent = same account)</li>
 *   <li>Circular references</li>
 *   <li>Assigning a descendant as parent</li>
 *   <li>Changing account type when children exist</li>
 * </ul>
 *
 * @author ERP Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountChartTreeValidator {

    private final AccountsChartRepository accountsChartRepository;

    /**
     * Validate that a parent exists and is eligible to be a parent.
     *
     * @param parentPk the parent's primary key
     * @return the verified parent entity
     */
    public AccountsChart validateParentExists(Long parentPk) {
        AccountsChart parent = accountsChartRepository.findByAccountChartPk(parentPk)
                .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, GlErrorCodes.GL_ACCOUNT_PARENT_NOT_FOUND, parentPk));

        if (!Boolean.TRUE.equals(parent.getIsActive())) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_ACCOUNT_PARENT_INACTIVE, parentPk);
        }

        return parent;
    }

    /**
     * Validate that assigning newParent to account does not create a self-reference.
     *
     * @param accountPk the account being updated
     * @param parentPk  the proposed parent PK
     */
    public void validateNoSelfReference(Long accountPk, Long parentPk) {
        if (accountPk != null && accountPk.equals(parentPk)) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_ACCOUNT_SELF_REFERENCE, accountPk);
        }
    }

    /**
     * Validate that assigning newParent to account does not create a circular reference.
     * Walks up the ancestor chain of potentialParent to check if account appears in the path.
     * Also detects if potentialParent is a descendant of account using BFS.
     *
     * @param account         the account being reassigned
     * @param potentialParent the proposed new parent
     */
    public void validateNoCircularReference(AccountsChart account, AccountsChart potentialParent) {
        if (account.getAccountChartPk() == null) {
            // New entity (not yet persisted) → no circular risk
            return;
        }

        // 1. Walk up the ancestor chain of potentialParent
        Set<Long> visited = new HashSet<>();
        AccountsChart current = potentialParent;
        while (current != null) {
            if (current.getAccountChartPk().equals(account.getAccountChartPk())) {
                throw new LocalizedException(Status.BAD_REQUEST,
                        GlErrorCodes.GL_ACCOUNT_CIRCULAR_REF, account.getAccountChartPk());
            }
            if (!visited.add(current.getAccountChartPk())) {
                break; // Already visited — prevent infinite loop from data corruption
            }
            try {
                current = current.getParent();
            } catch (Exception e) {
                // Lazy loading boundary — load explicitly
                if (current.getParent() != null) {
                    current = accountsChartRepository.findByIdWithParent(current.getAccountChartPk())
                            .map(AccountsChart::getParent)
                            .orElse(null);
                } else {
                    break;
                }
            }
        }

        // 2. BFS check: ensure potentialParent is not a descendant of account
        validateNotDescendant(account.getAccountChartPk(), potentialParent.getAccountChartPk());
    }

    /**
     * Validate that the proposed parent is not a descendant of the account.
     * Uses BFS to traverse the subtree of accountPk.
     *
     * @param accountPk  the account being modified
     * @param proposedParentPk the proposed parent PK
     */
    public void validateNotDescendant(Long accountPk, Long proposedParentPk) {
        Set<Long> descendants = collectAllDescendantPks(accountPk);
        if (descendants.contains(proposedParentPk)) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_ACCOUNT_DESCENDANT_AS_PARENT, accountPk, proposedParentPk);
        }
    }

    /**
     * Validate that account type can be changed.
     * Prevents type change if the account has any children.
     *
     * @param accountPk      the account PK
     * @param currentType    the current account type
     * @param requestedType  the requested new account type
     */
    public void validateAccountTypeChange(Long accountPk, String currentType, String requestedType) {
        if (currentType != null && !currentType.equals(requestedType)) {
            boolean hasChildren = accountsChartRepository.hasChildren(accountPk);
            if (hasChildren) {
                throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                        GlErrorCodes.GL_ACCOUNT_TYPE_CHANGE_WITH_CHILDREN, accountPk);
            }
        }
    }

    /**
     * Validate that child account type matches parent account type.
     *
     * @param childType  the child's account type
     * @param parentType the parent's account type
     */
    public void validateTypeMatchesParent(String childType, String parentType) {
        if (!parentType.equals(childType)) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_ACCOUNT_TYPE_MISMATCH, childType, parentType);
        }
    }

    // ==================== Private Helpers ====================

    /**
     * Collect all descendant PKs of a given account using BFS.
     *
     * @param accountPk the root of the subtree
     * @return set of all descendant PKs (excluding the accountPk itself)
     */
    private Set<Long> collectAllDescendantPks(Long accountPk) {
        Set<Long> descendants = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();
        queue.add(accountPk);

        while (!queue.isEmpty()) {
            Long currentPk = queue.poll();
            var children = accountsChartRepository.findDirectChildren(currentPk);
            for (AccountsChart child : children) {
                if (descendants.add(child.getAccountChartPk())) {
                    queue.add(child.getAccountChartPk());
                }
            }
        }

        return descendants;
    }
}
