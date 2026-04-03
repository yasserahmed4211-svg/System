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

import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';

import { PostingFacade } from 'src/app/modules/finance/gl/facades/posting.facade';
import { PostingApiService } from 'src/app/modules/finance/gl/services/posting-api.service';
import { AccPostingMstDto } from 'src/app/modules/finance/gl/models/posting.model';
import { SearchFilter } from 'src/app/modules/finance/gl/models/gl.model';
import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';

import {
  PostingConfirmActionDeps,
  confirmGenerateJournal
} from '../../helpers/posting-confirm-actions';

import { AgGridAngular } from 'ag-grid-angular';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ColDef, GridReadyEvent, GridApi, GridOptions } from 'ag-grid-community';

import {
  createPostingFilterOptions,
  createPostingColumnDefs,
  createPostingGridOptions,
  ERP_DEFAULT_COL_DEF
} from './postings-grid.config';

registerErpAgGridModules();

type Direction = 'ltr' | 'rtl' | 'auto';

/**
 * PostingsSearchComponent – GL Posting Search/List page.
 *
 * Displays postings in an AG Grid with filters,
 * inline "Generate Journal" action for READY_FOR_GL postings,
 * and pagination.
 */
@Component({
  selector: 'app-postings-search',
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
  templateUrl: './postings-search.component.html',
  styleUrl: './postings-search.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [PostingFacade, PostingApiService]
})
export class PostingsSearchComponent extends ErpListComponent implements OnInit, OnDestroy {
  private themeService = inject(ThemeService);
  private router = inject(Router);
  readonly translate = inject(TranslateService);
  private cdr = inject(ChangeDetectorRef);
  private zone = inject(NgZone);
  private destroyRef = inject(DestroyRef);
  readonly facade = inject(PostingFacade);

  private readonly modalService = inject(NgbModal);
  private readonly notificationService = inject(ErpNotificationService);
  private readonly authService = inject(AuthenticationService);

  get direction(): Direction {
    return this.translate.currentLang === 'ar' ? 'rtl' : 'ltr';
  }

  showGrid = true;
  private gridApi!: GridApi;
  theme = createAgGridTheme(false);
  showAdvancedFilters = false;

  get rowData(): AccPostingMstDto[] { return this.facade.postings(); }
  get isLoading(): boolean { return this.facade.postingsLoading(); }
  get hasError(): boolean { return !!this.facade.postingsError(); }
  currentFilters: SearchFilter[] = [];
  availableFields: SpecFieldOption[] = [];
  availableOperators: SpecOperatorOption[] = [];

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
      this.currentFilters = this.facade.filters();
      untracked(() => this.cdr.markForCheck());
    });
  }

  ngOnInit(): void {
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
    const { fields, operators } = createPostingFilterOptions(this.translate);
    this.availableFields = fields;
    this.availableOperators = operators;
  }

  private initializeColumnDefs(): void {
    this.columnDefs = createPostingColumnDefs(
      this.translate,
      this.zone,
      {
        onView: (p) => this.navigateToView(p),
        onGenerateJournal: (p) => this.generateJournal(p),
        onViewJournal: (p) => this.navigateToJournal(p)
      }
    );
  }

  private initializeGridOptions(): void {
    const result = createPostingGridOptions(this.translate);
    this.gridOptions = result.gridOptions;
    this.agLocaleText = result.localeText;
  }

  // ── Navigation ────────────────────────────────────────────

  navigateToView(posting: AccPostingMstDto): void {
    this.router.navigate(['/finance/gl/postings/view', posting.postingId]);
  }

  navigateToJournal(posting: AccPostingMstDto): void {
    if (posting.finJournalIdFk) {
      this.router.navigate(['/finance/gl/journals/view', posting.finJournalIdFk]);
    }
  }

  // ── Inline Actions ────────────────────────────────────────

  private get confirmDeps(): PostingConfirmActionDeps {
    return {
      modal: this.modalService,
      notify: this.notificationService,
      auth: this.authService,
      facade: this.facade
    };
  }

  generateJournal(posting: AccPostingMstDto): void {
    confirmGenerateJournal(this.confirmDeps, posting, () => this.reload());
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
      sortBy: state.sort || 'postingId',
      sortDir: state.direction || 'DESC',
      filters: this.facade.filters()
    });
  }

  ngOnDestroy(): void {
    this.facade.clearCurrentEntity();
  }
}
