package com.example.security.domain;

import com.example.erp.common.converter.BooleanNumberConverter;
import com.example.erp.common.domain.TenantAuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "USERS",
       uniqueConstraints = {@UniqueConstraint(name="UK_USERS_TENANT_USERNAME", columnNames={"TENANT_ID","USERNAME"})},
       indexes = {
           @Index(name = "IDX_USERS_TENANT", columnList = "TENANT_ID"),
       @Index(name = "IDX_USERS_ENABLED", columnList = "ENABLED"),
       @Index(name = "IDX_USERS_USERNAME", columnList = "USERNAME")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class UserAccount extends TenantAuditableEntity {

    @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
    private Long id;

    @Column(name = "USERNAME", nullable=false, length=80)
    private String username;

    @Column(name = "PASSWORD", nullable=false, length=200)
    private String password;

    @Column(name = "ENABLED", nullable=false)
    @Builder.Default
    @Convert(converter = BooleanNumberConverter.class)
    private Boolean enabled = Boolean.TRUE;

    public boolean isEnabled() {
      return Boolean.TRUE.equals(enabled);
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "USER_ROLES",
      joinColumns = @JoinColumn(name="USER_ID", referencedColumnName = "ID"),
      inverseJoinColumns = @JoinColumn(name="ROLE_ID", referencedColumnName = "ID"))
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}
