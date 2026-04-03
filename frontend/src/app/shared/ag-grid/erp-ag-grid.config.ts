import { ColDef, GridOptions } from 'ag-grid-community';

export const ERP_GRID_PAGE_SIZE_OPTIONS = [10, 20, 50] as const;
export const ERP_GRID_DEFAULT_PAGE_SIZE = 20;

export const ERP_DEFAULT_COL_DEF: ColDef = {
  flex: 1,
  minWidth: 150,
  filter: true,
  sortable: true,
  resizable: true,
  floatingFilter: true,
  filterParams: {
    debounceMs: 500,
    suppressAndOrCondition: true
  }
};

export interface ErpGridOptionsArgs {
  enableRtl?: boolean;
  localeText?: Record<string, string>;
  pageSize?: number;
}

/**
 * Standardized ag-Grid options for ERP list screens.
 * Individual screens can override/extend as needed.
 */
export function createErpGridOptions(args?: ErpGridOptionsArgs): GridOptions {
  return {
    enableRtl: args?.enableRtl ?? false,
    localeText: args?.localeText,

    pagination: true,
    paginationPageSize: args?.pageSize ?? ERP_GRID_DEFAULT_PAGE_SIZE,
    // Supported in recent ag-Grid versions; harmless if ignored.
    paginationPageSizeSelector: [...ERP_GRID_PAGE_SIZE_OPTIONS],

    suppressPaginationPanel: false,
    animateRows: true
  };
}
