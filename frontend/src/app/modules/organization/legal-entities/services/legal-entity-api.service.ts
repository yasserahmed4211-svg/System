import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import { BaseApiService } from 'src/app/shared/base/base-api.service';
import {
  LegalEntityDto,
  LegalEntityListItemDto,
  LegalEntityUsageDto,
  CreateLegalEntityRequest,
  UpdateLegalEntityRequest,
  PagedResponse,
  SearchRequest
} from '../models/legal-entity.model';

@Injectable()
export class LegalEntityApiService extends BaseApiService {
  private readonly baseUrl = `${environment.authApiUrl}/api/organization`;
  private readonly entityUrl = `${this.baseUrl}/legal-entities`;

  search(request: SearchRequest): Observable<PagedResponse<LegalEntityListItemDto>> {
    return this.doPost<PagedResponse<LegalEntityListItemDto>>(`${this.entityUrl}/search`, request);
  }

  getById(id: number): Observable<LegalEntityDto> {
    return this.doGet<LegalEntityDto>(`${this.entityUrl}/${id}`);
  }

  create(request: CreateLegalEntityRequest): Observable<LegalEntityDto> {
    return this.doPost<LegalEntityDto>(this.entityUrl, request);
  }

  update(id: number, request: UpdateLegalEntityRequest): Observable<LegalEntityDto> {
    return this.doPut<LegalEntityDto>(`${this.entityUrl}/${id}`, request);
  }

  deactivate(id: number): Observable<void> {
    return this.doPut<void>(`${this.entityUrl}/${id}/deactivate`, {});
  }

  getUsage(id: number): Observable<LegalEntityUsageDto> {
    return this.doGet<LegalEntityUsageDto>(`${this.entityUrl}/${id}/usage`);
  }
}
