// Page DTOs and Models for Pages Registry Feature

export interface PageDto {
  id: number;
  pageCode: string;
  nameAr: string;
  nameEn: string;
  route: string;
  icon?: string;
  module?: string;
  parentId?: number;
  displayOrder: number;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}

// Re-export shared ApiResponse – keeps existing imports working
export { ApiResponse } from 'src/app/shared/models/api-response.model';

export interface SearchFilter {
  field: string;
  op: FilterOperator;
  value?: string | number | boolean | string[];
  value2?: string | number;
}

export type FilterOperator = 'EQ' | 'NE' | 'GT' | 'GE' | 'LT' | 'LE' | 'LIKE' | 'IN' | 'IS_NULL' | 'IS_NOT_NULL' | 'BETWEEN';

export interface SearchRequest {
  filters: SearchFilter[];
  page: number;
  size: number;
  sortBy?: string;
  sortDir?: string;
}

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
 * Used by POST /search endpoints.
 */
export interface PageSearchRequest {
  filters: ContractFilter[];
  sorts?: ContractSort[];
  page: number;
  size: number;
}

export interface CreatePageRequest {
  pageCode: string;
  nameAr: string;
  nameEn: string;
  route: string;
  icon?: string;
  module?: string;
  parentId?: number;
  displayOrder: number;
  active: boolean;
}

export interface UpdatePageRequest {
  nameAr: string;
  nameEn: string;
  route: string;
  icon?: string;
  module?: string;
  parentId?: number;
  displayOrder: number;
  active: boolean;
}

export interface PageFilterOptions {
  module?: string;
  active?: boolean;
  keyword?: string;
}
