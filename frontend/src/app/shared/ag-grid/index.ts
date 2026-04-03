/**
 * ERP AG Grid Shared Module Exports
 *
 * Central export point for all AG Grid related utilities,
 * components, and helpers used across the ERP system.
 */

// Existing AG Grid configuration
export * from './erp-ag-grid.config';

// Module registration (call once in app initialization)
export { registerErpAgGridModules, ERP_AG_GRID_MODULES } from './erp-ag-grid-modules';

// Active status filter utilities
export {
  // Types
  type ActiveStatus,
  type ActiveFilterValue,
  type ActiveColumnLabels,
  type ActiveSearchFilter,

  // Conversion functions
  activeFilterToApi,
  apiToActiveFilter,
  normalizeActiveValue,

  // Cell renderer and formatter
  activeCellRenderer,
  activeValueFormatter,
  createActiveBadgeHtml,

  // Column definition helper
  createActiveColumnDef,

  // Filter utilities
  createActiveFilterOptions,
  createActiveSearchFilter,
  getActiveFilterFromSearchFilters,

  // Constants
  DEFAULT_ACTIVE_LABELS,
  DEFAULT_ACTIVE_FILTER
} from './active-status-filter.utils';

// Active filter component
export { ActiveFilterComponent } from './active-filter.component';
