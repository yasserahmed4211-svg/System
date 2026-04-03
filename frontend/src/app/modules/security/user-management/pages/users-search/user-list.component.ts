import { Component, OnInit, ViewChild, TemplateRef, inject, effect, ChangeDetectionStrategy, ChangeDetectorRef, NgZone, DestroyRef, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { CellClickedEvent } from 'ag-grid-community';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { ThemeService } from 'src/app/theme/shared/service/customs-theme.service';
import { createAgGridTheme } from 'src/app/shared/ag-grid/agGridTableStyle';

import { ErpListComponent } from 'src/app/shared/base/erp-list.component';
import { SpecificationFilterComponent } from 'src/app/shared/components/specification-filter/specification-filter.component';
import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';
import {
  registerErpAgGridModules
} from 'src/app/shared/ag-grid';
import { ErpGridState, SpecFieldOption, SpecFilter, SpecOperatorOption } from 'src/app/shared/models';

// Shared Foundation imports
import { ErpDualListComponent, DualListItem } from 'src/app/shared/components/erp-dual-list/erp-dual-list.component';
import { ErpDialogService } from 'src/app/shared/services/erp-dialog.service';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { ErpEmptyStateComponent } from 'src/app/shared/components/erp-empty-state/erp-empty-state.component';

import { UserFacade } from 'src/app/modules/security/user-management/facades/user.facade';
import { UserApiService } from 'src/app/modules/security/user-management/services/user-api.service';
import { UserDto, CreateUserRequest, UpdateUserRequest } from 'src/app/modules/security/user-management/models/user.model';
import { UserActionsCellComponent } from '../../components/user-actions-cell/user-actions-cell.component';

import { AgGridAngular } from 'ag-grid-angular';
import {
  ColDef,
  GridReadyEvent,
  GridApi,
  GridOptions
} from 'ag-grid-community';

import {
  createUserFilterOptions,
  createUserColumnDefs,
  createUserGridOptions,
  ERP_DEFAULT_COL_DEF
} from './user-grid.config';
import { confirmDeleteUser, UserConfirmActionDeps } from '../../helpers/user-confirm-actions';

// Register AG Grid modules (centralized, prevents duplicates)
registerErpAgGridModules();

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    AgGridAngular,
    FormsModule,
    TranslateModule,
    SpecificationFilterComponent,
    ErpPermissionDirective,
    ErpDualListComponent,
    ErpEmptyStateComponent
  ],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [UserFacade, UserApiService]
})
export class UserListComponent extends ErpListComponent implements OnInit {
  private themeService = inject(ThemeService);
  private modalService = inject(NgbModal);
  private translate = inject(TranslateService);
  private cdr = inject(ChangeDetectorRef);
  private zone = inject(NgZone);
  private destroyRef = inject(DestroyRef);
  private erpDialogService = inject(ErpDialogService);
  private notificationService = inject(ErpNotificationService);
  readonly facade = inject(UserFacade);

  @ViewChild('createUserModal') createUserModalRef!: TemplateRef<unknown>;
  @ViewChild('rolesModal') rolesModalRef!: TemplateRef<unknown>;

  get direction(): boolean {
    return this.translate.currentLang === 'ar';
  }
  
  showGrid = true;
  private gridApi!: GridApi;

  private agGridSearchFilters: unknown[] = [];
  private lastAgGridFilterSignature = '';
  private lastAgGridSortSignature = '';

  // Form state
  userForm = { username: '', password: '', tenantId: '', enabled: true, roles: [] as string[] };
  formSubmitting = false;
  formError = '';
  isEditMode = false;
  editingUserId: number | null = null;

  // Delete state
  deleteSubmitting = false;

  private formModalRef: NgbModalRef | null = null;

  showAdvancedFilters = false;

  // Theme
  theme = createAgGridTheme(this.themeService.isDarkMode());

  // Data from facade
  get rowData(): UserDto[] { return this.facade.users(); }
  get totalRows(): number { return this.facade.totalElements(); }
  get availableRoles(): string[] { return this.facade.availableRoles(); }
  get rolesLoading(): boolean { return this.facade.rolesLoading(); }
  get currentFilters() { return this.facade['currentFiltersSignal'](); }
  get isLoading(): boolean { return this.facade.loading(); }
  get hasError(): boolean { return !!this.facade.error(); }

  // State for ErpDualListComponent - stored separately to avoid re-computation
  dualListAvailableItems: DualListItem[] = [];
  dualListSelectedItems: DualListItem[] = [];

