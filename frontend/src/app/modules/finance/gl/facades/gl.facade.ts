import { Injectable, inject, signal, computed, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, tap, EMPTY, finalize } from 'rxjs';

import { GlApiService } from 'src/app/modules/finance/gl/services/gl-api.service';
import {
  AccountChartDto,
  AccountChartTreeNode,
  AccRuleHdrDto,
  CreateAccountRequest,
  UpdateAccountRequest,
  CreateRuleRequest,
  UpdateRuleRequest,
  EligibleParentAccountDto,
  SearchFilter,
  SearchRequest,
  ContractFilter
} from 'src/app/modules/finance/gl/models/gl.model';

import { ErpErrorMapperService } from 'src/app/shared/services/erp-error-mapper.service';
import { extractBackendErrorCode, extractBackendErrorMessage } from 'src/app/shared/utils/backend-error-message';

/**
 * GlFacade – single point of contact between GL components and services.
 *
 * Manages:
 * - Loading / error / saving state via signals
 * - Chart of accounts CRUD + tree
 * - Accounting rules CRUD
 * - Error mapping through ErpErrorMapperService
 *
 * @requirement FE-REQ-GL-001
 * @task TASK-FE-GL-001
 */
@Injectable()
export class GlFacade {
  private readonly api = inject(GlApiService);
  private readonly errorMapper = inject(ErpErrorMapperService);
  private readonly destroyRef = inject(DestroyRef);

  // ── Accounts state ────────────────────────────────────────

  private readonly accountsSignal = signal<AccountChartDto[]>([]);
  private readonly accountsTotalSignal = signal<number>(0);
  private readonly accountsLoadingSignal = signal<boolean>(false);
  private readonly accountsErrorSignal = signal<string | null>(null);
  private readonly accountTreeSignal = signal<AccountChartTreeNode[]>([]);
  private readonly accountTreeLoadingSignal = signal<boolean>(false);
  private readonly accountSubTreeSignal = signal<AccountChartTreeNode | null>(null);
  private readonly accountSubTreeLoadingSignal = signal<boolean>(false);
  private readonly eligibleParentsSignal = signal<EligibleParentAccountDto[]>([]);
  private readonly eligibleParentsLoadingSignal = signal<boolean>(false);
  private readonly eligibleParentsTotalSignal = signal<number>(0);

  // Accounts pagination + filters
  /** Single source of truth for the last accounts search request. */
  private readonly lastAccountSearchRequestSignal = signal<SearchRequest>({
    filters: [],
    sorts: [],
    page: 0,
    size: 20
  });
  private readonly accountFiltersSignal = signal<SearchFilter[]>([]);

  // ── Rules state ───────────────────────────────────────────

  private readonly rulesSignal = signal<AccRuleHdrDto[]>([]);
  private readonly rulesTotalSignal = signal<number>(0);
  private readonly rulesLoadingSignal = signal<boolean>(false);
  private readonly rulesErrorSignal = signal<string | null>(null);

  // Rules pagination + filters
  /** Single source of truth for the last rules search request. */
  private readonly lastRuleSearchRequestSignal = signal<SearchRequest>({
    filters: [],
    sorts: [{ field: 'ruleId', direction: 'DESC' }],
    page: 0,
    size: 20
  });
  private readonly ruleFiltersSignal = signal<SearchFilter[]>([]);

  // ── Shared operation state ────────────────────────────────

  private readonly savingSignal = signal<boolean>(false);
  private readonly saveErrorSignal = signal<string | null>(null);

  // ── Public computed accessors ─────────────────────────────

  readonly accounts = computed(() => this.accountsSignal());
  readonly accountsTotal = computed(() => this.accountsTotalSignal());
  readonly accountsLoading = computed(() => this.accountsLoadingSignal());
  readonly accountsError = computed(() => this.accountsErrorSignal());
  readonly accountTree = computed(() => this.accountTreeSignal());
  readonly accountTreeLoading = computed(() => this.accountTreeLoadingSignal());
  readonly accountSubTree = computed(() => this.accountSubTreeSignal());
  readonly accountSubTreeLoading = computed(() => this.accountSubTreeLoadingSignal());
  readonly eligibleParents = computed(() => this.eligibleParentsSignal());
  readonly eligibleParentsLoading = computed(() => this.eligibleParentsLoadingSignal());
  readonly eligibleParentsTotal = computed(() => this.eligibleParentsTotalSignal());
  readonly accountFilters = computed(() => this.accountFiltersSignal());
  readonly accountCurrentPage = computed(() => this.lastAccountSearchRequestSignal().page);
  readonly accountPageSize = computed(() => this.lastAccountSearchRequestSignal().size);

