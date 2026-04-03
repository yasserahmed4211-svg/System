import { TranslateService } from '@ngx-translate/core';
import { ColDef, GridOptions, RowClassParams } from 'ag-grid-community';
import { NgZone } from '@angular/core';

import {
  ERP_DEFAULT_COL_DEF,
  createErpGridOptions,
  createActiveColumnDef,
  ActiveColumnLabels
} from 'src/app/shared/ag-grid';
import { SpecFieldOption, SpecOperatorOption } from 'src/app/shared/models';
import { LookupSelectOption } from 'src/app/core/models/lookup-detail.model';

import { AccountChartDto } from '../../models/gl.model';
import { AccountActionsCellComponent } from '../../components/account-actions-cell/account-actions-cell.component';

/**
 * Creates filter field options for the Accounts search screen.
 */
export function createAccountFilterOptions(
  translate: TranslateService,
  accountTypeOptions: LookupSelectOption[] = []
): {
  fields: SpecFieldOption[];
  operators: SpecOperatorOption[];
} {
  const fields: SpecFieldOption[] = [
    { value: 'accountChartNo', label: translate.instant('GL.ACCOUNT_CODE') },
    { value: 'accountChartName', label: translate.instant('GL.ACCOUNT_NAME') },
    {
      value: 'accountType',
      label: translate.instant('GL.ACCOUNT_TYPE'),
      options: accountTypeOptions.map(o => ({
        value: o.value,
        label: o.label
      }))
    },
    {
      value: 'isActive',
      label: translate.instant('COMMON.STATUS'),
      options: [
        { value: true, label: translate.instant('COMMON.ACTIVE') },
        { value: false, label: translate.instant('COMMON.INACTIVE') }
      ]
    }
  ];

  const operators: SpecOperatorOption[] = [
    { value: 'eq', label: translate.instant('COMMON.EQUALS') },
    { value: 'like', label: translate.instant('COMMON.CONTAINS') }
  ];

  return { fields, operators };
}

/**
 * Creates column definitions for Accounts AG Grid.
 */
export function createAccountColumnDefs(
  translate: TranslateService,
  zone: NgZone,
  availableFields: SpecFieldOption[],
  callbacks: {
    onEdit: (account: AccountChartDto) => void;
    onDeactivate: (account: AccountChartDto) => void;
  }
): ColDef[] {
  const activeLabels: ActiveColumnLabels = {
    active: translate.instant('COMMON.ACTIVE'),
    inactive: translate.instant('COMMON.INACTIVE'),
    all: translate.instant('COMMON.ALL')
  };

  return [
    {
      field: 'accountChartNo',
      headerName: translate.instant('GL.ACCOUNT_CODE'),
      sortable: true,
      flex: 1,
      minWidth: 180,
      cellRenderer: (params: { data: AccountChartDto; value: string }) => {
        if (!params.data) return params.value;
        const level = params.data.level ?? 0;
        const indent = level > 0 ? level * 16 : 0;
        const isRoot = level === 0 || !params.data.accountChartFk;
        const fontWeight = isRoot ? 'font-weight: 700;' : '';
        const icon = isRoot
          ? '<i class="ti ti-folder me-1" style="color: #0d6efd;"></i>'
          : '<i class="ti ti-file-text me-1" style="color: #6c757d;"></i>';
        return `<span style="padding-inline-start: ${indent}px; ${fontWeight}">${icon}${params.value}</span>`;
      }
    },
    {
      field: 'accountChartName',
      headerName: translate.instant('GL.ACCOUNT_NAME'),
      sortable: true,
      flex: 1.5,
      minWidth: 200,
      tooltipField: 'accountChartName',
      cellRenderer: (params: { data: AccountChartDto; value: string }) => {
        if (!params.data) return params.value;
        const isRoot = (params.data.level ?? 0) === 0 || !params.data.accountChartFk;
        return isRoot ? `<strong>${params.value}</strong>` : params.value;
      }
    },
    {
      field: 'accountType',
      headerName: translate.instant('GL.ACCOUNT_TYPE'),
      sortable: true,
      flex: 1,
      minWidth: 150,
      valueFormatter: (params: { value: string }) => {
        if (!params.value) return '—';
        const option = availableFields
          .find(f => f.value === 'accountType')?.options
          ?.find((o: { value: unknown }) => String(o.value) === String(params.value));
        return option ? String(option.label) : params.value;
      }
    },
    {
      field: 'parentAccountName',
      headerName: translate.instant('GL.PARENT_ACCOUNT'),
      sortable: true,
      flex: 1,
      minWidth: 180,
      cellRenderer: (params: { data: AccountChartDto; value: string }) => {
        if (!params.data || !params.value) {
          return `<span class="text-muted fst-italic">${translate.instant('GL.ROOT_ACCOUNT')}</span>`;
        }
        const parentNo = params.data.parentAccountNo ? `${params.data.parentAccountNo} – ` : '';
        return `${parentNo}${params.value}`;
      }
    },
    {
      field: 'level',
      headerName: translate.instant('GL.LEVEL'),
      sortable: true,
      maxWidth: 120,
      cellRenderer: (params: { data: AccountChartDto; value: number }) => {
        if (!params.data) return params.value;
        const level = params.value ?? 0;
        const badgeClass = level === 0 ? 'bg-primary' : level <= 2 ? 'bg-info' : 'bg-secondary';
        return `<span class="badge ${badgeClass} rounded-pill">${level}</span>`;
      }
    },
    createActiveColumnDef(activeLabels, {
      headerName: translate.instant('COMMON.STATUS'),
      maxWidth: 120,
      floatingFilter: false
    }),
    {
      headerName: translate.instant('COMMON.ACTIONS'),
      maxWidth: 150,
      pinned: 'right',
      cellRenderer: AccountActionsCellComponent,
      cellRendererParams: {
        onEdit: (account: AccountChartDto) => zone.run(() => callbacks.onEdit(account)),
        onDeactivate: (account: AccountChartDto) => zone.run(() => callbacks.onDeactivate(account))
      }
    }
  ];
}

/**
 * Creates grid options for Accounts AG Grid.
 */
export function createAccountGridOptions(translate: TranslateService): {
  gridOptions: GridOptions;
  localeText: Record<string, string>;
} {
  const isRtl = translate.currentLang === 'ar';
  const gridOptions = createErpGridOptions({
    enableRtl: isRtl,
    pageSize: 20
  });

  // Row class rules for visual hierarchy
  gridOptions.rowClassRules = {
    'grid-row-root': (params: RowClassParams<AccountChartDto>) =>
      !!params.data && ((params.data.level ?? 0) === 0 || !params.data.accountChartFk),
    'grid-row-inactive': (params: RowClassParams<AccountChartDto>) =>
      !!params.data && !params.data.isActive,
    'grid-row-posting': (params: RowClassParams<AccountChartDto>) =>
      !!params.data && params.data.isActive && !!params.data.accountChartFk
  };

  const localeText: Record<string, string> = {
    empty: translate.instant('COMMON.SELECT'),
    true: translate.instant('COMMON.ACTIVE'),
    false: translate.instant('COMMON.INACTIVE')
  };

  return { gridOptions, localeText };
}

/** Re-export default column definition */
export { ERP_DEFAULT_COL_DEF };
