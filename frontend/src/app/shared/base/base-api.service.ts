import { inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

/**
 * BaseApiService — shared HTTP helper for all feature API services.
 *
 * Provides:
 *  • Centralised response unwrapping (`ApiResponse<T>.data → T`)
 *  • Typed GET / POST / PUT / DELETE shortcuts
 *  • Consistent error propagation
 *
 * Sub-classes only need to set `baseUrl` and implement domain-specific methods.
 *
 * NOTE: Authorization header is added globally by AuthInterceptor.
 *       Do NOT duplicate it in individual API services.
 */
export abstract class BaseApiService {
  protected readonly http = inject(HttpClient);

  // ── response unwrapping ──────────────────────────────────────────

  /**
   * Unwrap a backend response that may be wrapped in `{ success, message, data }`.
   * If the payload has a `data` property, returns `data`; otherwise returns the payload as-is.
   */
  protected unwrapResponse<T>(response: unknown): T {
    const anyResp = response as Record<string, unknown>;
    return (anyResp && typeof anyResp === 'object' && 'data' in anyResp ? anyResp['data'] : anyResp) as T;
  }

  // ── typed HTTP helpers ───────────────────────────────────────────

  protected doGet<T>(url: string, params?: HttpParams): Observable<T> {
    return this.http.get<unknown>(url, { params }).pipe(
      map(res => this.unwrapResponse<T>(res)),
      catchError(err => throwError(() => err))
    );
  }

  protected doPost<T>(url: string, body: unknown): Observable<T> {
    return this.http.post<unknown>(url, body).pipe(
      map(res => this.unwrapResponse<T>(res)),
      catchError(err => throwError(() => err))
    );
  }

  protected doPut<T>(url: string, body: unknown): Observable<T> {
    return this.http.put<unknown>(url, body).pipe(
      map(res => this.unwrapResponse<T>(res)),
      catchError(err => throwError(() => err))
    );
  }

  protected doDelete<T = void>(url: string): Observable<T> {
    return this.http.delete<unknown>(url).pipe(
      map(res => this.unwrapResponse<T>(res)),
      catchError(err => throwError(() => err))
    );
  }
}
