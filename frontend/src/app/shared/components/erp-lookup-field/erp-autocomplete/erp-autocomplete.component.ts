import {
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnDestroy,
  OnInit,
  Output,
  signal,
  ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, switchMap, tap } from 'rxjs/operators';

import { LookupConfig, LookupItem } from 'src/app/core/lookup/lookup.model';
import { LookupDataService } from 'src/app/core/lookup/lookup-data.service';

/**
 * ErpAutocompleteComponent
 *
 * Inline autocomplete dropdown for quick entity lookup.
 * Debounces input, delegates search to LookupDataService,
 * and emits the selected item.
 *
 * Features:
 *  - Debounce 300ms
 *  - Minimum character threshold (default 2)
 *  - Limit-based results (default 10)
 *  - Keyboard navigation (ArrowUp/Down, Enter, Escape)
 *  - Click-outside dismissal
 *  - Signal-based internal state
 *  - OnPush change detection
 *
 * @architecture Shared layer — UI only, no direct HTTP
 */
@Component({
  selector: 'erp-autocomplete',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './erp-autocomplete.component.html',
  styleUrls: ['./erp-autocomplete.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ErpAutocompleteComponent implements OnInit, OnDestroy {
  /** Lookup configuration */
  @Input({ required: true }) config!: LookupConfig;

  /** Current display text (bound externally) */
  @Input() displayValue = '';

  /** Emitted when the user selects an item */
  @Output() readonly itemSelected = new EventEmitter<LookupItem>();

  /** Emitted when the input is cleared */
  @Output() readonly cleared = new EventEmitter<void>();

  @ViewChild('searchInput') searchInput!: ElementRef<HTMLInputElement>;

  // ── Internal State (Signals) ─────────────────────────────────────

  readonly results = signal<LookupItem[]>([]);
  readonly isOpen = signal(false);
  readonly isLoading = signal(false);
  readonly highlightedIndex = signal(-1);
  readonly searchText = signal('');

  readonly hasResults = computed(() => this.results().length > 0);

  // ── RxJS plumbing ────────────────────────────────────────────────

  private readonly search$ = new Subject<string>();
  private searchSub = Subscription.EMPTY;

  private readonly minChars = computed(() => this.config?.minChars ?? 2);
  private readonly pageSize = computed(() => this.config?.pageSize ?? 10);

  constructor(
    private readonly lookupService: LookupDataService,
    private readonly elRef: ElementRef
  ) {}

  // ── Lifecycle ────────────────────────────────────────────────────

  ngOnInit(): void {
    this.searchSub = this.search$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        filter(term => term.length >= this.minChars()),
        tap(() => this.isLoading.set(true)),
        switchMap(term =>
          this.lookupService.quickSearch(
            this.config.endpoint,
            term,
            this.pageSize()
          )
        )
      )
      .subscribe({
        next: items => {
          this.results.set(items);
          this.isOpen.set(items.length > 0);
          this.highlightedIndex.set(-1);
          this.isLoading.set(false);
        },
        error: () => {
          this.results.set([]);
          this.isOpen.set(false);
          this.isLoading.set(false);
        }
      });
  }

  ngOnDestroy(): void {
    this.searchSub.unsubscribe();
    this.search$.complete();
  }

  // ── User Interaction ─────────────────────────────────────────────

  onInputChange(value: string): void {
    this.searchText.set(value);
    if (value.length < this.minChars()) {
      this.closeDropdown();
      return;
    }
    this.search$.next(value);
  }

  onFocus(): void {
    if (this.searchText().length >= this.minChars() && this.hasResults()) {
      this.isOpen.set(true);
    }
  }

  selectItem(item: LookupItem): void {
    this.displayValue = item.display;
    this.searchText.set(item.display);
    this.closeDropdown();
    this.itemSelected.emit(item);
  }

  clearInput(): void {
    this.displayValue = '';
    this.searchText.set('');
    this.results.set([]);
    this.closeDropdown();
    this.cleared.emit();
  }

  // ── Keyboard Navigation ──────────────────────────────────────────

  onKeyDown(event: KeyboardEvent): void {
    if (!this.isOpen()) return;

    const items = this.results();

    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.highlightedIndex.update(i => Math.min(i + 1, items.length - 1));
        break;

      case 'ArrowUp':
        event.preventDefault();
        this.highlightedIndex.update(i => Math.max(i - 1, 0));
        break;

      case 'Enter':
        event.preventDefault();
        const idx = this.highlightedIndex();
        if (idx >= 0 && idx < items.length) {
          this.selectItem(items[idx]);
        }
        break;

      case 'Escape':
        this.closeDropdown();
        break;
    }
  }

  // ── Click Outside ────────────────────────────────────────────────

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event): void {
    if (!this.elRef.nativeElement.contains(event.target)) {
      this.closeDropdown();
    }
  }

  // ── Track By ─────────────────────────────────────────────────────

  trackByItemId(_index: number, item: LookupItem): number {
    return item.id;
  }

  // ── Helpers ──────────────────────────────────────────────────────

  private closeDropdown(): void {
    this.isOpen.set(false);
    this.highlightedIndex.set(-1);
  }
}
