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

import { BranchListItemDto } from '../../models/branch.model';
import { BranchActionsCellComponent } from '../../components/branch-actions-cell/branch-actions-cell.component';

export function createBranchFilterOptions(
  translate: TranslateService,
  branchTypeOptions: Array<{ value: string; label: string }> = [],
  statusOptions: Array<{ value: string; label: string }> = []
): {
  fields: SpecFieldOption[];
  operators: SpecOperatorOption[];
} {
  const activeLabel = translate.instant('COMMON.ACTIVE') || 'Active';
  const inactiveLabel = translate.instant('COMMON.INACTIVE') || 'Inactive';

  const fields: SpecFieldOption[] = [
    { value: 'branchCode', label: translate.instant('BRANCHES.CODE') },
    { value: 'branchNameAr', label: translate.instant('BRANCHES.NAME_AR') },
    { value: 'legalEntityFk', label: translate.instant('BRANCHES.LEGAL_ENTITY') },
    { value: 'regionFk', label: translate.instant('BRANCHES.REGION') },
    {
      value: 'branchTypeId',
      label: translate.instant('BRANCHES.TYPE'),
      options: branchTypeOptions
    },
    {
      value: 'statusId',
      label: translate.instant('COMMON.STATUS'),
      options: statusOptions.length > 0
        ? statusOptions
        : [
            { value: 'ACTIVE', label: activeLabel },
            { value: 'INACTIVE', label: inactiveLabel }
          ]
    }
  ];

  const operators: SpecOperatorOption[] = [
    { value: 'eq', label: translate.instant('COMMON.EQUALS') },
    { value: 'like', label: translate.instant('COMMON.CONTAINS') }
  ];

  return { fields, operators };
}

export function createBranchColumnDefs(
  translate: TranslateService,
  zone: NgZone,
  callbacks: {
    onEdit: (branch: BranchListItemDto) => void;
    onDeactivate: (branch: BranchListItemDto) => void;
  }
): ColDef[] {
  const activeLabels: ActiveColumnLabels = {
    active: translate.instant('COMMON.ACTIVE') || 'Active',
    inactive: translate.instant('COMMON.INACTIVE') || 'Inactive',
    all: translate.instant('COMMON.ALL') || 'All'
  };

  return [
    { field: 'branchCode', headerName: translate.instant('BRANCHES.CODE'), sortable: true, flex: 1, minWidth: 120 },
    { field: 'branchNameAr', headerName: translate.instant('BRANCHES.NAME_AR'), sortable: true, flex: 1.5, minWidth: 160 },
    { field: 'branchNameEn', headerName: translate.instant('BRANCHES.NAME_EN'), sortable: true, flex: 1.5, minWidth: 160 },
    { field: 'legalEntityDisplay', headerName: translate.instant('BRANCHES.LEGAL_ENTITY'), flex: 1.3, minWidth: 150 },
    { field: 'regionDisplay', headerName: translate.instant('BRANCHES.REGION'), flex: 1.3, minWidth: 150 },
    { field: 'branchTypeDisplay', headerName: translate.instant('BRANCHES.TYPE'), flex: 1, minWidth: 120 },
    {
      field: 'isHeadquarterFl',
      headerName: translate.instant('BRANCHES.IS_HQ'),
      maxWidth: 100,
      floatingFilter: false,
      valueFormatter: (params) => params.value === 1
        ? translate.instant('COMMON.YES')
        : translate.instant('COMMON.NO')
    },
    createActiveColumnDef(activeLabels, {
      headerName: translate.instant('COMMON.STATUS'),
      field: 'statusId',
      maxWidth: 120,
      floatingFilter: false
    }),
    {
      headerName: translate.instant('COMMON.ACTIONS'),
      maxWidth: 140,
      pinned: 'right',
      cellRenderer: BranchActionsCellComponent,
      cellRendererParams: {
        onEdit: (branch: BranchListItemDto) => zone.run(() => callbacks.onEdit(branch)),
        onDeactivate: (branch: BranchListItemDto) => zone.run(() => callbacks.onDeactivate(branch))
      }
    }
  ];
}

export function createBranchGridOptions(translate: TranslateService): {
  gridOptions: GridOptions;
  localeText: Record<string, string>;
} {
  const isRtl = translate.currentLang === 'ar';
  const gridOptions = createErpGridOptions({ enableRtl: isRtl, pageSize: 20 });
  const localeText: Record<string, string> = {
    noRowsToShow: translate.instant('COMMON.NO_DATA'),
    loading: translate.instant('COMMON.LOADING')
  };
  return { gridOptions, localeText };
}

export { ERP_DEFAULT_COL_DEF };
