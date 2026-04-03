/**
 * ERP Active Status Filter Utilities
 *
 * Provides consistent handling of Active/Inactive filtering across AG Grid components.
 *
 * RULES:
 * 1. Frontend ALWAYS uses: isActive: boolean | null
 * 2. API requests send: isActive: true | false | null
 * 3. Database values (0/1, Y/N) NEVER appear in frontend
 * 4. null means "ALL" (no filter applied)
 *
 * @example
 * // In column definition
 * {
 *   field: 'active',
 *   ...createActiveColumnDef(translate)
 * }
 *
 * @example
 * // Converting filter for API
 * const apiValue = convertActiveFilterToApi(gridFilterValue); // true | false | null
 */

import { ColDef, ValueFormatterParams, ICellRendererParams } from 'ag-grid-community';

/**
 * Active status type - always boolean or null (for "ALL")
 * NEVER use string or number representations
 */
export type ActiveStatus = boolean | null;

/**
 * Active filter value from UI controls
 */
export type ActiveFilterValue = 'active' | 'inactive' | 'all';

/**
 * Maps UI filter value to API boolean
 *
 * @param filterValue - UI filter selection
 * @returns boolean | null for API request
 */
export function activeFilterToApi(filterValue: ActiveFilterValue): ActiveStatus {
  switch (filterValue) {
    case 'active':
      return true;
    case 'inactive':
      return false;
    case 'all':
    default:
      return null;
  }
}

/**
 * Maps API/model boolean to UI filter value
 *
 * @param apiValue - boolean | null from API/model
 * @returns UI filter value string
 */
export function apiToActiveFilter(apiValue: ActiveStatus): ActiveFilterValue {
  if (apiValue === true) return 'active';
  if (apiValue === false) return 'inactive';
  return 'all';
}

/**
 * Converts any active-related value to proper boolean
 * Handles edge cases from various sources
 *
 * @param value - Any value that might represent active status
 * @returns boolean | null
 */
export function normalizeActiveValue(value: unknown): ActiveStatus {
  // Already boolean
  if (typeof value === 'boolean') {
    return value;
  }

  // Null/undefined = ALL
  if (value === null || value === undefined) {
    return null;
  }

  // String handling (from form inputs, query params)
  if (typeof value === 'string') {
    const lower = value.toLowerCase().trim();
    if (lower === 'true' || lower === 'active' || lower === '1' || lower === 'yes') {
      return true;
    }
    if (lower === 'false' || lower === 'inactive' || lower === '0' || lower === 'no') {
      return false;
    }
    // 'all', empty string, or unknown = null
    return null;
  }

  // Number handling (SHOULD NOT happen in frontend, but defensive)
  if (typeof value === 'number') {
    if (value === 1) return true;
    if (value === 0) return false;
    return null;
  }

  return null;
}

/**
 * Interface for active column labels
 */
export interface ActiveColumnLabels {
  active: string;
  inactive: string;
  all?: string;
}

/**
 * Default labels (English) - should be replaced with translations
 */
export const DEFAULT_ACTIVE_LABELS: ActiveColumnLabels = {
  active: 'Active',
  inactive: 'Inactive',
  all: 'All'
};

/**
 * Creates cell renderer HTML for active status badge
 *
 * @param isActive - boolean value
 * @param labels - translated labels
 * @returns HTML string for badge
 */
export function createActiveBadgeHtml(isActive: boolean, labels: ActiveColumnLabels): string {
  return isActive
    ? `<span class="badge bg-success">${labels.active}</span>`
    : `<span class="badge bg-secondary">${labels.inactive}</span>`;
}

/**
 * Cell renderer function for active status column
 */
export function activeCellRenderer(
  params: ICellRendererParams,
  labels: ActiveColumnLabels
): string {
  const value = normalizeActiveValue(params.value);
  if (value === null) {
    return '<span class="badge bg-light text-dark">—</span>';
  }
  return createActiveBadgeHtml(value, labels);
}

