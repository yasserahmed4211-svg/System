package com.example.erp.finance.gl.repository;

import com.example.erp.finance.gl.entity.AccRuleLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccRuleLineRepository extends JpaRepository<AccRuleLine, Long> {

    List<AccRuleLine> findByRuleHdr_RuleId(Long ruleId);
}
