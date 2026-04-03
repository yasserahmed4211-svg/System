package com.example.erp.finance.gl.repository;

import com.example.erp.finance.gl.entity.GlJournalHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GlJournalHdrRepository extends JpaRepository<GlJournalHdr, Long>,
        JpaSpecificationExecutor<GlJournalHdr> {

    @Query("SELECT j FROM GlJournalHdr j LEFT JOIN FETCH j.lines WHERE j.id = :journalId")
    Optional<GlJournalHdr> findByIdWithLines(@Param("journalId") Long journalId);

    /**
     * Check if a journal number already exists.
     */
    boolean existsByJournalNo(String journalNo);

    /**
     * Generate the next journal number using the Oracle sequence.
     */
    @Query(value = "SELECT GL_JOURNAL_NO_SEQ.NEXTVAL FROM DUAL", nativeQuery = true)
    Long getNextJournalNoSequence();

    /**
     * Generate a formatted journal number (JRN-XXXXXX) using the DB sequence.
     * Shared by GlJournalService and PostingEngineService.
     */
    default String generateJournalNo() {
        Long seq = getNextJournalNoSequence();
        return String.format("JRN-%06d", seq);
    }

    /**
     * Check if any automatic journal exists for a given rule (sourcePostingIdFk)
     * with a specific type and any of the given statuses.
     * Used by AccRuleHdrService to enforce rule-in-use and pending-posting guards.
     */
    @Query("SELECT CASE WHEN COUNT(j) > 0 THEN true ELSE false END FROM GlJournalHdr j " +
            "WHERE j.sourcePostingIdFk = :ruleId " +
            "AND j.journalTypeIdFk = :journalType " +
            "AND j.statusIdFk IN :statuses")
    boolean existsBySourcePostingIdFkAndJournalTypeIdFkAndStatusIdFkIn(
            @Param("ruleId") Long ruleId,
            @Param("journalType") String journalType,
            @Param("statuses") List<String> statuses);

    /**
     * Find a journal linked to a specific ACC_POSTING_MST posting ID.
     * Used by JournalGenerationService for idempotency checks.
     */
    Optional<GlJournalHdr> findBySourcePostingIdFk(Long sourcePostingIdFk);
}
