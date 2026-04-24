package com.example.masterdata.repository;

import com.example.masterdata.entity.MdLookupDetail;
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
 * Repository for Lookup Detail Entity — Tenant-Agnostic
 * 
 * Architecture Rules:
 * - Rule 6.5: Repositories are module-internal
 * - Rule 6.2: Use findById() + custom exception pattern
 * - Tenant is managed ONLY in erp-security module
 * 
 * @author ERP Team
 */
@Repository
public interface LookupDetailRepository extends JpaRepository<MdLookupDetail, Long>, 
                                                  JpaSpecificationExecutor<MdLookupDetail> {

    /**
     * Find lookup detail by master lookup ID and code
     * 
     * @param masterLookupId Master lookup ID
     * @param code Detail code
     * @return Optional of LookupDetail
     */
    Optional<MdLookupDetail> findByMasterLookupIdAndCode(Long masterLookupId, String code);

    /**
     * Check if lookup detail exists by master lookup ID and code
     * 
     * @param masterLookupId Master lookup ID
     * @param code Detail code
     * @return true if exists
     */
    boolean existsByMasterLookupIdAndCode(Long masterLookupId, String code);

    /**
     * Check if lookup detail exists by master lookup ID and code, excluding specific ID
     * Used for update validation
     * 
     * @param masterLookupId Master lookup ID
     * @param code Detail code
     * @param id ID to exclude
     * @return true if exists
     */
    boolean existsByMasterLookupIdAndCodeAndIdNot(Long masterLookupId, String code, Long id);

    /**
     * Find all lookup details by master lookup ID
     * 
     * @param masterLookupId Master lookup ID
     * @param pageable Pagination
     * @return Page of LookupDetails
     */
    Page<MdLookupDetail> findByMasterLookupId(Long masterLookupId, Pageable pageable);

    /**
     * Find active lookup details by master lookup ID
     * 
     * @param masterLookupId Master lookup ID
     * @param isActive Active status
     * @param pageable Pagination
     * @return Page of active LookupDetails
     */
    Page<MdLookupDetail> findByMasterLookupIdAndIsActive(
        Long masterLookupId, Boolean isActive, Pageable pageable);

    /**
     * Search lookup details with explicit FETCH JOIN on master lookup (Best Practice)
     * Uses JPQL FETCH JOIN to eagerly load masterLookup and avoid N+1 queries
     * 
     * @param masterLookupId Master lookup ID (required for parent-child relationship)
     * @param pageable Pagination and sorting
     * @return Page of LookupDetails
     */
    @Query(value = "SELECT ld FROM MdLookupDetail ld " +
           "WHERE ld.masterLookup.id = :masterLookupId",
           countQuery = "SELECT COUNT(ld) FROM MdLookupDetail ld " +
           "WHERE ld.masterLookup.id = :masterLookupId")
    Page<MdLookupDetail> searchByMasterLookupId(
        @Param("masterLookupId") Long masterLookupId,
        Pageable pageable);

    /**
     * Search lookup details with explicit FETCH JOIN and active filter
     * 
     * @param masterLookupId Master lookup ID
     * @param isActive Active status filter
     * @param pageable Pagination and sorting
     * @return Page of LookupDetails
     */
    @Query(value = "SELECT ld FROM MdLookupDetail ld " +
           "WHERE ld.masterLookup.id = :masterLookupId " +
           "AND ld.isActive = :isActive",
           countQuery = "SELECT COUNT(ld) FROM MdLookupDetail ld " +
           "WHERE ld.masterLookup.id = :masterLookupId " +
           "AND ld.isActive = :isActive")
    Page<MdLookupDetail> searchByMasterLookupIdAndActive(
        @Param("masterLookupId") Long masterLookupId,
        @Param("isActive") Boolean isActive,
        Pageable pageable);

    /**
     * Find lookup details by master lookup key (for dropdown options)
     * 
     * @param lookupKey Master lookup key
     * @param isActive Active status filter
     * @return List of LookupDetails ordered by sort_order
     */
    @Query("SELECT ld FROM MdLookupDetail ld " +
           "JOIN ld.masterLookup ml " +
           "WHERE ml.lookupKey = :lookupKey " +
           "AND ld.isActive = :isActive " +
           "ORDER BY ld.sortOrder ASC, ld.nameAr ASC")
    List<MdLookupDetail> findByMasterLookupKeyAndActive(
        @Param("lookupKey") String lookupKey,
        @Param("isActive") Boolean isActive);

}
