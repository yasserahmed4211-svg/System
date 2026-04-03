package com.example.masterdata.repository;

import com.example.masterdata.entity.MdMasterLookup;
import com.example.masterdata.repository.projection.LookupValueProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Master Lookup Entity — Tenant-Agnostic
 * 
 * Architecture Rules:
 * - Rule 6.5: Repositories are module-internal
 * - Rule 6.2: Use findById() + custom exception pattern
 * - Tenant is managed ONLY in erp-security module
 * 
 * @author ERP Team
 */
@Repository
public interface MasterLookupRepository extends JpaRepository<MdMasterLookup, Long>, 
                                                  JpaSpecificationExecutor<MdMasterLookup> {

    /**
     * Find master lookup by lookup key
     * 
     * @param lookupKey Lookup key
     * @return Optional of MasterLookup
     */
    Optional<MdMasterLookup> findByLookupKey(String lookupKey);

    /**
     * Find lookup values by master lookup key using native query with JOIN
     * Returns master lookup info and all active detail values in a single query
     * 
     * Performance optimization: Single query with JOIN instead of separate queries
     * 
     * @param lookupKey Master lookup key
     * @param isActive Active status filter (1 for active)
     * @return List of lookup value projections
     */
    @Query(value = """
            SELECT 
                ml.ID_PK as masterLookupId,
                ml.LOOKUP_KEY as lookupKey,
                ml.IS_ACTIVE as masterIsActive,
                ld.CODE as code,
                ld.NAME_AR as nameAr,
                ld.NAME_EN as nameEn,
                ld.SORT_ORDER as sortOrder,
                ld.IS_ACTIVE as detailIsActive
            FROM MD_MASTER_LOOKUP ml
            LEFT JOIN MD_LOOKUP_DETAIL ld 
                ON ml.ID_PK = ld.MASTER_LOOKUP_ID_FK 
                AND ld.IS_ACTIVE = :isActive
            WHERE ml.LOOKUP_KEY = :lookupKey
            ORDER BY ld.SORT_ORDER ASC, ld.NAME_AR ASC
            """, nativeQuery = true)
    List<LookupValueProjection> findLookupValuesByKey(
            @Param("lookupKey") String lookupKey,
            @Param("isActive") Integer isActive);

    /**
     * Check if master lookup exists by lookup key
     * 
     * @param lookupKey Lookup key
     * @return true if exists
     */
    boolean existsByLookupKey(String lookupKey);

    /**
     * Single-query validation: check if a lookup detail code exists and is active
     * under an active master lookup key.
     *
     * Performance: executes ONE query with JOIN instead of two separate queries.
     *
     * @param lookupKey Master lookup key (must be UPPERCASE)
     * @param code      Detail code to validate
     * @return 1 if valid, 0 otherwise
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM MD_MASTER_LOOKUP ml
            INNER JOIN MD_LOOKUP_DETAIL ld
                ON ml.ID_PK = ld.MASTER_LOOKUP_ID_FK
            WHERE ml.LOOKUP_KEY = :lookupKey
              AND ml.IS_ACTIVE = 1
              AND ld.CODE = :code
              AND ld.IS_ACTIVE = 1
            """, nativeQuery = true)
    int countActiveByKeyAndCode(
            @Param("lookupKey") String lookupKey,
            @Param("code") String code);

    /**
     * Find all active master lookups
     * 
     * @param isActive Active status
     * @param pageable Pagination
     * @return Page of MasterLookups
     */
    Page<MdMasterLookup> findByIsActive(Boolean isActive, Pageable pageable);

    /**
     * Count lookup details for a master lookup
     * 
     * @param masterLookupId Master lookup ID
     * @return Count of lookup details
     */
    @Query("SELECT COUNT(ld) FROM MdLookupDetail ld WHERE ld.masterLookup.id = :masterLookupId")
    long countLookupDetails(@Param("masterLookupId") Long masterLookupId);

    /**
     * Count active lookup details for a master lookup
     * 
     * @param masterLookupId Master lookup ID
     * @return Count of active lookup details
     */
    @Query("SELECT COUNT(ld) FROM MdLookupDetail ld " +
           "WHERE ld.masterLookup.id = :masterLookupId AND ld.isActive = true")
    long countActiveLookupDetails(@Param("masterLookupId") Long masterLookupId);
}
