import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  NgZone,
  OnInit,
  effect,
  inject,
  untracked
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AgGridAngular } from 'ag-grid-angular';
import {
  ColDef,
  GridApi,
  GridOptions,
  GridReadyEvent
} from 'ag-grid-community';

import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { ThemeService } from 'src/app/theme/shared/service/customs-theme.service';
import { createAgGridTheme } from 'src/app/shared/ag-grid/agGridTableStyle';
import {
  registerErpAgGridModules
} from 'src/app/shared/ag-grid';

import { ErpListComponent } from 'src/app/shared/base/erp-list.component';
import { SpecificationFilterComponent } from 'src/app/shared/components/specification-filter/specification-filter.component';
import { ErpEmptyStateComponent } from 'src/app/shared/components/erp-empty-state/erp-empty-state.component';
import { ErpDialogService } from 'src/app/shared/services/erp-dialog.service';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';

import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';

import { ErpGridState, SpecFieldOption, SpecFilter, SpecOperatorOption } from 'src/app/shared/models';

import { RoleAccessFacade, RoleSearchFilter } from '../../facades/role-access.facade';
import { RoleAccessApiService } from '../../services/role-access-api.service';
import { RoleDto } from '../../models/role-access.model';
import { RoleActionsCellComponent } from '../role-actions-cell/role-actions-cell.component';

import {
  createRoleFilterOptions,
  createRoleColumnDefs,
  createRoleGridOptions,
  ERP_DEFAULT_COL_DEF
} from './role-access-grid.config';
import { confirmToggleRoleActive, confirmDeleteRole, RoleConfirmActionDeps } from '../../helpers/role-confirm-actions';

// Register AG Grid modules (centralized, prevents duplicates)
registerErpAgGridModules();

type Direction = 'ltr' | 'rtl' | 'auto';

@Component({
  selector: 'app-role-access-control',
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    AgGridAngular,
    TranslateModule,
    SpecificationFilterComponent,
    ErpPermissionDirective,
    ErpEmptyStateComponent
  ],
  templateUrl: './role-access-control.component.html',
  styleUrl: './role-access-control.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [RoleAccessFacade, RoleAccessApiService]
})
export class RoleAccessControlComponent extends ErpListComponent implements OnInit {
  private readonly themeService = inject(ThemeService);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly zone = inject(NgZone);
  private readonly destroyRef = inject(DestroyRef);

  readonly facade = inject(RoleAccessFacade);
  private readonly dialogService = inject(ErpDialogService);
  private readonly notificationService = inject(ErpNotificationService);
  private readonly authService = inject(AuthenticationService);

  get direction(): Direction {
    return this.translate.currentLang === 'ar' ? 'rtl' : 'ltr';
  }

  showGrid = true;
  private gridApi!: GridApi;
  theme = createAgGridTheme(this.themeService.isDarkMode());
  showAdvancedFilters = false;

  get rowData(): RoleDto[] {
    return this.facade.roles();
  }
  get isLoading(): boolean {
    return this.facade.loading();
  }
  get hasError(): boolean {
    return !!this.facade.error();
  }

  currentFilters: RoleSearchFilter[] = [];
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
      this.currentFilters = this.facade.currentFilters();
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
    const { fields, operators } = createRoleFilterOptions(this.translate);
    this.availableFields = fields;
    this.availableOperators = operators;
  }

  private initializeColumnDefs(): void {
    this.columnDefs = createRoleColumnDefs(
      this.translate,
      this.zone,
      {
        onEdit: (role) => this.navigateToEdit(role),
        onToggleActive: (role) => this.toggleRoleActive(role),
        onDelete: (role) => this.deleteRole(role)
      }
    );
  }

  private initializeGridOptions(): void {
    const result = createRoleGridOptions(this.translate);
    this.gridOptions = result.gridOptions;
    this.agLocaleText = result.localeText;
  }

  toggleAdvancedFilters(): void {
    this.showAdvancedFilters = !this.showAdvancedFilters;
    this.cdr.detectChanges();
  }

  onSpecFiltersApply(filters: SpecFilter[]): void {
    const apiFilters = filters.map((f) => {
      return {
        field: f.field,
        op: f.operator.toUpperCase() as 'EQ' | 'LIKE',
        value: f.value ?? undefined
      };
    }) as RoleSearchFilter[];

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

  navigateToCreate(): void {
    this.router.navigate(['/security/role-access/create']);
  }

  navigateToEdit(role: RoleDto): void {
    this.router.navigate(['/security/role-access/edit', role.id]);
  }

  toggleRoleActive(role: RoleDto): void {
    const deps: RoleConfirmActionDeps = {
      dialog: this.dialogService,
      notify: this.notificationService,
      auth: this.authService,
      facade: this.facade
    };
    confirmToggleRoleActive(deps, role, () => this.reload());
  }

  deleteRole(role: RoleDto): void {
    const deps: RoleConfirmActionDeps = {
      dialog: this.dialogService,
      notify: this.notificationService,
      auth: this.authService,
      facade: this.facade
    };
    confirmDeleteRole(deps, role, () => this.reload());
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
      sortBy: state.sort || 'roleName',
      sortDir: state.direction || 'ASC',
      filters: this.facade.currentFilters()
    });
  }
}
