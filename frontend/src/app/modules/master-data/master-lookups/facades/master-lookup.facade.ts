import { Injectable, inject, signal, computed, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, tap, of, finalize } from 'rxjs';

import { MasterLookupApiService } from '../services/master-lookup-api.service';
import {
  MasterLookupDto,
  MasterLookupUsageDto,
  CreateMasterLookupRequest,
  UpdateMasterLookupRequest,
  LookupDetailDto,
  CreateLookupDetailRequest,
  UpdateLookupDetailRequest,
  SearchFilter,
  SearchRequest,
  SearchSort
} from '../models/master-lookup.model';

import { ErpErrorMapperService } from 'src/app/shared/services/erp-error-mapper.service';
import { extractBackendErrorCode } from 'src/app/shared/utils/backend-error-message';

/**
 * Facade for Master Lookup Management
 * Manages state and orchestrates API calls
 */
@Injectable()
export class MasterLookupFacade {
  private apiService = inject(MasterLookupApiService);
  private readonly errorMapper = inject(ErpErrorMapperService);
  private readonly destroyRef = inject(DestroyRef);

  // ============================================
  // MASTER LOOKUP STATE
  // ============================================

  private masterLookupsSignal = signal<MasterLookupDto[]>([]);
  private totalElementsSignal = signal<number>(0);
  private loadingSignal = signal<boolean>(false);
  private errorSignal = signal<string | null>(null);

  private currentFiltersSignal = signal<SearchFilter[]>([]);

  /** Single source of truth for the last search request sent to the API. */
  private lastSearchRequestSignal = signal<SearchRequest>({
    filters: [],
    sorts: [{ field: 'lookupKey', direction: 'ASC' }],
    page: 0,
    size: 20
  });

  private savingSignal = signal<boolean>(false);
  private saveErrorSignal = signal<string | null>(null);

  // Current master lookup being edited
  private currentMasterLookupSignal = signal<MasterLookupDto | null>(null);
  private usageInfoSignal = signal<MasterLookupUsageDto | null>(null);

  // ============================================
  // LOOKUP DETAIL STATE
  // ============================================

  private lookupDetailsSignal = signal<LookupDetailDto[]>([]);
  private detailsTotalElementsSignal = signal<number>(0);
  private detailsLoadingSignal = signal<boolean>(false);
  private detailsErrorSignal = signal<string | null>(null);

  private detailsPageSignal = signal<number>(0);
  private detailsPageSizeSignal = signal<number>(50);
  private detailsSortSignal = signal<SearchSort | null>({ field: 'sortOrder', direction: 'ASC' });

  private detailSavingSignal = signal<boolean>(false);
  private detailSaveErrorSignal = signal<string | null>(null);

  // ============================================
  // COMPUTED (PUBLIC READONLY)
  // ============================================

  // Master Lookups
  readonly masterLookups = computed(() => this.masterLookupsSignal());
  readonly totalElements = computed(() => this.totalElementsSignal());
  readonly loading = computed(() => this.loadingSignal());
  readonly error = computed(() => this.errorSignal());
  readonly currentPage = computed(() => this.lastSearchRequestSignal().page);
  readonly pageSize = computed(() => this.lastSearchRequestSignal().size);
  readonly currentFilters = computed(() => this.currentFiltersSignal());
  readonly saving = computed(() => this.savingSignal());
  readonly saveError = computed(() => this.saveErrorSignal());
  readonly currentMasterLookup = computed(() => this.currentMasterLookupSignal());
  readonly usageInfo = computed(() => this.usageInfoSignal());

  // Lookup Details
  readonly lookupDetails = computed(() => this.lookupDetailsSignal());
  readonly detailsTotalElements = computed(() => this.detailsTotalElementsSignal());
  readonly detailsLoading = computed(() => this.detailsLoadingSignal());
  readonly detailsError = computed(() => this.detailsErrorSignal());
  readonly detailSaving = computed(() => this.detailSavingSignal());
  readonly detailSaveError = computed(() => this.detailSaveErrorSignal());
  readonly detailsCurrentPage = computed(() => this.detailsPageSignal());
  readonly detailsPageSize = computed(() => this.detailsPageSizeSignal());
  readonly detailsSort = computed(() => this.detailsSortSignal());

