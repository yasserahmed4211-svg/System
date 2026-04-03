import { FieldErrorItemDto } from '../models/api-response.model';

/**
 * @deprecated Use FieldErrorItemDto from shared models instead.
 */
export type BackendFieldErrorItem = FieldErrorItemDto;

/**
 * Extracts a human-readable backend error message from an HttpErrorResponse-ish object.
 *
 * Per governance rules, the FE should display backend messages as received.
 * This utility prefers the most user-useful backend-provided string, without constructing new text.
 */
export function extractBackendErrorMessage(error: unknown): string | null {
  const anyErr = error as any;

  // Angular HttpErrorResponse: `error` holds the response body.
  const body = anyErr?.error;

  // Standard ERP ApiResponse envelope: { message, error: { details, fieldErrors } }
  const details = body?.error?.details;
  if (typeof details === 'string' && details.trim().length > 0) return details;

  const fieldErrors = body?.error?.fieldErrors as BackendFieldErrorItem[] | undefined;
  if (Array.isArray(fieldErrors) && fieldErrors.length > 0) {
    const firstMessage = fieldErrors.find((fe) => typeof fe?.message === 'string' && fe.message.trim().length > 0)?.message;
    if (firstMessage) return firstMessage;
  }

  const message = body?.message;
  if (typeof message === 'string' && message.trim().length > 0) return message;

  // Some endpoints might return raw strings.
  if (typeof body === 'string' && body.trim().length > 0) return body;

  // Fallback to HttpErrorResponse.message
  const topMessage = anyErr?.message;
  if (typeof topMessage === 'string' && topMessage.trim().length > 0) return topMessage;

  return null;
}

export function extractBackendErrorCode(error: unknown): string | null {
  const anyErr = error as any;
  const body = anyErr?.error;
  const code = body?.error?.code || body?.code;
  return typeof code === 'string' && code.trim().length > 0 ? code : null;
}
