import { Injectable, inject, signal, computed, DestroyRef } from '@angular/core';
import { catchError, tap, finalize } from 'rxjs';
import { EMPTY } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { PostingApiService } from 'src/app/modules/finance/gl/services/posting-api.service';
import {
  AccPostingMstDto,
  PostingGenerateJournalResponse,
  JournalPreviewResponse
} from 'src/app/modules/finance/gl/models/posting.model';
import {
  SearchFilter,
  SearchRequest,
  ContractFilter
} from 'src/app/modules/finance/gl/models/gl.model';

import { ErpErrorMapperService } from 'src/app/shared/services/erp-error-mapper.service';
import { extractBackendErrorCode, extractBackendErrorMessage } from 'src/app/shared/utils/backend-error-message';

/**
 * PostingFacade – single point of contact between Posting components and services.
 *
 * Manages:
 * - Posting list loading / error / saving state via signals
 * - Posting detail view
 * - Generate Journal action
 * - Error mapping through ErpErrorMapperService
 */
@Injectable()
export class PostingFacade {
  private readonly api = inject(PostingApiService);
  private readonly errorMapper = inject(ErpErrorMapperService);
  private readonly destroyRef = inject(DestroyRef);

  // ── Posting list state ────────────────────────────────────

  private readonly postingsSignal = signal<AccPostingMstDto[]>([]);
  private readonly postingsTotalSignal = signal<number>(0);
  private readonly postingsLoadingSignal = signal<boolean>(false);
  private readonly postingsErrorSignal = signal<string | null>(null);

  // Pagination + filters
  private readonly lastSearchRequestSignal = signal<SearchRequest>({
    filters: [],
    sorts: [{ field: 'postingId', direction: 'DESC' }],
    page: 0,
    size: 20
  });
  private readonly filtersSignal = signal<SearchFilter[]>([]);
  private readonly sortSignal = signal<{ field: string; direction: string } | null>(null);

  // ── Shared operation state ────────────────────────────────

  private readonly savingSignal = signal<boolean>(false);
  private readonly saveErrorSignal = signal<string | null>(null);

  // ── Public computed accessors ─────────────────────────────

  readonly postings = computed(() => this.postingsSignal());
  readonly postingsTotal = computed(() => this.postingsTotalSignal());
  readonly postingsLoading = computed(() => this.postingsLoadingSignal());
  readonly postingsError = computed(() => this.postingsErrorSignal());
  readonly filters = computed(() => this.filtersSignal());
  readonly currentPage = computed(() => this.lastSearchRequestSignal().page);
  readonly pageSize = computed(() => this.lastSearchRequestSignal().size);

  readonly saving = computed(() => this.savingSignal());
  readonly saveError = computed(() => this.saveErrorSignal());

  // ══════════════════════════════════════════════════════════
  // POSTING QUERIES
  // ══════════════════════════════════════════════════════════

  loadPostings(): void {
    this.executeSearch(this.lastSearchRequestSignal());
  }

  private executeSearch(request: SearchRequest): void {
    this.lastSearchRequestSignal.set(request);
    this.postingsLoadingSignal.set(true);
    this.postingsErrorSignal.set(null);

    this.api
      .searchPostings(request)
      .pipe(
        tap((response) => {
          this.postingsSignal.set(response?.content ?? []);
          this.postingsTotalSignal.set(response?.totalElements ?? 0);
        }),
        catchError((error) => {
          this.postingsErrorSignal.set(this.mapError(error));
          this.postingsSignal.set([]);
          this.postingsTotalSignal.set(0);
          return EMPTY;
        }),
        finalize(() => this.postingsLoadingSignal.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  getPostingById(postingId: number, onSuccess?: (posting: AccPostingMstDto) => void): void {
    this.postingsLoadingSignal.set(true);
    this.postingsErrorSignal.set(null);

    this.api.getPostingById(postingId).pipe(
      tap((posting) => onSuccess?.(posting)),
      catchError((error) => {
        this.postingsErrorSignal.set(this.mapError(error));
        return EMPTY;
      }),
      finalize(() => this.postingsLoadingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  // ══════════════════════════════════════════════════════════
  // PREVIEW & GENERATE JOURNAL
  // ══════════════════════════════════════════════════════════

  previewJournal(postingId: number, onSuccess?: (result: JournalPreviewResponse) => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.previewJournal(postingId).pipe(
      tap((result) => onSuccess?.(result)),
      catchError((error) => {
        this.saveErrorSignal.set(this.mapError(error));
        return EMPTY;
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  generateJournal(postingId: number, onSuccess?: (result: PostingGenerateJournalResponse) => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.generateJournal(postingId).pipe(
      tap((result) => {
        this.loadPostings();
        onSuccess?.(result);
      }),
      catchError((error) => {
        this.saveErrorSignal.set(this.mapError(error));
        return EMPTY;
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  // ══════════════════════════════════════════════════════════
  // GRID STATE MANAGEMENT
  // ══════════════════════════════════════════════════════════

  applyGridStateAndLoad(state: {
    page: number;
    size: number;
    sortBy: string;
    sortDir: string;
    filters: SearchFilter[];
  }): void {
    this.filtersSignal.set(state.filters);
    this.sortSignal.set({ field: state.sortBy, direction: state.sortDir });
    const contractFilters = this.toContractFilters(state.filters);
    const request: SearchRequest = {
      filters: contractFilters,
      sorts: state.sortBy ? [{ field: state.sortBy, direction: state.sortDir as 'ASC' | 'DESC' }] : [],
      page: state.page,
      size: state.size
    };
    this.executeSearch(request);
  }

  setFilters(filters: SearchFilter[]): void {
    this.filtersSignal.set(filters);
    const contractFilters = this.toContractFilters(filters);
    this.lastSearchRequestSignal.update(r => ({ ...r, filters: contractFilters, page: 0 }));
  }

  // ── Error helpers ─────────────────────────────────────────

  clearError(): void {
    this.postingsErrorSignal.set(null);
    this.saveErrorSignal.set(null);
  }

  clearCurrentEntity(): void {
    this.postingsSignal.set([]);
    this.postingsTotalSignal.set(0);
    this.clearError();
  }

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
    const backendMessage = extractBackendErrorMessage(error);
    if (backendMessage) {
      return backendMessage;
    }
    return 'ERRORS.OPERATION_FAILED';
  }
}
