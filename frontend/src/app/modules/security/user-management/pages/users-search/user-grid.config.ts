import { TranslateService } from '@ngx-translate/core';
import { ColDef, GridOptions } from 'ag-grid-community';
import { NgZone } from '@angular/core';

import {
  ERP_DEFAULT_COL_DEF,
  createErpGridOptions,
  createActiveColumnDef,
  ActiveColumnLabels
} from 'src/app/shared/ag-grid';
import { SpecFieldOption, SpecOperatorOption } from 'src/app/shared/models';

import { UserDto } from '../../models/user.model';
import { UserActionsCellComponent } from '../../components/user-actions-cell/user-actions-cell.component';

/**
 * Creates filter field options for the User search screen.
 */
export function createUserFilterOptions(translate: TranslateService): {
  fields: SpecFieldOption[];
  operators: SpecOperatorOption[];
} {
  const activeLabels: ActiveColumnLabels = {
    active: translate.instant('USERS.ACTIVE'),
    inactive: translate.instant('USERS.INACTIVE'),
    all: translate.instant('COMMON.ALL')
  };

  const fields: SpecFieldOption[] = [
    { value: 'id', label: translate.instant('USERS.ID') },
    { value: 'username', label: translate.instant('USERS.USERNAME') },
    { value: 'tenantId', label: translate.instant('USERS.TENANT_ID') },
    {
      value: 'enabled',
      label: translate.instant('USERS.STATUS'),
      options: [
        { value: true, label: activeLabels.active },
        { value: false, label: activeLabels.inactive }
      ]
    }
  ];

  const operators: SpecOperatorOption[] = [
    { value: 'eq', label: translate.instant('USERS.EQUALS') },
    { value: 'like', label: translate.instant('USERS.CONTAINS') }
  ];

  return { fields, operators };
}

/**
 * Creates column definitions for User AG Grid.
 */
export function createUserColumnDefs(
  translate: TranslateService,
  zone: NgZone,
  callbacks: {
    onEdit: (user: UserDto) => void;
    onDelete: (user: UserDto) => void;
  }
): ColDef[] {
  const activeLabels: ActiveColumnLabels = {
    active: translate.instant('USERS.ACTIVE'),
    inactive: translate.instant('USERS.INACTIVE'),
    all: translate.instant('COMMON.ALL')
  };

  return [
    { field: 'id', headerName: translate.instant('USERS.ID'), filter: 'agNumberColumnFilter', maxWidth: 100, sortable: true },
    { field: 'username', headerName: translate.instant('USERS.USERNAME'), filter: 'agTextColumnFilter', sortable: true, flex: 1 },
    { field: 'tenantId', headerName: translate.instant('USERS.TENANT_ID'), filter: 'agTextColumnFilter', sortable: true, maxWidth: 150 },
    createActiveColumnDef(activeLabels, {
      field: 'enabled',
      headerName: translate.instant('USERS.STATUS'),
      maxWidth: 120,
      floatingFilter: false
    }),
    {
      field: 'roles', headerName: translate.instant('USERS.ROLES'), filter: false, sortable: false, flex: 1, minWidth: 200,
      cellRenderer: (params: { value: string[] }) => {
        if (!params.value || params.value.length === 0) return '<span class="text-muted">-</span>';
        const visibleCount = 2;
        const roles = params.value;
        let html = roles.slice(0, visibleCount).map(role =>
          `<span class="badge bg-primary me-1" style="cursor: pointer;" title="${role}">${role}</span>`
        ).join('');
        if (roles.length > visibleCount) {
          const remainingRoles = roles.slice(visibleCount).join(', ');
          html += `<span class="badge bg-secondary" style="cursor: pointer;" title="${remainingRoles}">+${roles.length - visibleCount}</span>`;
        }
        return html;
      }
    },
    {
      field: 'actions', headerName: translate.instant('USERS.ACTIONS'), filter: false, sortable: false, maxWidth: 150,
      cellRenderer: UserActionsCellComponent,
      cellRendererParams: {
        onEdit: (user: UserDto) => zone.run(() => callbacks.onEdit(user)),
        onDelete: (user: UserDto) => zone.run(() => callbacks.onDelete(user))
      }
    }
  ];
}

/**
 * Creates grid options for User AG Grid.
 */
export function createUserGridOptions(translate: TranslateService): {
  gridOptions: GridOptions;
  localeText: Record<string, string>;
} {
  const gridOptions = createErpGridOptions({
    enableRtl: translate.currentLang === 'ar',
    pageSize: 20
  });

  const localeText: Record<string, string> = {
    empty: translate.instant('COMMON.SELECT'),
    chooseOne: translate.instant('COMMON.SELECT'),
    true: translate.instant('USERS.ACTIVE'),
    false: translate.instant('USERS.INACTIVE')
  };

  return { gridOptions, localeText };
}

/** Re-export default column definition */
export { ERP_DEFAULT_COL_DEF };
