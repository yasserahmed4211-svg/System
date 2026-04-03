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
@Table(name = "ACC_RULE_HDR",
        uniqueConstraints = {
                @UniqueConstraint(name = "ACC_RULE_HDR_UK",
                        columnNames = {"COMPANY_ID_FK", "SOURCE_MODULE", "SOURCE_DOC_TYPE"})
        },
        indexes = {
                @Index(name = "IDX_ACC_RULE_HDR_ACTIVE", columnList = "IS_ACTIVE"),
                @Index(name = "IDX_ACC_RULE_HDR_COMPANY_FK", columnList = "COMPANY_ID_FK"),
                @Index(name = "IDX_ACC_RULE_HDR_MODULE", columnList = "SOURCE_MODULE")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AccRuleHdr extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "acc_rule_hdr_seq")
    @SequenceGenerator(name = "acc_rule_hdr_seq", sequenceName = "ACC_RULE_HDR_SEQ", allocationSize = 1)
    @Column(name = "RULE_ID")
    private Long ruleId;

    @NotNull(message = "{validation.required}")
    @Column(name = "COMPANY_ID_FK", nullable = false)
    private Long companyIdFk;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "SOURCE_MODULE", length = 50, nullable = false)
    private String sourceModule;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "SOURCE_DOC_TYPE", length = 50, nullable = false)
    private String sourceDocType;

    @NotNull
    @Column(name = "IS_ACTIVE", nullable = false)
    @Convert(converter = BooleanNumberConverter.class)
    @Builder.Default
    private Boolean isActive = Boolean.TRUE;

    @OneToMany(mappedBy = "ruleHdr", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AccRuleLine> lines = new ArrayList<>();

    /**
     * Adds a line to this rule header, maintaining the bidirectional relationship.
     */
    public void addLine(AccRuleLine line) {
        lines.add(line);
        line.setRuleHdr(this);
    }

    /**
     * Removes a line from this rule header.
     */
    public void removeLine(AccRuleLine line) {
        lines.remove(line);
        line.setRuleHdr(null);
    }

    /**
     * Clears all lines and replaces with new ones.
     */
    public void replaceLines(List<AccRuleLine> newLines) {
        this.lines.clear();
        if (newLines != null) {
            newLines.forEach(this::addLine);
        }
    }
}
