package com.example.erp.finance.gl.controller;

import com.example.erp.common.domain.status.ServiceResult;
import com.example.erp.common.web.ApiResponse;
import com.example.erp.common.web.OperationCode;
import com.erp.common.search.SearchRequest;
import com.example.erp.finance.gl.dto.*;
import com.example.erp.finance.gl.service.AccountsChartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gl/accounts")
@RequiredArgsConstructor
@Tag(name = "Chart of Accounts", description = "دليل الحسابات")
public class AccountsChartController {

    private final AccountsChartService accountsChartService;
    private final OperationCode operationCode;

    @PostMapping("/search")
    @Operation(summary = "Search Accounts",
            description = "Search accounts with dynamic filters, sorting, and pagination. "
                    + "Allowed filter fields: accountChartNo, accountChartName, accountType, isActive, organizationFk, parent.accountChartPk. "
                    + "Allowed sort fields: accountChartPk, accountChartNo, accountChartName, accountType, isActive, organizationFk, createdAt, updatedAt.")
    public ResponseEntity<ApiResponse<Page<AccountsChartResponse>>> search(@RequestBody AccountsChartSearchContractRequest searchRequest) {
        return operationCode.craftResponse(accountsChartService.search(searchRequest.toCommonSearchRequest()));
    }

    @GetMapping("/eligible-parents")
    @Operation(summary = "Eligible Parent Accounts (LOV)",
            description = "Returns paginated list of accounts eligible as parents. "
                    + "Excludes the given account and its descendants to prevent circular references. "
                    + "Only returns active accounts. Searchable by account code or name.")
    public ResponseEntity<ApiResponse<Page<EligibleParentAccountDto>>> getEligibleParents(
            @RequestParam Long organizationFk,
            @RequestParam(required = false) Long excludeAccountPk,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return operationCode.craftResponse(accountsChartService.getEligibleParents(excludeAccountPk, organizationFk, search, page, size));
    }

    @GetMapping("/{accountChartPk}")
    @Operation(summary = "Get Account Details")
    public ResponseEntity<ApiResponse<AccountsChartResponse>> getById(@PathVariable Long accountChartPk) {
        return operationCode.craftResponse(accountsChartService.getById(accountChartPk));
    }

    @PostMapping
    @Operation(summary = "Create Account",
            description = "Creates an account with auto-generated ACCOUNT_CHART_NO based on hierarchy. "
                    + "Set accountChartFk to null for root accounts, or to a parent PK for child accounts.")
    public ResponseEntity<ApiResponse<AccountsChartResponse>> create(@Valid @RequestBody AccountsChartCreateRequest request) {
        return operationCode.craftResponse(accountsChartService.create(request));
    }

    @PutMapping("/{accountChartPk}")
    @Operation(summary = "Update Account",
            description = "Updates account details. Account number (ACCOUNT_CHART_NO) is immutable and auto-generated.")
    public ResponseEntity<ApiResponse<AccountsChartResponse>> update(
            @PathVariable Long accountChartPk,
            @Valid @RequestBody AccountsChartUpdateRequest request) {
        return operationCode.craftResponse(accountsChartService.update(accountChartPk, request));
    }

    @DeleteMapping("/{accountChartPk}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate Account (Soft Delete)")
    public void deactivate(@PathVariable Long accountChartPk) {
        accountsChartService.deactivate(accountChartPk);
    }

    @GetMapping("/tree")
    @Operation(summary = "Account Tree View",
            description = "Retrieves the full hierarchical tree of accounts. "
                    + "Optionally filter by organization and/or account type (ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE, COGS, OTHER_COST, INTERNAL, STATISTICAL). "
                    + "When no organization is provided, returns all accounts.")
    public ResponseEntity<ApiResponse<List<AccountsChartTreeNode>>> getTree(
            @RequestParam(required = false) Long organizationFk,
            @RequestParam(required = false) String accountType) {
        return operationCode.craftResponse(accountsChartService.getTree(organizationFk, accountType));
    }

    @GetMapping("/tree/{accountChartPk}")
    @Operation(summary = "Account Sub-Tree View",
            description = "Retrieves the subtree starting from a specific account. Useful for lazy-loading tree nodes.")
    public ResponseEntity<ApiResponse<AccountsChartTreeNode>> getSubTree(@PathVariable Long accountChartPk) {
        return operationCode.craftResponse(accountsChartService.getSubTree(accountChartPk));
    }

    @GetMapping("/lookup")
    @Operation(summary = "Account Lookup (Paginated)",
            description = "Paginated account search for lookup dialogs. Searches by account number / name. Returns only active leaf accounts by default (no children). Set leafOnly=false to include parent accounts.")
    public ResponseEntity<ApiResponse<Page<AccountLookupDto>>> lookup(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "15") Integer size,
            @RequestParam(defaultValue = "true") Boolean leafOnly) {
        return operationCode.craftResponse(accountsChartService.lookupAccounts(search, page, size, leafOnly));
    }

    @GetMapping("/lookup/{accountChartPk}")
    @Operation(summary = "Account Lookup By ID",
            description = "Returns the display label for a single account. Used to resolve existing form values.")
    public ResponseEntity<ApiResponse<AccountLookupDto>> lookupById(@PathVariable Long accountChartPk) {
        return operationCode.craftResponse(accountsChartService.lookupAccountById(accountChartPk));
    }
}
