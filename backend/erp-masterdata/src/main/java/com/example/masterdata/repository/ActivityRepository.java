package com.example.masterdata.repository;

import com.example.masterdata.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Activity Repository — Tenant-Agnostic
 * 
 * Architecture Rules:
 * - Rule 6.5: Repositories are module-internal
 * - Rule 10.4: All FK columns must be indexed
 * - Tenant is managed ONLY in erp-security module
 * 
 * @author ERP Team
 */
@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {

    /**
     * Find activity by code (case-insensitive)
     * 
     * @param code Activity code
     * @return Optional Activity
     */
    Optional<Activity> findByCodeIgnoreCase(String code);

    /**
     * Check if activity exists by code
     * 
     * @param code Activity code
     * @return true if exists
     */
    boolean existsByCode(String code);

    /**
     * Find all active/inactive activities with pagination
     * 
     * @param isActive Active status
     * @param pageable Pagination info
     * @return Page of activities
     */
    Page<Activity> findByIsActive(Boolean isActive, Pageable pageable);

    /**
     * Count categories referencing this activity
     * Used to check if activity can be deleted or deactivated
     * 
     * @param activityId Activity ID
     * @return Count of categories
     */
    @Query("SELECT COUNT(c) FROM Category c WHERE c.activityId = :activityId")
    long countCategoriesByActivityId(@Param("activityId") Long activityId);

    /**
     * Count active categories referencing this activity
     * 
     * @param activityId Activity ID
     * @return Count of active categories
     */
    @Query("SELECT COUNT(c) FROM Category c WHERE c.activityId = :activityId AND c.isActive = true")
    long countActiveCategoriesByActivityId(@Param("activityId") Long activityId);
}
