package com.example.security.repo;

import com.example.security.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByJtiAndTenantId(String jti, String tenantId);
    long deleteByExpiresAtBefore(Instant cutoff);
    long countByUser_IdAndTenantId(Long userId, String tenantId);
}