  /** Prepare the dual list items for the inline roles selector */
  private prepareDualListItems(): void {
    // Get all available roles that are NOT currently selected
    this.dualListAvailableItems = this.availableRoles
      .filter(role => !this.userForm.roles.includes(role))
      .map(role => ({
        id: role,
        label: this.formatRoleName(role),
        secondaryLabel: role
      }));

    // Get currently selected roles
    this.dualListSelectedItems = this.userForm.roles.map(role => ({
      id: role,
      label: this.formatRoleName(role),
      secondaryLabel: role
    }));
  }

  onRolesSelectionChanged(selectedItems: DualListItem[]): void {
    // Update userForm.roles array with the newly selected role IDs
    this.userForm.roles.length = 0; // Clear existing array
    selectedItems.forEach(item => {
      this.userForm.roles.push(item.id as string);
    });

    // Keep available/selected lists consistent after change
    this.prepareDualListItems();
    this.cdr.markForCheck();
  }

  // Dynamic fields - will be translated
  availableFields: SpecFieldOption[] = [];

  // Dynamic operators - will be translated
  availableOperators: SpecOperatorOption[] = [];

  // Dynamic column definitions - will be translated
  columnDefs: ColDef[] = [];

  defaultColDef: ColDef = ERP_DEFAULT_COL_DEF;

  gridOptions: GridOptions = createUserGridOptions(this.translate).gridOptions;

  // AG Grid UI translations (used in filter dropdowns like "Choose one")
  agLocaleText: Record<string, string> = {};

  private rebuildTranslatedUiConfig(): void {
    const filterResult = createUserFilterOptions(this.translate);
    this.availableFields = filterResult.fields;
    this.availableOperators = filterResult.operators;

    this.columnDefs = createUserColumnDefs(
      this.translate,
      this.zone,
      {
        onEdit: (user) => this.openEditModal(user),
        onDelete: (user) => this.deleteUser(user)
      }
    );

    const gridResult = createUserGridOptions(this.translate);
    this.gridOptions = gridResult.gridOptions;
    this.agLocaleText = gridResult.localeText;
  }

  constructor() {
    super();
    effect(() => {
      const isDark = this.themeService.isDarkMode();
      untracked(() => {
        this.theme = createAgGridTheme(isDark);
      });
    });

    effect(() => {
      const availableRoles = this.facade.availableRoles();
      untracked(() => {
        // Keep the dual-list inputs in sync when roles are loaded/refreshed.
        this.dualListAvailableItems = availableRoles
          .filter(role => !this.userForm.roles.includes(role))
          .map(role => ({
            id: role,
            label: this.formatRoleName(role),
            secondaryLabel: role
          }));

        this.dualListSelectedItems = this.userForm.roles.map(role => ({
          id: role,
          label: this.formatRoleName(role),
          secondaryLabel: role
        }));

        this.cdr.markForCheck();
      });
    });
  }

