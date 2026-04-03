package com.example.erp.finance.gl.entity;

import com.example.erp.common.domain.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "ACC_RULE_LINE",
        indexes = {
                @Index(name = "IDX_ACC_RULE_LINE_HDR_FK", columnList = "RULE_ID_FK"),
                @Index(name = "IDX_ACC_RULE_LINE_ACCOUNT_FK", columnList = "ACCOUNT_ID_FK")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AccRuleLine extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "acc_rule_line_seq")
    @SequenceGenerator(name = "acc_rule_line_seq", sequenceName = "ACC_RULE_LINE_SEQ", allocationSize = 1)
    @Column(name = "RULE_LINE_ID")
    private Long ruleLineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RULE_ID_FK", nullable = false)
    private AccRuleHdr ruleHdr;

    @NotNull(message = "{validation.required}")
    @Column(name = "ACCOUNT_ID_FK", nullable = false)
    private Long accountIdFk;

    @NotBlank(message = "{validation.required}")
    @Size(max = 10, message = "{validation.size}")
    @Column(name = "ENTRY_SIDE", length = 10, nullable = false)
    private String entrySide;

    @NotNull(message = "{validation.required}")
    @Column(name = "PRIORITY", nullable = false)
    private Integer priority;

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Column(name = "AMOUNT_SOURCE_TYPE", length = 20, nullable = false)
    private String amountSourceType;

    @Column(name = "AMOUNT_SOURCE_VALUE", precision = 18, scale = 6)
    private BigDecimal amountSourceValue;

    @Size(max = 20, message = "{validation.size}")
    @Column(name = "PAYMENT_TYPE_CODE", length = 20)
    private String paymentTypeCode;

    @Size(max = 20, message = "{validation.size}")
    @Column(name = "ENTITY_TYPE", length = 20)
    private String entityType;
}
