import { Injectable, inject, signal, computed } from '@angular/core';
import { Observable, catchError, tap, of, map, finalize } from 'rxjs';

import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { UserApiService } from '../services/user-api.service';
import {
  UserDto,
  SearchFilter,
  SearchRequest,
  CreateUserRequest,
  UpdateUserRequest,
  CustomFilter,
  FilterOperator
} from '../models/user.model';

import { SpecFilter, SpecOperator } from 'src/app/shared/models';
import { ErpErrorMapperService } from 'src/app/shared/services/erp-error-mapper.service';
import { extractBackendErrorCode } from 'src/app/shared/utils/backend-error-message';

export interface UserListState {
  users: UserDto[];
  totalElements: number;
  loading: boolean;
  error: string | null;
}

@Injectable()
export class UserFacade {
  private userApiService = inject(UserApiService);
  private authService = inject(AuthenticationService);
  private readonly errorMapper = inject(ErpErrorMapperService);

  // State signals
  private usersSignal = signal<UserDto[]>([]);
  private totalElementsSignal = signal<number>(0);
  private loadingSignal = signal<boolean>(false);
  private errorSignal = signal<string | null>(null);
  private actionErrorSignal = signal<string | null>(null);
  private availableRolesSignal = signal<string[]>([]);
  private rolesLoadingSignal = signal<boolean>(false);

  /** Single source of truth for the last search request sent to the API. */
  private lastSearchRequestSignal = signal<SearchRequest>({
    filters: [],
    page: 0,
    size: 20,
    sortBy: 'id',
    sortDir: 'ASC'
  });

  // Filter and sort state
  private currentFiltersSignal = signal<SearchFilter[]>([]);
  private currentSortSignal = signal<{ field: string; direction: string } | null>(null);

  // Computed state
  readonly users = computed(() => this.usersSignal());
  readonly totalElements = computed(() => this.totalElementsSignal());
  readonly loading = computed(() => this.loadingSignal());
  readonly error = computed(() => this.errorSignal());
  readonly actionError = computed(() => this.actionErrorSignal());
  readonly availableRoles = computed(() => this.availableRolesSignal());
  readonly rolesLoading = computed(() => this.rolesLoadingSignal());
  readonly currentPage = computed(() => this.lastSearchRequestSignal().page);
  readonly pageSize = computed(() => this.lastSearchRequestSignal().size);

  loadUsers(): void {
    const token = this.authService.getToken();
    if (!token) {
      this.errorSignal.set('ERRORS.UNAUTHORIZED');
      return;
    }

    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.actionErrorSignal.set(null);

    const searchRequest: SearchRequest = {
      filters: this.currentFiltersSignal(),
      page: this.lastSearchRequestSignal().page,
      size: this.lastSearchRequestSignal().size,
      sortBy: this.currentSortSignal()?.field || 'id',
      sortDir: this.currentSortSignal()?.direction || 'ASC'
    };
    this.lastSearchRequestSignal.set(searchRequest);

    this.userApiService.searchUsers(searchRequest, token).pipe(
      tap(response => {
        // Handle both wrapped {data:{content:...}} and direct {content:...} responses
        const page = (response as any).data || response;

        // Normalize roles to string array if they come as objects
        const normalizedUsers = (page.content || []).map((user: any) => ({
          ...user,
          roles: Array.isArray(user.roles)
            ? user.roles
                .map((r: any) => {
                  if (typeof r === 'string') return r;
                  return (r.roleCode ?? r.roleName ?? r.name) as string | undefined;
                })
                .filter((v: unknown): v is string => typeof v === 'string' && (v as string).trim().length > 0)
            : []
        }));

        this.usersSignal.set(normalizedUsers);
        this.totalElementsSignal.set(page.totalElements || 0);
      }),
      catchError(error => {
        const backendCode = extractBackendErrorCode(error);
        const mappedKey =
          backendCode && this.errorMapper.hasMapping(backendCode)
            ? this.errorMapper.mapError(backendCode).translationKey
            : null;
        this.errorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
        this.usersSignal.set([]);
        this.totalElementsSignal.set(0);
        return of(null);
      }),
      finalize(() => this.loadingSignal.set(false))
    ).subscribe();
  }

  createUser(request: CreateUserRequest): Observable<UserDto | null> {
    const token = this.authService.getToken();
    if (!token) {
      this.actionErrorSignal.set('ERRORS.UNAUTHORIZED');
      return of(null);
    }

    this.actionErrorSignal.set(null);
    return this.userApiService.createUser(request, token).pipe(
      tap(() => this.loadUsers()),
      catchError(error => {
        const backendCode = extractBackendErrorCode(error);
        const mappedKey =
          backendCode && this.errorMapper.hasMapping(backendCode)
            ? this.errorMapper.mapError(backendCode).translationKey
            : null;
        this.actionErrorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
        return of(null);
      })
    );
  }

