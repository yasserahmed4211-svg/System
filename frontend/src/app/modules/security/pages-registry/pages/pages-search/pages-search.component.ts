import { Component, OnInit, inject, effect, ChangeDetectionStrategy, ChangeDetectorRef, DestroyRef, NgZone, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { ThemeService } from 'src/app/theme/shared/service/customs-theme.service';
import { createAgGridTheme } from 'src/app/shared/ag-grid/agGridTableStyle';

import { ErpListComponent } from 'src/app/shared/base/erp-list.component';
import { SpecificationFilterComponent } from 'src/app/shared/components/specification-filter/specification-filter.component';
import { ErpEmptyStateComponent } from 'src/app/shared/components/erp-empty-state/erp-empty-state.component';
import { registerErpAgGridModules } from 'src/app/shared/ag-grid';
import { ErpGridState, SpecFieldOption, SpecFilter, SpecOperatorOption, SpecOperator } from 'src/app/shared/models';

// Shared services - TASK-FE-PAGES-001 / FE-REQ-PAGES-001
import { ErpDialogService } from 'src/app/shared/services/erp-dialog.service';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';

import { PagesFacade } from 'src/app/modules/security/pages-registry/facades/pages.facade';
import { PagesApiService } from 'src/app/modules/security/pages-registry/services/pages-api.service';
import { PageDto, SearchFilter } from 'src/app/modules/security/pages-registry/models/page.model';
import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';

import { AgGridAngular } from 'ag-grid-angular';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import {
  ColDef,
  GridReadyEvent,
  GridApi,
  GridOptions,
  ICellRendererParams
} from 'ag-grid-community';

import {
  createPageFilterOptions,
  createPageColumnDefs,
  createPageGridOptions,
  ERP_DEFAULT_COL_DEF
} from './pages-grid.config';
import { confirmTogglePageActive, PageConfirmActionDeps } from '../../helpers/page-confirm-actions';

// Register AG Grid modules (centralized, prevents duplicates)
registerErpAgGridModules();

type Direction = 'ltr' | 'rtl' | 'auto';

/**
 * PagesSearchComponent (Page A of Unified Blueprint)
 * 
 * Purpose: Search, Filter, List, and Actions
 * 
 * Contains:
 * - Search / Filters
 * - Result Table (AG Grid)
 * - Add button → Navigate to Form Page (Create)
 * - Edit action → Navigate to Form Page (Edit)
 * - Delete action → Inline confirmation
 * 
 * Prohibitions:
 * - NO forms inside this page
 * - NO modals for primary create/edit
 * 
 * @requirement FE-REQ-PAGES-001
 * @task TASK-FE-PAGES-001
 * @blueprint Unified Blueprint - Page A (Search/List)
 */
@Component({
  selector: 'app-pages-search',
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    AgGridAngular,
    TranslateModule,
    SpecificationFilterComponent,
    ErpEmptyStateComponent,
    ErpPermissionDirective
  ],
  templateUrl: './pages-search.component.html',
  styleUrl: './pages-search.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [PagesFacade, PagesApiService]
})
export class PagesSearchComponent extends ErpListComponent implements OnInit {
  private themeService = inject(ThemeService);
  private router = inject(Router);
  readonly translate = inject(TranslateService);
  private cdr = inject(ChangeDetectorRef);
  private zone = inject(NgZone);
  private destroyRef = inject(DestroyRef);
  readonly facade = inject(PagesFacade);
  
  private readonly dialogService = inject(ErpDialogService);
  private readonly notificationService = inject(ErpNotificationService);
  private readonly authService = inject(AuthenticationService);

  get direction(): Direction {
    return this.translate.currentLang === 'ar' ? 'rtl' : 'ltr';
  }
  
  showGrid = true;
  private gridApi!: GridApi;
  theme = createAgGridTheme(false);

  showAdvancedFilters = false;

  // Data
  get rowData(): PageDto[] { return this.facade.pages(); }
  get isLoading(): boolean { return this.facade.loading(); }
  get hasError(): boolean { return !!this.facade.error(); }
  currentFilters: SearchFilter[] = [];
  availableFields: SpecFieldOption[] = [];
  availableOperators: SpecOperatorOption[] = [];
  
  // Grid configuration
  columnDefs: ColDef[] = [];
  defaultColDef: ColDef = ERP_DEFAULT_COL_DEF;
  gridOptions: GridOptions = {};
  agLocaleText: Record<string, string> = {};

  constructor() {
    super();

    this.initializeFilterOptions();
    this.initializeColumnDefs();
    this.initializeGridOptions();

    effect(() => {
      const isDark = this.themeService.isDarkMode();
      untracked(() => {
        this.theme = createAgGridTheme(isDark);
      });
    });

    effect(() => {
      this.currentFilters = this.facade.currentFilters();
    });
  }

