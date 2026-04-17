package com.example.org.repository;

import com.example.org.entity.OrgBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgBranchRepository extends JpaRepository<OrgBranch, Long>, JpaSpecificationExecutor<OrgBranch> {

    boolean existsByBranchCode(String branchCode);

    boolean existsByBranchCodeAndIdNot(String branchCode, Long id);

    @Query("SELECT COUNT(b) FROM OrgBranch b WHERE b.legalEntity.id = :legalEntityId AND b.isHeadquarter = true AND b.isActive = true")
    long countActiveHeadquarters(@Param("legalEntityId") Long legalEntityId);

    @Query("SELECT COUNT(b) FROM OrgBranch b WHERE b.legalEntity.id = :legalEntityId AND b.isHeadquarter = true AND b.isActive = true AND b.id <> :excludeId")
    long countActiveHeadquartersExcluding(@Param("legalEntityId") Long legalEntityId, @Param("excludeId") Long excludeId);

    @Query("SELECT COUNT(b) FROM OrgBranch b WHERE b.legalEntity.id = :legalEntityId AND b.isActive = true")
    long countActiveByLegalEntity(@Param("legalEntityId") Long legalEntityId);

    @Query("SELECT COUNT(d) FROM OrgDepartment d WHERE d.branch.id = :branchId AND d.isActive = true")
    long countActiveDepartments(@Param("branchId") Long branchId);
}