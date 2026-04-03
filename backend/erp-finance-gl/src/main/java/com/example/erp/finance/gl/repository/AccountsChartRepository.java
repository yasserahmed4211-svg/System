package com.example.erp.finance.gl.repository;

import com.example.erp.finance.gl.entity.AccountsChart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountsChartRepository extends JpaRepository<AccountsChart, Long>,
        JpaSpecificationExecutor<AccountsChart> {

    // ==================== Existence Checks ====================

    boolean existsByAccountChartNoAndOrganizationFk(String accountChartNo, Long organizationFk);

    boolean existsByAccountChartNoAndOrganizationFkAndAccountChartPkNot(
            String accountChartNo, Long organizationFk, Long excludePk);

    // ==================== Finders ====================

    Optional<AccountsChart> findByAccountChartPk(Long accountChartPk);

    @Query("SELECT a FROM AccountsChart a LEFT JOIN FETCH a.parent WHERE a.accountChartPk = :pk")
    Optional<AccountsChart> findByIdWithParent(@Param("pk") Long pk);

    @Query("SELECT a FROM AccountsChart a LEFT JOIN FETCH a.children WHERE a.accountChartPk = :pk")
    Optional<AccountsChart> findByIdWithChildren(@Param("pk") Long pk);

    // ==================== Children Queries ====================

    @Query("SELECT COUNT(c) FROM AccountsChart c WHERE c.parent.accountChartPk = :parentPk")
    long countChildrenByParentPk(@Param("parentPk") Long parentPk);

    @Query("SELECT COUNT(c) FROM AccountsChart c WHERE c.parent.accountChartPk = :parentPk AND c.isActive = true")
    long countActiveChildrenByParentPk(@Param("parentPk") Long parentPk);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM AccountsChart c WHERE c.parent.accountChartPk = :parentPk")
    boolean hasChildren(@Param("parentPk") Long parentPk);

    // ==================== Tree Queries ====================

    @Query("SELECT a FROM AccountsChart a LEFT JOIN FETCH a.children WHERE a.parent IS NULL AND a.organizationFk = :orgFk ORDER BY a.accountChartNo")
    List<AccountsChart> findRootAccountsByOrganization(@Param("orgFk") Long orgFk);

    @Query("SELECT a FROM AccountsChart a LEFT JOIN FETCH a.children WHERE a.parent IS NULL AND a.organizationFk = :orgFk AND a.accountType = :accountType ORDER BY a.accountChartNo")
    List<AccountsChart> findRootAccountsByOrganizationAndType(
            @Param("orgFk") Long orgFk, @Param("accountType") String accountType);

    @Query("SELECT a FROM AccountsChart a LEFT JOIN FETCH a.children WHERE a.parent IS NULL ORDER BY a.accountChartNo")
    List<AccountsChart> findAllRootAccounts();

    @Query("SELECT a FROM AccountsChart a LEFT JOIN FETCH a.children WHERE a.parent IS NULL AND a.accountType = :accountType ORDER BY a.accountChartNo")
    List<AccountsChart> findAllRootAccountsByType(@Param("accountType") String accountType);

    @Query("SELECT a FROM AccountsChart a WHERE a.organizationFk = :orgFk ORDER BY a.accountChartNo")
    List<AccountsChart> findAllByOrganization(@Param("orgFk") Long orgFk);

    // ==================== Bulk Tree Queries (N+1 fix) ====================

    /**
     * Fetch ALL accounts for a given organization in a single query.
     * Tree is built in-memory from this flat list to avoid N+1.
     */
    @Query("SELECT a FROM AccountsChart a LEFT JOIN FETCH a.parent WHERE a.organizationFk = :orgFk ORDER BY a.accountChartNo")
    List<AccountsChart> findAllForTreeByOrganization(@Param("orgFk") Long orgFk);

    @Query("SELECT a FROM AccountsChart a LEFT JOIN FETCH a.parent WHERE a.organizationFk = :orgFk AND a.accountType = :accountType ORDER BY a.accountChartNo")
    List<AccountsChart> findAllForTreeByOrganizationAndType(@Param("orgFk") Long orgFk, @Param("accountType") String accountType);

    @Query("SELECT a FROM AccountsChart a LEFT JOIN FETCH a.parent ORDER BY a.accountChartNo")
    List<AccountsChart> findAllForTree();

    @Query("SELECT a FROM AccountsChart a LEFT JOIN FETCH a.parent WHERE a.accountType = :accountType ORDER BY a.accountChartNo")
    List<AccountsChart> findAllForTreeByType(@Param("accountType") String accountType);

    // ==================== Leaf Check (no children) ====================

    @Query("SELECT CASE WHEN COUNT(c) = 0 THEN true ELSE false END FROM AccountsChart c WHERE c.parent.accountChartPk = :pk")
    boolean isLeafAccount(@Param("pk") Long pk);

    // ==================== Auto-Numbering Queries ====================

    /**
     * Fetch the maximum ACCOUNT_CHART_NO among root accounts for a given organization and account type.
     * Root accounts have ACCOUNT_CHART_FK IS NULL.
     */
    @Query("SELECT MAX(a.accountChartNo) FROM AccountsChart a " +
           "WHERE a.parent IS NULL AND a.organizationFk = :orgFk AND a.accountType = :accountType")
    Optional<String> findMaxRootAccountNo(@Param("orgFk") Long orgFk, @Param("accountType") String accountType);

    /**
     * Fetch the maximum ACCOUNT_CHART_NO among direct children of a given parent,
     * whose code starts with the parent's code prefix.
     */
    @Query("SELECT MAX(a.accountChartNo) FROM AccountsChart a " +
           "WHERE a.parent.accountChartPk = :parentPk AND a.accountChartNo LIKE :prefix")
    Optional<String> findMaxChildAccountNo(@Param("parentPk") Long parentPk, @Param("prefix") String prefix);

    /**
     * Fetch the maximum ACCOUNT_CHART_NO among ALL accounts in an organization
     * matching a given prefix and exact code length.
     * This prevents cross-hierarchy collisions (e.g., parent "1" child "103" vs parent "10" child "103").
     */
    @Query("SELECT MAX(a.accountChartNo) FROM AccountsChart a " +
           "WHERE a.organizationFk = :orgFk AND a.accountChartNo LIKE :prefix " +
           "AND LENGTH(a.accountChartNo) = :expectedLen")
    Optional<String> findMaxAccountNoByPrefixAndLength(
            @Param("orgFk") Long orgFk,
            @Param("prefix") String prefix,
            @Param("expectedLen") int expectedLen);

    // ==================== Descendant Detection ====================

    /**
     * Find all children (direct) of a given parent account.
     */
    @Query("SELECT a FROM AccountsChart a WHERE a.parent.accountChartPk = :parentPk ORDER BY a.accountChartNo")
    List<AccountsChart> findDirectChildren(@Param("parentPk") Long parentPk);

    // ==================== Eligible Parent LOV Queries ====================

    /**
     * Find eligible parent accounts for a given organization, excluding a set of PKs
     * (self + descendants). Active accounts only. Supports search by code or name.
     * Sorted by ACCOUNT_CHART_NO for consistent ordering.
     */
    @Query("SELECT a FROM AccountsChart a WHERE a.organizationFk = :orgFk " +
           "AND a.isActive = true " +
           "AND a.accountChartPk NOT IN :excludePks " +
           "AND (LOWER(a.accountChartNo) LIKE LOWER(:search) OR LOWER(a.accountChartName) LIKE LOWER(:search)) " +
           "ORDER BY a.accountChartNo")
    Page<AccountsChart> findEligibleParents(
            @Param("orgFk") Long orgFk,
            @Param("excludePks") Collection<Long> excludePks,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Find eligible parent accounts without search filter.
     */
    @Query("SELECT a FROM AccountsChart a WHERE a.organizationFk = :orgFk " +
           "AND a.isActive = true " +
           "AND a.accountChartPk NOT IN :excludePks " +
           "ORDER BY a.accountChartNo")
    Page<AccountsChart> findEligibleParentsNoSearch(
            @Param("orgFk") Long orgFk,
            @Param("excludePks") Collection<Long> excludePks,
            Pageable pageable);
}
