package com.example.security.repo;

import com.example.security.domain.Permission;
import com.example.security.dto.PermissionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PermissionRepository extends JpaRepository<Permission, Long>, JpaSpecificationExecutor<Permission> {

    Optional<Permission> findByNameAndTenantId(String name, String tenantId);

    boolean existsByNameAndTenantId(String name, String tenantId);

    List<Permission> findAllByTenantId(String tenantId);

    Page<Permission> findAllByTenantId(String tenantId, Pageable pageable);

    long deleteByIdAndTenantId(Long id, String tenantId);

    /**
     * Find all permissions by names and tenant
     */
    List<Permission> findByNameInAndTenantId(List<String> names, String tenantId);

    /**
     * Find all permissions for a specific page
     */
    List<Permission> findByPageIdAndTenantId(Long pageId, String tenantId);

    /**
     * Find VIEW permissions for a set of page IDs (to check which pages are assigned to roles)
     */
    @Query("SELECT p FROM Permission p WHERE p.tenantId = :tenantId " +
           "AND p.page.id IN :pageIds AND p.permissionType = 'VIEW'")
    List<Permission> findViewPermissionsByPageIds(
        @Param("tenantId") String tenantId,
        @Param("pageIds") Set<Long> pageIds
    );

    /**
     * Find all page permissions assigned to a role (via ROLE_PERMISSIONS join table)
     * Returns permissions with their linked pages in single query - optimal for ERP
     */
    @Query("SELECT DISTINCT p FROM Permission p " +
           "JOIN FETCH p.page pg " +
           "JOIN p.id pId " +
           "WHERE p.tenantId = :tenantId " +
           "AND p.page IS NOT NULL " +
           "AND p.id IN :permissionIds")
    List<Permission> findPagePermissionsWithPagesByIds(
        @Param("tenantId") String tenantId,
        @Param("permissionIds") Set<Long> permissionIds
    );
}
