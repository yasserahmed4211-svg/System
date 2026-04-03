package com.example.erp.finance.gl.entity;

import com.example.erp.common.domain.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "GL_JOURNAL_LINE",
        indexes = {
                @Index(name = "IDX_GL_JOURNAL_LINE_HDR_FK", columnList = "JOURNAL_ID_FK"),
                @Index(name = "IDX_GL_JOURNAL_LINE_ACCT_FK", columnList = "ACCOUNT_ID_FK"),
                @Index(name = "IDX_GL_JOURNAL_LINE_CUST_FK", columnList = "CUSTOMER_ID_FK"),
                @Index(name = "IDX_GL_JOURNAL_LINE_SUPP_FK", columnList = "SUPPLIER_ID_FK"),
                @Index(name = "IDX_GL_JOURNAL_LINE_CC_FK", columnList = "COST_CENTER_ID_FK")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GlJournalLine extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gl_journal_line_seq")
    @SequenceGenerator(name = "gl_journal_line_seq", sequenceName = "GL_JOURNAL_LINE_SEQ", allocationSize = 1)
    @Column(name = "ID_PK")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "JOURNAL_ID_FK", nullable = false)
    private GlJournalHdr journalHdr;

    @NotNull(message = "{validation.required}")
    @Column(name = "LINE_NO", nullable = false)
    private Integer lineNo;

    @NotNull(message = "{validation.required}")
    @Column(name = "ACCOUNT_ID_FK", nullable = false)
    private Long accountIdFk;

    @Column(name = "DEBIT_AMOUNT", precision = 18, scale = 2)
    private BigDecimal debitAmount;

    @Column(name = "CREDIT_AMOUNT", precision = 18, scale = 2)
    private BigDecimal creditAmount;

    @Column(name = "CUSTOMER_ID_FK")
    private Long customerIdFk;

    @Column(name = "SUPPLIER_ID_FK")
    private Long supplierIdFk;

    @Column(name = "COST_CENTER_ID_FK")
    private Long costCenterIdFk;

    @Size(max = 500, message = "{validation.size}")
    @Column(name = "DESCRIPTION", length = 500)
    private String description;
}
