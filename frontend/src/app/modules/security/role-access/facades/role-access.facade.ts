import { Injectable, inject, signal, computed } from '@angular/core';
import { catchError, concatMap, finalize, from, last, map, of, tap } from 'rxjs';

import { ErpErrorMapperService } from 'src/app/shared/services/erp-error-mapper.service';
import { extractBackendErrorCode } from 'src/app/shared/utils/backend-error-message';

import {
  ActivePageDto,
  AddRolePagesRequestItem,
  ContractFilter,
  CreateRoleRequest,
  RoleDto,
  RolePagePermissionDto,
  RoleSearchRequest
} from '../models/role-access.model';
import { RoleAccessApiService } from '../services/role-access-api.service';

type CrudPermission = 'CREATE' | 'UPDATE' | 'DELETE';

export interface RoleSearchFilter {
  field: string;
  op: 'EQ' | 'LIKE' | 'BETWEEN';
  value?: string | number | boolean;
  value2?: string | number;
}

@Injectable()
export class RoleAccessFacade {
  private readonly api = inject(RoleAccessApiService);
  private readonly errorMapper = inject(ErpErrorMapperService);

  private rolesSignal = signal<RoleDto[]>([]);
  private totalElementsSignal = signal<number>(0);
  private loadingSignal = signal<boolean>(false);
  private errorSignal = signal<string | null>(null);

  /** Single source of truth for the last search request sent to the API. */
  private lastSearchRequestSignal = signal<RoleSearchRequest>({
    filters: [],
    sorts: [{ field: 'roleName', direction: 'ASC' }],
    page: 0,
    size: 20
  });
  private currentFiltersSignal = signal<RoleSearchFilter[]>([]);
  private currentSortSignal = signal<{ field: string; direction: string } | null>({ field: 'roleName', direction: 'ASC' });

  private savingSignal = signal<boolean>(false);
  private saveErrorSignal = signal<string | null>(null);

  private selectedRoleSignal = signal<RoleDto | null>(null);
  private rolePagesSignal = signal<RolePagePermissionDto[]>([]);
  private activePagesSignal = signal<ActivePageDto[]>([]);

  readonly roles = computed(() => this.rolesSignal());
  readonly totalElements = computed(() => this.totalElementsSignal());
  readonly loading = computed(() => this.loadingSignal());
  readonly error = computed(() => this.errorSignal());

  readonly currentPage = computed(() => this.lastSearchRequestSignal().page);
  readonly pageSize = computed(() => this.lastSearchRequestSignal().size);
  readonly currentFilters = computed(() => this.currentFiltersSignal());

  readonly saving = computed(() => this.savingSignal());
  readonly saveError = computed(() => this.saveErrorSignal());

  readonly selectedRole = computed(() => this.selectedRoleSignal());
  readonly rolePages = computed(() => this.rolePagesSignal());
  readonly activePages = computed(() => this.activePagesSignal());

  loadRoles(): void {
    this.executeSearch(this.lastSearchRequestSignal());
  }

  /**
   * Execute a search request against the API.
   * Stores the request as the last search request for future reloads.
   * Size is capped at MAX_PAGE_SIZE to match backend validation.
   */
  private static readonly MAX_PAGE_SIZE = 50;

  private executeSearch(request: RoleSearchRequest): void {
    const safePage = Math.max(0, request.page);
    const safeSize = Math.min(Math.max(1, request.size), RoleAccessFacade.MAX_PAGE_SIZE);
    const safeRequest = { ...request, page: safePage, size: safeSize };

    this.lastSearchRequestSignal.set(safeRequest);
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    this.api
      .searchRoles(safeRequest)
      .pipe(
        tap((response) => {
          this.rolesSignal.set(response?.content ?? []);
          this.totalElementsSignal.set(response?.totalElements ?? 0);
        }),
        catchError((error) => {
          const backendCode = extractBackendErrorCode(error);
          const mappedKey =
            backendCode && this.errorMapper.hasMapping(backendCode)
              ? this.errorMapper.mapError(backendCode).translationKey
              : null;
          this.errorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
          this.rolesSignal.set([]);
          this.totalElementsSignal.set(0);
          return of(null);
        }),
        finalize(() => this.loadingSignal.set(false))
      )
      .subscribe();
  }

  applyGridStateAndLoad(state: {
    page: number;
    size: number;
    sortBy: string;
    sortDir: string;
    filters: RoleSearchFilter[];
  }): void {
    this.currentFiltersSignal.set(state.filters);
    this.currentSortSignal.set({ field: state.sortBy, direction: state.sortDir });
    const contractFilters = this.toContractFilters(state.filters);
    const request: RoleSearchRequest = {
      filters: contractFilters,
      sorts: state.sortBy ? [{ field: state.sortBy, direction: state.sortDir as 'ASC' | 'DESC' }] : [],
      page: state.page,
      size: state.size
    };
    this.executeSearch(request);
  }

