package com.example.erp.finance.gl.entity;

import com.example.erp.common.converter.BooleanNumberConverter;
import com.example.erp.common.domain.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ACCOUNTS_CHART",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_ACCOUNTS_CHART_NO_ORG",
                        columnNames = {"ACCOUNT_CHART_NO", "ORGANIZATION_FK"})
        },
        indexes = {
                @Index(name = "IDX_ACCOUNTS_CHART_ACTIVE", columnList = "ACTIVE"),
                @Index(name = "IDX_ACCOUNTS_CHART_ORG_FK", columnList = "ORGANIZATION_FK"),
                @Index(name = "IDX_ACCOUNTS_CHART_PARENT_FK", columnList = "ACCOUNT_CHART_FK"),
                @Index(name = "IDX_ACCOUNTS_CHART_TYPE", columnList = "ACCOUNT_TYPE")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AccountsChart extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accounts_chart_seq")
    @SequenceGenerator(name = "accounts_chart_seq", sequenceName = "ACCOUNTS_CHART_SEQ", allocationSize = 1)
    @Column(name = "ACCOUNT_CHART_PK")
    private Long accountChartPk;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "ACCOUNT_CHART_NO", length = 50, nullable = false)
    private String accountChartNo;

    @NotBlank(message = "{validation.required}")
    @Size(max = 500, message = "{validation.size}")
    @Column(name = "ACCOUNT_CHART_NAME", length = 500, nullable = false)
    private String accountChartName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACCOUNT_CHART_FK")
    private AccountsChart parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @Builder.Default
    private List<AccountsChart> children = new ArrayList<>();

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "ACCOUNT_TYPE", length = 50, nullable = false)
    private String accountType;

    @NotNull(message = "{validation.required}")
    @Column(name = "ORGANIZATION_FK", nullable = false)
    private Long organizationFk;

    @Column(name = "ORGANIZATION_SUB_FK")
    private Long organizationSubFk;

    @NotNull
    @Column(name = "ACTIVE", nullable = false)
    @Convert(converter = BooleanNumberConverter.class)
    @Builder.Default
    private Boolean isActive = Boolean.TRUE;

    /**
     * Returns true if this account has no children (eligible for posting).
     */
    @Transient
    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }
}
