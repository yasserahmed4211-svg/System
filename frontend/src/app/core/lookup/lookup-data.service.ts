import { Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from 'src/environments/environment';
import {
  LookupItem,
  LookupAdvancedSearchParams,
  LookupPagedResponse
} from './lookup.model';
import { BaseApiService } from 'src/app/shared/base/base-api.service';

/**
 * LookupDataService — centralised HTTP gateway for all lookup operations.
 *
 * Responsibilities:
 *  • Quick search (limit-based, returns LookupItem[])
 *  • Advanced paginated search (returns LookupPagedResponse)
 *  • Single-item fetch by ID (for display resolution)
 *
 * This service contains ZERO UI logic and ZERO component references.
 * All feature modules consume lookups exclusively through this service
 * (via the shared LookupFieldComponent).
 *
 * @architecture Core layer — HTTP + business logic only
 */
@Injectable({ providedIn: 'root' })
export class LookupDataService extends BaseApiService {
  private readonly lookupBaseUrl = environment.authApiUrl;

  // ── Quick Search ─────────────────────────────────────────────────

  /**
   * Perform a quick lookup search with a result limit.
   * Used by the autocomplete component for fast inline selection.
   *
   * @param endpoint - REST path (e.g. '/api/gl/accounts/lookup')
   * @param search   - User search term
   * @param limit    - Maximum results to return (default 10)
   * @returns Observable of LookupItem[]
   */
  quickSearch(endpoint: string, search: string, limit: number = 10): Observable<LookupItem[]> {
    const params = new HttpParams()
      .set('search', search)
      .set('limit', limit.toString());

    return this.doGet<unknown>(`${this.lookupBaseUrl}${endpoint}`, params)
      .pipe(map(res => this.unwrapToArray(res)));
  }

  // ── Advanced Paginated Search ────────────────────────────────────

  /**
   * Perform an advanced paginated lookup search.
   * Used by the lookup dialog for server-side pagination, sorting, and filtering.
   *
   * @param endpoint - REST path (e.g. '/api/gl/accounts/lookup')
   * @param params   - Search parameters including page, size, sort
   * @returns Observable of LookupPagedResponse
   */
  advancedSearch(endpoint: string, params: LookupAdvancedSearchParams, extraParams?: Record<string, string>): Observable<LookupPagedResponse> {
    let httpParams = new HttpParams()
      .set('search', params.search)
      .set('page', params.page.toString())
      .set('size', params.size.toString());

    if (params.sort) {
      httpParams = httpParams.set('sort', params.sort);
    }
    if (params.direction) {
      httpParams = httpParams.set('direction', params.direction);
    }
    if (extraParams) {
      for (const [key, value] of Object.entries(extraParams)) {
        httpParams = httpParams.set(key, value);
      }
    }

    return this.doGet<unknown>(`${this.lookupBaseUrl}${endpoint}`, httpParams)
      .pipe(map(res => this.unwrapPaged(res)));
  }

  // ── Single Item Fetch ────────────────────────────────────────────

  /**
   * Fetch a single lookup item by ID.
   * Used to resolve the display label when the form is initialised with an existing ID.
   *
   * @param endpoint - REST path
   * @param id       - Entity ID
   * @returns Observable of LookupItem
   */
  fetchById(endpoint: string, id: number): Observable<LookupItem> {
    return this.doGet<unknown>(`${this.lookupBaseUrl}${endpoint}/${id}`)
      .pipe(map(res => this.unwrapSingle(res)));
  }

  // ── Response Unwrapping ──────────────────────────────────────────

  /**
   * Unwrap for array responses (post-base-unwrap).
   */
  private unwrapToArray(data: unknown): LookupItem[] {
    return (Array.isArray(data) ? data : []) as LookupItem[];
  }

  /**
   * Unwrap for paged responses (post-base-unwrap).
   */
  private unwrapPaged(data: unknown): LookupPagedResponse {
    return (data ?? { content: [], totalElements: 0, totalPages: 0 }) as LookupPagedResponse;
  }

  /**
   * Unwrap for single-item responses (post-base-unwrap).
   */
  private unwrapSingle(data: unknown): LookupItem {
    return (data ?? { id: 0, display: '' }) as LookupItem;
  }
}
