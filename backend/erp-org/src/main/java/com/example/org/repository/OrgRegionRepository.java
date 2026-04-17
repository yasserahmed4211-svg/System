package com.example.org.repository;

import com.example.org.entity.OrgRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgRegionRepository extends JpaRepository<OrgRegion, Long>, JpaSpecificationExecutor<OrgRegion> {

    boolean existsByRegionCode(String regionCode);

    boolean existsByRegionCodeAndIdNot(String regionCode, Long id);

    @Query("SELECT COUNT(b) FROM OrgBranch b WHERE b.region.id = :regionId AND b.isActive = true")
    long countActiveBranches(@Param("regionId") Long regionId);
}