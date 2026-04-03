import { Injectable, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { environment } from 'src/environments/environment';

/**
 * Structured error entry for logging and telemetry.
 */
export interface ErrorLogEntry {
  timestamp: string;
  /** 'http' | 'runtime' | 'unhandled' */
  category: string;
  message: string;
  status?: number;
  url?: string;
  method?: string;
  errorCode?: string;
  stack?: string;
  /** Additional context provided by the caller. */
  context?: Record<string, unknown>;
}

/**
 * ErrorLoggerService — centralised error capture and forwarding.
 *
 * Goals:
 *  1. Single choke-point for ALL frontend errors.
 *  2. Structured log entries (ready for Sentry / ELK / backend endpoint).
 *  3. Dev-mode console output for easy debugging.
 *  4. Production-mode: silent (no `console.error` leaks).
 *
 * Plug-in points for future telemetry:
 *  • Override `sendToRemote()` to push to Sentry / custom endpoint.
 *  • Subscribe to the `recentErrors` buffer for in-app error dashboards.
 */
@Injectable({ providedIn: 'root' })
export class ErrorLoggerService {
  /** Ring buffer of last N errors — useful for dev-tools / diagnostics. */
  private readonly _buffer: ErrorLogEntry[] = [];
  private readonly MAX_BUFFER = 50;

  /** Read-only view of the buffer. */
  get recentErrors(): ReadonlyArray<ErrorLogEntry> {
    return this._buffer;
  }

  // ── HTTP errors ──────────────────────────────────────────────────

  /**
   * Log an HTTP error.  Called from `ErrorInterceptor`.
   */
  logHttpError(error: HttpErrorResponse, method?: string): void {
    const entry = this.buildHttpEntry(error, method);
    this.push(entry);
  }

  // ── Runtime / unhandled errors ───────────────────────────────────

  /**
   * Log a generic runtime error.
   */
  logError(error: unknown, context?: Record<string, unknown>): void {
    const entry = this.buildRuntimeEntry(error, context);
    this.push(entry);
  }

  // ── internals ────────────────────────────────────────────────────

  private push(entry: ErrorLogEntry): void {
    this._buffer.push(entry);
    if (this._buffer.length > this.MAX_BUFFER) {
      this._buffer.shift();
    }

    // Dev-mode: surface in console for developer convenience
    if (!environment.production) {
      // eslint-disable-next-line no-console
      console.warn('[ERP-Error]', entry);
    }

    this.sendToRemote(entry);
  }

  /**
   * Extension point — override in a subclass or patch via DI to send
   * entries to Sentry, Application Insights, or a backend `/api/logs` endpoint.
   */
  protected sendToRemote(_entry: ErrorLogEntry): void {
    // No-op by default.  Wire up when a telemetry backend is available.
  }

  // ── builders ─────────────────────────────────────────────────────

  private buildHttpEntry(err: HttpErrorResponse, method?: string): ErrorLogEntry {
    const errorCode = this.extractErrorCode(err);
    return {
      timestamp: new Date().toISOString(),
      category: 'http',
      message: err.message || `HTTP ${err.status}`,
      status: err.status,
      url: err.url ?? undefined,
      method,
      errorCode,
    };
  }

  private buildRuntimeEntry(error: unknown, context?: Record<string, unknown>): ErrorLogEntry {
    if (error instanceof Error) {
      return {
        timestamp: new Date().toISOString(),
        category: 'runtime',
        message: error.message,
        stack: error.stack,
        context,
      };
    }
    return {
      timestamp: new Date().toISOString(),
      category: 'runtime',
      message: String(error),
      context,
    };
  }

  /**
   * Try to extract a backend error code from the response body.
   * Handles shapes: `{ errorCode }`, `{ code }`, `{ error: { code } }`.
   */
  private extractErrorCode(err: HttpErrorResponse): string | undefined {
    const body = err.error;
    if (!body || typeof body !== 'object') return undefined;
    return body.errorCode ?? body.code ?? body.error?.code ?? undefined;
  }
}
