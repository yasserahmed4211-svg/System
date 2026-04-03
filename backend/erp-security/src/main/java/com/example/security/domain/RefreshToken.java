package com.example.security.domain;

import com.example.erp.common.converter.BooleanNumberConverter;
import com.example.erp.common.multitenancy.TenantContext;
import com.example.erp.common.util.SecurityContextHelper;
import com.example.erp.common.multitenancy.TenantEntityListener;
import com.example.erp.common.multitenancy.TenantScoped;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

// domain/RefreshToken.java
@Entity
@Table(name = "REFRESH_TOKENS")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name="JTI", nullable=false, unique=true, length=64)
    private String jti;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="USER_ID", referencedColumnName = "ID", nullable=false)
    private UserAccount user;

    @Column(name="TENANT_ID", nullable=false, length=64)
    private String tenantId;

    @CreationTimestamp
    @Column(name="CREATED_AT", nullable=false, updatable=false)
    private Instant createdAt;

    @Column(name="EXPIRES_AT", nullable=false)
    private Instant expiresAt;

    @Builder.Default
    @Column(name="REVOKED", nullable=false)
    @Convert(converter = BooleanNumberConverter.class)
    private Boolean revoked = Boolean.FALSE;

    public boolean isRevoked() {
        return Boolean.TRUE.equals(revoked);
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.tenantId == null)  this.tenantId  = SecurityContextHelper.getTenantId();
    }
}
