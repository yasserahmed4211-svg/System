import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import { BaseApiService } from 'src/app/shared/base/base-api.service';
import {
  BranchDto,
  BranchListItemDto,
  BranchUsageDto,
  CreateBranchRequest,
  UpdateBranchRequest,
  DepartmentDto,
  CreateDepartmentRequest,
  UpdateDepartmentRequest,
  PagedResponse,
  SearchRequest
} from '../models/branch.model';

@Injectable()
export class BranchApiService extends BaseApiService {
  private readonly baseUrl = `${environment.authApiUrl}/api/organization`;
  private readonly entityUrl = `${this.baseUrl}/branches`;
  private readonly deptUrl = `${this.baseUrl}/departments`;

  // ─── Branch CRUD ───────────────────────────────────────────────

  search(request: SearchRequest): Observable<PagedResponse<BranchListItemDto>> {
    return this.doPost<PagedResponse<BranchListItemDto>>(`${this.entityUrl}/search`, request);
  }

  getById(id: number): Observable<BranchDto> {
    return this.doGet<BranchDto>(`${this.entityUrl}/${id}`);
  }

  create(request: CreateBranchRequest): Observable<BranchDto> {
    return this.doPost<BranchDto>(this.entityUrl, request);
  }

  update(id: number, request: UpdateBranchRequest): Observable<BranchDto> {
    return this.doPut<BranchDto>(`${this.entityUrl}/${id}`, request);
  }

  deactivate(id: number): Observable<void> {
    return this.doPut<void>(`${this.entityUrl}/${id}/deactivate`, {});
  }

  getUsage(id: number): Observable<BranchUsageDto> {
    return this.doGet<BranchUsageDto>(`${this.entityUrl}/${id}/usage`);
  }

  getDepartments(branchId: number): Observable<DepartmentDto[]> {
    return this.doGet<DepartmentDto[]>(`${this.entityUrl}/${branchId}/departments`);
  }

  // ─── Department CRUD ──────────────────────────────────────────

  createDepartment(request: CreateDepartmentRequest): Observable<DepartmentDto> {
    return this.doPost<DepartmentDto>(this.deptUrl, request);
  }

  updateDepartment(id: number, request: UpdateDepartmentRequest): Observable<DepartmentDto> {
    return this.doPut<DepartmentDto>(`${this.deptUrl}/${id}`, request);
  }

  deactivateDepartment(id: number): Observable<void> {
    return this.doPut<void>(`${this.deptUrl}/${id}/deactivate`, {});
  }
}
