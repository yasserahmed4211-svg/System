import { Injectable } from '@angular/core';
import { AbstractControl } from '@angular/forms';

import { ErpErrorMapperService, ErpErrorMapping } from './erp-error-mapper.service';
import { FormErrorResult, getFormFieldError } from '../utils/form-error-resolver';

export type ErpUiMessageContext = 'FORM' | 'TABLE' | 'ACTION' | 'API';

export interface ErpUiMessageResult {
  key: string;
  params?: Record<string, unknown>;
}

export interface ErpTableStateContext {
  loading?: boolean;
  loadError?: unknown;
  rowCount?: number;
}

export type ErpActionType = 'CREATE' | 'UPDATE' | 'DELETE' | 'UNKNOWN';

export interface ErpActionContext {
  type?: ErpActionType;
  error?: unknown;
}

/**
 * ErpUiMessageResolverService
 *
 * Shared, stateless message resolver that returns ONLY translation keys + params.
 * Delegates:
 * - Form errors => form-error-resolver
 * - Backend/API errors => ErpErrorMapperService
 */
@Injectable({ providedIn: 'root' })
export class ErpUiMessageResolverService {
  constructor(private readonly errorMapper: ErpErrorMapperService) {}

  resolve(context: ErpUiMessageContext, payload?: unknown): ErpUiMessageResult | null {
    switch (context) {
      case 'FORM':
        return this.resolveFormValidation(payload as AbstractControl | null);
      case 'TABLE':
        return this.resolveTableState(payload as ErpTableStateContext);
      case 'ACTION':
        return this.resolveAction(payload as ErpActionContext);
      case 'API':
        return this.resolveApiError(payload);
      default:
        return { key: 'ERRORS.UNKNOWN_ERROR' };
    }
  }

  resolveFormValidation(control: AbstractControl | null): FormErrorResult | null {
    return getFormFieldError(control);
  }

  resolveTableState(state: ErpTableStateContext): ErpUiMessageResult {
    if (state?.loading) {
      return { key: 'TABLE.LOADING' };
    }

    if (state?.loadError) {
      return { key: 'TABLE.LOAD_FAILED' };
    }

    if (typeof state?.rowCount === 'number' && state.rowCount <= 0) {
      return { key: 'TABLE.EMPTY' };
    }

    return { key: 'TABLE.OK' };
  }

  resolveAction(action: ErpActionContext): ErpUiMessageResult {
    if (!action?.error) {
      return { key: 'ACTION.SUCCESS' };
    }

    const status = this.extractHttpStatus(action.error);
    if (status === 403) return { key: 'ACTION.FORBIDDEN' };
    if (status === 409) return { key: 'ACTION.CONFLICT' };

    switch (action.type) {
      case 'DELETE':
        return { key: 'ACTION.DELETE_FAILED' };
      case 'CREATE':
        return { key: 'ACTION.CREATE_FAILED' };
      case 'UPDATE':
        return { key: 'ACTION.UPDATE_FAILED' };
      default:
        return { key: 'ACTION.FAILED' };
    }
  }

  resolveApiError(error: unknown): ErpUiMessageResult {
    const status = this.extractHttpStatus(error);
    if (typeof status === 'number') {
      const mappedStatus = this.errorMapper.mapHttpStatus(status);
      return this.asUiMessage(mappedStatus);
    }

    const code = this.extractErrorCode(error);
    if (code) {
      const mapped = this.errorMapper.mapError(code);
      return this.asUiMessage(mapped);
    }

    return { key: 'ERRORS.UNKNOWN_ERROR' };
  }

  private asUiMessage(mapping: ErpErrorMapping): ErpUiMessageResult {
    return { key: mapping.translationKey, params: mapping.params };
  }

  private extractHttpStatus(error: unknown): number | null {
    const anyErr = error as any;
    const status = anyErr?.status;
    return typeof status === 'number' ? status : null;
  }

  private extractErrorCode(error: unknown): string | null {
    const anyErr = error as any;
    return (
      anyErr?.error?.code ||
      anyErr?.error?.errorCode ||
      anyErr?.code ||
      anyErr?.errorCode ||
      null
    );
  }
}