  // ============================================
  // MASTER LOOKUP OPERATIONS
  // ============================================

  /**
   * Load master lookups using the last search request (useful for refresh/reload).
   */
  loadMasterLookups(): void {
    this.executeSearch(this.lastSearchRequestSignal());
  }

  /**
   * Execute a search request against the API.
   * Stores the request as the last search request for future reloads.
   */
  private executeSearch(request: SearchRequest): void {
    this.lastSearchRequestSignal.set(request);
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    this.apiService.searchMasterLookups(request).pipe(
      tap((response) => {
        this.masterLookupsSignal.set(response?.content ?? []);
        this.totalElementsSignal.set(response?.totalElements ?? 0);
      }),
      catchError((error) => {
        this.handleError(error, this.errorSignal);
        this.masterLookupsSignal.set([]);
        this.totalElementsSignal.set(0);
        return of(null);
      }),
      finalize(() => this.loadingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Apply grid state from ErpListComponent and reload.
   * ErpListComponent owns page/size/sort state; the facade only owns filters.
   */
  applyGridStateAndLoad(params: {
    page: number;
    size: number;
    sortBy?: string;
    sortDir?: 'ASC' | 'DESC';
    filters?: SearchFilter[];
  }): void {
    if (params.filters !== undefined) {
      this.currentFiltersSignal.set(params.filters);
    }

    const request: SearchRequest = {
      filters: this.currentFiltersSignal(),
      sorts: params.sortBy
        ? [{ field: params.sortBy, direction: params.sortDir || 'ASC' }]
        : [],
      page: params.page,
      size: params.size
    };

    this.executeSearch(request);
  }

  /**
   * Set filters (resets page to 0 in the last search request)
   */
  setFilters(filters: SearchFilter[]): void {
    this.currentFiltersSignal.set(filters);
    this.lastSearchRequestSignal.update(r => ({ ...r, filters, page: 0 }));
  }

  /**
   * Get master lookup by ID (for edit)
   */
  getMasterLookupById(id: number, onSuccess?: (lookup: MasterLookupDto) => void): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    this.apiService.getMasterLookupById(id).pipe(
      tap((lookup) => {
        this.currentMasterLookupSignal.set(lookup);
        onSuccess?.(lookup);
      }),
      catchError((error) => {
        this.handleError(error, this.errorSignal);
        return of(null);
      }),
      finalize(() => this.loadingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Get usage info for master lookup
   */
  getUsageInfo(id: number, onSuccess?: (usage: MasterLookupUsageDto) => void): void {
    this.apiService.getMasterLookupUsage(id).pipe(
      tap((usage) => {
        this.usageInfoSignal.set(usage);
        onSuccess?.(usage);
      }),
      catchError((error) => {
        this.handleError(error, this.errorSignal);
        return of(null);
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Create master lookup
   */
  createMasterLookup(request: CreateMasterLookupRequest, onSuccess?: (lookup: MasterLookupDto) => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.apiService.createMasterLookup(request).pipe(
      tap((lookup) => {
        if (lookup?.id) {
          this.currentMasterLookupSignal.set(lookup);
          onSuccess?.(lookup);
        } else {
          this.saveErrorSignal.set('ERRORS.OPERATION_FAILED');
        }
      }),
      catchError((error) => {
        this.handleError(error, this.saveErrorSignal);
        return of(null);
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Update master lookup
   */
  updateMasterLookup(id: number, request: UpdateMasterLookupRequest, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.apiService.updateMasterLookup(id, request).pipe(
      tap((lookup) => {
        if (lookup?.id) {
          this.currentMasterLookupSignal.set(lookup);
          onSuccess?.();
        } else {
          this.saveErrorSignal.set('ERRORS.OPERATION_FAILED');
        }
      }),
      catchError((error) => {
        this.handleError(error, this.saveErrorSignal);
        return of(null);
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Toggle master lookup active status
   * Rule 19.5: Unified toggle-active endpoint
   */
  toggleMasterLookupActive(id: number, active: boolean, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.apiService.toggleMasterLookupActive(id, active).pipe(
      tap(() => {
        this.loadMasterLookups();
        onSuccess?.();
      }),
      catchError((error) => {
        this.handleError(error, this.saveErrorSignal);
        return of(null);
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Delete master lookup
   */
  deleteMasterLookup(id: number, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.apiService.deleteMasterLookup(id).pipe(
      tap(() => {
        this.loadMasterLookups();
        onSuccess?.();
      }),
      catchError((error) => {
        this.handleError(error, this.saveErrorSignal);
        return of(null);
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  // ============================================
  // LOOKUP DETAIL OPERATIONS
  // ============================================

  /**
   * Load lookup details for a master lookup
   */
  loadLookupDetails(masterLookupId: number, showLoading = true): void {
    if (showLoading) {
      this.detailsLoadingSignal.set(true);
    }
    this.detailsErrorSignal.set(null);

    const request: SearchRequest = {
      filters: [{ field: 'masterLookupId', operator: 'EQUALS', value: masterLookupId }],
      sorts: this.detailsSortSignal() ? [this.detailsSortSignal()!] : [],
      page: this.detailsPageSignal(),
      size: this.detailsPageSizeSignal()
    };

    this.apiService.searchLookupDetails(request).pipe(
      tap((response) => {
        this.lookupDetailsSignal.set(response?.content ?? []);
        this.detailsTotalElementsSignal.set(response?.totalElements ?? 0);
      }),
      catchError((error) => {
        this.handleError(error, this.detailsErrorSignal);
        this.lookupDetailsSignal.set([]);
        this.detailsTotalElementsSignal.set(0);
        return of(null);
      }),
      finalize(() => this.detailsLoadingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Create lookup detail
   */
  createLookupDetail(request: CreateLookupDetailRequest, onSuccess?: () => void): void {
    this.detailSavingSignal.set(true);
    this.detailSaveErrorSignal.set(null);

    this.apiService.createLookupDetail(request).pipe(
      tap((detail) => {
        if (detail?.id) {
          // Append locally instead of full reload to avoid grid refresh/flicker
          this.lookupDetailsSignal.update(details => [...details, detail]);
          this.detailsTotalElementsSignal.update(n => n + 1);
          this.refreshUsageInfo(request.masterLookupId);
          onSuccess?.();
        } else {
          this.detailSaveErrorSignal.set('ERRORS.OPERATION_FAILED');
        }
      }),
      catchError((error) => {
        this.handleError(error, this.detailSaveErrorSignal);
        return of(null);
      }),
      finalize(() => this.detailSavingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Update lookup detail
   */
  updateLookupDetail(id: number, masterLookupId: number, request: UpdateLookupDetailRequest, onSuccess?: () => void): void {
    this.detailSavingSignal.set(true);
    this.detailSaveErrorSignal.set(null);

    this.apiService.updateLookupDetail(id, request).pipe(
      tap((detail) => {
        if (detail?.id) {
          // Update in place locally to avoid full grid refresh/flicker
          this.lookupDetailsSignal.update(details =>
            details.map(d => d.id === id ? detail : d)
          );
          onSuccess?.();
        } else {
          this.detailSaveErrorSignal.set('ERRORS.OPERATION_FAILED');
        }
      }),
      catchError((error) => {
        this.handleError(error, this.detailSaveErrorSignal);
        return of(null);
      }),
      finalize(() => this.detailSavingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Toggle lookup detail active status
   * Rule 19.5: Unified toggle-active endpoint
   */
  toggleLookupDetailActive(id: number, active: boolean, masterLookupId: number, onSuccess?: () => void): void {
    this.detailSavingSignal.set(true);
    this.detailSaveErrorSignal.set(null);

    this.apiService.toggleLookupDetailActive(id, active).pipe(
      tap((detail) => {
        // Update in place locally to avoid full grid refresh/flicker
        this.lookupDetailsSignal.update(details =>
          details.map(d => d.id === id ? { ...d, isActive: active } : d)
        );
        this.refreshUsageInfo(masterLookupId);
        onSuccess?.();
      }),
      catchError((error) => {
        this.handleError(error, this.detailSaveErrorSignal);
        return of(null);
      }),
      finalize(() => this.detailSavingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Delete lookup detail
   */
  deleteLookupDetail(id: number, masterLookupId: number, onSuccess?: () => void): void {
    this.detailSavingSignal.set(true);
    this.detailSaveErrorSignal.set(null);

    this.apiService.deleteLookupDetail(id).pipe(
      tap(() => {
        // Remove locally to avoid full grid refresh/flicker
        this.lookupDetailsSignal.update(details => details.filter(d => d.id !== id));
        this.detailsTotalElementsSignal.update(n => Math.max(0, n - 1));
        this.refreshUsageInfo(masterLookupId);
        onSuccess?.();
      }),
      catchError((error) => {
        this.handleError(error, this.detailSaveErrorSignal);
        return of(null);
      }),
      finalize(() => this.detailSavingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Change details sorting and reload
   * @param masterLookupId Parent master lookup ID
   * @param field Field to sort by
   * @param direction Sort direction (ASC/DESC)
   */
  setDetailSort(masterLookupId: number, field: string, direction: 'ASC' | 'DESC' = 'ASC'): void {
    this.detailsSortSignal.set({ field, direction });
    this.detailsPageSignal.set(0); // Reset to first page on sort change
    this.loadLookupDetails(masterLookupId);
  }

  /**
   * Toggle details sort direction (or set new sort field)
   * @param masterLookupId Parent master lookup ID
   * @param field Field to sort by
   */
  toggleDetailSort(masterLookupId: number, field: string): void {
    const currentSort = this.detailsSortSignal();
    if (currentSort && currentSort.field === field) {
      // Toggle direction
      const newDirection = currentSort.direction === 'ASC' ? 'DESC' : 'ASC';
      this.detailsSortSignal.set({ field, direction: newDirection });
    } else {
      // New field, default to ASC
      this.detailsSortSignal.set({ field, direction: 'ASC' });
    }
    this.detailsPageSignal.set(0);
    this.loadLookupDetails(masterLookupId);
  }

  /**
   * Change details page and reload
   * @param masterLookupId Parent master lookup ID
   * @param page Page number (0-indexed)
   */
  setDetailPage(masterLookupId: number, page: number): void {
    this.detailsPageSignal.set(page);
    this.loadLookupDetails(masterLookupId);
  }

  // ============================================
  // UTILITIES
  // ============================================

  /**
   * Reset detail state (when leaving entry screen)
   */
  resetDetailState(): void {
    this.lookupDetailsSignal.set([]);
    this.detailsTotalElementsSignal.set(0);
    this.detailsErrorSignal.set(null);
    this.detailsPageSignal.set(0);
  }

  /**
   * Clear current master lookup (when leaving entry screen)
   */
  clearCurrentMasterLookup(): void {
    this.currentMasterLookupSignal.set(null);
    this.usageInfoSignal.set(null);
    this.resetDetailState();
  }

  /**
   * Refresh usage info
   */
  private refreshUsageInfo(masterLookupId: number): void {
    this.getUsageInfo(masterLookupId);
  }

  /**
   * Handle API errors
   */
  private handleError(error: unknown, errorSignal: ReturnType<typeof signal<string | null>>): void {
    const backendCode = extractBackendErrorCode(error);
    const mappedKey = backendCode && this.errorMapper.hasMapping(backendCode)
      ? this.errorMapper.mapError(backendCode).translationKey
      : null;
    errorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
  }
}
