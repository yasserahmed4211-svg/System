import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import { BaseApiService } from 'src/app/shared/base/base-api.service';
import { PagedResponse, SearchRequest } from 'src/app/modules/finance/gl/models/gl.model';
import { AccPostingMstDto, PostingGenerateJournalResponse, JournalPreviewResponse } from 'src/app/modules/finance/gl/models/posting.model';

/**
 * Posting API Service
 *
 * Communicates with the GL Posting backend endpoints.
 * Extends BaseApiService for unwrapping and typed HTTP helpers.
 */
@Injectable()
export class PostingApiService extends BaseApiService {
  private readonly postingsUrl = `${environment.authApiUrl}/api/gl/postings`;

  searchPostings(request: SearchRequest): Observable<PagedResponse<AccPostingMstDto>> {
    return this.doPost<PagedResponse<AccPostingMstDto>>(`${this.postingsUrl}/search`, request);
  }

  getPostingById(postingId: number): Observable<AccPostingMstDto> {
    return this.doGet<AccPostingMstDto>(`${this.postingsUrl}/${postingId}`);
  }

  previewJournal(postingId: number): Observable<JournalPreviewResponse> {
    return this.doGet<JournalPreviewResponse>(`${this.postingsUrl}/${postingId}/preview-journal`);
  }

  generateJournal(postingId: number): Observable<PostingGenerateJournalResponse> {
    return this.doPost<PostingGenerateJournalResponse>(`${this.postingsUrl}/${postingId}/generate-journal`, {});
  }
}