  ngOnInit(): void {
    this.facade.loadModules();
    this.facade.loadActivePages();
    this.initErpList();

    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
      this.initializeFilterOptions();
      this.initializeColumnDefs();
      this.initializeGridOptions();
      this.recreateGrid();
      this.cdr.detectChanges();
    });
  }

  private initializeFilterOptions(): void {
    const { fields, operators } = createPageFilterOptions(this.translate);
    this.availableFields = fields;
    this.availableOperators = operators;
  }

  private initializeColumnDefs(): void {
    this.columnDefs = createPageColumnDefs(
      this.translate,
      this.zone,
      {
        onEdit: (page) => this.navigateToEdit(page),
        onDeactivate: (page) => this.togglePageActive(page)
      }
    );
  }

  private initializeGridOptions(): void {
    const result = createPageGridOptions(this.translate);
    this.gridOptions = result.gridOptions;
    this.agLocaleText = result.localeText;
  }

  // ========================================
  // NAVIGATION ACTIONS (Blueprint Page A)
  // ========================================

  navigateToCreate(): void {
    if (!this.authService.hasPermission('PERM_PAGE_CREATE')) {
      this.notificationService.warning('MESSAGES.NO_PERMISSION');
      return;
    }
    this.router.navigate(['/security/pages-registry/create']);
  }

  navigateToEdit(page: PageDto): void {
    if (!this.authService.hasPermission('PERM_PAGE_UPDATE')) {
      this.notificationService.warning('MESSAGES.NO_PERMISSION');
      return;
    }
    this.router.navigate(['/security/pages-registry/edit', page.id]);
  }

  // ========================================
  // INLINE DELETE ACTION
  // ========================================

  togglePageActive(page: PageDto): void {
    const deps: PageConfirmActionDeps = {
      dialog: this.dialogService,
      notify: this.notificationService,
      auth: this.authService,
      facade: this.facade
    };
    confirmTogglePageActive(deps, page, () => this.reload());
  }

  // ========================================
  // FILTERS & GRID LIFECYCLE
  // ========================================

  toggleAdvancedFilters(): void {
    this.showAdvancedFilters = !this.showAdvancedFilters;
    this.cdr.detectChanges();
  }

  onSpecFiltersApply(filters: SpecFilter[]): void {
    const apiFilters = filters.map((f) => {
      let value: string | number | boolean | string[] | undefined;
      
      if (Array.isArray(f.value)) {
        value = f.value.map(v => String(v));
      } else {
        value = f.value ?? undefined;
      }

      return {
        field: f.field,
        op: f.operator.toUpperCase() as 'EQ' | 'LIKE',
        value
      };
    }) as SearchFilter[];

    this.facade.setFilters(apiFilters);
    this.reload();
  }

  onSpecFiltersClear(): void {
    this.facade.setFilters([]);
    this.reload();
  }

  refreshData(): void {
    this.reload();
  }

  onGridReady(event: GridReadyEvent): void {
    this.gridApi = event.api;
    this.gridApi.sizeColumnsToFit();
  }

  onPaginationChanged(): void {
    if (!this.gridApi) return;

    const newPage = this.gridApi.paginationGetCurrentPage();
    if (newPage !== this.gridState().page) this.setPage(newPage);

    const pageSize = this.gridApi.paginationGetPageSize?.();
    if (typeof pageSize === 'number' && pageSize > 0 && pageSize !== this.gridState().size) {
      this.setSize(pageSize);
    }
  }

  private onFilterChanged(): void {
    const filterModel = this.gridApi.getFilterModel() as Record<string, { filterType: string; type: string; filter: string; filterTo?: number }>;

    const specFilters: SpecFilter[] = Object.keys(filterModel).map((field) => {
      const colFilter = filterModel[field];
      let operator: SpecOperator = 'eq';
      let value: string | number | boolean | (string | number)[] | null | undefined = colFilter.filter;

      if (colFilter.type === 'contains') operator = 'like';
      else if (colFilter.type === 'equals') operator = 'eq';

      return { field, operator, value };
    });

    this.onSpecFiltersApply(specFilters);
  }

  private recreateGrid(): void {
    this.showGrid = false;
    this.cdr.detectChanges();
    setTimeout(() => {
      this.showGrid = true;
      this.cdr.detectChanges();
    }, 0);
  }

  protected load(state: ErpGridState): void {
    this.facade.applyGridStateAndLoad({
      page: state.page,
      size: state.size,
      sortBy: state.sort || 'displayOrder',
      sortDir: state.direction || 'ASC',
      filters: this.facade.currentFilters()
    });
  }

  private getPageDisplayName(page: PageDto): string {
    return this.translate.currentLang === 'ar' ? page.nameAr : page.nameEn;
  }
}
