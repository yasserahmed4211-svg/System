package com.example.erp.finance.gl.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccRuleHdrUpdateRequest {

    @JsonProperty("companyId")
    @JsonAlias("companyIdFk")
    @NotNull(message = "{validation.required}")
    private Long companyIdFk;

    @JsonProperty("sourceUnit")
    @JsonAlias("sourceModule")
    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    private String sourceModule;

    @JsonProperty("documentType")
    @JsonAlias("sourceDocType")
    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    private String sourceDocType;

    private Boolean isActive;

    @JsonProperty("debitLines")
    @NotEmpty(message = "{validation.not_empty}")
    @Valid
    @Builder.Default
    private List<AccRuleLineRequest> debitLines = new ArrayList<>();

    @JsonProperty("creditLines")
    @NotEmpty(message = "{validation.not_empty}")
    @Valid
    @Builder.Default
    private List<AccRuleLineRequest> creditLines = new ArrayList<>();

    public List<AccRuleLineRequest> getLines() {
        return Stream.concat(
                        normalizeLines(debitLines, "DEBIT").stream(),
                        normalizeLines(creditLines, "CREDIT").stream())
                .collect(Collectors.toList());
    }

    private List<AccRuleLineRequest> normalizeLines(List<AccRuleLineRequest> lines, String entrySide) {
        if (lines == null) {
            return List.of();
        }

        lines.forEach(line -> line.setEntrySide(entrySide));
        return lines;
    }
}
