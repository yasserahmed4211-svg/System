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

import { MasterLookupDto } from '../../models/master-lookup.model';
import { MasterLookupActionsCellComponent } from '../../components/master-lookup-actions-cell/master-lookup-actions-cell.component';

/**
 * Creates filter field options for the Master Lookup search screen.
 */
export function createMasterLookupFilterOptions(translate: TranslateService): {
  fields: SpecFieldOption[];
  operators: SpecOperatorOption[];
} {
  const activeLabel = translate.instant('COMMON.ACTIVE') || 'Active';
  const inactiveLabel = translate.instant('COMMON.INACTIVE') || 'Inactive';

  const fields: SpecFieldOption[] = [
    { value: 'lookupKey', label: translate.instant('MASTER_LOOKUPS.LOOKUP_KEY') },
    { value: 'lookupName', label: translate.instant('MASTER_LOOKUPS.LOOKUP_NAME') },
    {
      value: 'isActive',
      label: translate.instant('MASTER_LOOKUPS.STATUS'),
      options: [
        { value: true, label: activeLabel },
        { value: false, label: inactiveLabel }
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
 * Creates column definitions for Master Lookup AG Grid.
 */
export function createMasterLookupColumnDefs(
  translate: TranslateService,
  zone: NgZone,
  callbacks: {
    onEdit: (lookup: MasterLookupDto) => void;
    onToggleActive: (lookup: MasterLookupDto) => void;
    onDelete: (lookup: MasterLookupDto) => void;
  }
): ColDef[] {
  const activeLabels: ActiveColumnLabels = {
    active: translate.instant('COMMON.ACTIVE') || 'Active',
    inactive: translate.instant('COMMON.INACTIVE') || 'Inactive',
    all: translate.instant('COMMON.ALL') || 'All'
  };

  return [
    { field: 'lookupKey', headerName: translate.instant('MASTER_LOOKUPS.LOOKUP_KEY'), sortable: true, flex: 1, minWidth: 150 },
    { field: 'lookupName', headerName: translate.instant('MASTER_LOOKUPS.LOOKUP_NAME'), sortable: true, flex: 1.5, minWidth: 200 },
    { field: 'lookupNameEn', headerName: translate.instant('MASTER_LOOKUPS.LOOKUP_NAME_EN'), sortable: true, flex: 1.5, minWidth: 200 },
    { field: 'description', headerName: translate.instant('MASTER_LOOKUPS.DESCRIPTION'), sortable: true, flex: 2, minWidth: 250 },
    createActiveColumnDef(activeLabels, {
      headerName: translate.instant('MASTER_LOOKUPS.STATUS'),
      field: 'isActive',
      maxWidth: 120,
      floatingFilter: false
    }),
    {
      headerName: translate.instant('COMMON.ACTIONS'),
      maxWidth: 170,
      pinned: 'right',
      cellRenderer: MasterLookupActionsCellComponent,
      cellRendererParams: {
        onEdit: (lookup: MasterLookupDto) => zone.run(() => callbacks.onEdit(lookup)),
        onToggleActive: (lookup: MasterLookupDto) => zone.run(() => callbacks.onToggleActive(lookup)),
        onDelete: (lookup: MasterLookupDto) => zone.run(() => callbacks.onDelete(lookup))
      }
    }
  ];
}

/**
 * Creates grid options for Master Lookup AG Grid.
 */
export function createMasterLookupGridOptions(translate: TranslateService): {
  gridOptions: GridOptions;
  localeText: Record<string, string>;
} {
  const isRtl = translate.currentLang === 'ar';
  const gridOptions = createErpGridOptions({
    enableRtl: isRtl,
    pageSize: 20
  });

  const localeText: Record<string, string> = {
    empty: translate.instant('COMMON.SELECT'),
    chooseOne: translate.instant('COMMON.SELECT'),
    true: translate.instant('COMMON.ACTIVE'),
    false: translate.instant('COMMON.INACTIVE')
  };

  return { gridOptions, localeText };
}

/** Re-export default column definition */
export { ERP_DEFAULT_COL_DEF };
