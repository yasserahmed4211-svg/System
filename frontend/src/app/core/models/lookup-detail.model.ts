/**
 * LookupDetail — Dynamic lookup detail fetched from the backend.
 *
 * Represents a single option from the Master Lookup system.
 * Used across the entire application to replace static enum/constant arrays.
 *
 * API: GET /api/masterdata/master-lookups/details/options/{lookupKey}
 */
export interface LookupDetail {
  /** Unique database PK */
  idPk: number;
  /** Business code — the value stored in forms */
  code: string;
  /** Arabic display name (primary for Arabic UI) */
  nameAr: string;
  /** English display name (fallback) */
  nameEn?: string;
  /** Optional extra/metadata value */
  extraValue?: string;
  /** Sort order for display */
  sortOrder?: number;
}

/**
 * Mapped option ready for dropdown binding.
 * `value` = code, `label` = localized display name.
 */
export interface LookupSelectOption {
  value: string;
  label: string;
}