/**
 * Value formatter for active status (used in filters)
 */
export function activeValueFormatter(
  params: ValueFormatterParams,
  labels: ActiveColumnLabels
): string {
  const value = normalizeActiveValue(params.value);
  if (value === true) return labels.active;
  if (value === false) return labels.inactive;
  return labels.all ?? 'All';
}

/**
 * Creates standard column definition for active/status field
 *
 * IMPORTANT: Uses agTextColumnFilter (Community) instead of agSetColumnFilter (Enterprise)
 * because SetFilterModule is not registered.
 *
 * @param labels - Translated labels for Active/Inactive
 * @param options - Additional column options
 * @returns ColDef configuration
 */
export function createActiveColumnDef(
  labels: ActiveColumnLabels,
  options: Partial<ColDef> = {}
): ColDef {
  return {
    field: 'active',
    headerName: labels.active, // Will be overridden by caller usually
    maxWidth: 140,
    sortable: true,
    // Use text filter (Community module) - NOT agSetColumnFilter (Enterprise)
    filter: 'agTextColumnFilter',
    floatingFilter: true,
    filterParams: {
      filterOptions: ['equals'],
      suppressAndOrCondition: true,
      debounceMs: 300,
      // Text filter will receive 'true' or 'false' as string
      textMatcher: (params: { filterText: string; value: any }) => {
        const filterBool = normalizeActiveValue(params.filterText);
        const valueBool = normalizeActiveValue(params.value);
        if (filterBool === null) return true; // No filter = show all
        return filterBool === valueBool;
      }
    },
    cellRenderer: (params: ICellRendererParams) => activeCellRenderer(params, labels),
    valueFormatter: (params: ValueFormatterParams) => activeValueFormatter(params, labels),
    ...options
  };
}

/**
 * Creates filter options for external filter controls (button group, dropdown)
 *
 * @param labels - Translated labels
 * @returns Array of filter options
 */
export function createActiveFilterOptions(labels: ActiveColumnLabels): Array<{
  value: ActiveFilterValue;
  label: string;
  apiValue: ActiveStatus;
}> {
  return [
    { value: 'active', label: labels.active, apiValue: true },
    { value: 'inactive', label: labels.inactive, apiValue: false },
    { value: 'all', label: labels.all ?? 'All', apiValue: null }
  ];
}

/**
 * Search filter interface for active field
 */
export interface ActiveSearchFilter {
  field: 'active' | 'isActive' | 'enabled';
  op: 'EQ';
  value: boolean;
}

/**
 * Creates search filter for active status
 * Returns null if filterValue is 'all' (no filter needed)
 *
 * @param filterValue - UI filter selection
 * @param fieldName - Backend field name (default: 'active')
 * @returns SearchFilter or null
 */
export function createActiveSearchFilter(
  filterValue: ActiveFilterValue,
  fieldName: string = 'active'
): ActiveSearchFilter | null {
  const apiValue = activeFilterToApi(filterValue);
  if (apiValue === null) {
    return null; // No filter for "all"
  }
  return {
    field: fieldName as ActiveSearchFilter['field'],
    op: 'EQ',
    value: apiValue
  };
}

/**
 * Default active filter state
 * By default, show only active records (per ERP standard)
 */
export const DEFAULT_ACTIVE_FILTER: ActiveFilterValue = 'active';

/**
 * Extracts active filter value from search filters array
 *
 * @param filters - Array of search filters
 * @param fieldName - Field name to look for
 * @returns ActiveFilterValue
 */
export function getActiveFilterFromSearchFilters(
  filters: Array<{ field: string; op: string; value?: unknown }>,
  fieldName: string = 'active'
): ActiveFilterValue {
  const filter = filters.find(
    (f) => f.field === fieldName && f.op === 'EQ'
  );

  if (!filter) {
    return 'all';
  }

  const value = normalizeActiveValue(filter.value);
  return apiToActiveFilter(value);
}
