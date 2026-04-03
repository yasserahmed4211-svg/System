import { Component, OnInit, inject, effect, ChangeDetectionStrategy, ChangeDetectorRef, DestroyRef, NgZone, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AgGridAngular } from 'ag-grid-angular';
import { ColDef, GridReadyEvent, GridApi, GridOptions } from 'ag-grid-community';
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { ThemeService } from 'src/app/theme/shared/service/customs-theme.service';
import { createAgGridTheme } from 'src/app/shared/ag-grid/agGridTableStyle';
import { ErpListComponent } from 'src/app/shared/base/erp-list.component';
import { SpecificationFilterComponent } from 'src/app/shared/components/specification-filter/specification-filter.component';
import { ErpEmptyStateComponent } from 'src/app/shared/components/erp-empty-state/erp-empty-state.component';
import { registerErpAgGridModules } from 'src/app/shared/ag-grid';
import { ErpGridState, SpecFieldOption, SpecFilter, SpecOperatorOption } from 'src/app/shared/models';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { ErpDialogService } from 'src/app/shared/services/erp-dialog.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';
import { MasterLookupFacade } from '../../facades/master-lookup.facade';
import { MasterLookupApiService } from '../../services/master-lookup-api.service';
import { MasterLookupDto, SearchFilter } from '../../models/master-lookup.model';
import { createMasterLookupFilterOptions, createMasterLookupColumnDefs, createMasterLookupGridOptions, ERP_DEFAULT_COL_DEF } from './master-lookup-grid.config';
import { ConfirmActionDeps, confirmToggleLookupActive, confirmDeleteLookup } from '../../helpers/master-lookup-confirm-actions';

registerErpAgGridModules();

/** Page A (Search/List) — Master Lookups search with AG Grid */
@Component({
  selector: 'app-master-lookup-search',
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
  providers: [MasterLookupFacade, MasterLookupApiService],
  templateUrl: './master-lookup-search.component.html',
  styleUrl: './master-lookup-search.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MasterLookupSearchComponent extends ErpListComponent implements OnInit {
  private themeService = inject(ThemeService);
  private router = inject(Router);
  readonly translate = inject(TranslateService);
  private cdr = inject(ChangeDetectorRef);
  private zone = inject(NgZone);
  private destroyRef = inject(DestroyRef);
  readonly facade = inject(MasterLookupFacade);
  private readonly notificationService = inject(ErpNotificationService);
  private readonly authService = inject(AuthenticationService);
  private readonly confirmDeps: ConfirmActionDeps = {
    dialog: inject(ErpDialogService), notify: this.notificationService,
    auth: this.authService, facade: this.facade
  };

  showGrid = true;
  private gridApi!: GridApi;
  theme = createAgGridTheme(this.themeService.isDarkMode());

  showAdvancedFilters = false;

  get rowData(): MasterLookupDto[] { return this.facade.masterLookups(); }
  get isLoading(): boolean { return this.facade.loading(); }
  get hasError(): boolean { return !!this.facade.error(); }
  currentFilters: SearchFilter[] = [];
  availableFields: SpecFieldOption[] = [];
  availableOperators: SpecOperatorOption[] = [];
  
  columnDefs: ColDef[] = [];
  defaultColDef: ColDef = ERP_DEFAULT_COL_DEF;
  gridOptions: GridOptions = {};
  agLocaleText: Record<string, string> = {};

  constructor() {
    super();
    this.rebuildGridConfig();

    effect(() => {
      const isDark = this.themeService.isDarkMode();
      untracked(() => {
        this.theme = createAgGridTheme(isDark);
      });
    });

    effect(() => { this.currentFilters = this.facade.currentFilters(); });
    effect(() => {
      const saveError = this.facade.saveError();
      if (!saveError) return;
      untracked(() => this.notificationService.error(saveError));
    });
  }

  ngOnInit(): void {
    this.initErpList();

    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.rebuildGridConfig();
        this.recreateGrid();
        this.cdr.detectChanges();
      });
  }

  private rebuildGridConfig(): void {
    const filterOpts = createMasterLookupFilterOptions(this.translate);
    this.availableFields = filterOpts.fields;
    this.availableOperators = filterOpts.operators;

    this.columnDefs = createMasterLookupColumnDefs(this.translate, this.zone, {
      onEdit: (lookup) => this.navigateToEdit(lookup),
      onToggleActive: (lookup) => this.toggleActive(lookup),
      onDelete: (lookup) => this.deleteLookup(lookup)
    });

    const gridCfg = createMasterLookupGridOptions(this.translate);
    this.gridOptions = gridCfg.gridOptions;
    this.agLocaleText = gridCfg.localeText;
  }

  navigateToCreate(): void {
    if (!this.authService.hasPermission('PERM_MASTER_LOOKUP_CREATE')) { this.notificationService.warning('MESSAGES.NO_PERMISSION'); return; }
    this.router.navigate(['/master-data/master-lookups/create']);
  }

  navigateToEdit(lookup: MasterLookupDto): void {
    if (!this.authService.hasPermission('PERM_MASTER_LOOKUP_UPDATE')) { this.notificationService.warning('MESSAGES.NO_PERMISSION'); return; }
    this.router.navigate(['/master-data/master-lookups/edit', lookup.id]);
  }

  toggleActive(lookup: MasterLookupDto): void { confirmToggleLookupActive(this.confirmDeps, lookup, () => this.reload()); }
  deleteLookup(lookup: MasterLookupDto): void { confirmDeleteLookup(this.confirmDeps, lookup, () => this.reload()); }

  toggleAdvancedFilters(): void {
    this.showAdvancedFilters = !this.showAdvancedFilters;
    this.cdr.detectChanges();
  }

  onSpecFiltersApply(filters: SpecFilter[]): void {
    const apiFilters: SearchFilter[] = filters.map((f) => ({
      field: f.field,
      operator: f.operator === 'like' ? 'CONTAINS' as const : 'EQUALS' as const,
      value: Array.isArray(f.value) ? f.value.map(String) : f.value ?? undefined
    }));
    this.facade.setFilters(apiFilters);
    this.reload();
  }

  onSpecFiltersClear(): void { this.facade.setFilters([]); this.reload(); }
  refreshData(): void { this.reload(); }

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
      sortBy: state.sort || 'lookupKey',
      sortDir: (state.direction as 'ASC' | 'DESC') || 'ASC',
      filters: this.facade.currentFilters()
    });
  }
}
