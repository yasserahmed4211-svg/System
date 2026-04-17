import { Injectable } from '@angular/core';

/**
 * Error mapping result with translation key and optional parameters.
 */
export interface ErpErrorMapping {
  /** Translation key for the error message */
  translationKey: string;
  /** Optional parameters for translation interpolation */
  params?: Record<string, unknown>;
}

/**
 * ErpErrorMapperService
 * 
 * Maps backend error codes to translation keys.
 * Contains no UI rendering logic - only provides translation key mapping.
 * 
 * @requirement FE-REQ-SHARED-001
 * @task TASK-FE-SHARED-001
 */
@Injectable({
  providedIn: 'root'
})
export class ErpErrorMapperService {
  /**
   * Standard error code to translation key mappings.
   * Keys are backend error codes, values are translation keys.
   */
  private readonly errorMappings: Record<string, string> = {
    // Common HTTP errors
    'HTTP_400': 'ERRORS.BAD_REQUEST',
    'HTTP_401': 'ERRORS.UNAUTHORIZED',
    'HTTP_403': 'ERRORS.FORBIDDEN',
    'HTTP_404': 'ERRORS.NOT_FOUND',
    'HTTP_409': 'ERRORS.CONFLICT',
    'HTTP_422': 'ERRORS.VALIDATION_FAILED',
    'HTTP_500': 'ERRORS.INTERNAL_SERVER_ERROR',
    'HTTP_502': 'ERRORS.BAD_GATEWAY',
    'HTTP_503': 'ERRORS.SERVICE_UNAVAILABLE',
    'HTTP_504': 'ERRORS.GATEWAY_TIMEOUT',
    
    // Network errors
    'NETWORK_ERROR': 'ERRORS.NETWORK_ERROR',
    'TIMEOUT_ERROR': 'ERRORS.TIMEOUT',
    'CONNECTION_REFUSED': 'ERRORS.CONNECTION_REFUSED',
    
    // Validation errors
    'VALIDATION_ERROR': 'ERRORS.VALIDATION_FAILED',
    'REQUIRED_FIELD': 'ERRORS.REQUIRED_FIELD',
    'INVALID_FORMAT': 'ERRORS.INVALID_FORMAT',
    'DUPLICATE_ENTRY': 'ERRORS.DUPLICATE_ENTRY',
    'INVALID_LENGTH': 'ERRORS.INVALID_LENGTH',
    
    // Authentication/Authorization
    'INVALID_CREDENTIALS': 'ERRORS.INVALID_CREDENTIALS',
    'SESSION_EXPIRED': 'ERRORS.SESSION_EXPIRED',
    'TOKEN_EXPIRED': 'ERRORS.TOKEN_EXPIRED',
    'ACCESS_DENIED': 'ERRORS.ACCESS_DENIED',
    'INSUFFICIENT_PERMISSIONS': 'ERRORS.INSUFFICIENT_PERMISSIONS',

    // Security module (backend) codes
    'USER_NOT_FOUND': 'ERRORS.USER_NOT_FOUND',
    'USER_ENTITY_NOT_FOUND': 'ERRORS.USER_NOT_FOUND',
    'USERNAME_ALREADY_EXISTS': 'ERRORS.USERNAME_EXISTS',
    'ROLE_NOT_FOUND': 'ERRORS.ROLE_NOT_FOUND',
    'DUPLICATE_ROLE_CODE': 'ERRORS.DUPLICATE_ROLE_CODE',
    'DUPLICATE_ROLE_NAME': 'ERRORS.DUPLICATE_ROLE_NAME',
    'ROLE_IN_USE': 'ERRORS.ROLE_IN_USE',
    'PAGE_NOT_FOUND': 'ERRORS.PAGE_NOT_FOUND',
    'PAGE_NOT_FOUND_BY_CODE': 'ERRORS.PAGE_NOT_FOUND',
    'DUPLICATE_PAGE_CODE': 'ERRORS.DUPLICATE_PAGE_CODE',
    'INVALID_PAGE_CODE_FORMAT': 'ERRORS.INVALID_PAGE_CODE_FORMAT',
    'INVALID_PAGE_CODE_LENGTH': 'ERRORS.INVALID_PAGE_CODE_LENGTH',
    'INVALID_ROUTE_FORMAT': 'ERRORS.INVALID_ROUTE_FORMAT',
    'PARENT_PAGE_NOT_FOUND': 'ERRORS.PARENT_PAGE_NOT_FOUND',
    
    // Business errors
    'ENTITY_NOT_FOUND': 'ERRORS.ENTITY_NOT_FOUND',
    'ENTITY_IN_USE': 'ERRORS.ENTITY_IN_USE',
    'OPERATION_FAILED': 'ERRORS.OPERATION_FAILED',
    'STATE_CONFLICT': 'ERRORS.STATE_CONFLICT',
    'DUPLICATE_ROUTE': 'ERRORS.DUPLICATE_ROUTE',
    'USERNAME_EXISTS': 'ERRORS.USERNAME_EXISTS',

    // GL Module - Chart of Accounts errors
    'GL_ACCOUNT_NOT_FOUND': 'ERRORS.GL_ACCOUNT_NOT_FOUND',
    'GL_DUPLICATE_ACCOUNT_CODE': 'ERRORS.GL_DUPLICATE_ACCOUNT_CODE',
    'GL_ACCOUNT_IN_USE': 'ERRORS.GL_ACCOUNT_IN_USE',
    'GL_ACCOUNT_HAS_CHILDREN': 'ERRORS.GL_ACCOUNT_HAS_CHILDREN',
    'GL_ACCOUNT_IN_ACTIVE_RULE': 'ERRORS.GL_ACCOUNT_IN_ACTIVE_RULE',
    'GL_ACCOUNT_HAS_BALANCE': 'ERRORS.GL_ACCOUNT_HAS_BALANCE',
    'GL_ACCOUNT_ORG_LOCKED': 'ERRORS.GL_ACCOUNT_ORG_LOCKED',
    'GL_ACCOUNT_NOT_LEAF': 'ERRORS.GL_ACCOUNT_NOT_LEAF',
    'GL_INACTIVE_ACCOUNT': 'ERRORS.GL_INACTIVE_ACCOUNT',
    'GL_ACCOUNT_CIRCULAR_REF': 'ERRORS.GL_ACCOUNT_CIRCULAR_REF',
    'GL_ACCOUNT_TYPE_MISMATCH': 'ERRORS.GL_ACCOUNT_TYPE_MISMATCH',
    'GL_ACCOUNT_PARENT_NOT_FOUND': 'ERRORS.GL_ACCOUNT_PARENT_NOT_FOUND',
    'GL_ACCOUNT_PARENT_INACTIVE': 'ERRORS.GL_ACCOUNT_PARENT_INACTIVE',
    'GL_ACCOUNT_SELF_REFERENCE': 'ERRORS.GL_ACCOUNT_SELF_REFERENCE',
    'GL_ACCOUNT_DESCENDANT_AS_PARENT': 'ERRORS.GL_ACCOUNT_DESCENDANT_AS_PARENT',
    'GL_ACCOUNT_TYPE_CHANGE_WITH_CHILDREN': 'ERRORS.GL_ACCOUNT_TYPE_CHANGE_WITH_CHILDREN',
    'GL_ACCOUNT_NO_GENERATION_FAILED': 'ERRORS.GL_ACCOUNT_NO_GENERATION_FAILED',
    'GL_INVALID_ROOT_ACCOUNT_TYPE': 'ERRORS.GL_INVALID_ROOT_ACCOUNT_TYPE',
    'GL_ACCOUNT_NO_MANUAL_OVERRIDE': 'ERRORS.GL_ACCOUNT_NO_MANUAL_OVERRIDE',

    // GL Module - Accounting Rules errors
    'GL_RULE_NOT_FOUND': 'ERRORS.GL_RULE_NOT_FOUND',
    'GL_DUPLICATE_ACTIVE_RULE': 'ERRORS.GL_DUPLICATE_ACTIVE_RULE',
    'GL_RULE_IN_USE': 'ERRORS.GL_RULE_IN_USE',
    'GL_RULE_HAS_PENDING_POSTINGS': 'ERRORS.GL_RULE_HAS_PENDING_POSTINGS',
    'GL_UNBALANCED_ENTRY': 'ERRORS.GL_UNBALANCED_ENTRY',
    'GL_MISSING_RULE': 'ERRORS.GL_MISSING_RULE',
    'GL_RULE_NO_LINES': 'ERRORS.GL_RULE_NO_LINES',
    'GL_RULE_MISSING_SIDES': 'ERRORS.GL_RULE_MISSING_SIDES',
    'GL_RULE_DUPLICATE_PRIORITY': 'ERRORS.GL_RULE_DUPLICATE_PRIORITY',
    'GL_RULE_INVALID_AMOUNT_SOURCE': 'ERRORS.GL_RULE_INVALID_AMOUNT_SOURCE',
    'GL_RULE_INVALID_ENTRY_SIDE': 'ERRORS.GL_RULE_INVALID_ENTRY_SIDE',
    'GL_RULE_INVALID_AMOUNT_SOURCE_TYPE': 'ERRORS.GL_RULE_INVALID_AMOUNT_SOURCE_TYPE',
    'GL_RULE_INVALID_PRIORITY': 'ERRORS.GL_RULE_INVALID_PRIORITY',
    'GL_RULE_INVALID_SOURCE_MODULE': 'ERRORS.GL_RULE_INVALID_SOURCE_MODULE',
    'GL_RULE_INVALID_SOURCE_DOC_TYPE': 'ERRORS.GL_RULE_INVALID_SOURCE_DOC_TYPE',
    'GL_RULE_AMOUNT_TOTAL_NO_VALUE': 'ERRORS.GL_RULE_AMOUNT_TOTAL_NO_VALUE',
    'GL_RULE_AMOUNT_FIXED_POSITIVE': 'ERRORS.GL_RULE_AMOUNT_FIXED_POSITIVE',
    'GL_RULE_AMOUNT_PERCENT_RANGE': 'ERRORS.GL_RULE_AMOUNT_PERCENT_RANGE',

    // GL Module - General errors
    'GL_VALIDATION_ERROR': 'ERRORS.GL_VALIDATION_ERROR',
    'GL_ACCESS_DENIED': 'ERRORS.GL_ACCESS_DENIED',

    // Master Data / Master Lookup errors
    'NOT_FOUND': 'ERRORS.NOT_FOUND',
    'MASTER_LOOKUP_NOT_FOUND': 'ERRORS.MASTER_LOOKUP_NOT_FOUND',
    'MASTER_LOOKUP_ACCESS_DENIED': 'ERRORS.ACCESS_DENIED',
    'MASTER_LOOKUP_KEY_DUPLICATE': 'ERRORS.MASTER_LOOKUP_KEY_DUPLICATE',
    'MASTER_LOOKUP_ACTIVE_DETAILS_EXIST': 'ERRORS.MASTER_LOOKUP_ACTIVE_DETAILS_EXIST',
    'MASTER_LOOKUP_DETAILS_EXIST': 'ERRORS.MASTER_LOOKUP_DETAILS_EXIST',
    'MASTER_LOOKUP_FK_VIOLATION': 'ERRORS.MASTER_LOOKUP_FK_VIOLATION',
    'MASTER_LOOKUP_IN_USE': 'ERRORS.MASTER_LOOKUP_FK_VIOLATION',
    'MASTER_LOOKUP_INACTIVE': 'ERRORS.MASTER_LOOKUP_INACTIVE',
    'LOOKUP_VALUE_INVALID': 'ERRORS.LOOKUP_VALUE_INVALID',
    'LOOKUP_DETAIL_NOT_FOUND': 'ERRORS.LOOKUP_DETAIL_NOT_FOUND',
    'LOOKUP_DETAIL_ACCESS_DENIED': 'ERRORS.ACCESS_DENIED',
    'LOOKUP_DETAIL_CODE_DUPLICATE': 'ERRORS.LOOKUP_DETAIL_CODE_DUPLICATE',
    'LOOKUP_DETAIL_FK_VIOLATION': 'ERRORS.LOOKUP_DETAIL_FK_VIOLATION',
    'LOOKUP_DETAIL_IN_USE': 'ERRORS.LOOKUP_DETAIL_FK_VIOLATION',

    // Security module errors
    'USER_HAS_ACTIVE_REFRESH_TOKENS': 'ERRORS.USER_HAS_ACTIVE_REFRESH_TOKENS',
    'USER_HAS_DEPENDENCIES': 'ERRORS.USER_HAS_DEPENDENCIES',
    'ROLE_ALREADY_EXISTS': 'ERRORS.ROLE_ALREADY_EXISTS',
    'PERMISSION_NOT_FOUND': 'ERRORS.PERMISSION_NOT_FOUND',
    'PERMISSION_ALREADY_EXISTS': 'ERRORS.PERMISSION_ALREADY_EXISTS',
    'PERMISSION_NOT_ASSIGNED_TO_ROLE': 'ERRORS.PERMISSION_NOT_ASSIGNED',
    'PERMISSIONS_NOT_FOUND': 'ERRORS.PERMISSIONS_NOT_FOUND',
    'INVALID_PERMISSION_TYPE': 'ERRORS.INVALID_PERMISSION_TYPE',
    'INVALID_PARENT_PAGE': 'ERRORS.INVALID_PARENT_PAGE',
    'CANNOT_REMOVE_VIEW_PERMISSION': 'ERRORS.CANNOT_REMOVE_VIEW_PERMISSION',
    'PAGE_ALREADY_ASSIGNED_TO_ROLE': 'ERRORS.PAGE_ALREADY_ASSIGNED',
    'PAGE_NOT_ASSIGNED_TO_ROLE': 'ERRORS.PAGE_NOT_ASSIGNED',
    'NO_REFRESH_COOKIE': 'ERRORS.SESSION_EXPIRED',
    'REFRESH_REVOKED': 'ERRORS.SESSION_EXPIRED',
    'REFRESH_EXPIRED_OR_REVOKED': 'ERRORS.SESSION_EXPIRED',
    'MISSING_TENANT': 'ERRORS.MISSING_TENANT',
    'INVALID_OPERATION': 'ERRORS.INVALID_OPERATION',
    'NO_PERMISSIONS_TO_COPY': 'ERRORS.NO_PERMISSIONS_TO_COPY',
    'DB_CONSTRAINT_VIOLATION': 'ERRORS.DB_CONSTRAINT_VIOLATION',
    
    // Generic fallback
    'UNKNOWN_ERROR': 'ERRORS.UNKNOWN_ERROR'
  };