  setFilters(filters: RoleSearchFilter[]): void {
    this.currentFiltersSignal.set(filters);
    const contractFilters = this.toContractFilters(filters);
    this.lastSearchRequestSignal.update(r => ({ ...r, filters: contractFilters, page: 0 }));
  }

  setPage(page: number): void {
    this.lastSearchRequestSignal.update(r => ({ ...r, page }));
  }

  setSize(size: number): void {
    this.lastSearchRequestSignal.update(r => ({ ...r, size, page: 0 }));
  }

  /** Clear transient entity state when leaving the page. */
  clearCurrentEntity(): void {
    this.selectedRoleSignal.set(null);
    this.rolePagesSignal.set([]);
    this.activePagesSignal.set([]);
    this.errorSignal.set(null);
    this.saveErrorSignal.set(null);
  }

  /** Convert domain RoleSearchFilter[] to backend ContractFilter[] format. */
  private toContractFilters(filters: RoleSearchFilter[]): ContractFilter[] {
    const result: ContractFilter[] = [];
    for (const f of filters) {
      if (f.field === 'search' && f.value !== undefined && f.value !== '') {
        result.push({ field: 'roleName', operator: 'CONTAINS', value: f.value });
      } else if (f.field === 'active' && f.value !== undefined) {
        result.push({ field: 'active', operator: 'EQUALS', value: f.value });
      }
    }
    return result;
  }

  createRole(request: CreateRoleRequest, onSuccess?: (role: RoleDto) => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api
      .createRole(request)
      .pipe(
        tap((role) => {
          if (role?.id) {
            onSuccess?.(role);
            this.loadRoles();
          } else {
            this.saveErrorSignal.set('ERRORS.OPERATION_FAILED');
          }
        }),
        catchError((error) => {
          const backendCode = extractBackendErrorCode(error);
          const mappedKey =
            backendCode && this.errorMapper.hasMapping(backendCode)
              ? this.errorMapper.mapError(backendCode).translationKey
              : null;
          this.saveErrorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
          return of(null);
        }),
        finalize(() => this.savingSignal.set(false))
      )
      .subscribe();
  }

  private toCrudPermissions(flags: { create?: boolean; update?: boolean; delete?: boolean }): CrudPermission[] {
    const result: CrudPermission[] = [];
    if (flags.create) result.push('CREATE');
    if (flags.update) result.push('UPDATE');
    if (flags.delete) result.push('DELETE');
    return result;
  }

  toggleRoleActive(role: RoleDto, active: boolean, onSuccess?: (updated: RoleDto) => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api
      .toggleRoleActive(role.id, { active })
      .pipe(
        tap((updated) => {
          onSuccess?.(updated);
          this.loadRoles();
        }),
        catchError((error) => {
          const backendCode = extractBackendErrorCode(error);
          const mappedKey =
            backendCode && this.errorMapper.hasMapping(backendCode)
              ? this.errorMapper.mapError(backendCode).translationKey
              : null;
          this.saveErrorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
          return of(null);
        }),
        finalize(() => this.savingSignal.set(false))
      )
      .subscribe();
  }

  deleteRole(roleId: number, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api
      .deleteRole(roleId)
      .pipe(
        tap(() => {
          onSuccess?.();
          this.loadRoles();
        }),
        catchError((error) => {
          const backendCode = extractBackendErrorCode(error);
          const mappedKey =
            backendCode && this.errorMapper.hasMapping(backendCode)
              ? this.errorMapper.mapError(backendCode).translationKey
              : null;
          this.saveErrorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
          return of(null);
        }),
        finalize(() => this.savingSignal.set(false))
      )
      .subscribe();
  }