  updateUser(userId: number, request: UpdateUserRequest): Observable<UserDto | null> {
    const token = this.authService.getToken();
    if (!token) {
      this.actionErrorSignal.set('ERRORS.UNAUTHORIZED');
      return of(null);
    }

    this.actionErrorSignal.set(null);
    return this.userApiService.updateUser(userId, request, token).pipe(
      tap(() => this.loadUsers()),
      catchError(error => {
        const backendCode = extractBackendErrorCode(error);
        const mappedKey =
          backendCode && this.errorMapper.hasMapping(backendCode)
            ? this.errorMapper.mapError(backendCode).translationKey
            : null;
        this.actionErrorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
        return of(null);
      })
    );
  }

  deleteUser(userId: number): Observable<void | null> {
    const token = this.authService.getToken();
    if (!token) {
      this.actionErrorSignal.set('ERRORS.UNAUTHORIZED');
      return of(null);
    }

    this.actionErrorSignal.set(null);
    return this.userApiService.deleteUser(userId, token).pipe(
      tap(() => this.loadUsers()),
      catchError(error => {
        const backendCode = extractBackendErrorCode(error);
        const mappedKey =
          backendCode && this.errorMapper.hasMapping(backendCode)
            ? this.errorMapper.mapError(backendCode).translationKey
            : null;
        this.actionErrorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
        return of(null);
      })
    );
  }

  loadAvailableRoles(): void {
    const token = this.authService.getToken();
    if (!token) {
      this.availableRolesSignal.set([]);
      this.rolesLoadingSignal.set(false);
      return;
    }

    this.rolesLoadingSignal.set(true);
    this.userApiService.getRoles(token, 1000).pipe(
      map((response): string[] => {
        // Handle both wrapped and unwrapped responses
        const data = (response as any).data || response;
        
        if (data && data.content && Array.isArray(data.content)) {
          const roleNames: string[] = data.content
            .map((role: any) => (role?.roleCode ?? role?.roleName ?? role?.name) as string)
            .filter((name: string) => name && name.trim().length > 0)
            .sort(); // Sort alphabetically
          
          return roleNames;
        }

        return [];
      }),
      tap((roleNames: string[]) => this.availableRolesSignal.set(roleNames)),
      catchError((error) => {
        this.availableRolesSignal.set([]);
        return of([]);
      }),
      finalize(() => this.rolesLoadingSignal.set(false))
    ).subscribe();
  }

  private extractRolesFromUsers(): void {
    const uniqueRoles = new Set<string>();
    this.usersSignal().forEach(user => {
      if (user.roles && Array.isArray(user.roles)) {
        user.roles.forEach(role => {
          const roleName = typeof role === 'string'
            ? role
            : ((role as any).roleCode ?? (role as any).roleName ?? (role as any).name);
          if (typeof roleName === 'string' && roleName.trim().length > 0) uniqueRoles.add(roleName);
        });
      }
    });

    if (uniqueRoles.size > 0) {
      this.availableRolesSignal.set(Array.from(uniqueRoles).sort());
    } else {
      this.availableRolesSignal.set(['ROLE_USER', 'ROLE_ADMIN', 'ROLE_MANAGER']);
    }
  }

  setFilters(filters: SearchFilter[]): void {
    this.currentFiltersSignal.set(filters);
    this.lastSearchRequestSignal.update(r => ({ ...r, filters, page: 0 }));
  }

  setSort(field: string, direction: string): void {
    this.currentSortSignal.set({ field, direction });
  }

  clearSort(): void {
    this.currentSortSignal.set(null);
  }

  setPage(page: number): void {
    this.lastSearchRequestSignal.update(r => ({ ...r, page }));
  }

  refreshData(): void {
    this.currentFiltersSignal.set([]);
    this.currentSortSignal.set(null);
    this.lastSearchRequestSignal.update(r => ({ ...r, page: 0 }));
    this.loadUsers();
  }

  /**
   * New shared-architecture entry point: apply list state in one shot and load once.
   * This avoids duplicate loads when the UI updates multiple state dimensions.
   */
  applyGridStateAndLoad(state: {
    page: number;
    size: number;
    sortBy: string;
    sortDir: string;
    filters: SearchFilter[];
  }): void {
    this.lastSearchRequestSignal.set({
      filters: state.filters,
      page: state.page,
      size: state.size,
      sortBy: state.sortBy,
      sortDir: state.sortDir
    });
    this.currentSortSignal.set({ field: state.sortBy, direction: state.sortDir });
    this.currentFiltersSignal.set(state.filters);
    this.loadUsers();
  }

  /** Clear transient entity state when leaving the page. */
  clearCurrentEntity(): void {
    this.usersSignal.set([]);
    this.totalElementsSignal.set(0);
    this.errorSignal.set(null);
    this.actionErrorSignal.set(null);
  }

