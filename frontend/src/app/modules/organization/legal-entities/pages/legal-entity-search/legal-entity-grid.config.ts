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

import { LegalEntityListItemDto } from '../../models/legal-entity.model';
import { LegalEntityActionsCellComponent } from '../../components/legal-entity-actions-cell/legal-entity-actions-cell.component';

/**
 * Creates filter field options for the Legal Entity search screen.
 */
export function createLegalEntityFilterOptions(
  translate: TranslateService,
  statusOptions: Array<{ value: string; label: string }> = []
): {
  fields: SpecFieldOption[];
  operators: SpecOperatorOption[];
} {
  const activeLabel = translate.instant('COMMON.ACTIVE') || 'Active';
  const inactiveLabel = translate.instant('COMMON.INACTIVE') || 'Inactive';

  const fields: SpecFieldOption[] = [
    { value: 'legalEntityCode', label: translate.instant('LEGAL_ENTITIES.CODE') },
    { value: 'legalEntityNameAr', label: translate.instant('LEGAL_ENTITIES.NAME_AR') },
    { value: 'countryFk', label: translate.instant('LEGAL_ENTITIES.COUNTRY') },
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

/**
 * Creates column definitions for Legal Entity AG Grid.
 */
export function createLegalEntityColumnDefs(
  translate: TranslateService,
  zone: NgZone,
  callbacks: {
    onEdit: (entity: LegalEntityListItemDto) => void;
    onDeactivate: (entity: LegalEntityListItemDto) => void;
  }
): ColDef[] {
  const activeLabels: ActiveColumnLabels = {
    active: translate.instant('COMMON.ACTIVE') || 'Active',
    inactive: translate.instant('COMMON.INACTIVE') || 'Inactive',
    all: translate.instant('COMMON.ALL') || 'All'
  };

  return [
    { field: 'legalEntityCode', headerName: translate.instant('LEGAL_ENTITIES.CODE'), sortable: true, flex: 1, minWidth: 140 },
    { field: 'legalEntityNameAr', headerName: translate.instant('LEGAL_ENTITIES.NAME_AR'), sortable: true, flex: 1.5, minWidth: 180 },
    { field: 'legalEntityNameEn', headerName: translate.instant('LEGAL_ENTITIES.NAME_EN'), sortable: true, flex: 1.5, minWidth: 180 },
    { field: 'countryDisplay', headerName: translate.instant('LEGAL_ENTITIES.COUNTRY'), flex: 1, minWidth: 130 },
    { field: 'currencyDisplay', headerName: translate.instant('LEGAL_ENTITIES.CURRENCY'), flex: 1, minWidth: 130 },
    createActiveColumnDef(activeLabels, {
      headerName: translate.instant('COMMON.STATUS'),
      field: 'activeFl',
      maxWidth: 120,
      floatingFilter: false
    }),
    {
      headerName: translate.instant('COMMON.ACTIONS'),
      maxWidth: 140,
      pinned: 'right',
      cellRenderer: LegalEntityActionsCellComponent,
      cellRendererParams: {
        onEdit: (entity: LegalEntityListItemDto) => zone.run(() => callbacks.onEdit(entity)),
        onDeactivate: (entity: LegalEntityListItemDto) => zone.run(() => callbacks.onDeactivate(entity))
      }
    }
  ];
}

/**
 * Creates grid options for Legal Entity AG Grid.
 */
export function createLegalEntityGridOptions(translate: TranslateService): {
  gridOptions: GridOptions;
  localeText: Record<string, string>;
} {
  const isRtl = translate.currentLang === 'ar';
  const gridOptions = createErpGridOptions({
    enableRtl: isRtl,
    pageSize: 20
  });

  const localeText: Record<string, string> = {
    noRowsToShow: translate.instant('COMMON.NO_DATA'),
    loading: translate.instant('COMMON.LOADING')
  };

  return { gridOptions, localeText };
}

export { ERP_DEFAULT_COL_DEF };
