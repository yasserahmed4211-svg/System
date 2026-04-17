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

export interface LegalEntityListItemDto {
  legalEntityPk: number;
  legalEntityCode: string;
  legalEntityNameAr: string;
  legalEntityNameEn?: string;
  countryFk?: number;
  countryDisplay?: string;
  functionalCurrencyFk?: number;
  currencyDisplay?: string;
  statusId?: string;
  activeFl?: number;
}

// ─── Full Detail DTO ──────────────────────────────────────────────────────────

export interface LegalEntityDto {
  legalEntityPk: number;
  legalEntityCode: string;
  legalEntityNameAr: string;
  legalEntityNameEn?: string;
  countryFk?: number;
  functionalCurrencyFk?: number;
  taxNumber?: string;
  commercialRegNumber?: string;
  fiscalYearStartMonth?: number;
  addressLine1?: string;
  addressLine2?: string;
  cityName?: string;
  phone?: string;
  email?: string;
  website?: string;
  statusId: string;
  activeFl?: number;
  createdAt?: string;
  createdBy?: string;
  updatedAt?: string;
  updatedBy?: string;
}

// ─── Usage DTO ────────────────────────────────────────────────────────────────

export interface LegalEntityUsageDto {
  canBeDeleted: boolean;
  canDeactivate: boolean;
  hasFinancialTransactions?: boolean;
}

// ─── Requests ─────────────────────────────────────────────────────────────────

export interface CreateLegalEntityRequest {
  legalEntityNameAr: string;
  legalEntityNameEn: string;
  countryFk: number;
  functionalCurrencyFk: number;
  taxNumber?: string;
  commercialRegNumber?: string;
  fiscalYearStartMonth?: number;
  addressLine1?: string;
  addressLine2?: string;
  cityName?: string;
  phone?: string;
  email?: string;
  website?: string;
  statusId: string;
}

export interface UpdateLegalEntityRequest {
  legalEntityNameAr: string;
  legalEntityNameEn: string;
  taxNumber?: string;
  commercialRegNumber?: string;
  fiscalYearStartMonth?: number;
  addressLine1?: string;
  addressLine2?: string;
  cityName?: string;
  phone?: string;
  email?: string;
  website?: string;
  statusId: string;
}
