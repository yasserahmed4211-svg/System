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
import { JournalFacade } from 'src/app/modules/finance/gl/facades/journal.facade';
import { JournalApiService } from 'src/app/modules/finance/gl/services/journal-api.service';
import { GlJournalHdrDto, JOURNAL_LOOKUP_KEYS } from 'src/app/modules/finance/gl/models/journal.model';
import { SearchFilter } from 'src/app/modules/finance/gl/models/gl.model';
import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';

import {
  JournalConfirmActionDeps,
  confirmDeactivateJournal,
  confirmApproveJournal,
  confirmPostJournal,
  confirmReverseJournal,
  confirmCancelJournal
} from '../../helpers/journal-confirm-actions';

import { AgGridAngular } from 'ag-grid-angular';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ColDef, GridReadyEvent, GridApi, GridOptions } from 'ag-grid-community';

import {
  createJournalFilterOptions,
  createJournalColumnDefs,
  createJournalGridOptions,
  ERP_DEFAULT_COL_DEF
} from './journals-grid.config';

registerErpAgGridModules();

type Direction = 'ltr' | 'rtl' | 'auto';

/**
 * JournalsSearchComponent – GL Journal Search/List page.
 *
 * Displays journals in an AG Grid with advanced filters,
 * inline state-transition actions, and pagination.
 */
@Component({
  selector: 'app-journals-search',
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
  templateUrl: './journals-search.component.html',
  styleUrl: './journals-search.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [JournalFacade, JournalApiService]
})
export class JournalsSearchComponent extends ErpListComponent implements OnInit, OnDestroy {
  private themeService = inject(ThemeService);
  private router = inject(Router);
  readonly translate = inject(TranslateService);
  private cdr = inject(ChangeDetectorRef);
  private zone = inject(NgZone);
  private destroyRef = inject(DestroyRef);
  readonly facade = inject(JournalFacade);

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

  get rowData(): GlJournalHdrDto[] { return this.facade.journals(); }
  get isLoading(): boolean { return this.facade.journalsLoading(); }
  get hasError(): boolean { return !!this.facade.journalsError(); }
  currentFilters: SearchFilter[] = [];
  availableFields: SpecFieldOption[] = [];
  availableOperators: SpecOperatorOption[] = [];

  columnDefs: ColDef[] = [];
  defaultColDef: ColDef = ERP_DEFAULT_COL_DEF;
  gridOptions: GridOptions = {};
  agLocaleText: Record<string, string> = {};

  private journalTypeOptions: LookupSelectOption[] = [];
  private statusOptions: LookupSelectOption[] = [];

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
      this.currentFilters = this.facade.filters();
      untracked(() => this.cdr.markForCheck());
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
      journalTypes: this.lookupService.getOptions(JOURNAL_LOOKUP_KEYS.JOURNAL_TYPE, lang),
      statuses: this.lookupService.getOptions(JOURNAL_LOOKUP_KEYS.JOURNAL_STATUS, lang),
      sourceModules: this.lookupService.getOptions(JOURNAL_LOOKUP_KEYS.SOURCE_MODULE, lang)
    }).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ journalTypes, statuses, sourceModules }) => {
        this.journalTypeOptions = journalTypes;
        this.statusOptions = statuses;
        this.initializeFilterOptions(journalTypes, statuses, sourceModules);
        this.initializeColumnDefs();
        this.recreateGrid();
        this.cdr.markForCheck();
      });
  }

  private initializeFilterOptions(
    journalTypeOptions: LookupSelectOption[] = [],
    statusOptions: LookupSelectOption[] = [],
    sourceModuleOptions: LookupSelectOption[] = []
  ): void {
    const { fields, operators } = createJournalFilterOptions(
      this.translate, journalTypeOptions, statusOptions, sourceModuleOptions
    );
    this.availableFields = fields;
    this.availableOperators = operators;
  }

  private initializeColumnDefs(): void {
    this.columnDefs = createJournalColumnDefs(
      this.translate,
      this.zone,
      {
        onView: (j) => this.navigateToView(j),
        onEdit: (j) => this.navigateToEdit(j),
        onDeactivate: (j) => this.deactivateJournal(j),
        onApprove: (j) => this.approveJournal(j),
        onPost: (j) => this.postJournal(j),
        onReverse: (j) => this.reverseJournal(j),
        onCancel: (j) => this.cancelJournal(j)
      },
      {
        journalTypeOptions: this.journalTypeOptions,
        statusOptions: this.statusOptions
      }
    );
  }

  private initializeGridOptions(): void {
    const result = createJournalGridOptions(this.translate);
    this.gridOptions = result.gridOptions;
    this.agLocaleText = result.localeText;
  }

  // ── Navigation ────────────────────────────────────────────

  navigateToCreate(): void {
    if (!this.authService.hasPermission('PERM_GL_JOURNAL_CREATE')) {
      this.notificationService.warning('MESSAGES.NO_PERMISSION');
      return;
    }
    this.router.navigate(['/finance/gl/journals/create']);
  }

  navigateToEdit(journal: GlJournalHdrDto): void {
    if (!this.authService.hasPermission('PERM_GL_JOURNAL_UPDATE')) {
      this.notificationService.warning('MESSAGES.NO_PERMISSION');
      return;
    }
    this.router.navigate(['/finance/gl/journals/edit', journal.id]);
  }

  navigateToView(journal: GlJournalHdrDto): void {
    this.router.navigate(['/finance/gl/journals/view', journal.id]);
  }

  // ── Inline Actions ────────────────────────────────────────

  private get confirmDeps(): JournalConfirmActionDeps {
    return {
      dialog: this.dialogService,
      notify: this.notificationService,
      auth: this.authService,
      facade: this.facade
    };
  }

  deactivateJournal(journal: GlJournalHdrDto): void {
    confirmDeactivateJournal(this.confirmDeps, journal, () => this.reload());
  }

  approveJournal(journal: GlJournalHdrDto): void {
    confirmApproveJournal(this.confirmDeps, journal, () => this.reload());
  }

  postJournal(journal: GlJournalHdrDto): void {
    confirmPostJournal(this.confirmDeps, journal, () => this.reload());
  }

  reverseJournal(journal: GlJournalHdrDto): void {
    confirmReverseJournal(this.confirmDeps, journal, () => this.reload());
  }

  cancelJournal(journal: GlJournalHdrDto): void {
    confirmCancelJournal(this.confirmDeps, journal, () => this.reload());
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
      sortBy: state.sort || 'id',
      sortDir: state.direction || 'DESC',
      filters: this.facade.filters()
    });
  }

  ngOnDestroy(): void {
    this.facade.clearCurrentEntity();
  }
}
