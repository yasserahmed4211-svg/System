import { Injectable, inject, signal, computed, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, tap, of, finalize } from 'rxjs';
import { PagesApiService } from 'src/app/modules/security/pages-registry/services/pages-api.service';
import {
  PageDto,
  SearchFilter,
  CreatePageRequest,
  UpdatePageRequest,
  FilterOperator,
  ContractFilter,
  PageSearchRequest
} from 'src/app/modules/security/pages-registry/models/page.model';

import { ErpErrorMapperService } from 'src/app/shared/services/erp-error-mapper.service';
import { extractBackendErrorCode } from 'src/app/shared/utils/backend-error-message';

import { SpecFilter, SpecOperator } from 'src/app/shared/models';

export interface PagesListState {
  pages: PageDto[];
  totalElements: number;
  loading: boolean;
  error: string | null;
}

@Injectable()
export class PagesFacade {
  private pagesApiService = inject(PagesApiService);
  private readonly errorMapper = inject(ErpErrorMapperService);
  private readonly destroyRef = inject(DestroyRef);

  // State signals
  private pagesSignal = signal<PageDto[]>([]);
  private totalElementsSignal = signal<number>(0);
  private loadingSignal = signal<boolean>(false);
  private errorSignal = signal<string | null>(null);
  private activePagesSignal = signal<PageDto[]>([]);
  private modulesSignal = signal<string[]>([]);

  // Filter state (domain-level SearchFilter[] before contract conversion)
  private currentFiltersSignal = signal<SearchFilter[]>([]);

  // Operation state
  private savingSignal = signal<boolean>(false);
  private saveErrorSignal = signal<string | null>(null);

  /** Single source of truth for the last search request sent to the API. */
  private lastSearchRequestSignal = signal<PageSearchRequest>({
    filters: [],
    sorts: [{ field: 'displayOrder', direction: 'ASC' }],
    page: 0,
    size: 20
  });

  // Computed state (public readonly)
  readonly pages = computed(() => this.pagesSignal());
  readonly totalElements = computed(() => this.totalElementsSignal());
  readonly loading = computed(() => this.loadingSignal());
  readonly error = computed(() => this.errorSignal());
  readonly activePages = computed(() => this.activePagesSignal());
  readonly modules = computed(() => this.modulesSignal());
  readonly currentPage = computed(() => this.lastSearchRequestSignal().page);
  readonly pageSize = computed(() => this.lastSearchRequestSignal().size);
  readonly saving = computed(() => this.savingSignal());
  readonly saveError = computed(() => this.saveErrorSignal());
  readonly currentFilters = computed(() => this.currentFiltersSignal());

  /**
   * Load pages with current filters and pagination
   */
  loadPages(): void {
    this.executeSearch(this.lastSearchRequestSignal());
  }

