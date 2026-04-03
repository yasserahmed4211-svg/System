import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

import { environment } from 'src/environments/environment';
import { BaseApiService } from 'src/app/shared/base/base-api.service';
import { PagedResponse, SearchRequest } from 'src/app/modules/finance/gl/models/gl.model';
import {
  GlJournalHdrDto,
  ManualCreateJournalRequest,
  ManualUpdateJournalRequest,
  UpdateJournalRequest
} from 'src/app/modules/finance/gl/models/journal.model';

/**
 * Journal API Service
 *
 * Communicates with the GL Journal backend endpoints.
 * Extends BaseApiService for unwrapping and typed HTTP helpers.
 */
@Injectable()
export class JournalApiService extends BaseApiService {
  private readonly journalsUrl = `${environment.authApiUrl}/api/gl/journals`;

  // ── Core CRUD ──────────────────────────────────────────────

  searchJournals(request: SearchRequest): Observable<PagedResponse<GlJournalHdrDto>> {
    return this.doPost<PagedResponse<GlJournalHdrDto>>(`${this.journalsUrl}/search`, request);
  }

  getJournalById(journalId: number): Observable<GlJournalHdrDto> {
    return this.doGet<GlJournalHdrDto>(`${this.journalsUrl}/${journalId}`);
  }

  createJournal(request: ManualCreateJournalRequest): Observable<GlJournalHdrDto> {
    return this.doPost<GlJournalHdrDto>(`${this.journalsUrl}/manual`, request);
  }

  updateJournal(journalId: number, request: ManualUpdateJournalRequest): Observable<GlJournalHdrDto> {
    return this.doPut<GlJournalHdrDto>(`${this.journalsUrl}/manual/${journalId}`, request);
  }

  updateJournalGeneral(journalId: number, request: UpdateJournalRequest): Observable<GlJournalHdrDto> {
    return this.doPut<GlJournalHdrDto>(`${this.journalsUrl}/${journalId}`, request);
  }

  toggleActiveJournal(journalId: number, active: boolean): Observable<GlJournalHdrDto> {
    return this.doPut<GlJournalHdrDto>(`${this.journalsUrl}/${journalId}/toggle-active`, { active });
  }

  // ── State Transitions ─────────────────────────────────────

  approveJournal(journalId: number): Observable<GlJournalHdrDto> {
    return this.http.patch<unknown>(`${this.journalsUrl}/${journalId}/approve`, {}).pipe(
      map(res => this.unwrapResponse<GlJournalHdrDto>(res)),
      catchError(err => throwError(() => err))
    );
  }

  postJournal(journalId: number): Observable<GlJournalHdrDto> {
    return this.http.patch<unknown>(`${this.journalsUrl}/${journalId}/post`, {}).pipe(
      map(res => this.unwrapResponse<GlJournalHdrDto>(res)),
      catchError(err => throwError(() => err))
    );
  }

  reverseJournal(journalId: number): Observable<GlJournalHdrDto> {
    return this.http.patch<unknown>(`${this.journalsUrl}/${journalId}/reverse`, {}).pipe(
      map(res => this.unwrapResponse<GlJournalHdrDto>(res)),
      catchError(err => throwError(() => err))
    );
  }

  cancelJournal(journalId: number): Observable<GlJournalHdrDto> {
    return this.http.patch<unknown>(`${this.journalsUrl}/${journalId}/cancel`, {}).pipe(
      map(res => this.unwrapResponse<GlJournalHdrDto>(res)),
      catchError(err => throwError(() => err))
    );
  }
}
