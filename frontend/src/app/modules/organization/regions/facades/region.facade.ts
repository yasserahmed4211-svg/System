import { Injectable, inject, signal, computed, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, tap, of, finalize } from 'rxjs';

import { RegionApiService } from '../services/region-api.service';
import {
  RegionDto,
  RegionListItemDto,
  RegionUsageDto,
  CreateRegionRequest,
  UpdateRegionRequest,
  SearchFilter,
  SearchRequest
} from '../models/region.model';
import { ErpErrorMapperService } from 'src/app/shared/services/erp-error-mapper.service';
import { extractBackendErrorCode } from 'src/app/shared/utils/backend-error-message';

@Injectable()
export class RegionFacade {
  private readonly api = inject(RegionApiService);
  private readonly errorMapper = inject(ErpErrorMapperService);
  private readonly destroyRef = inject(DestroyRef);

  // ─── List State ───────────────────────────────────────────────
  private readonly entitiesSignal = signal<RegionListItemDto[]>([]);
  private readonly totalElementsSignal = signal<number>(0);
  private readonly loadingSignal = signal<boolean>(false);
  private readonly errorSignal = signal<string | null>(null);
  private readonly currentFiltersSignal = signal<SearchFilter[]>([]);
  private readonly lastSearchRequestSignal = signal<SearchRequest>({
    filters: [],
    sorts: [{ field: 'regionCode', direction: 'ASC' }],
    page: 0,
    size: 20
  });

  // ─── Entry State ──────────────────────────────────────────────
  private readonly savingSignal = signal<boolean>(false);
  private readonly saveErrorSignal = signal<string | null>(null);
  private readonly currentEntitySignal = signal<RegionDto | null>(null);
  private readonly usageInfoSignal = signal<RegionUsageDto | null>(null);

  // ─── Computed (Public Readonly) ───────────────────────────────
  readonly entities = computed(() => this.entitiesSignal());
  readonly totalElements = computed(() => this.totalElementsSignal());
  readonly loading = computed(() => this.loadingSignal());
  readonly error = computed(() => this.errorSignal());
  readonly currentPage = computed(() => this.lastSearchRequestSignal().page);
  readonly pageSize = computed(() => this.lastSearchRequestSignal().size);
  readonly currentFilters = computed(() => this.currentFiltersSignal());
  readonly saving = computed(() => this.savingSignal());
  readonly saveError = computed(() => this.saveErrorSignal());
  readonly currentEntity = computed(() => this.currentEntitySignal());
  readonly usageInfo = computed(() => this.usageInfoSignal());

  // ─── List Operations ──────────────────────────────────────────

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
      sorts: params.sortBy ? [{ field: params.sortBy, direction: params.sortDir ?? 'ASC' }]
                           : [{ field: 'regionCode', direction: 'ASC' }],
      page: params.page,
      size: params.size
    };
    this.executeSearch(request);
  }

  setFilters(filters: SearchFilter[]): void {
    this.currentFiltersSignal.set(filters);
    const current = this.lastSearchRequestSignal();
    this.executeSearch({ ...current, filters, page: 0 });
  }

  reload(): void {
    this.executeSearch(this.lastSearchRequestSignal());
  }

  private executeSearch(request: SearchRequest): void {
    this.lastSearchRequestSignal.set(request);
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    this.api.search(request).pipe(
      tap((response) => {
        this.entitiesSignal.set(response?.content ?? []);
        this.totalElementsSignal.set(response?.totalElements ?? 0);
      }),
      catchError((error) => {
        this.handleError(error, this.errorSignal);
        this.entitiesSignal.set([]);
        this.totalElementsSignal.set(0);
        return of(null);
      }),
      finalize(() => this.loadingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  // ─── Entry Operations ─────────────────────────────────────────

  getById(id: number, onSuccess?: (entity: RegionDto) => void): void {
    this.loadingSignal.set(true);
    this.api.getById(id).pipe(
      tap((entity) => {
        this.currentEntitySignal.set(entity);
        onSuccess?.(entity);
      }),
      catchError((error) => {
        this.handleError(error, this.errorSignal);
        return of(null);
      }),
      finalize(() => this.loadingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  create(request: CreateRegionRequest, onSuccess?: (entity: RegionDto) => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);
    this.api.create(request).pipe(
      tap((entity) => {
        this.currentEntitySignal.set(entity);
        onSuccess?.(entity);
      }),
      catchError((error) => {
        this.handleError(error, this.saveErrorSignal);
        return of(null);
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  update(id: number, request: UpdateRegionRequest, onSuccess?: (entity: RegionDto) => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);
    this.api.update(id, request).pipe(
      tap((entity) => {
        this.currentEntitySignal.set(entity);
        onSuccess?.(entity);
      }),
      catchError((error) => {
        this.handleError(error, this.saveErrorSignal);
        return of(null);
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  deactivate(id: number, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);
    this.api.deactivate(id).pipe(
      tap(() => onSuccess?.()),
      catchError((error) => {
        this.handleError(error, this.saveErrorSignal);
        return of(null);
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  getUsageInfo(id: number, onSuccess?: (usage: RegionUsageDto) => void): void {
    this.api.getUsage(id).pipe(
      tap((usage) => {
        this.usageInfoSignal.set(usage);
        onSuccess?.(usage);
      }),
      catchError(() => of(null)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  clearCurrentEntity(): void {
    this.currentEntitySignal.set(null);
    this.usageInfoSignal.set(null);
    this.saveErrorSignal.set(null);
  }

  private handleError(error: unknown, target: ReturnType<typeof signal<string | null>>): void {
    const code = extractBackendErrorCode(error);
    const key = code ? this.errorMapper.mapError(code).translationKey : 'ERRORS.OPERATION_FAILED';
    target.set(key);
  }
}
