import { Injectable } from '@angular/core';
import { Observable, of, shareReplay, catchError, map } from 'rxjs';

import { environment } from 'src/environments/environment';
import { LookupDetail, LookupSelectOption } from '../models/lookup-detail.model';
import { BaseApiService } from 'src/app/shared/base/base-api.service';

/**
 * LookupService — Centralised service for fetching dynamic lookup options.
 *
 * Features:
 *  • In-memory cache per lookupKey using `shareReplay(1)`
 *  • Lazy loading — HTTP call made only on first use
 *  • Error resilience — returns empty array on failure, logs error
 *  • Language-aware label mapping via `getOptions()`
 *
 * All feature modules MUST use this service instead of static enum arrays.
 *
 * @architecture Core layer — no UI dependencies
 */
@Injectable({ providedIn: 'root' })
export class LookupService extends BaseApiService {
  private readonly lookupBaseUrl = `${environment.authApiUrl}/api/masterdata/master-lookups/details/options`;

  /**
   * In-memory cache: Map<lookupKey, Observable<LookupDetail[]>>
   * Each entry is a shared, replayed observable that avoids duplicate HTTP calls.
   */
  private readonly cache = new Map<string, Observable<LookupDetail[]>>();

  // ── Public API ───────────────────────────────────────────────────

  /**
   * Get raw lookup details for a given lookup key.
   * Cached after the first call — subsequent calls return the same observable.
   *
   * @param lookupKey - Master lookup code (e.g. 'GL_ACCOUNT_TYPE', 'ENTRY_SIDE')
   * @returns Observable<LookupDetail[]>
   */
  getLookup(lookupKey: string): Observable<LookupDetail[]> {
    if (!this.cache.has(lookupKey)) {
      const url = `${this.lookupBaseUrl}/${encodeURIComponent(lookupKey)}?active=true`;

      const request$ = this.doGet<unknown>(url).pipe(
        map(response => this.mapAndDedup(response)),
        catchError(err => {
          console.error(`[LookupService] Failed to load lookup "${lookupKey}":`, err);
          // Remove from cache so it can be retried on next access
          this.cache.delete(lookupKey);
          return of([] as LookupDetail[]);
        }),
        shareReplay({ bufferSize: 1, refCount: false })
      );

      this.cache.set(lookupKey, request$);
    }

    return this.cache.get(lookupKey)!;
  }

  /**
   * Get lookup options mapped for dropdown binding.
   * Returns `{ value: code, label: nameAr/nameEn }` based on language.
   *
   * @param lookupKey - Master lookup code
   * @param lang - Current language ('ar' | 'en'), defaults to 'ar'
   * @returns Observable<LookupSelectOption[]>
   */
  getOptions(lookupKey: string, lang: string = 'ar'): Observable<LookupSelectOption[]> {
    return this.getLookup(lookupKey).pipe(
      map(details => details.map(d => ({
        value: d.code,
        label: lang === 'ar' ? d.nameAr : (d.nameEn || d.nameAr)
      })))
    );
  }

  /**
   * Invalidate cache for a specific lookup key.
   * Useful after admin updates to master lookup data.
   */
  invalidate(lookupKey: string): void {
    this.cache.delete(lookupKey);
  }

  /**
   * Invalidate all cached lookups.
   */
  invalidateAll(): void {
    this.cache.clear();
  }

  // ── Response Mapping ─────────────────────────────────────────

  /**
   * Map and deduplicate raw API data into LookupDetail[].
   * The base unwrapResponse already extracts the envelope; this handles
   * shape normalization and deduplication.
   */
  private mapAndDedup(data: unknown): LookupDetail[] {
    const arr = Array.isArray(data) ? data : [];
    if (arr.length === 0) return [];

    // Map from API DTO shape (id, code, nameAr, nameEn, ...) to LookupDetail
    const mapped = arr.map((item: Record<string, unknown>) => ({
      idPk: (item['id'] ?? item['idPk'] ?? 0) as number,
      code: (item['code'] ?? '') as string,
      nameAr: (item['nameAr'] ?? '') as string,
      nameEn: (item['nameEn'] ?? undefined) as string | undefined,
      extraValue: (item['extraValue'] ?? undefined) as string | undefined,
      sortOrder: (item['sortOrder'] ?? undefined) as number | undefined
    }));

    // Deduplicate by code AND by nameEn (catches semantic duplicates
    // with different codes, e.g. code "1" / "ASSET" both meaning Asset).
    // Sort by id ascending first so the original (older) entry is kept.
    mapped.sort((a, b) => a.idPk - b.idPk);

    const seenCodes = new Set<string>();
    const seenNames = new Set<string>();
    const deduped = mapped.filter(item => {
      if (seenCodes.has(item.code)) return false;

      const nameKey = (item.nameEn || '').toLowerCase().trim();
      if (nameKey && seenNames.has(nameKey)) return false;

      seenCodes.add(item.code);
      if (nameKey) seenNames.add(nameKey);
      return true;
    });

    // Restore sortOrder-based ordering
    return deduped.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
  }
}
