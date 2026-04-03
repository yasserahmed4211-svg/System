/**
 * Date Formatting Utilities
 *
 * Single source of truth for all date/datetime display formatting across the ERP system.
 * All AG Grid `valueFormatter` functions and template pipes should use these helpers.
 *
 * @example
 * // AG Grid column:
 * { field: 'createdAt', valueFormatter: (p) => formatDateValue(p.value, currentLang) }
 *
 * // AG Grid column with time:
 * { field: 'updatedAt', valueFormatter: (p) => formatDateTimeValue(p.value, currentLang) }
 */

/**
 * Resolves the locale string for date formatting based on the current language.
 *
 * @param lang - The language code (e.g., 'ar', 'en')
 * @returns BCP 47 locale string suitable for `Intl.DateTimeFormat`
 */
export function resolveLocale(lang: string): string {
  return (lang || 'en') === 'ar' ? 'ar-SA' : 'en-GB';
}

/**
 * Formats a date value into a human-readable date string (no time component).
 *
 * @param value - ISO date string, Date object, or null/undefined
 * @param lang  - Current language code ('ar' | 'en')
 * @returns Formatted date string (e.g., "02 Apr 2026" / "٠٢ أبر ٢٠٢٦") or '—' for null/invalid
 *
 * @example
 * formatDateValue('2026-04-02T10:30:00Z', 'en') // "02 Apr 2026"
 * formatDateValue('2026-04-02T10:30:00Z', 'ar') // "٠٢ أبر ٢٠٢٦"
 * formatDateValue(null, 'en')                   // "—"
 */
export function formatDateValue(value: unknown, lang: string): string {
  if (!value) return '—';
  const date = new Date(value as string);
  if (isNaN(date.getTime())) return String(value);
  return date.toLocaleDateString(resolveLocale(lang), {
    day: '2-digit', month: 'short', year: 'numeric'
  });
}

/**
 * Formats a datetime value into a human-readable date + time string.
 *
 * @param value - ISO datetime string, Date object, or null/undefined
 * @param lang  - Current language code ('ar' | 'en')
 * @returns Formatted datetime string (e.g., "02 Apr 2026, 10:30 AM") or '—' for null/invalid
 *
 * @example
 * formatDateTimeValue('2026-04-02T10:30:00Z', 'en') // "02 Apr 2026, 10:30 AM"
 * formatDateTimeValue(null, 'en')                   // "—"
 */
export function formatDateTimeValue(value: unknown, lang: string): string {
  if (!value) return '—';
  const date = new Date(value as string);
  if (isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat(resolveLocale(lang), {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit', hour12: true
  }).format(date);
}
