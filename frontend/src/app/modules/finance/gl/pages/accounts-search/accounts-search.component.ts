import { Component, OnInit, OnDestroy, inject, effect, ChangeDetectionStrategy, ChangeDetectorRef, DestroyRef, NgZone, untracked } from '@angular/core';
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
import { ErpGridState, SpecFieldOption, SpecFilter, SpecOperatorOption } from 'src/app/shared/models';

import { ErpDialogService } from 'src/app/shared/services/erp-dialog.service';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';

import { GlFacade } from 'src/app/modules/finance/gl/facades/gl.facade';
import { GlApiService } from 'src/app/modules/finance/gl/services/gl-api.service';
import { AccountChartDto, SearchFilter, GL_LOOKUP_KEYS } from 'src/app/modules/finance/gl/models/gl.model';
import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';

import { AgGridAngular } from 'ag-grid-angular';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ColDef, GridReadyEvent, GridApi, GridOptions } from 'ag-grid-community';
import { LookupService } from 'src/app/core/services/lookup.service';
import { LookupSelectOption } from 'src/app/core/models/lookup-detail.model';

import {
  createAccountFilterOptions,
  createAccountColumnDefs,
  createAccountGridOptions,
  ERP_DEFAULT_COL_DEF
} from './accounts-grid.config';
import { confirmDeactivateAccount, GlConfirmActionDeps } from '../../helpers/gl-confirm-actions';

registerErpAgGridModules();

type Direction = 'ltr' | 'rtl' | 'auto';

/**
 * AccountsSearchComponent (Blueprint Level 2 – Page A: Search/List)
 *
 * @requirement FE-REQ-GL-001 §3
 * @task TASK-FE-GL-001
 */
@Component({
  selector: 'app-accounts-search',
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
  templateUrl: './accounts-search.component.html',
  styleUrl: './accounts-search.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [GlFacade, GlApiService]
})
export class AccountsSearchComponent extends ErpListComponent implements OnInit, OnDestroy {
  private themeService = inject(ThemeService);
  private router = inject(Router);
  readonly translate = inject(TranslateService);
  private cdr = inject(ChangeDetectorRef);
  private zone = inject(NgZone);
  private destroyRef = inject(DestroyRef);
  readonly facade = inject(GlFacade);

  private readonly dialogService = inject(ErpDialogService);
  private readonly notificationService = inject(ErpNotificationService);
  private readonly authService = inject(AuthenticationService);
  private readonly lookupService = inject(LookupService);

  get direction(): Direction {
    return this.translate.currentLang === 'ar' ? 'rtl' : 'ltr';
  }

  showGrid = true;
  private gridApi!: GridApi;
  theme = createAgGridTheme(false);
  showAdvancedFilters = false;

  // Data
  get rowData(): AccountChartDto[] { return this.facade.accounts(); }
  get isLoading(): boolean { return this.facade.accountsLoading(); }
  get hasError(): boolean { return !!this.facade.accountsError(); }
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
      this.currentFilters = this.facade.accountFilters();
    });
  }

  ngOnInit(): void {
    this.initErpList();
    this.loadFilterLookups();

    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.loadFilterLookups();
        this.initializeColumnDefs();
        this.initializeGridOptions();
        this.recreateGrid();
        this.cdr.detectChanges();
      });
  }

  /**
   * Load account type options from backend and rebuild filter fields.
   */
  private loadFilterLookups(): void {
    this.lookupService.getOptions(GL_LOOKUP_KEYS.ACCOUNT_TYPE, this.translate.currentLang)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(options => {
        this.initializeFilterOptions(options);
        this.cdr.detectChanges();
      });
  }

  private initializeFilterOptions(accountTypeOptions: LookupSelectOption[] = []): void {
    const { fields, operators } = createAccountFilterOptions(this.translate, accountTypeOptions);
    this.availableFields = fields;
    this.availableOperators = operators;
  }

  private initializeColumnDefs(): void {
    this.columnDefs = createAccountColumnDefs(
      this.translate,
      this.zone,
      this.availableFields,
      {
        onEdit: (account) => this.navigateToEdit(account),
        onDeactivate: (account) => this.deactivateAccount(account)
      }
    );
  }

  private initializeGridOptions(): void {
    const result = createAccountGridOptions(this.translate);
    this.gridOptions = result.gridOptions;
    this.agLocaleText = result.localeText;
  }

  // ── Navigation ────────────────────────────────────────────

  navigateToCreate(): void {
    if (!this.authService.hasPermission('PERM_GL_ACCOUNT_CREATE')) {
      this.notificationService.warning('MESSAGES.NO_PERMISSION');
      return;
    }
    this.router.navigate(['/finance/gl/accounts/create']);
  }

  navigateToEdit(account: AccountChartDto): void {
    if (!this.authService.hasPermission('PERM_GL_ACCOUNT_UPDATE')) {
      this.notificationService.warning('MESSAGES.NO_PERMISSION');
      return;
    }
    this.router.navigate(['/finance/gl/accounts/edit', account.accountChartPk]);
  }

  navigateToTree(): void {
    this.router.navigate(['/finance/gl/accounts/tree']);
  }

  // ── Inline Deactivation ───────────────────────────────────

  deactivateAccount(account: AccountChartDto): void {
    const deps: GlConfirmActionDeps = {
      dialog: this.dialogService,
      notify: this.notificationService,
      auth: this.authService,
      facade: this.facade
    };
    confirmDeactivateAccount(deps, account, () => this.reload());
  }

  // ── Filters & Grid ────────────────────────────────────────

  toggleAdvancedFilters(): void {
    this.showAdvancedFilters = !this.showAdvancedFilters;
    this.cdr.detectChanges();
  }

  onSpecFiltersApply(filters: SpecFilter[]): void {
    const apiFilters: SearchFilter[] = filters.map((f) => ({
      field: f.field,
      op: f.operator.toUpperCase() as SearchFilter['op'],
      value: f.value ?? undefined
    }));

    this.facade.setAccountFilters(apiFilters);
    this.reload();
  }

  onSpecFiltersClear(): void {
    this.facade.setAccountFilters([]);
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
    this.facade.applyAccountGridStateAndLoad({
      page: state.page,
      size: state.size,
      sortBy: state.sort || 'accountChartNo',
      sortDir: state.direction || 'ASC',
      filters: this.facade.accountFilters()
    });
  }

  ngOnDestroy(): void {
    this.facade.clearCurrentEntity();
  }
}
