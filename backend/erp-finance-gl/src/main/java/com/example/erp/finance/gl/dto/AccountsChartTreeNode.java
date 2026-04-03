package com.example.erp.finance.gl.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountsChartTreeNode {

    private Long accountChartPk;
    private String accountChartNo;
    private String accountChartName;
    private String accountType;
    private Integer level;
    private Boolean isActive;
    private Boolean isLeaf;
    private Long parentPk;
    private Long organizationFk;
    private int childCount;
    private List<AccountsChartTreeNode> children;
}
