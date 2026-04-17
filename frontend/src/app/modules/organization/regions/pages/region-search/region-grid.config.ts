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

import { RegionListItemDto } from '../../models/region.model';
import { RegionActionsCellComponent } from '../../components/region-actions-cell/region-actions-cell.component';

export function createRegionFilterOptions(
  translate: TranslateService,
  statusOptions: Array<{ value: string; label: string }> = []
): {
  fields: SpecFieldOption[];
  operators: SpecOperatorOption[];
} {
  const activeLabel = translate.instant('COMMON.ACTIVE') || 'Active';
  const inactiveLabel = translate.instant('COMMON.INACTIVE') || 'Inactive';

  const fields: SpecFieldOption[] = [
    { value: 'regionCode', label: translate.instant('REGIONS.CODE') },
    { value: 'regionNameAr', label: translate.instant('REGIONS.NAME_AR') },
    { value: 'legalEntityFk', label: translate.instant('REGIONS.LEGAL_ENTITY') },
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

export function createRegionColumnDefs(
  translate: TranslateService,
  zone: NgZone,
  callbacks: {
    onEdit: (region: RegionListItemDto) => void;
    onDeactivate: (region: RegionListItemDto) => void;
  }
): ColDef[] {
  const activeLabels: ActiveColumnLabels = {
    active: translate.instant('COMMON.ACTIVE') || 'Active',
    inactive: translate.instant('COMMON.INACTIVE') || 'Inactive',
    all: translate.instant('COMMON.ALL') || 'All'
  };

  return [
    { field: 'regionCode', headerName: translate.instant('REGIONS.CODE'), sortable: true, flex: 1, minWidth: 130 },
    { field: 'regionNameAr', headerName: translate.instant('REGIONS.NAME_AR'), sortable: true, flex: 1.5, minWidth: 170 },
    { field: 'regionNameEn', headerName: translate.instant('REGIONS.NAME_EN'), sortable: true, flex: 1.5, minWidth: 170 },
    { field: 'legalEntityDisplay', headerName: translate.instant('REGIONS.LEGAL_ENTITY'), flex: 1.5, minWidth: 170 },
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
      cellRenderer: RegionActionsCellComponent,
      cellRendererParams: {
        onEdit: (region: RegionListItemDto) => zone.run(() => callbacks.onEdit(region)),
        onDeactivate: (region: RegionListItemDto) => zone.run(() => callbacks.onDeactivate(region))
      }
    }
  ];
}

export function createRegionGridOptions(translate: TranslateService): {
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
