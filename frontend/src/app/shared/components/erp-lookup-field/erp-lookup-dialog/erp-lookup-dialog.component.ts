import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  Input,
  OnDestroy,
  OnInit,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { NgbActiveModal, NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, tap } from 'rxjs/operators';

import {
  LookupColumn,
  LookupConfig,
  LookupItem
} from 'src/app/core/lookup/lookup.model';
import { LookupDataService } from 'src/app/core/lookup/lookup-data.service';

type SortDirection = 'ASC' | 'DESC' | null;

/**
 * ErpLookupDialogComponent
 *
 * Advanced entity selection dialog with:
 *  - Server-side pagination
 *  - Dynamic column rendering (from LookupConfig.columns)
 *  - Sortable columns
 *  - Search filtering
 *
 * Opened via NgbModal. Returns the selected LookupItem on close.
 *
 * @architecture Shared layer — UI only, delegates HTTP to LookupDataService
 */
@Component({
  selector: 'erp-lookup-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule, NgbPaginationModule],
  templateUrl: './erp-lookup-dialog.component.html',
  styleUrls: ['./erp-lookup-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ErpLookupDialogComponent implements OnInit, OnDestroy {
  /** Set externally by the modal opener */
  @Input() config!: LookupConfig;

  private readonly activeModal = inject(NgbActiveModal);
  private readonly lookupService = inject(LookupDataService);

  // ── State (Signals) ──────────────────────────────────────────────

  readonly results = signal<LookupItem[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(1);
  readonly isLoading = signal(false);

  readonly searchTerm = signal('');
  readonly sortColumn = signal<string | null>(null);
  readonly sortDirection = signal<SortDirection>(null);

  readonly selectedItem = signal<LookupItem | null>(null);

  readonly pageSize = computed(() => this.config?.pageSize ?? 10);
  readonly columns = computed<LookupColumn[]>(() => this.config?.columns ?? []);
  readonly dialogTitle = computed(() => this.config?.dialogTitleKey ?? 'LOOKUP.DIALOG_TITLE');

  readonly hasSelection = computed(() => this.selectedItem() !== null);

  // ── RxJS ─────────────────────────────────────────────────────────

  private readonly search$ = new Subject<void>();
  private searchSub = Subscription.EMPTY;

  // ── Lifecycle ────────────────────────────────────────────────────

  ngOnInit(): void {
    this.searchSub = this.search$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(() =>
          // Compare search state tuple for dedup
          false
        ),
        tap(() => this.isLoading.set(true)),
        switchMap(() =>
          this.lookupService.advancedSearch(this.config.endpoint, {
            search: this.searchTerm(),
            page: this.currentPage() - 1, // Backend is 0-indexed
            size: this.pageSize(),
            sort: this.sortColumn() ?? undefined,
            direction: this.sortDirection() ?? undefined
          }, this.config.extraParams)
        )
      )
      .subscribe({
        next: response => {
          this.results.set(response.content);
          this.totalElements.set(response.totalElements);
          this.totalPages.set(response.totalPages);
          this.isLoading.set(false);
        },
        error: () => {
          this.results.set([]);
          this.totalElements.set(0);
          this.totalPages.set(0);
          this.isLoading.set(false);
        }
      });

    // Run initial search
    this.triggerSearch();
  }

  ngOnDestroy(): void {
    this.searchSub.unsubscribe();
    this.search$.complete();
  }

  // ── Actions ──────────────────────────────────────────────────────

  onSearchChange(term: string): void {
    this.searchTerm.set(term);
    this.currentPage.set(1);
    this.triggerSearch();
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.selectedItem.set(null);
    this.triggerSearch();
  }

  onSort(column: LookupColumn): void {
    if (this.sortColumn() === column.key) {
      // Cycle: ASC → DESC → none
      const current = this.sortDirection();
      if (current === 'ASC') {
        this.sortDirection.set('DESC');
      } else {
        this.sortColumn.set(null);
        this.sortDirection.set(null);
      }
    } else {
      this.sortColumn.set(column.key);
      this.sortDirection.set('ASC');
    }
    this.currentPage.set(1);
    this.triggerSearch();
  }

  selectRow(item: LookupItem): void {
    this.selectedItem.set(item);
  }

  confirmSelection(): void {
    const item = this.selectedItem();
    if (item) {
      this.activeModal.close(item);
    }
  }

  cancel(): void {
    this.activeModal.dismiss('cancel');
  }

  // ── Sort Icon Helper ─────────────────────────────────────────────

  getSortIcon(column: LookupColumn): string {
    if (this.sortColumn() !== column.key) return 'ti ti-arrows-sort';
    return this.sortDirection() === 'ASC' ? 'ti ti-sort-ascending' : 'ti ti-sort-descending';
  }

  // ── Track By ─────────────────────────────────────────────────────

  trackByItemId(_index: number, item: LookupItem): number {
    return item.id;
  }

  trackByColumnKey(_index: number, col: LookupColumn): string {
    return col.key;
  }

  // ── Private ──────────────────────────────────────────────────────

  private triggerSearch(): void {
    this.search$.next();
  }
}
