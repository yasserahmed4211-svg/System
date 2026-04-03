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

import { PageDto } from '../../models/page.model';
import { PageActionsCellComponent } from '../../components/page-actions-cell/page-actions-cell.component';

/**
 * Creates filter field options for the Pages search screen.
 */
export function createPageFilterOptions(translate: TranslateService): {
  fields: SpecFieldOption[];
  operators: SpecOperatorOption[];
} {
  const activeLabel = translate.instant('COMMON.ACTIVE') || translate.instant('USERS.ACTIVE');
  const inactiveLabel = translate.instant('COMMON.INACTIVE') || translate.instant('USERS.INACTIVE');

  const fields: SpecFieldOption[] = [
    { value: 'id', label: translate.instant('USERS.ID') },
    { value: 'pageCode', label: translate.instant('PAGES.PAGE_CODE') },
    { value: 'nameEn', label: translate.instant('PAGES.PAGE_NAME_EN') },
    { value: 'nameAr', label: translate.instant('PAGES.PAGE_NAME_AR') },
    { value: 'route', label: translate.instant('PAGES.ROUTE') },
    { value: 'module', label: translate.instant('PAGES.MODULE') },
    { value: 'displayOrder', label: translate.instant('PAGES.DISPLAY_ORDER') },
    {
      value: 'active',
      label: translate.instant('PAGES.IS_ACTIVE'),
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
 * Creates column definitions for Pages AG Grid.
 */
export function createPageColumnDefs(
  translate: TranslateService,
  zone: NgZone,
  callbacks: {
    onEdit: (page: PageDto) => void;
    onDeactivate: (page: PageDto) => void;
  }
): ColDef[] {
  const activeLabels: ActiveColumnLabels = {
    active: translate.instant('COMMON.ACTIVE') || translate.instant('USERS.ACTIVE'),
    inactive: translate.instant('COMMON.INACTIVE') || translate.instant('USERS.INACTIVE'),
    all: translate.instant('COMMON.ALL')
  };

  return [
    { field: 'pageCode', headerName: translate.instant('PAGES.PAGE_CODE'), sortable: true, flex: 1, minWidth: 150 },
    { field: 'nameEn', headerName: translate.instant('PAGES.PAGE_NAME_EN'), sortable: true, flex: 1.5, minWidth: 200 },
    { field: 'nameAr', headerName: translate.instant('PAGES.PAGE_NAME_AR'), sortable: true, flex: 1.5, minWidth: 200 },
    { field: 'route', headerName: translate.instant('PAGES.ROUTE'), sortable: true, flex: 1.2, minWidth: 180 },
    { field: 'module', headerName: translate.instant('PAGES.MODULE'), sortable: true, maxWidth: 150 },
    { field: 'icon', headerName: translate.instant('PAGES.ICON'), sortable: true, maxWidth: 160 },
    { field: 'displayOrder', headerName: translate.instant('PAGES.DISPLAY_ORDER'), sortable: true, maxWidth: 120 },
    createActiveColumnDef(activeLabels, {
      headerName: translate.instant('USERS.STATUS'),
      maxWidth: 120,
      floatingFilter: false
    }),
    {
      headerName: translate.instant('COMMON.ACTIONS'),
      maxWidth: 150,
      pinned: 'right',
      cellRenderer: PageActionsCellComponent,
      cellRendererParams: {
        onEdit: (page: PageDto) => zone.run(() => callbacks.onEdit(page)),
        onDeactivate: (page: PageDto) => zone.run(() => callbacks.onDeactivate(page))
      }
    }
  ];
}

/**
 * Creates grid options for Pages AG Grid.
 */
export function createPageGridOptions(translate: TranslateService): {
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
    true: translate.instant('COMMON.ACTIVE') || translate.instant('USERS.ACTIVE'),
    false: translate.instant('COMMON.INACTIVE') || translate.instant('USERS.INACTIVE')
  };

  return { gridOptions, localeText };
}

/** Re-export default column definition */
export { ERP_DEFAULT_COL_DEF };
