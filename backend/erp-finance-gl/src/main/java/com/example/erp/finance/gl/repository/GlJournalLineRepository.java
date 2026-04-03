package com.example.erp.finance.gl.repository;

import com.example.erp.finance.gl.entity.GlJournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlJournalLineRepository extends JpaRepository<GlJournalLine, Long>,
        JpaSpecificationExecutor<GlJournalLine> {

    List<GlJournalLine> findByJournalHdr_Id(Long journalHdrId);
}
