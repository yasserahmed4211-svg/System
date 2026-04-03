import { Injectable, inject, signal, computed, DestroyRef } from '@angular/core';
import { catchError, tap, finalize } from 'rxjs';
import { EMPTY } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { JournalApiService } from 'src/app/modules/finance/gl/services/journal-api.service';
import {
  GlJournalHdrDto,
  ManualCreateJournalRequest,
  ManualUpdateJournalRequest,
  UpdateJournalRequest
} from 'src/app/modules/finance/gl/models/journal.model';
import {
  SearchFilter,
  SearchRequest,
  ContractFilter
} from 'src/app/modules/finance/gl/models/gl.model';

import { ErpErrorMapperService } from 'src/app/shared/services/erp-error-mapper.service';
import { extractBackendErrorCode, extractBackendErrorMessage } from 'src/app/shared/utils/backend-error-message';

/**
 * JournalFacade – single point of contact between Journal components and services.
 *
 * Manages:
 * - Journal list loading / error / saving state via signals
 * - Journal CRUD (create, update, deactivate)
 * - Journal state transitions (approve, post, reverse, cancel)
 * - Error mapping through ErpErrorMapperService
 */
@Injectable()
export class JournalFacade {
  private readonly api = inject(JournalApiService);
  private readonly errorMapper = inject(ErpErrorMapperService);
  private readonly destroyRef = inject(DestroyRef);

  // ── Journal list state ────────────────────────────────────

  private readonly journalsSignal = signal<GlJournalHdrDto[]>([]);
  private readonly journalsTotalSignal = signal<number>(0);
  private readonly journalsLoadingSignal = signal<boolean>(false);
  private readonly journalsErrorSignal = signal<string | null>(null);

  // Pagination + filters
  private readonly lastSearchRequestSignal = signal<SearchRequest>({
    filters: [],
    sorts: [{ field: 'id', direction: 'DESC' }],
    page: 0,
    size: 20
  });
  private readonly filtersSignal = signal<SearchFilter[]>([]);
  private readonly sortSignal = signal<{ field: string; direction: string } | null>(null);

  // ── Shared operation state ────────────────────────────────

  private readonly savingSignal = signal<boolean>(false);
  private readonly saveErrorSignal = signal<string | null>(null);

  // ── Public computed accessors ─────────────────────────────

  readonly journals = computed(() => this.journalsSignal());
  readonly journalsTotal = computed(() => this.journalsTotalSignal());
  readonly journalsLoading = computed(() => this.journalsLoadingSignal());
  readonly journalsError = computed(() => this.journalsErrorSignal());
  readonly filters = computed(() => this.filtersSignal());
  readonly currentPage = computed(() => this.lastSearchRequestSignal().page);
  readonly pageSize = computed(() => this.lastSearchRequestSignal().size);

  readonly saving = computed(() => this.savingSignal());
  readonly saveError = computed(() => this.saveErrorSignal());

  // ══════════════════════════════════════════════════════════
  // JOURNAL CRUD
  // ══════════════════════════════════════════════════════════

  loadJournals(): void {
    this.executeSearch(this.lastSearchRequestSignal());
  }

  private executeSearch(request: SearchRequest): void {
    this.lastSearchRequestSignal.set(request);
    this.journalsLoadingSignal.set(true);
    this.journalsErrorSignal.set(null);

    this.api
      .searchJournals(request)
      .pipe(
        tap((response) => {
          this.journalsSignal.set(response?.content ?? []);
          this.journalsTotalSignal.set(response?.totalElements ?? 0);
        }),
        catchError((error) => {
          this.journalsErrorSignal.set(this.mapError(error));
          this.journalsSignal.set([]);
          this.journalsTotalSignal.set(0);
          return EMPTY;
        }),
        finalize(() => this.journalsLoadingSignal.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  getJournalById(journalId: number, onSuccess?: (journal: GlJournalHdrDto) => void): void {
    this.journalsLoadingSignal.set(true);
    this.journalsErrorSignal.set(null);

    this.api.getJournalById(journalId).pipe(
      tap((journal) => onSuccess?.(journal)),
      catchError((error) => {
        this.journalsErrorSignal.set(this.mapError(error));
        return EMPTY;
      }),
      finalize(() => this.journalsLoadingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  createJournal(request: ManualCreateJournalRequest, onSuccess?: (created: GlJournalHdrDto) => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.createJournal(request).pipe(
      tap((created) => {
        this.loadJournals();
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

  updateJournal(journalId: number, request: ManualUpdateJournalRequest, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.updateJournal(journalId, request).pipe(
      tap(() => {
        this.loadJournals();
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

  updateJournalGeneral(journalId: number, request: UpdateJournalRequest, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.updateJournalGeneral(journalId, request).pipe(
      tap(() => {
        this.loadJournals();
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

  toggleActiveJournal(journalId: number, active: boolean, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.toggleActiveJournal(journalId, active).pipe(
      tap(() => {
        this.loadJournals();
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

  // ══════════════════════════════════════════════════════════
  // STATE TRANSITIONS
  // ══════════════════════════════════════════════════════════

  approveJournal(journalId: number, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.approveJournal(journalId).pipe(
      tap(() => {
        this.loadJournals();
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

  postJournal(journalId: number, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.postJournal(journalId).pipe(
      tap(() => {
        this.loadJournals();
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

  reverseJournal(journalId: number, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.reverseJournal(journalId).pipe(
      tap(() => {
        this.loadJournals();
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

  cancelJournal(journalId: number, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api.cancelJournal(journalId).pipe(
      tap(() => {
        this.loadJournals();
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
    this.journalsErrorSignal.set(null);
    this.saveErrorSignal.set(null);
  }

  clearCurrentEntity(): void {
    this.journalsSignal.set([]);
    this.journalsTotalSignal.set(0);
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