  readonly rules = computed(() => this.rulesSignal());
  readonly rulesTotal = computed(() => this.rulesTotalSignal());
  readonly rulesLoading = computed(() => this.rulesLoadingSignal());
  readonly rulesError = computed(() => this.rulesErrorSignal());
  readonly ruleFilters = computed(() => this.ruleFiltersSignal());
  readonly ruleCurrentPage = computed(() => this.lastRuleSearchRequestSignal().page);
  readonly rulePageSize = computed(() => this.lastRuleSearchRequestSignal().size);

  readonly saving = computed(() => this.savingSignal());
  readonly saveError = computed(() => this.saveErrorSignal());

  // ══════════════════════════════════════════════════════════
  // CHART OF ACCOUNTS
  // ══════════════════════════════════════════════════════════

  loadAccounts(): void {
    this.executeAccountSearch(this.lastAccountSearchRequestSignal());
  }

  /**
   * Execute an accounts search request against the API.
   * Stores the request as the last search request for future reloads.
   */
  private executeAccountSearch(request: SearchRequest): void {
    this.lastAccountSearchRequestSignal.set(request);
    this.accountsLoadingSignal.set(true);
    this.accountsErrorSignal.set(null);

    this.api
      .searchAccounts(request)
      .pipe(
        tap((response) => {
          this.accountsSignal.set(response?.content ?? []);
          this.accountsTotalSignal.set(response?.totalElements ?? 0);
        }),
        catchError((error) => {
          this.accountsErrorSignal.set(this.mapError(error));
          this.accountsSignal.set([]);
          this.accountsTotalSignal.set(0);
          return EMPTY;
        }),
        finalize(() => this.accountsLoadingSignal.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  getAccountById(pk: number, onSuccess?: (account: AccountChartDto) => void): void {
    this.accountsLoadingSignal.set(true);
    this.accountsErrorSignal.set(null);

    this.api.getAccountById(pk).pipe(
      tap((account) => onSuccess?.(account)),
      catchError((error) => {
        this.accountsErrorSignal.set(this.mapError(error));
        return EMPTY;
      }),
      finalize(() => this.accountsLoadingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  createAccount(request: CreateAccountRequest, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.createAccount(request).pipe(
      tap(() => {
        this.loadAccounts();
        onSuccess?.();
      }),
      catchError((error) => {
        this.saveErrorSignal.set(this.mapError(error));
        return EMPTY;
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  updateAccount(pk: number, request: UpdateAccountRequest, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.updateAccount(pk, request).pipe(
      tap(() => {
        this.loadAccounts();
        onSuccess?.();
      }),
      catchError((error) => {
        this.saveErrorSignal.set(this.mapError(error));
        return EMPTY;
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  deactivateAccount(pk: number, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.deactivateAccount(pk).pipe(
      tap(() => {
        this.loadAccounts();
        onSuccess?.();
      }),
      catchError((error) => {
        this.saveErrorSignal.set(this.mapError(error));
        return EMPTY;
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  loadAccountTree(organizationFk?: number, accountType?: string): void {
    this.accountTreeLoadingSignal.set(true);

    this.api.getAccountTree(organizationFk, accountType).pipe(
      tap((tree) => this.accountTreeSignal.set(tree ?? [])),
      catchError(() => {
        this.accountTreeSignal.set([]);
        return EMPTY;
      }),
      finalize(() => this.accountTreeLoadingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  loadAccountSubTree(accountChartPk: number, onSuccess?: (node: AccountChartTreeNode) => void): void {
    this.accountSubTreeLoadingSignal.set(true);

    this.api.getAccountSubTree(accountChartPk).pipe(
      tap((node) => {
        this.accountSubTreeSignal.set(node ?? null);
        if (node) onSuccess?.(node);
      }),
      catchError(() => {
        this.accountSubTreeSignal.set(null);
        return EMPTY;
      }),
      finalize(() => this.accountSubTreeLoadingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Load eligible parent accounts for the LOV (erp-dual-list).
   * Excludes the given account and its descendants to prevent circular references.
   */
  loadEligibleParents(organizationFk: number, excludeAccountPk?: number, search?: string,
                      page: number = 0, size: number = 100): void {
    this.eligibleParentsLoadingSignal.set(true);

    this.api.getEligibleParents({ organizationFk, excludeAccountPk, search, page, size }).pipe(
      tap((response) => {
        this.eligibleParentsSignal.set(response?.content ?? []);
        this.eligibleParentsTotalSignal.set(response?.totalElements ?? 0);
      }),
      catchError(() => {
        this.eligibleParentsSignal.set([]);
        this.eligibleParentsTotalSignal.set(0);
        return EMPTY;
      }),
      finalize(() => this.eligibleParentsLoadingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  clearEligibleParents(): void {
    this.eligibleParentsSignal.set([]);
    this.eligibleParentsTotalSignal.set(0);
  }

  // ── Inline tree CRUD helpers ──────────────────────────────

  /**
   * Create account and insert the new node into the tree signal in-place.
   * If parentPk is provided, inserts under that parent; otherwise adds as root.
   */
  createAccountInTree(
    request: CreateAccountRequest,
    organizationFk?: number,
    accountType?: string,
    onSuccess?: (created: AccountChartDto) => void
  ): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.createAccount(request).pipe(
      tap((created) => {
        // Reload the full tree to get correct structure from backend
        this.loadAccountTree(organizationFk, accountType);
        onSuccess?.(created);
      }),
      catchError((error) => {
        this.saveErrorSignal.set(this.mapError(error));
        return EMPTY;
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Update account and refresh the tree to reflect changes.
   */
  updateAccountInTree(
    pk: number,
    request: UpdateAccountRequest,
    organizationFk?: number,
    accountType?: string,
    onSuccess?: () => void
  ): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.updateAccount(pk, request).pipe(
      tap(() => {
        this.loadAccountTree(organizationFk, accountType);
        onSuccess?.();
      }),
      catchError((error) => {
        this.saveErrorSignal.set(this.mapError(error));
        return EMPTY;
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Deactivate account and refresh the tree.
   */
  deactivateAccountInTree(
    pk: number,
    organizationFk?: number,
    accountType?: string,
    onSuccess?: () => void
  ): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.deactivateAccount(pk).pipe(
      tap(() => {
        this.loadAccountTree(organizationFk, accountType);
        onSuccess?.();
      }),
      catchError((error) => {
        this.saveErrorSignal.set(this.mapError(error));
        return EMPTY;
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Toggle account active/inactive status via update (no delete).
   * Uses PUT to set isActive flag, then refreshes tree.
   */
  toggleAccountStatusInTree(
    pk: number,
    request: UpdateAccountRequest,
    organizationFk?: number,
    accountType?: string,
    onSuccess?: () => void
  ): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.updateAccount(pk, request).pipe(
      tap(() => {
        this.loadAccountTree(organizationFk, accountType);
        onSuccess?.();
      }),
      catchError((error) => {
        this.saveErrorSignal.set(this.mapError(error));
        return EMPTY;
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  applyAccountGridStateAndLoad(state: {
    page: number;
    size: number;
    sortBy: string;
    sortDir: string;
    filters: SearchFilter[];
  }): void {
    this.accountFiltersSignal.set(state.filters);
    const contractFilters = this.toContractFilters(state.filters);
    const request: SearchRequest = {
      filters: contractFilters,
      sorts: state.sortBy ? [{ field: state.sortBy, direction: state.sortDir as 'ASC' | 'DESC' }] : [],
      page: state.page,
      size: state.size
    };
    this.executeAccountSearch(request);
  }

  setAccountFilters(filters: SearchFilter[]): void {
    this.accountFiltersSignal.set(filters);
    const contractFilters = this.toContractFilters(filters);
    this.lastAccountSearchRequestSignal.update(r => ({ ...r, filters: contractFilters, page: 0 }));
  }

  // ══════════════════════════════════════════════════════════
  // ACCOUNTING RULES
  // ══════════════════════════════════════════════════════════

  loadRules(): void {
    this.executeRuleSearch(this.lastRuleSearchRequestSignal());
  }

  private normalizeRuleSortField(field?: string | null): string {
    return !field || field === 'id' ? 'ruleId' : field;
  }

  /**
   * Execute a rules search request against the API.
   * Stores the request as the last search request for future reloads.
   */
  private executeRuleSearch(request: SearchRequest): void {
    const normalizedSorts = request.sorts?.length
      ? request.sorts.map((sort) => ({
          ...sort,
          field: this.normalizeRuleSortField(sort.field)
        }))
      : [{ field: 'ruleId', direction: 'DESC' as const }];

    const normalizedRequest: SearchRequest = {
      ...request,
      sorts: normalizedSorts
    };

    this.lastRuleSearchRequestSignal.set(normalizedRequest);
    this.rulesLoadingSignal.set(true);
    this.rulesErrorSignal.set(null);

    this.api
      .searchRules(normalizedRequest)
      .pipe(
        tap((response) => {
          this.rulesSignal.set(response?.content ?? []);
          this.rulesTotalSignal.set(response?.totalElements ?? 0);
        }),
        catchError((error) => {
          this.rulesErrorSignal.set(this.mapError(error));
          this.rulesSignal.set([]);
          this.rulesTotalSignal.set(0);
          return EMPTY;
        }),
        finalize(() => this.rulesLoadingSignal.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  getRuleById(ruleId: number, onSuccess?: (rule: AccRuleHdrDto) => void): void {
    this.rulesLoadingSignal.set(true);
    this.rulesErrorSignal.set(null);

    this.api.getRuleById(ruleId).pipe(
      tap((rule) => onSuccess?.(rule)),
      catchError((error) => {
        this.rulesErrorSignal.set(this.mapError(error));
        return EMPTY;
      }),
      finalize(() => this.rulesLoadingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  createRule(request: CreateRuleRequest, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.createRule(request).pipe(
      tap(() => {
        this.loadRules();
        onSuccess?.();
      }),
      catchError((error) => {
        this.saveErrorSignal.set(this.mapError(error));
        return EMPTY;
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  updateRule(ruleId: number, request: UpdateRuleRequest, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.updateRule(ruleId, request).pipe(
      tap(() => {
        this.loadRules();
        onSuccess?.();
      }),
      catchError((error) => {
        this.saveErrorSignal.set(this.mapError(error));
        return EMPTY;
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  deactivateRule(ruleId: number, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.deactivateRule(ruleId).pipe(
      tap(() => {
        this.loadRules();
        onSuccess?.();
      }),
      catchError((error) => {
        this.saveErrorSignal.set(this.mapError(error));
        return EMPTY;
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  applyRuleGridStateAndLoad(state: {
    page: number;
    size: number;
    sortBy: string;
    sortDir: string;
    filters: SearchFilter[];
  }): void {
    const sortField = this.normalizeRuleSortField(state.sortBy);

    this.ruleFiltersSignal.set(state.filters);
    const contractFilters = this.toContractFilters(state.filters);
    const request: SearchRequest = {
      filters: contractFilters,
      sorts: [{ field: sortField, direction: state.sortDir as 'ASC' | 'DESC' }],
      page: state.page,
      size: state.size
    };
    this.executeRuleSearch(request);
  }

  setRuleFilters(filters: SearchFilter[]): void {
    this.ruleFiltersSignal.set(filters);
    const contractFilters = this.toContractFilters(filters);
    this.lastRuleSearchRequestSignal.update(r => ({ ...r, filters: contractFilters, page: 0 }));
  }

  // ── Error helpers ─────────────────────────────────────────

  clearError(): void {
    this.accountsErrorSignal.set(null);
    this.rulesErrorSignal.set(null);
    this.saveErrorSignal.set(null);
  }

  /** Clear transient entity state when leaving an entry page. */
  clearCurrentEntity(): void {
    this.accountsSignal.set([]);
    this.accountsTotalSignal.set(0);
    this.accountTreeSignal.set([]);
    this.accountSubTreeSignal.set(null);
    this.eligibleParentsSignal.set([]);
    this.eligibleParentsTotalSignal.set(0);
    this.rulesSignal.set([]);
    this.rulesTotalSignal.set(0);
    this.clearError();
  }

  /** Convert domain SearchFilter[] to backend ContractFilter[] format. */
  private toContractFilters(filters: SearchFilter[]): ContractFilter[] {
    const result: ContractFilter[] = [];
    for (const f of filters) {
      if (f.value !== undefined && f.value !== null && f.value !== '') {
        result.push({
          field: f.field,
          operator: f.op === 'LIKE' ? 'CONTAINS' : f.op === 'EQ' ? 'EQUALS' : f.op,
          value: f.value
        });
      }
    }
    return result;
  }

  private mapError(error: unknown): string {
    const backendCode = extractBackendErrorCode(error);
    if (backendCode && this.errorMapper.hasMapping(backendCode)) {
      return this.errorMapper.mapError(backendCode).translationKey;
    }
    // Fallback: use the backend's localized message directly
    // (backend LocalizationService already resolves i18n messages)
    const backendMessage = extractBackendErrorMessage(error);
    if (backendMessage) {
      return backendMessage;
    }
    return 'ERRORS.OPERATION_FAILED';
  }
}
