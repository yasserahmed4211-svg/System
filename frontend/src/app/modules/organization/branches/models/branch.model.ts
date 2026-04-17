import { PagedResponse as CommonPagedResponse } from 'src/app/shared/models/paged-response.model';

export interface SearchFilter {
  field: string;
  operator: string;
  value?: string | number | boolean | Array<string | number>;
}

export interface SearchSort {
  field: string;
  direction: 'ASC' | 'DESC';
}

export interface SearchRequest {
  filters: SearchFilter[];
  sorts?: SearchSort[];
  page: number;
  size: number;
}

export type PagedResponse<T> = CommonPagedResponse<T>;

// ─── List DTO ─────────────────────────────────────────────────────────────────

export interface BranchListItemDto {
  branchPk: number;
  branchCode: string;
  branchNameAr: string;
  branchNameEn?: string;
  legalEntityFk?: number;
  legalEntityDisplay?: string;
  regionFk?: number;
  regionDisplay?: string;
  branchTypeId?: string;
  branchTypeDisplay?: string;
  isHeadquarterFl?: number;
  statusId: string;
  activeFl?: number;
}

// ─── Department DTO ───────────────────────────────────────────────────────────

export interface DepartmentDto {
  departmentPk: number;
  departmentCode: string;
  branchFk: number;
  departmentNameAr: string;
  departmentNameEn: string;
  departmentTypeId: string;
  activeFl: number;
}

// ─── Full Detail DTO ──────────────────────────────────────────────────────────

export interface BranchDto {
  branchPk: number;
  branchCode: string;
  branchNameAr: string;
  branchNameEn?: string;
  legalEntityFk?: number;
  legalEntityDisplay?: string;
  regionFk?: number;
  regionDisplay?: string;
  branchTypeId?: string;
  isHeadquarterFl?: number;
  statusId: string;
  activeFl?: number;
  addressLine1?: string;
  addressLine2?: string;
  cityName?: string;
  phone?: string;
  email?: string;
  departments?: DepartmentDto[];
  createdAt?: string;
  createdBy?: string;
  updatedAt?: string;
  updatedBy?: string;
}

// ─── Usage DTO ────────────────────────────────────────────────────────────────

export interface BranchUsageDto {
  canBeDeleted: boolean;
  canDeactivate: boolean;
  hasDepartments?: boolean;
}

// ─── Requests ─────────────────────────────────────────────────────────────────

export interface CreateBranchRequest {
  legalEntityFk: number;
  regionFk?: number | null;
  branchNameAr: string;
  branchNameEn: string;
  branchTypeId: string;
  isHeadquarterFl?: number;
  statusId: string;
  addressLine1?: string;
  addressLine2?: string;
  cityName?: string;
  phone?: string;
  email?: string;
}

export interface UpdateBranchRequest {
  branchNameAr: string;
  branchNameEn: string;
  branchTypeId: string;
  isHeadquarterFl?: number;
  statusId: string;
  addressLine1?: string;
  addressLine2?: string;
  cityName?: string;
  phone?: string;
  email?: string;
}

export interface CreateDepartmentRequest {
  branchFk: number;
  departmentNameAr: string;
  departmentNameEn: string;
  departmentTypeId: string;
  activeFl?: number;
}

export interface UpdateDepartmentRequest {
  departmentNameAr?: string;
  departmentNameEn?: string;
  departmentTypeId?: string;
}
