import { Component, OnInit, OnDestroy, inject, effect, ChangeDetectionStrategy, ChangeDetectorRef, DestroyRef, NgZone, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';

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

import { LookupService } from 'src/app/core/services';
import { LookupSelectOption } from 'src/app/core/models';
import { GlFacade } from 'src/app/modules/finance/gl/facades/gl.facade';
import { GlApiService } from 'src/app/modules/finance/gl/services/gl-api.service';
import { AccRuleHdrDto, SearchFilter, GL_LOOKUP_KEYS } from 'src/app/modules/finance/gl/models/gl.model';
import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';

import { AgGridAngular } from 'ag-grid-angular';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ColDef, GridReadyEvent, GridApi, GridOptions } from 'ag-grid-community';

import {
  createRuleFilterOptions,
  createRuleColumnDefs,
  createRuleGridOptions,
  ERP_DEFAULT_COL_DEF
} from './rules-grid.config';
import { confirmDeactivateRule, GlConfirmActionDeps } from '../../helpers/gl-confirm-actions';

registerErpAgGridModules();

type Direction = 'ltr' | 'rtl' | 'auto';

/**
 * RulesSearchComponent (Blueprint Level 2 – Page A: Search/List)
 *
 * @requirement FE-REQ-GL-001 §4
 * @task TASK-FE-GL-001
 */
@Component({
  selector: 'app-rules-search',
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
  templateUrl: './rules-search.component.html',
  styleUrl: './rules-search.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [GlFacade, GlApiService]
})
export class RulesSearchComponent extends ErpListComponent implements OnInit, OnDestroy {
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

  get rowData(): AccRuleHdrDto[] { return this.facade.rules(); }
  get isLoading(): boolean { return this.facade.rulesLoading(); }
  get hasError(): boolean { return !!this.facade.rulesError(); }
  currentFilters: SearchFilter[] = [];
  availableFields: SpecFieldOption[] = [];
  availableOperators: SpecOperatorOption[] = [];

  columnDefs: ColDef[] = [];
  defaultColDef: ColDef = ERP_DEFAULT_COL_DEF;
  gridOptions: GridOptions = {};
  agLocaleText: Record<string, string> = {};

  private sourceModuleMap = new Map<string, string>();
  private sourceDocTypeMap = new Map<string, string>();

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
      this.currentFilters = this.facade.ruleFilters();
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

  private loadFilterLookups(): void {
    const lang = this.translate.currentLang || 'ar';
    forkJoin({
      sourceModules: this.lookupService.getOptions(GL_LOOKUP_KEYS.SOURCE_MODULE, lang),
      sourceDocTypes: this.lookupService.getOptions(GL_LOOKUP_KEYS.SOURCE_DOC_TYPE, lang)
    }).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ sourceModules, sourceDocTypes }) => {
        this.sourceModuleMap = new Map(sourceModules.map(o => [o.value, o.label]));
        this.sourceDocTypeMap = new Map(sourceDocTypes.map(o => [o.value, o.label]));
        this.initializeFilterOptions(sourceModules, sourceDocTypes);
        this.initializeColumnDefs();
        if (this.gridApi) {
          this.gridApi.setGridOption('columnDefs', this.columnDefs);
        }
        this.cdr.markForCheck();
      });
  }

  private initializeFilterOptions(
    sourceModuleOptions: LookupSelectOption[] = [],
    sourceDocTypeOptions: LookupSelectOption[] = []
  ): void {
    const { fields, operators } = createRuleFilterOptions(this.translate, sourceModuleOptions, sourceDocTypeOptions);
    this.availableFields = fields;
    this.availableOperators = operators;
  }

  private initializeColumnDefs(): void {
    this.columnDefs = createRuleColumnDefs(
      this.translate,
      this.zone,
      {
        onEdit: (rule) => this.navigateToEdit(rule),
        onDeactivate: (rule) => this.deactivateRule(rule)
      },
      {
        sourceModuleMap: this.sourceModuleMap,
        sourceDocTypeMap: this.sourceDocTypeMap
      }
    );
  }

  private initializeGridOptions(): void {
    const result = createRuleGridOptions(this.translate);
    this.gridOptions = result.gridOptions;
    this.agLocaleText = result.localeText;
  }

  // ── Navigation ────────────────────────────────────────────

  navigateToCreate(): void {
    if (!this.authService.hasPermission('PERM_GL_RULE_CREATE')) {
      this.notificationService.warning('MESSAGES.NO_PERMISSION');
      return;
    }
    this.router.navigate(['/finance/gl/rules/create']);
  }

  navigateToEdit(rule: AccRuleHdrDto): void {
    if (!this.authService.hasPermission('PERM_GL_RULE_UPDATE')) {
      this.notificationService.warning('MESSAGES.NO_PERMISSION');
      return;
    }
    this.router.navigate(['/finance/gl/rules/edit', rule.ruleId]);
  }

  // ── Inline Deactivation ───────────────────────────────────

  deactivateRule(rule: AccRuleHdrDto): void {
    const deps: GlConfirmActionDeps = {
      dialog: this.dialogService,
      notify: this.notificationService,
      auth: this.authService,
      facade: this.facade
    };
    confirmDeactivateRule(deps, rule, () => this.reload());
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

    this.facade.setRuleFilters(apiFilters);
    this.reload();
  }

  onSpecFiltersClear(): void {
    this.facade.setRuleFilters([]);
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
    this.facade.applyRuleGridStateAndLoad({
      page: state.page,
      size: state.size,
      sortBy: state.sort || 'ruleId',
      sortDir: state.direction || 'ASC',
      filters: this.facade.ruleFilters()
    });
  }

  ngOnDestroy(): void {
    this.facade.clearCurrentEntity();
  }
}
