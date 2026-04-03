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

import { RoleDto } from '../../models/role-access.model';
import { RoleActionsCellComponent } from '../../components/role-actions-cell/role-actions-cell.component';

/**
 * Creates filter field options for the Role Access search screen.
 */
export function createRoleFilterOptions(translate: TranslateService): {
  fields: SpecFieldOption[];
  operators: SpecOperatorOption[];
} {
  const activeLabel = translate.instant('COMMON.ACTIVE') || translate.instant('USERS.ACTIVE');
  const inactiveLabel = translate.instant('COMMON.INACTIVE') || translate.instant('USERS.INACTIVE');

  const fields: SpecFieldOption[] = [
    { value: 'search', label: translate.instant('ROLE_ACCESS.ROLE_NAME') },
    {
      value: 'active',
      label: translate.instant('USERS.STATUS'),
      options: [
        { value: true, label: activeLabel },
        { value: false, label: inactiveLabel }
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
 * Creates column definitions for Role Access AG Grid.
 */
export function createRoleColumnDefs(
  translate: TranslateService,
  zone: NgZone,
  callbacks: {
    onEdit: (role: RoleDto) => void;
    onToggleActive: (role: RoleDto) => void;
    onDelete: (role: RoleDto) => void;
  }
): ColDef[] {
  const activeLabels: ActiveColumnLabels = {
    active: translate.instant('COMMON.ACTIVE') || translate.instant('USERS.ACTIVE'),
    inactive: translate.instant('COMMON.INACTIVE') || translate.instant('USERS.INACTIVE'),
    all: translate.instant('COMMON.ALL')
  };

  return [
    {
      field: 'roleName',
      headerName: translate.instant('ROLE_ACCESS.ROLE_NAME'),
      sortable: true,
      flex: 1,
      minWidth: 220
    },
    createActiveColumnDef(activeLabels, {
      headerName: translate.instant('USERS.STATUS'),
      floatingFilter: false
    }),
    {
      headerName: translate.instant('COMMON.ACTIONS'),
      maxWidth: 170,
      pinned: 'right',
      cellRenderer: RoleActionsCellComponent,
      cellRendererParams: {
        onEdit: (role: RoleDto) => zone.run(() => callbacks.onEdit(role)),
        onToggleActive: (role: RoleDto) => zone.run(() => callbacks.onToggleActive(role)),
        onDelete: (role: RoleDto) => zone.run(() => callbacks.onDelete(role))
      }
    }
  ];
}

/**
 * Creates grid options for Role Access AG Grid.
 */
export function createRoleGridOptions(translate: TranslateService): {
  gridOptions: GridOptions;
  localeText: Record<string, string>;
} {
  const isRtl = translate.currentLang === 'ar';
  const gridOptions = createErpGridOptions({
    enableRtl: isRtl,
    pageSize: 20
  });

  const localeText: Record<string, string> = {
    true: translate.instant('COMMON.ACTIVE') || translate.instant('USERS.ACTIVE'),
    false: translate.instant('COMMON.INACTIVE') || translate.instant('USERS.INACTIVE')
  };

  return { gridOptions, localeText };
}

/** Re-export default column definition */
export { ERP_DEFAULT_COL_DEF };