  /**
   * Map a backend error code to a translation key.
   * 
   * @param errorCode - The backend error code
   * @param params - Optional parameters to include in the mapping result
   * @returns The translation key and optional parameters
   * 
   * @example
   * ```typescript
   * const mapping = errorMapper.mapError('HTTP_404');
   * // { translationKey: 'ERRORS.NOT_FOUND', params: undefined }
   * 
   * const mapping = errorMapper.mapError('DUPLICATE_ENTRY', { field: 'username' });
   * // { translationKey: 'ERRORS.DUPLICATE_ENTRY', params: { field: 'username' } }
   * ```
   */
  mapError(errorCode: string, params?: Record<string, unknown>): ErpErrorMapping {
    const normalizedCode = this.normalizeErrorCode(errorCode);
    const translationKey = this.errorMappings[normalizedCode] || 'ERRORS.UNKNOWN_ERROR';
    
    return {
      translationKey,
      params
    };
  }

  /**
   * Map an HTTP status code to a translation key.
   * 
   * @param statusCode - The HTTP status code
   * @returns The translation key
   */
  mapHttpStatus(statusCode: number): ErpErrorMapping {
    return this.mapError(`HTTP_${statusCode}`);
  }

  /**
   * Check if an error code has a known mapping.
   * 
   * @param errorCode - The error code to check
   * @returns True if a mapping exists
   */
  hasMapping(errorCode: string): boolean {
    const normalizedCode = this.normalizeErrorCode(errorCode);
    return normalizedCode in this.errorMappings;
  }

  /**
   * Register a custom error mapping.
   * Useful for feature-specific error codes.
   * 
   * @param errorCode - The error code
   * @param translationKey - The translation key to map to
   */
  registerMapping(errorCode: string, translationKey: string): void {
    const normalizedCode = this.normalizeErrorCode(errorCode);
    this.errorMappings[normalizedCode] = translationKey;
  }

  /**
   * Register multiple custom error mappings at once.
   * 
   * @param mappings - Object with error codes as keys and translation keys as values
   */
  registerMappings(mappings: Record<string, string>): void {
    Object.entries(mappings).forEach(([code, key]) => {
      this.registerMapping(code, key);
    });
  }

  /**
   * Normalize an error code for consistent lookup.
   * Converts to uppercase and replaces common separators.
   */
  private normalizeErrorCode(errorCode: string): string {
    if (!errorCode) return 'UNKNOWN_ERROR';
    return errorCode
      .toUpperCase()
      .replace(/[.\-:]/g, '_')
      .replace(/\s+/g, '_');
  }
}
