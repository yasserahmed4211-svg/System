/**
 * Number Formatting Utilities
 *
 * Single source of truth for all numeric display formatting across the ERP system.
 * Covers amounts, quantities, and percentages in AG Grid and templates.
 *
 * @example
 * // AG Grid amount column:
 * { field: 'totalAmount', valueFormatter: (p) => formatAmount(p.value) }
 */

/**
 * Formats a numeric amount using locale-aware thousand separators.
 * Returns '—' for null/undefined values.
 *
 * @param value - Numeric value, string representation of a number, null, or undefined
 * @returns Formatted number string (e.g., "1,500.50") or '—'
 *
 * @example
 * formatAmount(1500.5)   // "1,500.5"
 * formatAmount(0)        // "0"
 * formatAmount(null)     // "—"
 * formatAmount(undefined)// "—"
 */
export function formatAmount(value: unknown): string {
  if (value == null) return '—';
  const num = Number(value);
  if (isNaN(num)) return String(value);
  return num.toLocaleString();
}