  loadRoleDetails(roleId: number): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    this.api
      .getRoleById(roleId)
      .pipe(
        tap((role) => this.selectedRoleSignal.set(role)),
        catchError((error) => {
          const backendCode = extractBackendErrorCode(error);
          const mappedKey =
            backendCode && this.errorMapper.hasMapping(backendCode)
              ? this.errorMapper.mapError(backendCode).translationKey
              : null;
          this.errorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
          this.selectedRoleSignal.set(null);
          return of(null);
        }),
        finalize(() => this.loadingSignal.set(false))
      )
      .subscribe();
  }

  loadActivePages(): void {
    this.api
      .getActivePages()
      .pipe(
        tap((pages) => this.activePagesSignal.set(pages ?? [])),
        catchError(() => {
          this.activePagesSignal.set([]);
          return of([]);
        })
      )
      .subscribe();
  }

  loadRolePages(roleId: number): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    this.api
      .getRolePages(roleId)
      .pipe(
        tap((resp) => {
          const assignments = resp?.assignments ?? [];
          const uniqueByPageCode = new Map<string, RolePagePermissionDto>();
          for (const a of assignments) {
            const key = String(a?.pageCode ?? '').trim().toUpperCase();
            if (!key) continue;
            uniqueByPageCode.set(key, { ...a, pageCode: key });
          }
          this.rolePagesSignal.set(Array.from(uniqueByPageCode.values()));
        }),
        catchError((error) => {
          const backendCode = extractBackendErrorCode(error);
          const mappedKey =
            backendCode && this.errorMapper.hasMapping(backendCode)
              ? this.errorMapper.mapError(backendCode).translationKey
              : null;
          this.errorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
          this.rolePagesSignal.set([]);
          return of(null);
        }),
        finalize(() => this.loadingSignal.set(false))
      )
      .subscribe();
  }

  addRolePages(roleId: number, items: AddRolePagesRequestItem[], onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    const normalized = (items ?? [])
      .map((i) => ({
        pageCode: String(i.pageCode ?? '').trim().toUpperCase(),
        create: !!i.create,
        update: !!i.update,
        delete: !!i.delete
      }))
      .filter((i) => i.pageCode.length > 0);

    const uniqueByPageCode = new Map<string, AddRolePagesRequestItem>();
    for (const item of normalized) uniqueByPageCode.set(item.pageCode, item);
    const deduped = Array.from(uniqueByPageCode.values());

    if (deduped.length === 0) {
      this.saveErrorSignal.set('ERRORS.OPERATION_FAILED');
      this.savingSignal.set(false);
      return;
    }

    from(deduped)
      .pipe(
        concatMap((item: AddRolePagesRequestItem) =>
          this.api.addPageToRole(roleId, {
            pageCode: String(item.pageCode),
            permissions: this.toCrudPermissions(item)
          })
        ),
        last(),
        map(() => null)
      )
      .pipe(
        tap(() => {
          onSuccess?.();
          this.loadRolePages(roleId);
        }),
        catchError((error) => {
          const backendCode = extractBackendErrorCode(error);
          const mappedKey =
            backendCode && this.errorMapper.hasMapping(backendCode)
              ? this.errorMapper.mapError(backendCode).translationKey
              : null;
          this.saveErrorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
          return of(null);
        }),
        finalize(() => this.savingSignal.set(false))
      )
      .subscribe();
  }

  syncRolePages(roleId: number, assignments: RolePagePermissionDto[], onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    const normalized = (assignments ?? [])
      .map((a) => ({
        pageCode: String(a.pageCode ?? '').trim().toUpperCase(),
        create: !!a.create,
        update: !!a.update,
        delete: !!a.delete
      }))
      .filter((a) => a.pageCode.length > 0);

    const uniqueByPageCode = new Map<string, RolePagePermissionDto>();
    for (const a of normalized) uniqueByPageCode.set(a.pageCode, a);
    const deduped = Array.from(uniqueByPageCode.values());

    this.api
      .syncRolePages(roleId, {
        assignments: deduped.map((a) => ({
          pageCode: a.pageCode,
          permissions: this.toCrudPermissions(a)
        }))
      })
      .pipe(
        tap(() => {
          onSuccess?.();
          this.loadRolePages(roleId);
        }),
        catchError((error) => {
          const backendCode = extractBackendErrorCode(error);
          const mappedKey =
            backendCode && this.errorMapper.hasMapping(backendCode)
              ? this.errorMapper.mapError(backendCode).translationKey
              : null;
          this.saveErrorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
          return of(null);
        }),
        finalize(() => this.savingSignal.set(false))
      )
      .subscribe();
  }

  removeRolePage(roleId: number, pageCode: string, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.api
      .removeRolePage(roleId, pageCode)
      .pipe(
        tap(() => {
          onSuccess?.();
          this.loadRolePages(roleId);
        }),
        catchError((error) => {
          const backendCode = extractBackendErrorCode(error);
          const mappedKey =
            backendCode && this.errorMapper.hasMapping(backendCode)
              ? this.errorMapper.mapError(backendCode).translationKey
              : null;
          this.saveErrorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
          return of(null);
        }),
        finalize(() => this.savingSignal.set(false))
      )
      .subscribe();
  }

  copyFromRole(roleId: number, sourceRoleId: number, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    if (!Number.isFinite(roleId) || roleId <= 0 || !Number.isFinite(sourceRoleId) || sourceRoleId <= 0) {
      this.saveErrorSignal.set('ERRORS.OPERATION_FAILED');
      this.savingSignal.set(false);
      return;
    }

    this.api
      .copyFromRole(roleId, sourceRoleId)
      .pipe(
        tap(() => {
          onSuccess?.();
          this.loadRolePages(roleId);
        }),
        catchError((error) => {
          const backendCode = extractBackendErrorCode(error);
          const mappedKey =
            backendCode && this.errorMapper.hasMapping(backendCode)
              ? this.errorMapper.mapError(backendCode).translationKey
              : null;
          this.saveErrorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
          return of(null);
        }),
        finalize(() => this.savingSignal.set(false))
      )
      .subscribe();
  }
}
