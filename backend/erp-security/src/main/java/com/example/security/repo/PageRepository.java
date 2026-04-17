package com.example.security.repo;

import com.example.security.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository for Page entity
 */
public interface PageRepository extends JpaRepository<Page, Long>, JpaSpecificationExecutor<Page> {

    /**
     * Find page by ID and tenant
     */
    Optional<Page> findByIdAndTenantId(Long id, String tenantId);

    /**
     * Find page by code and tenant
     */
    Optional<Page> findByPageCodeAndTenantId(String pageCode, String tenantId);

    /**
     * Check if page code exists for tenant
     */
    boolean existsByPageCodeAndTenantId(String pageCode, String tenantId);

    /**
     * Check if route exists for tenant
     */
    boolean existsByRouteAndTenantId(String route, String tenantId);

    /**
     * Check if route exists for tenant excluding specific page ID
     */
    boolean existsByRouteAndTenantIdAndIdNot(String route, String tenantId, Long id);

    /**
     * Find all active pages by tenant
     */
    List<Page> findByTenantIdAndActiveOrderByDisplayOrder(String tenantId, Boolean active);

    /**
     * Find all pages by tenant with pagination
     */
    org.springframework.data.domain.Page<Page> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Find pages by codes and tenant (for optimized batch loading)
     * This eliminates N+1 query problem in getRolePages
     */
    @Query("SELECT p FROM Page p WHERE p.pageCode IN :pageCodes AND p.tenantId = :tenantId")
    List<Page> findByPageCodesAndTenantId(@Param("pageCodes") Set<String> pageCodes, @Param("tenantId") String tenantId);

    /**
     * Find active pages by page codes, tenant, and active status (for menu building)
     */
    @Query("SELECT p FROM Page p WHERE p.pageCode IN :pageCodes AND p.tenantId = :tenantId AND p.active = :active ORDER BY p.displayOrder")
    List<Page> findByPageCodeInAndTenantIdAndActive(
        @Param("pageCodes") Set<String> pageCodes, 
        @Param("tenantId") String tenantId, 
        @Param("active") Boolean active
    );

    /**
     * ✅ NEW: Find active pages by IDs, tenant, and active status (uses FK from PERMISSIONS)
     * This method is used by MenuService to resolve pages using PAGE_ID_FK from PERMISSIONS table.
     * This approach survives page code renaming and is more resilient.
     */
    @Query("SELECT p FROM Page p WHERE p.id IN :pageIds AND p.tenantId = :tenantId AND p.active = :active ORDER BY p.displayOrder")
    List<Page> findByIdInAndTenantIdAndActive(
        @Param("pageIds") Set<Long> pageIds, 
        @Param("tenantId") String tenantId, 
        @Param("active") Boolean active
    );

    /**
     * Find pages by module and tenant
     */
    org.springframework.data.domain.Page<Page> findByTenantIdAndModule(String tenantId, String module, Pageable pageable);

    /**
     * Find active pages by tenant with pagination
     */
    org.springframework.data.domain.Page<Page> findByTenantIdAndActive(String tenantId, Boolean active, Pageable pageable);

    /**
     * Search pages by name (Arabic or English) - case insensitive
     */
    @Query("SELECT p FROM Page p WHERE p.tenantId = :tenantId " +
           "AND (LOWER(p.nameAr) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.nameEn) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.pageCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    org.springframework.data.domain.Page<Page> searchPages(
        @Param("tenantId") String tenantId,
        @Param("search") String search,
        Pageable pageable
    );

    /**
     * Delete page by ID and tenant (tenant-scoped delete)
     */
    long deleteByIdAndTenantId(Long id, String tenantId);
}