  /**
   * Execute a search request against the API.
   * Stores the request as the last search request for future reloads.
   */
  private executeSearch(request: PageSearchRequest): void {
    this.lastSearchRequestSignal.set(request);
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    this.pagesApiService
      .searchPages(request)
      .pipe(
        tap((response) => {
          this.pagesSignal.set(response?.content ?? []);
          this.totalElementsSignal.set(response?.totalElements ?? 0);
        }),
        catchError((error) => {
          const backendCode = extractBackendErrorCode(error);
          const mappedKey =
            backendCode && this.errorMapper.hasMapping(backendCode)
              ? this.errorMapper.mapError(backendCode).translationKey
              : null;
          this.errorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
          this.pagesSignal.set([]);
          this.totalElementsSignal.set(0);
          return of(null);
        }),
        finalize(() => this.loadingSignal.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  /**
   * Load active pages for dropdowns
   */
  loadActivePages(): void {
    this.pagesApiService.getActivePages().pipe(
      tap(response => {
        if (response && Array.isArray(response)) {
          this.activePagesSignal.set(response);
        } else {
          this.activePagesSignal.set([]);
        }
      }),
      catchError(() => {
        this.activePagesSignal.set([]);
        return of(null);
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Load available modules for filter dropdown
   * Fetches all pages and extracts unique modules
   */
  loadModules(): void {
    // Fetch pages to extract unique modules for filter dropdown
    this.pagesApiService.searchPages({ filters: [], sorts: [], page: 0, size: 100 }).pipe(
      tap(response => {
        const content = (response as any)?.content;
        if (Array.isArray(content)) {
          this.modulesSignal.set(this.pagesApiService.getModulesFromPages(content));
        } else {
          this.modulesSignal.set([]);
        }
      }),
      catchError(() => {
        this.modulesSignal.set([]);
        return of(null);
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Get page by ID (for edit mode in form page)
   */
  getPageById(id: number, onSuccess?: (page: PageDto) => void): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    this.pagesApiService.getPageById(id).pipe(
      tap((page) => onSuccess?.(page)),
      catchError((error) => {
        const backendCode = extractBackendErrorCode(error);
        const mappedKey =
          backendCode && this.errorMapper.hasMapping(backendCode)
            ? this.errorMapper.mapError(backendCode).translationKey
            : null;
        this.errorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
        return of(null);
      }),
      finalize(() => this.loadingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Create a new page
   */
  createPage(request: CreatePageRequest, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.pagesApiService.createPage(request).pipe(
      tap((response) => {
        if (response && response.id) {
          this.loadPages();
          this.loadActivePages();
          this.loadModules();
          onSuccess?.();
        } else {
          this.saveErrorSignal.set('ERRORS.OPERATION_FAILED');
        }
      }),
      catchError((error) => {
        const backendCode = extractBackendErrorCode(error);
        const mappedKey =
          backendCode && this.errorMapper.hasMapping(backendCode)
            ? this.errorMapper.mapError(backendCode).translationKey
            : null;
        this.saveErrorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
        return of(null);
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Update an existing page
   */
  updatePage(id: number, request: UpdatePageRequest, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.pagesApiService.updatePage(id, request).pipe(
      tap((response) => {
        if (response && response.id) {
          this.loadPages();
          this.loadActivePages();
          this.loadModules();
          onSuccess?.();
        } else {
          this.saveErrorSignal.set('ERRORS.OPERATION_FAILED');
        }
      }),
      catchError((error) => {
        const backendCode = extractBackendErrorCode(error);
        const mappedKey =
          backendCode && this.errorMapper.hasMapping(backendCode)
            ? this.errorMapper.mapError(backendCode).translationKey
            : null;
        this.saveErrorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
        return of(null);
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Deactivate a page (soft delete)
   */
  deactivatePage(id: number, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.pagesApiService.deactivatePage(id).pipe(
      tap((response) => {
        if (response && response.id) {
          this.loadPages();
          this.loadActivePages();
          this.loadModules();
          onSuccess?.();
        } else {
          this.saveErrorSignal.set('ERRORS.OPERATION_FAILED');
        }
      }),
      catchError((error) => {
        const backendCode = extractBackendErrorCode(error);
        const mappedKey =
          backendCode && this.errorMapper.hasMapping(backendCode)
            ? this.errorMapper.mapError(backendCode).translationKey
            : null;
        this.saveErrorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
        return of(null);
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Reactivate a page (restore from deactivated state)
   */
  reactivatePage(id: number, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.pagesApiService.reactivatePage(id).pipe(
      tap((response) => {
        if (response && response.id) {
          this.loadPages();
          this.loadActivePages();
          this.loadModules();
          onSuccess?.();
        } else {
          this.saveErrorSignal.set('ERRORS.OPERATION_FAILED');
        }
      }),
      catchError((error) => {
        const backendCode = extractBackendErrorCode(error);
        const mappedKey =
          backendCode && this.errorMapper.hasMapping(backendCode)
            ? this.errorMapper.mapError(backendCode).translationKey
            : null;
        this.saveErrorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
        return of(null);
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Toggle page active status (activate or deactivate based on current state)
   */
  togglePageActive(page: PageDto, onSuccess?: () => void): void {
    if (page.active) {
      this.deactivatePage(page.id, onSuccess);
    } else {
      this.reactivatePage(page.id, onSuccess);
    }
  }

  /**
   * Update filters and reload
   */
  setFilters(filters: SearchFilter[]): void {
    this.currentFiltersSignal.set(filters);
    const contractFilters = this.toContractFilters(filters);
    this.lastSearchRequestSignal.update(r => ({ ...r, filters: contractFilters, page: 0 }));
  }

  setSort(field: string, direction: string): void {
    this.lastSearchRequestSignal.update(r => ({
      ...r,
      sorts: [{ field, direction: direction as 'ASC' | 'DESC' }]
    }));
  }

  clearSort(): void {
    this.lastSearchRequestSignal.update(r => ({ ...r, sorts: [] }));
  }

  setPage(page: number): void {
    this.lastSearchRequestSignal.update(r => ({ ...r, page }));
  }

  setPageSize(size: number): void {
    this.lastSearchRequestSignal.update(r => ({ ...r, size, page: 0 }));
  }

  /**
   * Apply list state in one shot and load once (aligned with User Management).
   */
  applyGridStateAndLoad(state: {
    page: number;
    size: number;
    sortBy: string;
    sortDir: string;
    filters: SearchFilter[];
  }): void {
    this.currentFiltersSignal.set(state.filters);
    const contractFilters = this.toContractFilters(state.filters);
    const request: PageSearchRequest = {
      filters: contractFilters,
      sorts: state.sortBy ? [{ field: state.sortBy, direction: state.sortDir as 'ASC' | 'DESC' }] : [],
      page: state.page,
      size: state.size
    };
    this.executeSearch(request);
  }

  /** Clear transient entity state when leaving the page. */
  clearCurrentEntity(): void {
    this.pagesSignal.set([]);
    this.totalElementsSignal.set(0);
    this.errorSignal.set(null);
    this.saveErrorSignal.set(null);
  }

  /**
   * Clear error state
   */
  clearError(): void {
    this.errorSignal.set(null);
    this.saveErrorSignal.set(null);
  }

  /** Convert domain SearchFilter[] to backend ContractFilter[] format. */
  private toContractFilters(filters: SearchFilter[]): ContractFilter[] {
    const result: ContractFilter[] = [];
    for (const f of filters) {
      if (f.value !== undefined && f.value !== null && f.value !== '') {
        let operator = 'EQUALS';
        if (f.op === 'LIKE') operator = 'CONTAINS';
        else if (f.op === 'EQ') operator = 'EQUALS';
        else operator = f.op;
        result.push({ field: f.field, operator, value: f.value });
      }
    }
    return result;
  }

  /** Adapter from shared SpecFilter model to backend SearchFilter model. */
  convertSpecFiltersToSearchFilters(filters: SpecFilter[]): SearchFilter[] {
    return filters
      .map((f) => ({
        field: f.field,
        op: this.mapSpecOperator(f.operator),
        value: f.value
      }))
      .filter((f) => !!f.field && !!f.op)
      .map((f) => ({
        field: f.field,
        op: f.op,
        value: this.normalizeSearchValue(f.field, f.op, f.value)
      }));
  }

  private mapSpecOperator(op: SpecOperator): FilterOperator {
    const map: Record<SpecOperator, FilterOperator> = {
      eq: 'EQ',
      like: 'LIKE',
      gt: 'GT',
      lt: 'LT',
      in: 'IN',
      isNull: 'IS_NULL',
      isNotNull: 'IS_NOT_NULL'
    };
    return map[op];
  }

  private normalizeSearchValue(
    field: string,
    op: FilterOperator,
    value: SpecFilter['value']
  ): SearchFilter['value'] {
    if (op === 'IS_NULL' || op === 'IS_NOT_NULL') return undefined;

    if (op === 'IN') {
      if (Array.isArray(value)) return value as string[];
      if (typeof value === 'string') {
        return value
          .split(',')
          .map((v) => v.trim())
          .filter((v) => v.length > 0);
      }
      return value as any;
    }

    if (field === 'active') {
      if (value === true || value === 'true') return true;
      if (value === false || value === 'false') return false;
    }

    if ((field === 'id' || field === 'displayOrder') && value !== null && value !== undefined && value !== '') {
      return Number(value);
    }

    return value as any;
  }

  convertAgGridFiltersToSearchFilters(filterModel: Record<string, AgGridFilter>): SearchFilter[] {
    const filters: SearchFilter[] = [];

    Object.keys(filterModel).forEach(field => {
      const filter = filterModel[field];

      if (filter.filterType === 'text') {
        let filterValue: string | boolean = filter.filter;
        if (field === 'active') {
          filterValue = filter.filter === 'true';
        }
        const op = this.mapTextFilterType(filter.type);
        if (op) filters.push({ field, op, value: filterValue });
      } else if (filter.filterType === 'number') {
        const op = this.mapNumberFilterType(filter.type);
        if (op) {
          if (filter.type === 'inRange') {
            filters.push({ field, op, value: filter.filter, value2: filter.filterTo });
          } else {
            filters.push({ field, op, value: filter.filter });
          }
        }
      }
    });

    return filters;
  }

  private mapTextFilterType(type: string): FilterOperator | null {
    const map: Record<string, FilterOperator> = {
      equals: 'EQ',
      notEqual: 'NE',
      contains: 'LIKE',
      startsWith: 'LIKE',
      endsWith: 'LIKE'
    };
    return map[type] || null;
  }

  private mapNumberFilterType(type: string): FilterOperator | null {
    const map: Record<string, FilterOperator> = {
      equals: 'EQ',
      notEqual: 'NE',
      greaterThan: 'GT',
      greaterThanOrEqual: 'GE',
      lessThan: 'LT',
      lessThanOrEqual: 'LE',
      inRange: 'BETWEEN'
    };
    return map[type] || null;
  }

  convertCustomFiltersToSearchFilters(customFilters: CustomFilter[]): SearchFilter[] {
    return customFilters
      .filter(cf => {
        if (cf.op === 'IS_NULL' || cf.op === 'IS_NOT_NULL') {
          return true;
        }
        return cf.value !== null && cf.value !== undefined && cf.value !== '';
      })
      .map(cf => ({
        field: cf.field,
        op: cf.op,
        value: this.convertFilterValue(cf)
      }));
  }

  private convertFilterValue(filter: CustomFilter): string | number | boolean | string[] | undefined {
    if (filter.op === 'IS_NULL' || filter.op === 'IS_NOT_NULL') {
      return undefined;
    }

    if (filter.op === 'IN' && typeof filter.value === 'string') {
      return filter.value.split(',').map(v => v.trim());
    }

    if (filter.field === 'active') {
      if (filter.value === 'true' || filter.value === true) return true;
      if (filter.value === 'false' || filter.value === false) return false;
    }

    if (filter.field === 'id' || filter.field === 'displayOrder') {
      return Number(filter.value);
    }

    return filter.value;
  }
}

interface AgGridFilter {
  filterType: string;
  type: string;
  filter: any;
  filterTo?: number;
}

interface CustomFilter {
  field: string;
  op: FilterOperator;
  value: any;
}
