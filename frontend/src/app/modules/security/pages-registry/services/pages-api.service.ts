import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import { BaseApiService } from 'src/app/shared/base/base-api.service';
import {
  PageDto,
  PagedResponse,
  PageSearchRequest,
  CreatePageRequest,
  UpdatePageRequest
} from 'src/app/modules/security/pages-registry/models/page.model';

@Injectable()
export class PagesApiService extends BaseApiService {
  private readonly baseUrl = `${environment.authApiUrl}/api/pages`;

  /**
   * POST /api/pages/search — search pages with dynamic filters, sorting, and pagination
   */
  searchPages(request: PageSearchRequest): Observable<PagedResponse<PageDto>> {
    return this.doPost<PagedResponse<PageDto>>(`${this.baseUrl}/search`, request);
  }

  getActivePages(): Observable<PageDto[]> {
    return this.doGet<PageDto[]>(`${this.baseUrl}/active`);
  }

  getPageById(id: number): Observable<PageDto> {
    return this.doGet<PageDto>(`${this.baseUrl}/${id}`);
  }

  createPage(request: CreatePageRequest): Observable<PageDto> {
    return this.doPost<PageDto>(this.baseUrl, request);
  }

  updatePage(id: number, request: UpdatePageRequest): Observable<PageDto> {
    return this.doPut<PageDto>(`${this.baseUrl}/${id}`, request);
  }

  deactivatePage(id: number): Observable<PageDto> {
    return this.doPut<PageDto>(`${this.baseUrl}/${id}/deactivate`, {});
  }

  reactivatePage(id: number): Observable<PageDto> {
    return this.doPut<PageDto>(`${this.baseUrl}/${id}/reactivate`, {});
  }

  /**
   * Get distinct modules from loaded pages
   * (No backend endpoint - extract from pages)
   */
  getModulesFromPages(pages: PageDto[]): string[] {
    const modules = new Set<string>();
    pages.forEach(p => {
      if (p.module) modules.add(p.module);
    });
    return Array.from(modules).sort();
  }
}