  /** Adapter from shared SpecFilter model to backend SearchFilter model. */
  convertSpecFiltersToSearchFilters(filters: SpecFilter[]): SearchFilter[] {
    return filters
      .map((f) => ({
        field: f.field,
        op: this.mapSpecOperator(f.operator),
        value: f.value
      }))
      .filter((f) => !!f.field && !!f.op)
      .map((f) => ({
        field: f.field,
        op: f.op,
        value: this.normalizeSearchValue(f.field, f.op, f.value)
      }));
  }

  private mapSpecOperator(op: SpecOperator): FilterOperator {
    const map: Record<SpecOperator, FilterOperator> = {
      eq: 'EQ',
      like: 'LIKE',
      gt: 'GT',
      lt: 'LT',
      in: 'IN',
      isNull: 'IS_NULL',
      isNotNull: 'IS_NOT_NULL'
    };
    return map[op];
  }

  private normalizeSearchValue(
    field: string,
    op: FilterOperator,
    value: SpecFilter['value']
  ): SearchFilter['value'] {
    if (op === 'IS_NULL' || op === 'IS_NOT_NULL') return undefined;

    if (op === 'IN') {
      if (Array.isArray(value)) return value as string[];
      if (typeof value === 'string') {
        return value
          .split(',')
          .map((v) => v.trim())
          .filter((v) => v.length > 0);
      }
      return value as any;
    }

    if (field === 'enabled') {
      if (value === true || value === 'true') return true;
      if (value === false || value === 'false') return false;
    }

    if (field === 'id' && value !== null && value !== undefined && value !== '') {
      return Number(value);
    }

    return value as any;
  }

  convertAgGridFiltersToSearchFilters(filterModel: Record<string, AgGridFilter>): SearchFilter[] {
    const filters: SearchFilter[] = [];
    const fieldMap: Record<string, string> = {
      'id': 'id',
      'username': 'username',
      'tenantId': 'tenantId',
      'enabled': 'enabled'
    };

    Object.keys(filterModel).forEach(field => {
      const filter = filterModel[field];
      const apiField = fieldMap[field] || field;

      if (filter.filterType === 'text') {
        let filterValue: string | boolean = filter.filter;
        
        if (apiField === 'enabled') {
          filterValue = filter.filter === 'true';
        }
        
        const op = this.mapTextFilterType(filter.type);
        if (op) {
          filters.push({ field: apiField, op, value: filterValue });
        }
      } else if (filter.filterType === 'number') {
        const op = this.mapNumberFilterType(filter.type);
        if (op) {
          if (filter.type === 'inRange') {
            filters.push({ field: apiField, op, value: filter.filter, value2: filter.filterTo });
          } else {
            filters.push({ field: apiField, op, value: filter.filter });
          }
        }
      }
    });

    return filters;
  }

  private mapTextFilterType(type: string): FilterOperator | null {
    const map: Record<string, FilterOperator> = {
      'equals': 'EQ',
      'notEqual': 'NE',
      'contains': 'LIKE',
      'startsWith': 'LIKE',
      'endsWith': 'LIKE'
    };
    return map[type] || null;
  }

  private mapNumberFilterType(type: string): FilterOperator | null {
    const map: Record<string, FilterOperator> = {
      'equals': 'EQ',
      'notEqual': 'NE',
      'greaterThan': 'GT',
      'greaterThanOrEqual': 'GE',
      'lessThan': 'LT',
      'lessThanOrEqual': 'LE',
      'inRange': 'BETWEEN'
    };
    return map[type] || null;
  }

  convertCustomFiltersToSearchFilters(customFilters: CustomFilter[]): SearchFilter[] {
    return customFilters
      .filter(cf => {
        if (cf.op === 'IS_NULL' || cf.op === 'IS_NOT_NULL') {
          return true;
        }
        return cf.value !== null && cf.value !== undefined && cf.value !== '';
      })
      .map(cf => ({
        field: cf.field,
        op: cf.op,
        value: this.convertFilterValue(cf)
      }));
  }

  private convertFilterValue(filter: CustomFilter): string | number | boolean | string[] | undefined {
    if (filter.op === 'IS_NULL' || filter.op === 'IS_NOT_NULL') {
      return undefined;
    }

    if (filter.op === 'IN' && typeof filter.value === 'string') {
      return filter.value.split(',').map(v => v.trim());
    }

    if (filter.field === 'enabled') {
      if (filter.value === 'true' || filter.value === true) return true;
      if (filter.value === 'false' || filter.value === false) return false;
    }

    if (filter.field === 'id') {
      return Number(filter.value);
    }

    return filter.value;
  }
}

interface AgGridFilter {
  filterType: string;
  type: string;
  filter: string;
  filterTo?: number;
}
