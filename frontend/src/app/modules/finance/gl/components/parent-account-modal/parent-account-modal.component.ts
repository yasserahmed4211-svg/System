import { Component, ChangeDetectionStrategy, inject, Input, signal, computed, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { ErpDualListComponent, DualListItem } from 'src/app/shared/components/erp-dual-list/erp-dual-list.component';
import { GlApiService } from 'src/app/modules/finance/gl/services/gl-api.service';
import { EligibleParentAccountDto } from 'src/app/modules/finance/gl/models/gl.model';
import { finalize } from 'rxjs';

/**
 * Modal component for selecting a parent account via erp-dual-list.
 * Supports search, single-select, and excludes self + descendants.
 * Enhanced with level display, inactive visual indicators, and hierarchy warnings.
 *
 * @requirement FE-REQ-GL-001
 */
@Component({
  selector: 'app-parent-account-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule, ErpDualListComponent],
  template: `
    <div class="modal-header bg-light">
      <h5 class="modal-title">
        <i class="ti ti-sitemap me-2 text-primary"></i>{{ 'GL.SELECT_PARENT_ACCOUNT' | translate }}
      </h5>
      <button type="button" class="btn-close" aria-label="Close" (click)="onCancel()"></button>
    </div>
    <div class="modal-body">
      <!-- Search Input -->
      <div class="mb-3">
        <div class="input-group">
          <span class="input-group-text bg-light"><i class="ti ti-search"></i></span>
          <input
            type="text"
            class="form-control"
            [placeholder]="'GL.SEARCH_ACCOUNTS' | translate"
            [(ngModel)]="searchTerm"
            (keyup.enter)="onSearch()"
          />
          <button class="btn btn-primary" type="button" (click)="onSearch()">
            {{ 'COMMON.SEARCH' | translate }}
          </button>
        </div>
      </div>

      <!-- Loading indicator -->
      @if (loading()) {
      <div class="text-center py-3">
        <div class="spinner-border spinner-border-sm text-primary" role="status">
          <span class="visually-hidden">{{ 'COMMON.LOADING' | translate }}</span>
        </div>
        <div class="text-muted small mt-1">{{ 'COMMON.LOADING' | translate }}...</div>
      </div>
      }

      <!-- Dual List -->
      <erp-dual-list
        [availableItems]="availableItems"
        [selectedItems]="selectedItems"
        [singleSelect]="true"
        [searchable]="false"
        [disabled]="loading()"
        availableTitleKey="GL.AVAILABLE_ACCOUNTS"
        selectedTitleKey="GL.SELECTED_PARENT"
        (selectedChange)="onSelectionChanged($event)"
      />

      <!-- Pagination info -->
      @if (totalElements() > 0) {
      <div class="d-flex justify-content-between align-items-center mt-2">
        <small class="text-muted">
          <i class="ti ti-list-numbers me-1"></i>
          {{ 'COMMON.SHOWING' | translate }} {{ availableItems.length + selectedItems.length }}
          {{ 'COMMON.OF' | translate }} {{ totalElements() }}
        </small>
        @if (hasMore()) {
        <button class="btn btn-sm btn-outline-primary" (click)="loadMore()" [disabled]="loading()">
          <i class="ti ti-dots me-1"></i>{{ 'COMMON.LOAD_MORE' | translate }}
        </button>
        }
      </div>
      }

      <!-- Parent change warning (only in edit mode) -->
      @if (currentParentPk !== null) {
      <div class="alert alert-warning mt-3 mb-0 py-2 d-flex align-items-start">
        <i class="ti ti-alert-triangle me-2 mt-1"></i>
        <small>{{ 'GL.PARENT_CHANGE_WARNING' | translate }}</small>
      </div>
      }

      <!-- Make Root hint -->
      <div class="mt-3 p-2 bg-light rounded border">
        <small class="text-muted d-flex align-items-center">
          <i class="ti ti-info-circle me-2 text-info"></i> {{ 'GL.PARENT_ACCOUNT_HINT' | translate }}
        </small>
      </div>
    </div>
    <div class="modal-footer">
      <button type="button" class="btn btn-outline-warning" (click)="onMakeRoot()">
        <i class="ti ti-crown me-1"></i> {{ 'GL.MAKE_ROOT_ACCOUNT' | translate }}
      </button>
      <button type="button" class="btn btn-secondary" (click)="onCancel()">
        {{ 'COMMON.CANCEL' | translate }}
      </button>
      <button type="button" class="btn btn-primary" (click)="onConfirm()" [disabled]="selectedItems.length === 0">
        <i class="ti ti-check me-1"></i>{{ 'COMMON.CONFIRM' | translate }}
      </button>
    </div>
  `,
  styles: [`
    .modal-body {
      min-height: 380px;
    }
    .modal-header {
      border-bottom: 2px solid var(--bs-primary, #0d6efd);
    }
    :host ::ng-deep .dual-list-item {
      font-size: 0.875rem;
    }
    :host ::ng-deep .dual-list-item.inactive-parent {
      opacity: 0.5;
      text-decoration: line-through;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ParentAccountModalComponent implements OnInit {
  private readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(GlApiService);
  private readonly cdr = inject(ChangeDetectorRef);

  /** Organization FK to scope accounts */
  @Input() organizationFk!: number;

  /** Account PK to exclude (self + descendants). Null for create mode. */
  @Input() excludeAccountPk: number | null = null;

  /** Currently selected parent PK (to pre-select in the dual list) */
  @Input() currentParentPk: number | null = null;

  searchTerm = '';
  availableItems: DualListItem[] = [];
  selectedItems: DualListItem[] = [];

  private allLoadedItems: EligibleParentAccountDto[] = [];
  private currentPage = 0;
  private readonly pageSize = 50;

  protected loading = signal(false);
  protected totalElements = signal(0);
  protected hasMore = computed(() =>
    (this.currentPage + 1) * this.pageSize < this.totalElements()
  );

  ngOnInit(): void {
    this.loadEligibleParents();
  }

  onSearch(): void {
    this.currentPage = 0;
    this.allLoadedItems = [];
    this.loadEligibleParents();
  }

  loadMore(): void {
    this.currentPage++;
    this.loadEligibleParents(true);
  }

  onSelectionChanged(items: DualListItem[]): void {
    this.selectedItems = items;
    this.cdr.detectChanges();
  }

  onConfirm(): void {
    if (this.selectedItems.length > 0) {
      const selectedPk = Number(this.selectedItems[0].id);
      const selectedLabel = this.selectedItems[0].label;
      const selectedDto = this.allLoadedItems.find(i => i.accountChartPk === selectedPk);
      this.activeModal.close({
        parentPk: selectedPk,
        parentLabel: selectedLabel,
        accountType: selectedDto?.accountType ?? null
      });
    }
  }

  onMakeRoot(): void {
    this.activeModal.close({ parentPk: null, parentLabel: null, accountType: null });
  }

  onCancel(): void {
    this.activeModal.dismiss();
  }

  private loadEligibleParents(append = false): void {
    this.loading.set(true);

    const search = this.searchTerm.trim() || undefined;

    this.api.getEligibleParents({
      organizationFk: this.organizationFk,
      excludeAccountPk: this.excludeAccountPk ?? undefined,
      search,
      page: this.currentPage,
      size: this.pageSize
    }).pipe(
      finalize(() => {
        this.loading.set(false);
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (response) => {
        const newItems = response?.content ?? [];
        this.totalElements.set(response?.totalElements ?? 0);

        if (append) {
          this.allLoadedItems = [...this.allLoadedItems, ...newItems];
        } else {
          this.allLoadedItems = newItems;
        }

        this.buildDualListItems();
      },
      error: () => {
        this.allLoadedItems = [];
        this.totalElements.set(0);
        this.buildDualListItems();
      }
    });
  }

  private buildDualListItems(): void {
    const allItems: DualListItem[] = this.allLoadedItems.map(p => ({
      id: p.accountChartPk,
      label: `${p.accountChartNo} - ${p.accountChartName}`,
      secondaryLabel: `${p.accountTypeName}${!p.isActive ? ' (inactive)' : ''}`,
      disabled: !p.isActive
    }));

    if (this.currentParentPk) {
      this.selectedItems = allItems.filter(item => Number(item.id) === this.currentParentPk);
      this.availableItems = allItems.filter(item => Number(item.id) !== this.currentParentPk);
    } else {
      this.selectedItems = [];
      this.availableItems = allItems;
    }
  }
}
