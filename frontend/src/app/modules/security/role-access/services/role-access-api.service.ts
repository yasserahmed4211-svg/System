import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from 'src/environments/environment';
import { BaseApiService } from 'src/app/shared/base/base-api.service';
import {
  ActivePageDto,
  AddPageToRoleRequest,
  CreateRoleRequest,
  RoleDto,
  RolePagedResponse,
  RoleSearchRequest,
  RolePagesResponse,
  ToggleRoleActiveRequest,
  SyncRolePagesRequest
} from '../models/role-access.model';

@Injectable()
export class RoleAccessApiService extends BaseApiService {
  private readonly rolesBaseUrl = `${environment.authApiUrl}/api/roles`;
  private readonly pagesBaseUrl = `${environment.authApiUrl}/api/pages`;

  private normalizeRole(raw: any): RoleDto {
    return {
      id: Number(raw?.id ?? raw?.roleId ?? 0),
      roleCode: String(raw?.roleCode ?? raw?.code ?? ''),
      roleName: String(raw?.roleName ?? raw?.name ?? ''),
      description: raw?.description != null ? String(raw.description) : undefined,
      active: Boolean(raw?.active ?? true),
      createdAt: raw?.createdAt ? String(raw.createdAt) : undefined
    };
  }

  /**
   * POST /api/roles/search — search roles with dynamic filters, sorting, and pagination
   */
  searchRoles(request: RoleSearchRequest): Observable<RolePagedResponse> {
    return this.http.post<unknown>(`${this.rolesBaseUrl}/search`, request).pipe(
      map((resp) => {
        const unwrapped = this.unwrapResponse<any>(resp);
        const content = Array.isArray(unwrapped?.content) ? unwrapped.content : Array.isArray(unwrapped?.items) ? unwrapped.items : [];
        const totalElements = Number(unwrapped?.totalElements ?? unwrapped?.paging?.totalItems ?? 0);
        const totalPages = Number(unwrapped?.totalPages ?? unwrapped?.paging?.totalPages ?? 0);

        return {
          content: content.map((r: any) => this.normalizeRole(r)),
          totalElements,
          totalPages
        } as RolePagedResponse;
      })
    );
  }

  getRoleById(roleId: number): Observable<RoleDto> {
    return this.http.get<unknown>(`${this.rolesBaseUrl}/${roleId}`).pipe(
      map((resp) => this.normalizeRole(this.unwrapResponse<any>(resp)))
    );
  }

  createRole(request: CreateRoleRequest): Observable<RoleDto> {
    return this.http.post<unknown>(this.rolesBaseUrl, request).pipe(
      map((resp) => this.normalizeRole(this.unwrapResponse<any>(resp)))
    );
  }

  toggleRoleActive(roleId: number, request: ToggleRoleActiveRequest): Observable<RoleDto> {
    return this.http.put<unknown>(`${this.rolesBaseUrl}/${roleId}/toggle-active`, request).pipe(
      map((resp) => this.normalizeRole(this.unwrapResponse<any>(resp)))
    );
  }

  deleteRole(roleId: number): Observable<unknown> {
    return this.http.delete<unknown>(`${this.rolesBaseUrl}/${roleId}`);
  }

  getRolePages(roleId: number): Observable<RolePagesResponse> {
    return this.http.get<unknown>(`${this.rolesBaseUrl}/${roleId}/pages`).pipe(
      map((resp) => {
        const unwrapped = this.unwrapResponse<any>(resp);
        const assignments = Array.isArray(unwrapped?.assignments)
          ? unwrapped.assignments
          : Array.isArray(unwrapped)
            ? unwrapped
            : [];
        return {
          roleId: Number(unwrapped?.roleId ?? roleId),
          assignments: assignments.map((a: any) => ({
            pageCode: String(a?.pageCode ?? a?.page?.pageCode ?? ''),
            create: Array.isArray(a?.permissions) ? a.permissions.includes('CREATE') : Boolean(a?.create ?? false),
            update: Array.isArray(a?.permissions) ? a.permissions.includes('UPDATE') : Boolean(a?.update ?? false),
            delete: Array.isArray(a?.permissions) ? a.permissions.includes('DELETE') : Boolean(a?.delete ?? false)
          }))
        } as RolePagesResponse;
      })
    );
  }

  addPageToRole(roleId: number, request: AddPageToRoleRequest): Observable<unknown> {
    return this.http.post<unknown>(`${this.rolesBaseUrl}/${roleId}/pages`, request).pipe(map((resp) => this.unwrapResponse(resp)));
  }

  syncRolePages(roleId: number, request: SyncRolePagesRequest): Observable<RolePagesResponse> {
    return this.http.put<unknown>(`${this.rolesBaseUrl}/${roleId}/pages`, request).pipe(
      map((resp) => {
        // Backend returns RolePagesMatrixResponse (assignments with permissions[])
        const unwrapped = this.unwrapResponse<any>(resp);
        const assignments = Array.isArray(unwrapped?.assignments) ? unwrapped.assignments : [];
        return {
          roleId: Number(unwrapped?.roleId ?? roleId),
          assignments: assignments.map((a: any) => ({
            pageCode: String(a?.pageCode ?? ''),
            create: Array.isArray(a?.permissions) ? a.permissions.includes('CREATE') : false,
            update: Array.isArray(a?.permissions) ? a.permissions.includes('UPDATE') : false,
            delete: Array.isArray(a?.permissions) ? a.permissions.includes('DELETE') : false
          }))
        } as RolePagesResponse;
      })
    );
  }

  removeRolePage(roleId: number, pageCode: string): Observable<unknown> {
    return this.http.delete<unknown>(`${this.rolesBaseUrl}/${roleId}/pages/${encodeURIComponent(pageCode)}`);
  }

  copyFromRole(roleId: number, sourceRoleId: number): Observable<RolePagesResponse> {
    return this.http.post<unknown>(`${this.rolesBaseUrl}/${roleId}/copy-from/${sourceRoleId}`, {}).pipe(
      map((resp) => {
        const unwrapped = this.unwrapResponse<any>(resp);
        const assignments = Array.isArray(unwrapped?.assignments) ? unwrapped.assignments : [];
        return {
          roleId: Number(unwrapped?.roleId ?? roleId),
          assignments: assignments.map((a: any) => ({
            pageCode: String(a?.pageCode ?? ''),
            create: Array.isArray(a?.permissions) ? a.permissions.includes('CREATE') : false,
            update: Array.isArray(a?.permissions) ? a.permissions.includes('UPDATE') : false,
            delete: Array.isArray(a?.permissions) ? a.permissions.includes('DELETE') : false
          }))
        } as RolePagesResponse;
      })
    );
  }

  getActivePages(): Observable<ActivePageDto[]> {
    return this.http.get<unknown>(`${this.pagesBaseUrl}/active`).pipe(
      map((resp) => {
        const unwrapped = this.unwrapResponse<any>(resp);
        const pages = Array.isArray(unwrapped) ? unwrapped : Array.isArray(unwrapped?.pages) ? unwrapped.pages : [];
        return pages.map((p: any) => ({
          pageCode: String(p?.pageCode ?? ''),
          nameAr: p?.nameAr ? String(p.nameAr) : undefined,
          nameEn: p?.nameEn ? String(p.nameEn) : undefined,
          active: Boolean(p?.active ?? true)
        })) as ActivePageDto[];
      })
    );
  }
}
