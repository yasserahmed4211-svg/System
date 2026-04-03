// Master Lookup DTOs and Models per API Contract: master-lookup.contract.md

// ============================================
// MASTER LOOKUP TYPES
// ============================================

export interface MasterLookupDto {
  id: number;
  lookupKey: string;
  lookupName: string;
  lookupNameEn?: string;
  description?: string;
  isActive: boolean;
  detailCount?: number;
  createdAt?: string;
  createdBy?: string;
  updatedAt?: string;
  updatedBy?: string;
}

export interface MasterLookupUsageDto {
  masterLookupId: number;
  lookupKey: string;
  totalDetails: number;
  activeDetails: number;
  canDelete: boolean;
  canDeactivate: boolean;
}

export interface CreateMasterLookupRequest {
  lookupKey: string;
  lookupName: string;
  lookupNameEn?: string;
  description?: string;
  isActive?: boolean;
}

export interface UpdateMasterLookupRequest {
  lookupName: string;
  lookupNameEn?: string;
  description?: string;
}

// ============================================
// LOOKUP DETAIL TYPES
// ============================================

export interface LookupDetailDto {
  id: number;
  masterLookupId: number;
  masterLookupKey: string;
  masterLookupName?: string;
  code: string;
  nameAr: string;
  nameEn?: string;
  extraValue?: string;
  sortOrder?: number;
  isActive: boolean;
  createdAt?: string;
  createdBy?: string;
  updatedAt?: string;
  updatedBy?: string;
}

export interface LookupDetailOptionDto {
  id: number;
  code: string;
  nameAr: string;
  nameEn?: string;
  extraValue?: string;
  sortOrder?: number;
}

export interface CreateLookupDetailRequest {
  masterLookupId: number;
  code: string;
  nameAr: string;
  nameEn?: string;
  extraValue?: string;
  sortOrder?: number;
}

export interface UpdateLookupDetailRequest {
  nameAr: string;
  nameEn?: string;
  extraValue?: string;
  sortOrder?: number;
}

// ============================================
// PAGED RESPONSE & SEARCH
// ============================================

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

export interface SearchFilter {
  field: string;
  operator: FilterOperator;
  value?: string | number | boolean | string[];
}

export type FilterOperator = 'EQUALS' | 'CONTAINS' | 'STARTS_WITH';

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
