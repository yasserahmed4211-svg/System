import { Injector, effect, inject, signal, untracked } from '@angular/core';

import { ErpGridState, ErpSortDirection, SpecFilter } from '../models';

export interface ErpListInitOptions {
  autoLoad?: boolean;
}

/**
 * Base class for all ERP list screens.
 *
 * Responsibilities:
 * - Centralize list state (paging, sorting, filters)
 * - Provide consistent state mutation helpers
 * - Trigger data loading when state changes (once initialized)
 */
export abstract class ErpListComponent {
  private readonly injector = inject(Injector);

  protected readonly gridState = signal<ErpGridState>({
    page: 0,
    size: 20,
    sort: undefined,
    direction: undefined,
    filters: []
  });

  private initialized = false;
  private options: Required<ErpListInitOptions> = { autoLoad: true };

  /** Implement in derived class to load data based on current state. */
  protected abstract load(state: ErpGridState): void;

  /**
   * Must be called by derived class (typically in ngOnInit) after dependencies are ready.
   * This avoids Angular constructor/injection ordering issues.
   */
  protected initErpList(options?: ErpListInitOptions): void {
    this.options = { autoLoad: options?.autoLoad ?? true };
    this.initialized = true;

    if (this.options.autoLoad) {
      effect(
        () => {
          const state = this.gridState();
          if (!this.initialized) return;
          untracked(() => this.load(state));
        },
        { injector: this.injector }
      );
    }
  }

  /** Manual load trigger (useful when autoLoad=false). */
  protected reload(): void {
    this.load(this.gridState());
  }

  protected setPage(page: number): void {
    this.gridState.update((s) => ({ ...s, page }));
  }

  protected setSize(size: number): void {
    this.gridState.update((s) => ({ ...s, size, page: 0 }));
  }

  protected setSort(sort: string | undefined, direction: ErpSortDirection | undefined): void {
    this.gridState.update((s) => ({ ...s, sort, direction, page: 0 }));
  }

  protected clearSort(): void {
    this.setSort(undefined, undefined);
  }

  protected setFilters(filters: SpecFilter[]): void {
    this.gridState.update((s) => ({ ...s, filters, page: 0 }));
  }

  protected clearFilters(): void {
    this.setFilters([]);
  }

  protected patchState(patch: Partial<ErpGridState>): void {
    this.gridState.update((s) => ({ ...s, ...patch }));
  }
}
