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

export interface RegionListItemDto {
  regionPk: number;
  regionCode: string;
  regionNameAr: string;
  regionNameEn?: string;
  legalEntityFk?: number;
  legalEntityDisplay?: string;
  statusId: string;
  activeFl?: number;
}

// ─── Full Detail DTO ──────────────────────────────────────────────────────────

export interface RegionDto {
  regionPk: number;
  regionCode: string;
  regionNameAr: string;
  regionNameEn?: string;
  legalEntityFk?: number;
  descriptionAr?: string;
  statusId: string;
  activeFl?: number;
  createdAt?: string;
  createdBy?: string;
  updatedAt?: string;
  updatedBy?: string;
}

// ─── Usage DTO ────────────────────────────────────────────────────────────────

export interface RegionUsageDto {
  canBeDeleted: boolean;
  canDeactivate: boolean;
}

// ─── Requests ─────────────────────────────────────────────────────────────────

export interface CreateRegionRequest {
  legalEntityFk: number;
  regionNameAr: string;
  regionNameEn: string;
  descriptionAr?: string;
  statusId: string;
}

export interface UpdateRegionRequest {
  regionNameAr: string;
  regionNameEn: string;
  descriptionAr?: string;
  statusId: string;
}
