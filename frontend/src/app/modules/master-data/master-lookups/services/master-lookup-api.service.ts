import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import { BaseApiService } from 'src/app/shared/base/base-api.service';
import {
  MasterLookupDto,
  MasterLookupUsageDto,
  CreateMasterLookupRequest,
  UpdateMasterLookupRequest,
  LookupDetailDto,
  LookupDetailOptionDto,
  CreateLookupDetailRequest,
  UpdateLookupDetailRequest,
  PagedResponse,
  SearchRequest
} from '../models/master-lookup.model';

/**
 * API Service for Master Lookup Management
 * Follows: master-lookup.contract.md
 */
@Injectable()
export class MasterLookupApiService extends BaseApiService {
  private readonly baseUrl = `${environment.authApiUrl}/api/masterdata`;
  private readonly masterLookupsUrl = `${this.baseUrl}/master-lookups`;
  /** All detail endpoints are now under /master-lookups/details (unified controller) */
  private readonly lookupDetailsUrl = `${this.masterLookupsUrl}/details`;

  // ============================================
  // MASTER LOOKUP ENDPOINTS
  // ============================================

  searchMasterLookups(request: SearchRequest): Observable<PagedResponse<MasterLookupDto>> {
    return this.doPost<PagedResponse<MasterLookupDto>>(`${this.masterLookupsUrl}/search`, request);
  }

  getMasterLookupById(id: number): Observable<MasterLookupDto> {
    return this.doGet<MasterLookupDto>(`${this.masterLookupsUrl}/${id}`);
  }

  createMasterLookup(request: CreateMasterLookupRequest): Observable<MasterLookupDto> {
    return this.doPost<MasterLookupDto>(this.masterLookupsUrl, request);
  }

  updateMasterLookup(id: number, request: UpdateMasterLookupRequest): Observable<MasterLookupDto> {
    return this.doPut<MasterLookupDto>(`${this.masterLookupsUrl}/${id}`, request);
  }

  toggleMasterLookupActive(id: number, active: boolean): Observable<MasterLookupDto> {
    return this.doPut<MasterLookupDto>(`${this.masterLookupsUrl}/${id}/toggle-active`, { active });
  }

  deleteMasterLookup(id: number): Observable<void> {
    return this.doDelete(`${this.masterLookupsUrl}/${id}`);
  }

  getMasterLookupUsage(id: number): Observable<MasterLookupUsageDto> {
    return this.doGet<MasterLookupUsageDto>(`${this.masterLookupsUrl}/${id}/usage`);
  }

  // ============================================
  // LOOKUP DETAIL ENDPOINTS
  // ============================================

  searchLookupDetails(request: SearchRequest): Observable<PagedResponse<LookupDetailDto>> {
    return this.doPost<PagedResponse<LookupDetailDto>>(`${this.lookupDetailsUrl}/search`, request);
  }

  getLookupDetailById(id: number): Observable<LookupDetailDto> {
    return this.doGet<LookupDetailDto>(`${this.lookupDetailsUrl}/${id}`);
  }

  getLookupOptions(lookupKey: string, activeOnly: boolean = true): Observable<LookupDetailOptionDto[]> {
    const url = `${this.lookupDetailsUrl}/options/${encodeURIComponent(lookupKey)}`;
    const params = activeOnly ? '?active=true' : '?active=false';
    return this.doGet<LookupDetailOptionDto[]>(`${url}${params}`);
  }

  createLookupDetail(request: CreateLookupDetailRequest): Observable<LookupDetailDto> {
    return this.doPost<LookupDetailDto>(this.lookupDetailsUrl, request);
  }

  updateLookupDetail(id: number, request: UpdateLookupDetailRequest): Observable<LookupDetailDto> {
    return this.doPut<LookupDetailDto>(`${this.lookupDetailsUrl}/${id}`, request);
  }

  toggleLookupDetailActive(id: number, active: boolean): Observable<LookupDetailDto> {
    return this.doPut<LookupDetailDto>(`${this.lookupDetailsUrl}/${id}/toggle-active`, { active });
  }

  deleteLookupDetail(id: number): Observable<void> {
    return this.doDelete(`${this.lookupDetailsUrl}/${id}`);
  }
}
