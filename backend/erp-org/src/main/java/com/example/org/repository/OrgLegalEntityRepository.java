package com.example.org.repository;

import com.example.org.entity.OrgLegalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgLegalEntityRepository extends JpaRepository<OrgLegalEntity, Long>, JpaSpecificationExecutor<OrgLegalEntity> {

    boolean existsByLegalEntityCode(String legalEntityCode);

    boolean existsByLegalEntityCodeAndIdNot(String legalEntityCode, Long id);

    @Query("SELECT COUNT(b) FROM OrgBranch b WHERE b.legalEntity.id = :legalEntityId AND b.isActive = true")
    long countActiveBranches(@Param("legalEntityId") Long legalEntityId);

    @Query("SELECT COUNT(le) FROM OrgLegalEntity le WHERE le.isActive = true")
    long countAllActive();

    @Query(value = """
            SELECT CASE WHEN (
                (SELECT COUNT(*) FROM GL_LEDGER_ENTRY gle
                 JOIN ORG_BRANCH ob ON gle.BRANCH_ID_FK = ob.ID_PK
                 WHERE ob.LEGAL_ENTITY_ID_FK = :legalEntityId) +
                (SELECT COUNT(*) FROM ACC_POSTING_MST apm
                 JOIN ORG_BRANCH ob ON apm.BRANCH_ID_FK = ob.ID_PK
                 WHERE ob.LEGAL_ENTITY_ID_FK = :legalEntityId)
            ) > 0 THEN 1 ELSE 0 END FROM DUAL
            """, nativeQuery = true)
    int hasFinancialTransactions(@Param("legalEntityId") Long legalEntityId);
}