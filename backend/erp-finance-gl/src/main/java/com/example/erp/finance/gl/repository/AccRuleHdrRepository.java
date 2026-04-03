package com.example.erp.finance.gl.repository;

import com.example.erp.finance.gl.entity.AccRuleHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccRuleHdrRepository extends JpaRepository<AccRuleHdr, Long>,
        JpaSpecificationExecutor<AccRuleHdr> {

    @Query("SELECT r FROM AccRuleHdr r LEFT JOIN FETCH r.lines WHERE r.ruleId = :ruleId")
    Optional<AccRuleHdr> findByIdWithLines(@Param("ruleId") Long ruleId);

    /**
     * Check if a duplicate active rule exists for the same combination (R-004).
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM AccRuleHdr r " +
            "WHERE r.companyIdFk = :companyId AND r.sourceModule = :sourceModule " +
            "AND r.sourceDocType = :sourceDocType AND r.isActive = true")
    boolean existsActiveRuleForCombination(
            @Param("companyId") Long companyId,
            @Param("sourceModule") String sourceModule,
            @Param("sourceDocType") String sourceDocType);

    /**
     * Check if a duplicate active rule exists for the same combination, excluding a given rule (for update).
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM AccRuleHdr r " +
            "WHERE r.companyIdFk = :companyId AND r.sourceModule = :sourceModule " +
            "AND r.sourceDocType = :sourceDocType AND r.isActive = true " +
            "AND r.ruleId <> :excludeRuleId")
    boolean existsActiveRuleForCombinationExcluding(
            @Param("companyId") Long companyId,
            @Param("sourceModule") String sourceModule,
            @Param("sourceDocType") String sourceDocType,
            @Param("excludeRuleId") Long excludeRuleId);

    /**
     * Check if an account is used in any active rule line (for deactivation guard).
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM AccRuleLine l " +
            "JOIN l.ruleHdr r WHERE l.accountIdFk = :accountPk AND r.isActive = true")
    boolean isAccountUsedInActiveRules(@Param("accountPk") Long accountPk);

    /**
     * Find the active accounting rule for a given company + module + doc type combination.
     * Used by the Posting Engine to resolve which rule to apply.
     */
    @Query("SELECT r FROM AccRuleHdr r LEFT JOIN FETCH r.lines " +
            "WHERE r.companyIdFk = :companyId AND r.sourceModule = :sourceModule " +
            "AND r.sourceDocType = :sourceDocType AND r.isActive = true")
    Optional<AccRuleHdr> findActiveRuleWithLines(
            @Param("companyId") Long companyId,
            @Param("sourceModule") String sourceModule,
            @Param("sourceDocType") String sourceDocType);
}
