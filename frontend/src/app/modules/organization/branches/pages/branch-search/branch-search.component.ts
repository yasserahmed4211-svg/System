import {
  Component, OnInit, inject, effect, ChangeDetectionStrategy,
  ChangeDetectorRef, DestroyRef, NgZone, untracked
} from '@angular/core';
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
import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { ErpDialogService } from 'src/app/shared/services/erp-dialog.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { LookupService } from 'src/app/core/services/lookup.service';
import { LookupSelectOption } from 'src/app/core/models/lookup-detail.model';
import { registerErpAgGridModules } from 'src/app/shared/ag-grid';
import { ErpGridState } from 'src/app/shared/models';
import { SpecFilter } from 'src/app/shared/models';

import { BranchFacade } from '../../facades/branch.facade';
import { BranchApiService } from '../../services/branch-api.service';
import { BranchListItemDto } from '../../models/branch.model';
import { ConfirmActionDeps, confirmDeactivateBranch } from '../../helpers/branch-confirm-actions';
import {
  createBranchFilterOptions,
  createBranchColumnDefs,
  createBranchGridOptions,
  ERP_DEFAULT_COL_DEF
} from './branch-grid.config';

registerErpAgGridModules();

@Component({
  selector: 'app-branch-search',
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
  providers: [BranchFacade, BranchApiService],
  templateUrl: './branch-search.component.html',
  styleUrl: './branch-search.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BranchSearchComponent extends ErpListComponent implements OnInit {
  private themeService = inject(ThemeService);
  private router = inject(Router);
  readonly translate = inject(TranslateService);
  private cdr = inject(ChangeDetectorRef);
  private zone = inject(NgZone);
  private destroyRef = inject(DestroyRef);
  readonly facade = inject(BranchFacade);
  private readonly lookupService = inject(LookupService);
  private readonly notificationService = inject(ErpNotificationService);
  private readonly authService = inject(AuthenticationService);

  private readonly confirmDeps: ConfirmActionDeps = {
    dialog: inject(ErpDialogService),
    notify: this.notificationService,
    auth: this.authService,
    facade: this.facade
  };

  showGrid = true;
  private gridApi!: GridApi;
  theme = createAgGridTheme(this.themeService.isDarkMode());
  showAdvancedFilters = false;

  columnDefs: ColDef[] = [];
  defaultColDef: ColDef = ERP_DEFAULT_COL_DEF;
  gridOptions: GridOptions = {};
  agLocaleText: Record<string, string> = {};
  availableFields: any[] = [];
  availableOperators: any[] = [];
  private statusOptions: LookupSelectOption[] = [];
  private branchTypeOptions: LookupSelectOption[] = [];

  get rowData(): BranchListItemDto[] { return this.facade.entities(); }
  get isLoading(): boolean { return this.facade.loading(); }
  get hasError(): boolean { return !!this.facade.error(); }

  constructor() {
    super();
    this.rebuildGridConfig();

    effect(() => {
      const isDark = this.themeService.isDarkMode();
      untracked(() => { this.theme = createAgGridTheme(isDark); });
    });

    effect(() => {
      const saveError = this.facade.saveError();
      if (!saveError) return;
      untracked(() => this.notificationService.error(saveError));
    });
  }

  ngOnInit(): void {
    this.loadLookups();
    this.initErpList();

    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadLookups());
  }

  private rebuildGridConfig(): void {
    const filterOpts = createBranchFilterOptions(this.translate, this.branchTypeOptions, this.statusOptions);
    this.availableFields = filterOpts.fields;
    this.availableOperators = filterOpts.operators;

    this.columnDefs = createBranchColumnDefs(this.translate, this.zone, {
      onEdit: (branch) => this.navigateToEdit(branch),
      onDeactivate: (branch) => this.deactivateBranch(branch)
    });

    const gridCfg = createBranchGridOptions(this.translate);
    this.gridOptions = gridCfg.gridOptions;
    this.agLocaleText = gridCfg.localeText;
  }

  private loadLookups(): void {
    const lang = this.translate.currentLang || 'ar';

    this.lookupService.getOptions('STATUS', lang)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((options) => {
        this.statusOptions = options;
        this.rebuildGridConfig();
        this.cdr.markForCheck();
      });

    this.lookupService.getOptions('BRANCH_TYPE', lang)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((options) => {
        this.branchTypeOptions = options;
        this.rebuildGridConfig();
        this.cdr.markForCheck();
      });
  }

  navigateToCreate(): void { this.router.navigate(['/organization/branches/create']); }

  navigateToEdit(branch: BranchListItemDto): void {
    this.router.navigate(['/organization/branches/edit', branch.branchPk]);
  }

  deactivateBranch(branch: BranchListItemDto): void {
    confirmDeactivateBranch(this.confirmDeps, branch, () => this.reload());
  }

  toggleAdvancedFilters(): void {
    this.showAdvancedFilters = !this.showAdvancedFilters;
    this.cdr.detectChanges();
  }

  onSpecFiltersApply(filters: SpecFilter[]): void {
    this.facade.setFilters(filters as any);
    this.reload();
  }

  onSpecFiltersClear(): void { this.facade.setFilters([]); this.reload(); }
  refreshData(): void { this.reload(); }

  onGridReady(event: GridReadyEvent): void {
    this.gridApi = event.api;
    this.gridApi.sizeColumnsToFit();
  }

  onPaginationChanged(): void { /* handled by ErpListComponent */ }

  private recreateGrid(): void {
    this.showGrid = false;
    this.cdr.detectChanges();
    setTimeout(() => { this.showGrid = true; this.cdr.detectChanges(); }, 0);
  }

  protected load(state: ErpGridState): void {
    this.facade.applyGridStateAndLoad({
      page: state.page,
      size: state.size,
      sortBy: state.sort || 'branchCode',
      sortDir: (state.direction as 'ASC' | 'DESC') || 'ASC',
      filters: this.facade.currentFilters()
    });
  }
}
