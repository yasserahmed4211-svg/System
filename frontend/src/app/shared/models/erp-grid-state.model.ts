import { SpecFilter } from './spec-filter.model';

export type ErpSortDirection = 'ASC' | 'DESC';

export interface ErpGridState {
  page: number;
  size: number;
  sort?: string;
  direction?: ErpSortDirection;
  filters: SpecFilter[];
}
