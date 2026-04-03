import { ICellRendererParams } from 'ag-grid-community';
import { LookupSelectOption } from 'src/app/core/models/lookup-detail.model';

/**
 * Status Badge Utilities
 *
 * Generic helpers for rendering status values as colored Bootstrap badges
 * in AG Grid cells. Feature modules provide their own CSS class maps while
 * reusing the shared cell renderer factory.
 *
 * @example
 * // In a grid config file, define a feature-specific class map:
 * const JOURNAL_STATUS_BADGE_MAP: Record<string, string> = {
 *   DRAFT:     'bg-secondary',
 *   APPROVED:  'bg-info',
 *   POSTED:    'bg-success',
 *   REVERSED:  'bg-warning text-dark',
 *   CANCELLED: 'bg-danger',
 * };
 *
 * // Then create a cell renderer with the map + lookup options:
 * { field: 'statusIdFk', cellRenderer: createStatusBadgeCellRenderer(JOURNAL_STATUS_BADGE_MAP, statusOptions) }
 */

/**
 * Generates the HTML string for a Bootstrap badge given a status code,
 * a CSS class map, and an optional resolved label.
 *
 * @param code     - Raw status code (e.g., 'DRAFT')
 * @param classMap - Map of status code → Bootstrap badge CSS class
 * @param label    - Human-readable label to display inside the badge
 * @returns HTML string for the badge element
 */
export function createStatusBadgeHtml(
  code: string,
  classMap: Record<string, string>,
  label: string
): string {
  const cls = classMap[code] ?? 'bg-secondary';
  return `<span class="badge ${cls}" style="font-size:0.8em;padding:4px 8px">${label}</span>`;
}

/**
 * Creates an AG Grid `cellRenderer` function that renders a colored badge.
 * The label is resolved from the provided `LookupSelectOption` array;
 * falls back to the raw code when no matching option is found.
 *
 * @param classMap - Map of status code → Bootstrap badge CSS classes
 * @param options  - Lookup options array `[{ value, label }]` for label resolution
 * @returns AG Grid `cellRenderer` function
 *
 * @example
 * {
 *   field: 'statusIdFk',
 *   headerName: translate.instant('GL.JOURNAL_STATUS'),
 *   cellRenderer: createStatusBadgeCellRenderer(JOURNAL_STATUS_BADGE_MAP, statusOptions)
 * }
 */
export function createStatusBadgeCellRenderer(
  classMap: Record<string, string>,
  options: LookupSelectOption[]
): (params: ICellRendererParams) => string {
  return (params: ICellRendererParams): string => {
    const code: string = params.value;
    if (!code) return '<span class="text-muted">—</span>';
    const label = options.find(o => o.value === code)?.label ?? code;
    return createStatusBadgeHtml(code, classMap, label);
  };
}
