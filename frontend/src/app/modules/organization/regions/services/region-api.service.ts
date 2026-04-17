import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import { BaseApiService } from 'src/app/shared/base/base-api.service';
import {
  RegionDto,
  RegionListItemDto,
  RegionUsageDto,
  CreateRegionRequest,
  UpdateRegionRequest,
  PagedResponse,
  SearchRequest
} from '../models/region.model';

@Injectable()
export class RegionApiService extends BaseApiService {
  private readonly baseUrl = `${environment.authApiUrl}/api/organization`;
  private readonly entityUrl = `${this.baseUrl}/regions`;

  search(request: SearchRequest): Observable<PagedResponse<RegionListItemDto>> {
    return this.doPost<PagedResponse<RegionListItemDto>>(`${this.entityUrl}/search`, request);
  }

  getById(id: number): Observable<RegionDto> {
    return this.doGet<RegionDto>(`${this.entityUrl}/${id}`);
  }

  create(request: CreateRegionRequest): Observable<RegionDto> {
    return this.doPost<RegionDto>(this.entityUrl, request);
  }

  update(id: number, request: UpdateRegionRequest): Observable<RegionDto> {
    return this.doPut<RegionDto>(`${this.entityUrl}/${id}`, request);
  }

  deactivate(id: number): Observable<void> {
    return this.doPut<void>(`${this.entityUrl}/${id}/deactivate`, {});
  }

  getUsage(id: number): Observable<RegionUsageDto> {
    return this.doGet<RegionUsageDto>(`${this.entityUrl}/${id}/usage`);
  }
}
