package com.example.erp.finance.gl.entity;

import com.example.erp.common.domain.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ACC_POSTING_MST",
        indexes = {
                @Index(name = "IDX_ACC_POSTING_MST_STATUS", columnList = "STATUS"),
                @Index(name = "IDX_ACC_POSTING_MST_COMPANY_FK", columnList = "COMPANY_ID_FK"),
                @Index(name = "IDX_ACC_POSTING_MST_MODULE", columnList = "SOURCE_MODULE"),
                @Index(name = "IDX_ACC_POSTING_MST_JOURNAL_FK", columnList = "FIN_JOURNAL_ID_FK")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AccPostingMst extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "acc_posting_mst_seq")
    @SequenceGenerator(name = "acc_posting_mst_seq", sequenceName = "ACC_POSTING_MST_SEQ", allocationSize = 1)
    @Column(name = "POSTING_ID")
    private Long postingId;

    @Column(name = "BRANCH_ID_FK")
    private Long branchIdFk;

    @NotNull(message = "{validation.required}")
    @Column(name = "COMPANY_ID_FK", nullable = false)
    private Long companyIdFk;

    @Size(max = 3, message = "{validation.size}")
    @Column(name = "CURRENCY_CODE", length = 3)
    private String currencyCode;

    @NotNull(message = "{validation.required}")
    @Column(name = "DOC_DATE", nullable = false)
    private LocalDate docDate;

    @Size(max = 1000, message = "{validation.size}")
    @Column(name = "ERROR_MESSAGE", length = 1000)
    private String errorMessage;

    @Column(name = "FIN_JOURNAL_ID_FK")
    private Long finJournalIdFk;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FIN_JOURNAL_ID_FK", insertable = false, updatable = false)
    private GlJournalHdr journal;

    @Column(name = "REVERSAL_POSTING_ID_FK")
    private Long reversalPostingIdFk;

    @NotNull(message = "{validation.required}")
    @Column(name = "SOURCE_DOC_ID", nullable = false)
    private Long sourceDocId;

    @Size(max = 100, message = "{validation.size}")
    @Column(name = "SOURCE_DOC_NO", length = 100)
    private String sourceDocNo;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "SOURCE_DOC_TYPE", length = 50, nullable = false)
    private String sourceDocType;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "SOURCE_MODULE", length = 50, nullable = false)
    private String sourceModule;

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Column(name = "STATUS", length = 20, nullable = false)
    private String status;

    @Column(name = "TOTAL_AMOUNT", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "postingMst", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AccPostingDtl> details = new ArrayList<>();

    @Formula("(SELECT COUNT(*) FROM ACC_POSTING_DTL d WHERE d.POSTING_ID_FK = POSTING_ID)")
    private Integer detailCount;

    // ── Lifecycle Hooks ─────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        if (sourceModule != null) sourceModule = sourceModule.toUpperCase();
        if (sourceDocType != null) sourceDocType = sourceDocType.toUpperCase();
        if (status != null) status = status.toUpperCase();
    }

    @PreUpdate
    protected void onUpdate() {
        if (sourceModule != null) sourceModule = sourceModule.toUpperCase();
        if (sourceDocType != null) sourceDocType = sourceDocType.toUpperCase();
        if (status != null) status = status.toUpperCase();
    }

    // ── Convenience methods ─────────────────────────────────

    public void addDetail(AccPostingDtl detail) {
        details.add(detail);
        detail.setPostingMst(this);
    }

    public void removeDetail(AccPostingDtl detail) {
        details.remove(detail);
        detail.setPostingMst(null);
    }
}
