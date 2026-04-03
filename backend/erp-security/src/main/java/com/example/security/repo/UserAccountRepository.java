package com.example.security.repo;

import com.example.security.domain.UserAccount;
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
 * User Account Repository with optimized lazy loading (Rule 6.4)
 * Supports dynamic search via JpaSpecificationExecutor
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long>, JpaSpecificationExecutor<UserAccount> {

    Optional<UserAccount> findByUsernameIgnoreCaseAndTenantId(String username, String tenantId);

    // Fetch join to avoid N+1 when loading user with roles and permissions
    @Query("SELECT DISTINCT u FROM UserAccount u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.username = :username AND u.tenantId = :tenantId")
    Optional<UserAccount> findByUsernameWithRoles(@Param("username") String username, @Param("tenantId") String tenantId);

    // Fetch join to avoid N+1 when loading user by ID with roles and permissions
    @Query("SELECT DISTINCT u FROM UserAccount u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.id = :id AND u.tenantId = :tenantId")
    Optional<UserAccount> findByIdWithRoles(@Param("id") Long id, @Param("tenantId") String tenantId);

    // EntityGraph to optimize loading user with roles (Rule 6.4)
    @EntityGraph(attributePaths = {"roles"})
    Optional<UserAccount> findByIdAndTenantId(Long id, String tenantId);

    boolean existsByUsernameIgnoreCaseAndTenantId(String username, String tenantId);

    List<UserAccount> findAllByTenantId(String tenantId);

    // EntityGraph for paginated queries to avoid N+1 (Rule 6.4)
    @EntityGraph(attributePaths = {"roles"})
    Page<UserAccount> findAllByTenantId(String tenantId, Pageable pageable);

    long deleteByIdAndTenantId(Long id, String tenantId);
}
