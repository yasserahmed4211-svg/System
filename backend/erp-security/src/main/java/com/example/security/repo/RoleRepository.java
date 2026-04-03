package com.example.security.repo;

import com.example.security.domain.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Role entity
 * 
 * Governance: BE-REQ-ROLEACCESS-001
 * Contract: role-access.contract.md
 * 
 * <h2>Active Flag Query Pattern:</h2>
 * When the IS_ACTIVE column is added to ROLES table, use this pattern:
 * <pre>
 * (:isActive IS NULL OR r.active = :isActive)
 * </pre>
 * 
 * This allows:
 * - isActive = null → returns ALL roles
 * - isActive = true → returns only active roles  
 * - isActive = false → returns only inactive roles
 */
public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {

    // Find by roleName (unique within tenant)
    Optional<Role> findByRoleNameAndTenantId(String roleName, String tenantId);

    // Legacy support - maps to roleCode
    @Deprecated
    default Optional<Role> findByNameAndTenantId(String name, String tenantId) {
        return findByRoleNameAndTenantId(name, tenantId);
    }

    // Fetch join to avoid N+1 when loading role with permissions AND their pages
    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions p LEFT JOIN FETCH p.page WHERE r.id = :id AND r.tenantId = :tenantId")
    Optional<Role> findByIdWithPermissions(@Param("id") Long id, @Param("tenantId") String tenantId);

    // EntityGraph to optimize loading role with permissions (Rule 6.4)
    @EntityGraph(attributePaths = {"permissions"})
    Optional<Role> findByIdAndTenantId(Long id, String tenantId);

    List<Role> findAllByTenantId(String tenantId);

    // EntityGraph for paginated queries to avoid N+1 (Rule 6.4)
    @EntityGraph(attributePaths = {"permissions"})
    Page<Role> findAllByTenantId(String tenantId, Pageable pageable);

    /**
     * Paginated search with filters per contract.
     * 
     * @param tenantId the tenant ID
     * @param search optional search term for role name
     * @param active optional active filter (null = ALL, true = active only, false = inactive only)
     * @param pageable pagination info
     * @return paginated roles matching criteria
     */
    @Query("SELECT r FROM Role r WHERE r.tenantId = :tenantId " +
           "AND (:search IS NULL OR LOWER(r.roleName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:active IS NULL OR r.active = :active)")
    Page<Role> findByFilters(
            @Param("tenantId") String tenantId,
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable);

    long deleteByIdAndTenantId(Long id, String tenantId);

    // Check if role has any user assignments (for delete validation)
    @Query("SELECT COUNT(u) > 0 FROM UserAccount u JOIN u.roles r WHERE r.id = :roleId AND r.tenantId = :tenantId")
    boolean hasUserAssignments(@Param("roleId") Long roleId, @Param("tenantId") String tenantId);
    
    /**
     * Find all active roles for a tenant.
     */
    @Query("SELECT r FROM Role r WHERE r.tenantId = :tenantId AND r.active = true")
    List<Role> findActiveByTenantId(@Param("tenantId") String tenantId);
}
