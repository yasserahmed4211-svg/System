package com.example.erp.finance.gl.entity;

import com.example.erp.common.converter.BooleanNumberConverter;
import com.example.erp.common.domain.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static java.util.Optional.ofNullable;

@Entity
@Table(name = "GL_JOURNAL_HDR",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_GL_JOURNAL_NO", columnNames = {"JOURNAL_NO"})
        },
        indexes = {
                @Index(name = "IDX_GL_JOURNAL_HDR_DATE", columnList = "JOURNAL_DATE"),
                @Index(name = "IDX_GL_JOURNAL_HDR_TYPE_FK", columnList = "JOURNAL_TYPE_ID_FK"),
                @Index(name = "IDX_GL_JOURNAL_HDR_STATUS_FK", columnList = "STATUS_ID_FK"),
                @Index(name = "IDX_GL_JOURNAL_HDR_SRC_MOD", columnList = "SOURCE_MODULE_ID_FK"),
                @Index(name = "IDX_GL_JOURNAL_HDR_ACTIVE", columnList = "ACTIVE_FL"),
                @Index(name = "IDX_GL_JOURNAL_HDR_POST_FK", columnList = "SOURCE_POSTING_ID_FK")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GlJournalHdr extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gl_journal_hdr_seq")
    @SequenceGenerator(name = "gl_journal_hdr_seq", sequenceName = "GL_JOURNAL_HDR_SEQ", allocationSize = 1)
    @Column(name = "ID_PK")
    private Long id;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "JOURNAL_NO", length = 50, nullable = false, unique = true)
    private String journalNo;

    @NotNull(message = "{validation.required}")
    @Column(name = "JOURNAL_DATE", nullable = false)
    private LocalDate journalDate;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "JOURNAL_TYPE_ID_FK", length = 50, nullable = false)
    private String journalTypeIdFk;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "STATUS_ID_FK", length = 50, nullable = false)
    private String statusIdFk;

    @Size(max = 500, message = "{validation.size}")
    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Size(max = 50, message = "{validation.size}")
    @Column(name = "SOURCE_MODULE_ID_FK", length = 50)
    private String sourceModuleIdFk;

    @Size(max = 50, message = "{validation.size}")
    @Column(name = "SOURCE_DOC_TYPE_ID", length = 50)
    private String sourceDocTypeId;

    @Column(name = "SOURCE_DOC_ID_FK")
    private Long sourceDocIdFk;

    @Column(name = "SOURCE_POSTING_ID_FK")
    private Long sourcePostingIdFk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SOURCE_POSTING_ID_FK", insertable = false, updatable = false)
    private AccPostingMst posting;

    @NotNull
    @Column(name = "TOTAL_DEBIT", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalDebit = BigDecimal.ZERO;

    @NotNull
    @Column(name = "TOTAL_CREDIT", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @NotNull
    @Column(name = "ACTIVE_FL", nullable = false)
    @Convert(converter = BooleanNumberConverter.class)
    @Builder.Default
    private Boolean activeFl = Boolean.TRUE;

    @OneToMany(mappedBy = "journalHdr", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GlJournalLine> lines = new ArrayList<>();

    // ── Lifecycle Hooks ─────────────────────────────────────

    @PrePersist
    private void prePersist() {
        if (journalTypeIdFk != null) journalTypeIdFk = journalTypeIdFk.toUpperCase();
        if (sourceModuleIdFk != null) sourceModuleIdFk = sourceModuleIdFk.toUpperCase();
        if (statusIdFk != null) statusIdFk = statusIdFk.toUpperCase();
    }

    // ── Convenience methods ─────────────────────────────────

    public void addLine(GlJournalLine line) {
        lines.add(line);
        line.setJournalHdr(this);
    }

    public void removeLine(GlJournalLine line) {
        lines.remove(line);
        line.setJournalHdr(null);
    }

    public void replaceLines(List<GlJournalLine> newLines) {
        this.lines.clear();
        if (newLines != null) {
            newLines.forEach(this::addLine);
        }
    }

    /**
     * Recalculate totalDebit and totalCredit from lines.
     */
    public void recalculateTotals() {
        this.totalDebit = lines.stream()
                .map(l -> l.getDebitAmount() != null ? l.getDebitAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalCredit = lines.stream()
                .map(l -> l.getCreditAmount() != null ? l.getCreditAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Check if debit and credit totals are balanced.
     */
    public boolean isBalanced() {
        recalculateTotals();
        return totalDebit.compareTo(totalCredit) == 0;
    }

    // ── Active State Helpers ─────────────────────────────────

    public void activate() {
        this.activeFl = Boolean.TRUE;
    }

    public void deactivate() {
        this.activeFl = Boolean.FALSE;
    }
}
