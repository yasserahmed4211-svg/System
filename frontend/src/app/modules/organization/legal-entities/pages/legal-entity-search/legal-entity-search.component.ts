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

import { LegalEntityFacade } from '../../facades/legal-entity.facade';
import { LegalEntityApiService } from '../../services/legal-entity-api.service';
import { LegalEntityListItemDto } from '../../models/legal-entity.model';
import { ConfirmActionDeps, confirmDeactivateLegalEntity } from '../../helpers/legal-entity-confirm-actions';
import {
  createLegalEntityFilterOptions,
  createLegalEntityColumnDefs,
  createLegalEntityGridOptions,
  ERP_DEFAULT_COL_DEF
} from './legal-entity-grid.config';

registerErpAgGridModules();

@Component({
  selector: 'app-legal-entity-search',
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
  providers: [LegalEntityFacade, LegalEntityApiService],
  templateUrl: './legal-entity-search.component.html',
  styleUrl: './legal-entity-search.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LegalEntitySearchComponent extends ErpListComponent implements OnInit {
  private themeService = inject(ThemeService);
  private router = inject(Router);
  readonly translate = inject(TranslateService);
  private cdr = inject(ChangeDetectorRef);
  private zone = inject(NgZone);
  private destroyRef = inject(DestroyRef);
  readonly facade = inject(LegalEntityFacade);
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

  get rowData(): LegalEntityListItemDto[] { return this.facade.entities(); }
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
    const filterOpts = createLegalEntityFilterOptions(this.translate, this.statusOptions);
    this.availableFields = filterOpts.fields;
    this.availableOperators = filterOpts.operators;

    this.columnDefs = createLegalEntityColumnDefs(this.translate, this.zone, {
      onEdit: (entity) => this.navigateToEdit(entity),
      onDeactivate: (entity) => this.deactivateEntity(entity)
    });

    const gridCfg = createLegalEntityGridOptions(this.translate);
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
  }

  navigateToCreate(): void { this.router.navigate(['/organization/legal-entities/create']); }

  navigateToEdit(entity: LegalEntityListItemDto): void {
    this.router.navigate(['/organization/legal-entities/edit', entity.legalEntityPk]);
  }

  deactivateEntity(entity: LegalEntityListItemDto): void {
    confirmDeactivateLegalEntity(this.confirmDeps, entity, () => this.reload());
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
    setTimeout(() => {
      this.showGrid = true;
      this.cdr.detectChanges();
    }, 0);
  }

  protected load(state: ErpGridState): void {
    this.facade.applyGridStateAndLoad({
      page: state.page,
      size: state.size,
      sortBy: state.sort || 'legalEntityCode',
      sortDir: (state.direction as 'ASC' | 'DESC') || 'ASC',
      filters: this.facade.currentFilters()
    });
  }
}