  ngOnInit(): void {
    this.rebuildTranslatedUiConfig();
    this.facade.loadAvailableRoles();
    this.initErpList();
    
    // Subscribe to language changes and recreate grid
    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
      this.rebuildTranslatedUiConfig();
      // Force grid recreation to apply RTL/LTR
      this.showGrid = false;
      
      setTimeout(() => {
        this.showGrid = true;
      }, 0);

      this.cdr.markForCheck();
    });
  }

  private filterChangedHandler = (): void => {
    clearTimeout(this.filterTimeout);
    this.filterTimeout = setTimeout(() => this.onFilterChanged(), 500);
  };
  private sortChangedHandler = (): void => this.onSortChanged();
  private filterTimeout?: ReturnType<typeof setTimeout>;

  onGridReady(event: GridReadyEvent): void {
    this.gridApi = event.api;
    // Data loading is handled by the shared ErpListComponent base class.

    this.gridApi.addEventListener('filterChanged', this.filterChangedHandler);
    this.gridApi.addEventListener('sortChanged', this.sortChangedHandler);

    // Clean up AG Grid listeners on destroy
    this.destroyRef.onDestroy(() => {
      if (this.gridApi) {
        this.gridApi.removeEventListener('filterChanged', this.filterChangedHandler);
        this.gridApi.removeEventListener('sortChanged', this.sortChangedHandler);
      }
    });
  }

  private onFilterChanged(): void {
    const filterModel = this.gridApi.getFilterModel() as Record<string, { filterType: string; type: string; filter: string; filterTo?: number }>;
    const nextFilters = this.facade.convertAgGridFiltersToSearchFilters(filterModel);
    const signature = JSON.stringify(nextFilters);
    if (signature === this.lastAgGridFilterSignature) return;
    this.lastAgGridFilterSignature = signature;
    this.agGridSearchFilters = nextFilters;
    this.reload();
  }

  private onSortChanged(): void {
    const sortModel = this.gridApi.getColumnState().find(col => col.sort !== null);
    const nextSort = sortModel && sortModel.colId
      ? { field: sortModel.colId, direction: sortModel.sort === 'asc' ? 'ASC' as const : 'DESC' as const }
      : null;

    const signature = nextSort ? `${nextSort.field}:${nextSort.direction}` : '';
    if (signature === this.lastAgGridSortSignature) return;
    this.lastAgGridSortSignature = signature;

    const current = this.gridState();
    if (nextSort) {
      if (current.sort === nextSort.field && current.direction === nextSort.direction) return;
      this.setSort(nextSort.field, nextSort.direction);
    } else {
      if (!current.sort && !current.direction) return;
      this.clearSort();
    }
  }

  onPaginationChanged(): void {
    if (this.gridApi) {
      const newPage = this.gridApi.paginationGetCurrentPage();
      if (newPage !== this.gridState().page) this.setPage(newPage);

      // Sync page size if user changes it via selector
      const pageSize = this.gridApi.paginationGetPageSize?.();
      if (typeof pageSize === 'number' && pageSize > 0 && pageSize !== this.gridState().size) {
        this.setSize(pageSize);
      }
    }
  }

  refreshData(): void {
    if (this.gridApi) {
      this.gridApi.setFilterModel(null);
      this.gridApi.applyColumnState({ defaultState: { sort: null } });
    }
    this.agGridSearchFilters = [];
    this.lastAgGridFilterSignature = '';
    this.lastAgGridSortSignature = '';
    this.patchState({ filters: [], sort: undefined, direction: undefined, page: 0 });
  }

  protected load(state: ErpGridState): void {
    const specFilters = this.facade.convertSpecFiltersToSearchFilters(state.filters);
    const combined = [...(this.agGridSearchFilters as any[]), ...specFilters];

    this.facade.applyGridStateAndLoad({
      page: state.page,
      size: state.size,
      sortBy: state.sort ?? 'id',
      sortDir: state.direction ?? 'ASC',
      filters: combined
    });
  }

  // Modal operations
  openCreateModal(content: TemplateRef<unknown>): void {
    // Reset form state completely
    this.isEditMode = false;
    this.editingUserId = null;
    this.formError = '';
    
    // Create new form object to ensure change detection
    this.userForm = {
      username: '',
      password: '',
      tenantId: '',
      enabled: true,
      roles: []
    };

    this.prepareDualListItems();
    
    // Force change detection
    this.cdr.detectChanges();
    
    let modalRef: NgbModalRef;
    try {
      modalRef = this.modalService.open(content, { size: 'md', centered: true });
    } catch (e) {
      this.notificationService.error('ERRORS.OPERATION_FAILED');
      return;
    }
    
    // Reset form when modal is dismissed/closed (always handle rejection to avoid unhandled promise)
    modalRef.result.then(() => undefined, () => undefined).finally(() => {
      this.userForm = { username: '', password: '', tenantId: '', enabled: true, roles: [] };
      this.formError = '';
      this.isEditMode = false;
      this.editingUserId = null;

      this.dualListAvailableItems = [];
      this.dualListSelectedItems = [];
      this.cdr.detectChanges();
    });
  }

  openRolesModal(): void {
    if (this.rolesLoading) return;
    if (this.availableRoles.length === 0) return;
    if (!this.rolesModalRef) return;

    this.prepareDualListItems();
    this.cdr.markForCheck();

    try {
      this.modalService.open(this.rolesModalRef, { size: 'lg', centered: true, scrollable: true });
    } catch (e) {
      this.notificationService.error('ERRORS.OPERATION_FAILED');
    }
  }

  openEditModal(user: UserDto): void {
    this.isEditMode = true;
    this.editingUserId = user.id;
    const normalizedRoles = Array.isArray(user.roles) 
      ? user.roles
          .map(r => {
            if (typeof r === 'string') return r;
            const obj = r as any;
            return (obj.roleCode ?? obj.roleName ?? obj.name) as string | undefined;
          })
          .filter((v): v is string => typeof v === 'string' && v.trim().length > 0)
      : [];
    this.userForm = { 
      username: user.username, 
      password: '', 
      tenantId: user.tenantId || '', 
      enabled: user.enabled, 
      roles: normalizedRoles
    };
    this.formError = '';

    this.prepareDualListItems();

    this.cdr.detectChanges();

    if (!this.createUserModalRef) {
      this.notificationService.error('ERRORS.OPERATION_FAILED');
      return;
    }

    let modalRef: NgbModalRef;
    try {
      modalRef = this.modalService.open(this.createUserModalRef, { size: 'md', centered: true });
    } catch (e) {
      this.notificationService.error('ERRORS.OPERATION_FAILED');
      return;
    }
    
    // Reset form when modal is dismissed/closed (always handle rejection to avoid unhandled promise)
    modalRef.result.then(() => undefined, () => undefined).finally(() => {
      this.userForm = { username: '', password: '', tenantId: '', enabled: true, roles: [] };
      this.formError = '';
      this.isEditMode = false;
      this.editingUserId = null;

      this.dualListAvailableItems = [];
      this.dualListSelectedItems = [];
      this.cdr.detectChanges();
    });
  }

  createUser(modal: NgbModalRef): void {
    this.userForm.username = this.userForm.username?.trim() || '';
    if (!this.userForm.username || this.userForm.username.length < 3) {
      this.formError = this.userForm.username
        ? 'USERS.USERNAME_MIN_LENGTH'
        : 'USERS.ENTER_USERNAME';
      return;
    }

    if (!this.isEditMode && !this.userForm.password) {
      this.formError = 'USERS.ENTER_PASSWORD';
      return;
    }

    if (this.userForm.password && this.userForm.password.length < 6) {
      this.formError = 'USERS.PASSWORD_MIN_LENGTH';
      return;
    }
    
    this.formSubmitting = true;
    this.formError = '';
    this.formModalRef = modal;

    if (this.isEditMode && this.editingUserId) {
      const request: UpdateUserRequest = { 
        username: this.userForm.username, 
        enabled: this.userForm.enabled,
        tenantId: this.userForm.tenantId || undefined,
        roleNames: this.userForm.roles.length > 0 ? this.userForm.roles : undefined
      };
      if (this.userForm.password) request.password = this.userForm.password;

      this.facade.updateUser(this.editingUserId, request).subscribe((response) => {
        this.formSubmitting = false;
        if (!response) {
          this.formError = this.facade.actionError() || 'ERRORS.OPERATION_FAILED';
          this.cdr.markForCheck();
          return;
        }

        modal.close();
        this.notificationService.success('MESSAGES.UPDATE_SUCCESS');
        this.cdr.markForCheck();
      });
    } else {
      const request: CreateUserRequest = { 
        username: this.userForm.username, 
        password: this.userForm.password, 
        enabled: this.userForm.enabled,
        tenantId: this.userForm.tenantId || undefined,
        roleNames: this.userForm.roles.length > 0 ? this.userForm.roles : undefined
      };
      
      this.facade.createUser(request).subscribe((response) => {
        this.formSubmitting = false;
        if (!response) {
          this.formError = this.facade.actionError() || 'ERRORS.OPERATION_FAILED';
          this.cdr.markForCheck();
          return;
        }

        modal.close();
        this.notificationService.success('MESSAGES.CREATE_SUCCESS');
        this.cdr.markForCheck();
      });
    }
  }

  deleteUser(user: UserDto): void {
    const deps: UserConfirmActionDeps = {
      dialog: this.erpDialogService,
      notify: this.notificationService
    };
    confirmDeleteUser(deps, user, () => this.performDelete(user.id));
  }

  private performDelete(userId: number): void {
    this.deleteSubmitting = true;
    this.cdr.markForCheck();

    this.facade.deleteUser(userId).subscribe((result) => {
      this.deleteSubmitting = false;

      if (result === null) {
        this.notificationService.error(this.facade.actionError() || 'MESSAGES.DELETE_FAILED');
        this.cdr.markForCheck();
        return;
      }

      this.notificationService.success('MESSAGES.DELETE_SUCCESS');
      this.cdr.markForCheck();
    });
  }

  // Advanced filters
  toggleAdvancedFilters(): void {
    this.showAdvancedFilters = !this.showAdvancedFilters;
  }

  onSpecFiltersApply(filters: SpecFilter[]): void {
    this.setFilters(filters);
  }

  onSpecFiltersClear(): void {
    this.clearFilters();
    this.reload();
  }

  formatRoleName(role: string): string {
    // Prefer translation key, fallback to role code (no English generation).
    const key = `ROLES.${role}`;
    const translated = this.translate.instant(key);
    return translated !== key ? translated : role;
  }

  quickRemoveRole(role: string): void {
    const index = this.userForm.roles.indexOf(role);
    if (index > -1) {
      this.userForm.roles.splice(index, 1);
      this.prepareDualListItems();
    }
    this.cdr.markForCheck();
  }

}
