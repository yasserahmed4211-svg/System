/**
 * Standard API response envelope matching the backend's ApiResponse<T>.
 *
 * Every REST response from the ERP backend follows this structure:
 * {
 *   "success": true|false,
 *   "message": "...",
 *   "data": { ... },
 *   "error": { "code": "...", "details": "...", ... },
 *   "timestamp": "2025-06-27T12:00:00Z"
 * }
 */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  error?: ApiErrorDto | null;
  timestamp?: string;
}

/**
 * Error details included in ApiResponse when success=false.
 * Mirrors the backend's ApiError class.
 */
export interface ApiErrorDto {
  code?: string;
  details?: string;
  fieldErrors?: FieldErrorItemDto[];
  timestamp?: string;
  path?: string;
}

/**
 * Field-level validation error.
 * Mirrors the backend's FieldErrorItem class.
 */
export interface FieldErrorItemDto {
  field?: string;
  message?: string;
}
