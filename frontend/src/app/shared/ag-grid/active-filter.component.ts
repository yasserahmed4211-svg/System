/**
 * Active Status Filter Component
 *
 * A reusable toggle button group for filtering Active/Inactive status
 * in AG Grid lists. Works with the active-status-filter utilities.
 *
 * @example
 * <erp-active-filter
 *   [(value)]="activeFilter"
 *   (valueChange)="onActiveFilterChange($event)"
 *   [defaultShowActive]="true">
 * </erp-active-filter>
 */

import { Component, Input, Output, EventEmitter, OnInit, inject, ChangeDetectionStrategy, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ActiveFilterValue,
  ActiveStatus,
  activeFilterToApi,
  apiToActiveFilter,
  DEFAULT_ACTIVE_FILTER
} from './active-status-filter.utils';

/**
 * Component for filtering by Active/Inactive status
 *
 * Features:
 * - Three-way toggle: Active | Inactive | All
 * - Integrates with i18n (ngx-translate)
 * - Emits both UI value and API value
 * - Consistent styling across ERP system (uses Bootstrap)
 */
@Component({
  selector: 'erp-active-filter',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="erp-active-filter">
      <div class="btn-group btn-group-sm" role="group" aria-label="Active status filter">
        <button
          type="button"
          class="btn"
          [class.btn-success]="value === 'active'"
          [class.btn-outline-success]="value !== 'active'"
          (click)="selectFilter('active')"
        >
          {{ labels.active }}
        </button>
        <button
          type="button"
          class="btn"
          [class.btn-secondary]="value === 'inactive'"
          [class.btn-outline-secondary]="value !== 'inactive'"
          (click)="selectFilter('inactive')"
        >
          {{ labels.inactive }}
        </button>
        <button
          type="button"
          class="btn"
          [class.btn-info]="value === 'all'"
          [class.btn-outline-info]="value !== 'all'"
          (click)="selectFilter('all')"
        >
          {{ labels.all }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .erp-active-filter {
      display: inline-flex;
      align-items: center;
    }

    .btn-group .btn {
      min-width: 70px;
    }
  `]
})
export class ActiveFilterComponent implements OnInit {
  private readonly translateService = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Current filter value (two-way binding)
   */
  @Input() value: ActiveFilterValue = DEFAULT_ACTIVE_FILTER;

  /**
   * Whether to default to 'active' on init
   * If false, defaults to 'all'
   */
  @Input() defaultShowActive = true;

  /**
   * Optional custom labels (override translations)
   */
  @Input() customLabels?: { active?: string; inactive?: string; all?: string };

  /**
   * Emits when filter value changes (UI value)
   */
  @Output() valueChange = new EventEmitter<ActiveFilterValue>();

  /**
   * Emits when filter value changes (API value: boolean | null)
   * Use this for API requests
   */
  @Output() apiValueChange = new EventEmitter<ActiveStatus>();

  /**
   * Translated labels
   */
  labels = {
    active: 'Active',
    inactive: 'Inactive',
    all: 'All'
  };

  ngOnInit(): void {
    this.loadLabels();

    // Set default value if not explicitly set
    if (this.value === undefined || this.value === null) {
      this.value = this.defaultShowActive ? 'active' : 'all';
    }

    // Subscribe to language changes
    this.translateService.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
      this.loadLabels();
    });
  }

  /**
   * Load translated labels
   */
  private loadLabels(): void {
    // Use custom labels if provided, otherwise use translations
    this.labels = {
      active: this.customLabels?.active ??
        this.translateService.instant('COMMON.ACTIVE') ?? 'Active',
      inactive: this.customLabels?.inactive ??
        this.translateService.instant('COMMON.INACTIVE') ?? 'Inactive',
      all: this.customLabels?.all ??
        this.translateService.instant('COMMON.ALL') ?? 'All'
    };
  }

  /**
   * Handle filter selection
   */
  selectFilter(filterValue: ActiveFilterValue): void {
    if (this.value === filterValue) {
      return; // No change
    }

    this.value = filterValue;
    this.valueChange.emit(filterValue);
    this.apiValueChange.emit(activeFilterToApi(filterValue));
  }

  /**
   * Programmatically set value from API boolean
   */
  setFromApiValue(apiValue: ActiveStatus): void {
    this.value = apiToActiveFilter(apiValue);
  }
}
