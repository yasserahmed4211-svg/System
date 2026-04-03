export interface UserDto {
  id: number;
  username: string;
  tenantId?: string;
  enabled: boolean;
  roles: string[] | RoleDto[]; // Support both formats
  permissions: string[];
}

export interface RoleDto {
  id: number;
  tenantId: string;
  name: string;
}

export interface PageUserDto {
  content: UserDto[];
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

export interface CreateUserRequest {
  username: string;
  password: string;
  tenantId?: string;
  enabled?: boolean;
  roleNames?: string[];
}

export interface UpdateUserRequest {
  username: string;
  password?: string;
  tenantId?: string;
  enabled?: boolean;
  roleNames?: string[];
}

export interface RoleDto {
  id: number;
  name: string;
  description?: string;
}

export interface PageRoleDto {
  content: RoleDto[];
  totalElements: number;
  totalPages: number;
}

export interface CustomFilter {
  id: number;
  field: string;
  op: FilterOperator;
  value: string | number | boolean;
}
