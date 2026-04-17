import { PagedResponse } from 'src/app/shared/models/paged-response.model';

export interface RoleDto {
  id: number;
  roleCode: string;
  roleName: string;
  description?: string;
  active: boolean;
  createdAt?: string;
}

export type RolePagedResponse = PagedResponse<RoleDto>;

/**
 * Contract filter matching backend BaseSearchContractRequest.ContractFilter
 */
export interface ContractFilter {
  field: string;
  operator: string;
  value?: string | number | boolean | Array<string | number>;
}

/**
 * Sort entry matching backend BaseSearchContractRequest.ContractSort
 */
export interface ContractSort {
  field: string;
  direction: 'ASC' | 'DESC';
}

/**
 * Search request body matching backend BaseSearchContractRequest contract.
 */
export interface RoleSearchRequest {
  filters: ContractFilter[];
  sorts?: ContractSort[];
  page: number;
  size: number;
}

export interface RolePagePermissionDto {
  pageCode:    string;
  pageName?:   string;   // English page name
  pageNameAr?: string;   // Arabic page name
  create:  boolean;
  update:  boolean;
  delete:  boolean;
}

export interface RolePagesResponse {
  roleId: number;
  assignments: RolePagePermissionDto[];
}

export interface ActivePageDto {
  pageCode: string;
  nameAr?: string;
  nameEn?: string;
  active: boolean;
}

export interface CreateRoleRequest {
  roleCode: string;
  roleName: string;
  description?: string;
  active?: boolean;
}

export interface ToggleRoleActiveRequest {
  active: boolean;
}

export interface AddRolePagesRequestItem {
  pageCode: string;
  create?: boolean;
  update?: boolean;
  delete?: boolean;
}

export interface AddPageToRoleRequest {
  pageCode: string;
  permissions: Array<'CREATE' | 'UPDATE' | 'DELETE'>;
}

export interface SyncRolePagesRequest {
  assignments: Array<{
    pageCode: string;
    permissions: Array<'CREATE' | 'UPDATE' | 'DELETE'>;
  }>;
}
