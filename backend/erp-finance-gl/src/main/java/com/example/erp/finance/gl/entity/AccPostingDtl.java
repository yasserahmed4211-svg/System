package com.example.erp.finance.gl.entity;

import com.example.erp.common.domain.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * ACC_POSTING_DTL — source document detail lines for a posting.
 * <p>
 * ARCHITECTURE NOTE: These detail lines are NOT the source of accounting amounts
 * for journal generation. The Rule Engine (AccRuleHdr/AccRuleLine) is the SOLE
 * source of amounts, accounts, and entry sides.
 * <p>
 * The amount, sign, and businessSide fields are descriptive metadata from the
 * source module (e.g., billing, contract). They are preserved for:
 * <ul>
 *   <li>Entity extraction (customerIdFk, supplierIdFk) used by the posting engine</li>
 *   <li>Audit trail and source document reference</li>
 *   <li>Future extensibility</li>
 * </ul>
 */
@Entity
@Table(name = "ACC_POSTING_DTL",
        indexes = {
                @Index(name = "IDX_ACC_POSTING_DTL_MST_FK", columnList = "POSTING_ID_FK"),
                @Index(name = "IDX_ACC_POSTING_DTL_SIDE", columnList = "BUSINESS_SIDE")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AccPostingDtl extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "acc_posting_dtl_seq")
    @SequenceGenerator(name = "acc_posting_dtl_seq", sequenceName = "ACC_POSTING_DTL_SEQ", allocationSize = 1)
    @Column(name = "POSTING_DTL_ID")
    private Long postingDtlId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POSTING_ID_FK", nullable = false,
            foreignKey = @ForeignKey(name = "FK_ACC_POSTING_DTL_MST"))
    private AccPostingMst postingMst;

    @PrePersist
    protected void onCreate() {
        if (businessSide != null) businessSide = businessSide.toUpperCase();
    }

    @PreUpdate
    protected void onUpdate() {
        if (businessSide != null) businessSide = businessSide.toUpperCase();
    }

    @NotNull(message = "{validation.required}")
    @Column(name = "LINE_NO", nullable = false)
    private Integer lineNo;

    @NotNull(message = "{validation.required}")
    @Column(name = "AMOUNT", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "BUSINESS_SIDE", length = 50, nullable = false)
    private String businessSide;

    @NotNull(message = "{validation.required}")
    @Column(name = "SIGN", nullable = false)
    private Integer sign;

    @Size(max = 500, message = "{validation.size}")
    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "CUSTOMER_ID_FK")
    private Long customerIdFk;

    @Column(name = "SUPPLIER_ID_FK")
    private Long supplierIdFk;

    @Column(name = "COST_CENTER_ID_FK")
    private Long costCenterIdFk;

    @Column(name = "CONTRACT_ID_FK")
    private Long contractIdFk;

    @Column(name = "ITEM_ID_FK")
    private Long itemIdFk;
}
