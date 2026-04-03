package com.example.erp.finance.gl.repository;

import com.example.erp.finance.gl.entity.AccPostingMst;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccPostingMstRepository extends JpaRepository<AccPostingMst, Long>,
        JpaSpecificationExecutor<AccPostingMst> {

    @Query("SELECT p FROM AccPostingMst p LEFT JOIN FETCH p.details WHERE p.postingId = :postingId")
    Optional<AccPostingMst> findByIdWithDetails(@Param("postingId") Long postingId);

    @Query("SELECT p FROM AccPostingMst p LEFT JOIN FETCH p.journal WHERE p.postingId = :postingId")
    Optional<AccPostingMst> findWithJournal(@Param("postingId") Long postingId);
}
