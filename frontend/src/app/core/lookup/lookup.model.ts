/**
 * Lookup Framework — Domain Models
 *
 * Strictly-typed interfaces for the unified lookup system.
 * Used by both the core data service and shared UI components.
 *
 * @architecture Core layer — no UI dependencies
 */

// ── Lookup Item ────────────────────────────────────────────────────

/**
 * Represents a single lookup result item.
 * `id` is persisted; `display` is the formatted label shown to the user.
 * Additional dynamic properties are supported for advanced column rendering.
 */
export interface LookupItem {
  /** Unique identifier — the only value saved to the form */
  id: number;
  /** Formatted display label (e.g. "ACC-001 - Cash") */
  display: string;
  /** Dynamic properties for advanced dialog columns */
  [key: string]: unknown;
}

// ── Lookup Column ──────────────────────────────────────────────────

/**
 * Defines a column in the advanced lookup dialog table.
 * Columns are fully dynamic — no hardcoded entity assumptions.
 */
export interface LookupColumn {
  /** Property key on LookupItem to render */
  key: string;
  /** Column header label (translation key) */
  label: string;
  /** Optional fixed width (CSS value, e.g. '120px') */
  width?: string;
}

// ── Lookup Config ──────────────────────────────────────────────────

/**
 * Configuration for a lookup field instance.
 * Passed as input to the shared LookupFieldComponent.
 */
export interface LookupConfig {
  /** REST endpoint path (appended to authApiUrl), e.g. '/api/gl/accounts/lookup' */
  endpoint: string;
  /** Selection mode: 'quick' for autocomplete, 'advanced' for dialog */
  mode: 'quick' | 'advanced';
  /** Minimum characters before triggering search (default: 2) */
  minChars?: number;
  /** Maximum results for quick mode / page size for advanced mode (default: 10) */
  pageSize?: number;
  /** Column definitions for advanced dialog — required when mode='advanced' */
  columns?: LookupColumn[];
  /** Placeholder translation key */
  placeholderKey?: string;
  /** Dialog title translation key (used in advanced mode) */
  dialogTitleKey?: string;
  /** Extra query parameters appended to every lookup request */
  extraParams?: Record<string, string>;
}

// ── Lookup Search Request ──────────────────────────────────────────

/**
 * Parameters sent to the lookup endpoint for quick search.
 */
export interface LookupQuickSearchParams {
  search: string;
  limit: number;
}

/**
 * Parameters sent to the lookup endpoint for advanced paginated search.
 */
export interface LookupAdvancedSearchParams {
  search: string;
  page: number;
  size: number;
  sort?: string;
  direction?: 'ASC' | 'DESC';
}

// ── Lookup Paged Response ──────────────────────────────────────────

/**
 * Server response for advanced paginated lookup.
 * Mirrors the standard PagedResponse<T> contract.
 */
export interface LookupPagedResponse {
  content: LookupItem[];
  totalElements: number;
  totalPages: number;
}
