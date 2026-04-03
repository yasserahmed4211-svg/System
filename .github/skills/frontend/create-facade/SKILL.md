---
description: "Generates Signal-based facade for state management and API orchestration. Phase 2, Step 2.3. Private signals + computed public, lastSearchRequestSignal for pagination, takeUntilDestroyed, onSuccess callbacks, child local updates."
---

# Skill: create-facade

## Name
`create-facade`

## Description
Generates the Signal-based facade for state management and API orchestration. The facade owns ALL state via private signals, exposes public computed readonly, and mediates between components and the API service. This is **Phase 2, Step 2.3** of the execution template.

## When to Use
- When implementing a new frontend feature's state management layer
- When the execution template Phase 2, Step 2.3 is being started
- AFTER models and API service are defined, BEFORE components

## Variables (Must Be Defined First)

| Variable | Example | Description |
|----------|---------|-------------|
| `ENTITY_NAME` | `MasterLookup` | PascalCase entity name |
| `ENTITY_KEBAB` | `master-lookup` | kebab-case entity name |
| `DEFAULT_SORT_FIELD` | `lookupKey` | Default sort field for search |
| `HAS_CHILD` | `true/false` | Whether entity has child entities |
| `CHILD_NAME` | `LookupDetail` | PascalCase child name (if applicable) |

---

## Steps

### 1. Create Facade File
- **Location:** `frontend/src/app/modules/<DOMAIN_DIR>/<FEATURE_DIR>/facades/<ENTITY_KEBAB>.facade.ts`

### 2. Class Declaration
```typescript
import { Injectable, signal, computed, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { <ENTITY_NAME>ApiService } from '../services/<ENTITY_KEBAB>-api.service';
import { ErpErrorMapperService } from '../../../../shared/services/erp-error-mapper.service';
import { extractBackendErrorCode } from '../../../../shared/utils/backend-error-message';
import {
  <ENTITY_NAME>Dto,
  <ENTITY_NAME>UsageDto,
  Create<ENTITY_NAME>Request,
  Update<ENTITY_NAME>Request,
  SearchRequest,
  SearchFilter,
  PagedResponse
} from '../models/<ENTITY_KEBAB>.model';
import { tap, catchError, finalize } from 'rxjs/operators';
import { EMPTY } from 'rxjs';

@Injectable()  // NOT providedIn: 'root'
export class <ENTITY_NAME>Facade {

  private readonly apiService = inject(<ENTITY_NAME>ApiService);
  private readonly errorMapper = inject(ErpErrorMapperService);
  private readonly destroyRef = inject(DestroyRef);
```

### 3. Private Signals (State)
```typescript
  // ─── Primary List State ───
  private readonly entitiesSignal = signal<<ENTITY_NAME>Dto[]>([]);
  private readonly totalElementsSignal = signal<number>(0);
  private readonly loadingSignal = signal<boolean>(false);
  private readonly errorSignal = signal<string | null>(null);

  // ─── Write State ───
  private readonly savingSignal = signal<boolean>(false);
  private readonly saveErrorSignal = signal<string | null>(null);

  // ─── Search State ───
  private readonly currentFiltersSignal = signal<SearchFilter[]>([]);
  private readonly lastSearchRequestSignal = signal<SearchRequest>({
    filters: [],
    sorts: [{ field: '<DEFAULT_SORT_FIELD>', direction: 'ASC' }],
    page: 0,
    size: 20
  });

  // ─── Current Entity State ───
  private readonly currentEntitySignal = signal<<ENTITY_NAME>Dto | null>(null);
  private readonly usageInfoSignal = signal<<ENTITY_NAME>UsageDto | null>(null);
```

### 4. Public Computed (Readonly)
```typescript
  // ─── Public Computed ───
  readonly entities = computed(() => this.entitiesSignal());
  readonly totalElements = computed(() => this.totalElementsSignal());
  readonly loading = computed(() => this.loadingSignal());
  readonly error = computed(() => this.errorSignal());
  readonly saving = computed(() => this.savingSignal());
  readonly saveError = computed(() => this.saveErrorSignal());
  readonly currentEntity = computed(() => this.currentEntitySignal());
  readonly usageInfo = computed(() => this.usageInfoSignal());

  // ─── Derived from lastSearchRequestSignal (NOT separate writable signals) ───
  readonly currentPage = computed(() => this.lastSearchRequestSignal().page);
  readonly pageSize = computed(() => this.lastSearchRequestSignal().size);
  readonly currentFilters = computed(() => this.currentFiltersSignal());
```

### 5. List Operations
```typescript
  // ─── List Operations ───

  applyGridStateAndLoad(params: {
    page: number; size: number; sortBy?: string; sortDir?: string; filters?: SearchFilter[];
  }): void {
    const request: SearchRequest = {
      filters: params.filters ?? this.currentFiltersSignal(),
      sorts: params.sortBy
        ? [{ field: params.sortBy, direction: (params.sortDir as 'ASC' | 'DESC') ?? 'ASC' }]
        : this.lastSearchRequestSignal().sorts,
      page: params.page,
      size: params.size
    };
    this.executeSearch(request);
  }

  setFilters(filters: SearchFilter[]): void {
    this.currentFiltersSignal.set(filters);
  }

  private executeSearch(request: SearchRequest): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.lastSearchRequestSignal.set(request);

    this.apiService.search(request).pipe(
      tap((response: PagedResponse<<ENTITY_NAME>Dto>) => {
        this.entitiesSignal.set(response.content);
        this.totalElementsSignal.set(response.totalElements);
      }),
      catchError(error => {
        this.handleError(error, this.errorSignal);
        return EMPTY;
      }),
      finalize(() => this.loadingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }
```

### 6. CRUD Operations
```typescript
  // ─── CRUD Operations ───

  getById(id: number): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    this.apiService.getById(id).pipe(
      tap(entity => this.currentEntitySignal.set(entity)),
      catchError(error => {
        this.handleError(error, this.errorSignal);
        return EMPTY;
      }),
      finalize(() => this.loadingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  create(request: Create<ENTITY_NAME>Request, onSuccess?: (entity: <ENTITY_NAME>Dto) => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.apiService.create(request).pipe(
      tap(entity => {
        this.currentEntitySignal.set(entity);
        onSuccess?.(entity);
      }),
      catchError(error => {
        this.handleError(error, this.saveErrorSignal);
        return EMPTY;
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  update(id: number, request: Update<ENTITY_NAME>Request, onSuccess?: (entity: <ENTITY_NAME>Dto) => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.apiService.update(id, request).pipe(
      tap(entity => {
        this.currentEntitySignal.set(entity);
        onSuccess?.(entity);
      }),
      catchError(error => {
        this.handleError(error, this.saveErrorSignal);
        return EMPTY;
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  toggleActive(id: number, active: boolean, onSuccess?: (entity: <ENTITY_NAME>Dto) => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.apiService.toggleActive(id, active).pipe(
      tap(entity => {
        this.currentEntitySignal.set(entity);
        this.entitiesSignal.update(list =>
          list.map(e => e.id === id ? entity : e)
        );
        onSuccess?.(entity);
      }),
      catchError(error => {
        this.handleError(error, this.saveErrorSignal);
        return EMPTY;
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  deleteEntity(id: number, onSuccess?: () => void): void {
    this.savingSignal.set(true);
    this.saveErrorSignal.set(null);

    this.apiService.delete(id).pipe(
      tap(() => {
        this.entitiesSignal.update(list => list.filter(e => e.id !== id));
        this.totalElementsSignal.update(n => n - 1);
        onSuccess?.();
      }),
      catchError(error => {
        this.handleError(error, this.saveErrorSignal);
        return EMPTY;
      }),
      finalize(() => this.savingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  getUsageInfo(id: number): void {
    this.apiService.getUsage(id).pipe(
      tap(usage => this.usageInfoSignal.set(usage)),
      catchError(error => {
        this.handleError(error, this.errorSignal);
        return EMPTY;
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }
```

### 7. Child Operations (if `HAS_CHILD = true`)
```typescript
  // ─── Child Entity State ───
  private readonly childEntitiesSignal = signal<<CHILD_NAME>Dto[]>([]);
  private readonly childLoadingSignal = signal<boolean>(false);
  private readonly childSavingSignal = signal<boolean>(false);
  private readonly childErrorSignal = signal<string | null>(null);

  readonly childEntities = computed(() => this.childEntitiesSignal());
  readonly childLoading = computed(() => this.childLoadingSignal());
  readonly childSaving = computed(() => this.childSavingSignal());
  readonly childError = computed(() => this.childErrorSignal());

  loadChildren(parentId: number): void {
    this.childLoadingSignal.set(true);
    this.childErrorSignal.set(null);

    const request: SearchRequest = {
      filters: [{ field: 'parentId', operator: 'EQUALS', value: parentId }],
      sorts: [{ field: 'sortOrder', direction: 'ASC' }],
      page: 0,
      size: 50  // Detail list default: 50
    };

    this.apiService.searchChildren(request).pipe(
      tap(response => this.childEntitiesSignal.set(response.content)),
      catchError(error => {
        this.handleError(error, this.childErrorSignal);
        return EMPTY;
      }),
      finalize(() => this.childLoadingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  createChild(request: Create<CHILD_NAME>Request, onSuccess?: (child: <CHILD_NAME>Dto) => void): void {
    this.childSavingSignal.set(true);
    this.childErrorSignal.set(null);

    this.apiService.createChild(request).pipe(
      tap(child => {
        // LOCAL SIGNAL UPDATE — append, no full reload
        this.childEntitiesSignal.update(items => [...items, child]);
        this.refreshUsageInfo();
        onSuccess?.(child);
      }),
      catchError(error => {
        this.handleError(error, this.childErrorSignal);
        return EMPTY;
      }),
      finalize(() => this.childSavingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  updateChild(id: number, request: Update<CHILD_NAME>Request, onSuccess?: (child: <CHILD_NAME>Dto) => void): void {
    this.childSavingSignal.set(true);
    this.childErrorSignal.set(null);

    this.apiService.updateChild(id, request).pipe(
      tap(updated => {
        // LOCAL SIGNAL UPDATE — map in-place, no full reload
        this.childEntitiesSignal.update(items =>
          items.map(d => d.id === id ? updated : d)
        );
        onSuccess?.(updated);
      }),
      catchError(error => {
        this.handleError(error, this.childErrorSignal);
        return EMPTY;
      }),
      finalize(() => this.childSavingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  toggleChildActive(id: number, active: boolean, onSuccess?: (child: <CHILD_NAME>Dto) => void): void {
    this.childSavingSignal.set(true);
    this.childErrorSignal.set(null);

    this.apiService.toggleChildActive(id, active).pipe(
      tap(updated => {
        this.childEntitiesSignal.update(items =>
          items.map(d => d.id === id ? updated : d)
        );
        onSuccess?.(updated);
      }),
      catchError(error => {
        this.handleError(error, this.childErrorSignal);
        return EMPTY;
      }),
      finalize(() => this.childSavingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  deleteChild(id: number, onSuccess?: () => void): void {
    this.childSavingSignal.set(true);
    this.childErrorSignal.set(null);

    this.apiService.deleteChild(id).pipe(
      tap(() => {
        // LOCAL SIGNAL UPDATE — filter out, no full reload
        this.childEntitiesSignal.update(items => items.filter(d => d.id !== id));
        this.refreshUsageInfo();
        onSuccess?.();
      }),
      catchError(error => {
        this.handleError(error, this.childErrorSignal);
        return EMPTY;
      }),
      finalize(() => this.childSavingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  private refreshUsageInfo(): void {
    const entity = this.currentEntitySignal();
    if (entity) {
      this.getUsageInfo(entity.id);
    }
  }

  resetChildState(): void {
    this.childEntitiesSignal.set([]);
    this.childLoadingSignal.set(false);
    this.childErrorSignal.set(null);
  }
```

### 8. Cleanup and Error Handling
```typescript
  // ─── Cleanup ───

  clearCurrentEntity(): void {
    this.currentEntitySignal.set(null);
    this.usageInfoSignal.set(null);
    this.saveErrorSignal.set(null);
    this.errorSignal.set(null);
  }

  // ─── Error Handling ───

  private handleError(error: any, errorSignal: ReturnType<typeof signal<string | null>>): void {
    const backendCode = extractBackendErrorCode(error);
    const mappedKey = backendCode && this.errorMapper.hasMapping(backendCode)
      ? this.errorMapper.mapError(backendCode).translationKey
      : null;
    errorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
  }
}
```

---

## SHARED LAYER MANDATE

Before creating a new facade, verify the following shared resources are consumed — do NOT reinvent:

| # | Requirement | Shared Resource | Import Path |
|---|-------------|----------------|-------------|
| SH.1 | Error code extraction from HTTP errors | `extractBackendErrorCode()` | `shared/utils/backend-error-message` |
| SH.2 | Error code → i18n key mapping | `ErpErrorMapperService` | `shared/services/erp-error-mapper.service` |
| SH.3 | Subscription cleanup on destroy | `takeUntilDestroyed()` + `DestroyRef` | `@angular/core/rxjs-interop` |
| SH.4 | Shared search/pagination DTOs | `PagedResponse<T>`, `SearchRequest`, `SearchFilter` | `shared/models/` (if barrel export exists, else feature model) |
| SH.5 | Common RxJS error handling pattern | `catchError` → `EMPTY`, `finalize` → reset loading | Standard RxJS operators |

**Rules:**
- NEVER implement custom error extraction — use `extractBackendErrorCode()`
- NEVER create a custom error mapper — use `ErpErrorMapperService.mapError()`
- NEVER manage subscriptions manually — use `takeUntilDestroyed(this.destroyRef)`
- NEVER add notification/dialog/navigation logic in facade — use `onSuccess` callbacks to components
- NEVER use `BehaviorSubject` or `ReplaySubject` — use Angular `signal()` exclusively

> **Cross-reference:** After creating the facade, run [`enforce-reusability`](../enforce-reusability/SKILL.md) to verify no shared code was duplicated.

---

## Contract Rules

| # | Rule | Source | Violation |
|---|------|--------|-----------|
| B.3.1 | State managed via Angular `signal()` — private writable, public `computed()` readonly | Contract B.3.1 | Using `BehaviorSubject` or component state |
| B.3.2 | NOT `providedIn: 'root'` — provided at page component level | Contract B.3.2 | `@Injectable({ providedIn: 'root' })` singleton facade |
| B.3.3 | Every API call follows: set loading → call API → `tap(success)` → `catchError` → `finalize(reset loading)` → `takeUntilDestroyed(this.destroyRef)` → `.subscribe()` | Contract B.3.3 | Inconsistent loading state management |
| B.3.4 | `onSuccess` callback for post-action component behavior (navigation, notification) | Contract B.3.4 | Business logic (notifications) in facade |
| B.3.5 | Child mutations update signals locally (append/map/filter) — avoid full reload | Contract B.3.5 | Full grid reload on every child save |
| B.3.6 | After child create/delete: call `refreshUsageInfo(parentId)` | Contract B.3.6 | Stale usage data after child mutations |
| B.3.7 | Error handling via `extractBackendErrorCode()` → `ErpErrorMapperService` | Contract B.3.7 | Displaying raw HTTP error messages |
| B.3.8 | Provides `clearCurrentEntity()` for cleanup on destroy | Contract B.3.8 | Stale state between page navigations |
| B.3.9 | Default sort defined in `lastSearchRequestSignal` initialization | Contract B.3.9 | No default sort |
| B.3.10 | Pagination consolidated into `lastSearchRequestSignal`. `currentPage`/`pageSize` are `computed()` | Contract B.3.10 | Separate writable signals for page, size, sort |
| B.3.11 | Filters are the only independently managed state: `currentFiltersSignal` | Contract B.3.11 | Scattering search state across many signals |
| B.3.12 | All `.subscribe()` calls preceded by `takeUntilDestroyed(this.destroyRef)` | Contract B.3.12 | Leaked subscriptions on component destroy |
| D.5.3 | Facades MUST NOT maintain manual in-memory caches (`Map`, objects) | Contract D.5.3 | `private lookupCache = new Map()` |

---

## Violations Requiring Immediate Rejection

| Pattern | Rule Violated |
|---------|--------------|
| `@Injectable({ providedIn: 'root' })` on facade | B.3.2 |
| `BehaviorSubject` or `ReplaySubject` for state management | B.3.1 |
| Separate writable signals for `currentPage`, `pageSize`, `currentSort` | B.3.10 |
| Missing `clearCurrentEntity()` method | B.3.8 |
| Child mutation triggers full `loadChildren()` reload | B.3.5 |
| Missing `onSuccess?` callback parameter on write operations | B.3.4 |
| Notifications/dialog logic inside facade | B.3.4 |
| Raw HTTP error messages shown to user | B.3.7 |
| `private entityCache = new Map<number, EntityDto>()` in facade | D.5.3 |
| No `refreshUsageInfo()` after child create/delete | B.3.6 |
| Missing `finalize()` to reset loading state | B.3.3 |
| Missing `takeUntilDestroyed(this.destroyRef)` before `.subscribe()` | B.3.12 |
| Missing `DestroyRef` injection in facade | B.3.12 |

---

## Real ERP Example: MasterLookup Facade (Abbreviated)

```typescript
@Injectable()
export class MasterLookupFacade {
  private readonly apiService = inject(MasterLookupApiService);
  private readonly errorMapper = inject(ErpErrorMapperService);
  private readonly destroyRef = inject(DestroyRef);

  // Private state
  private readonly entitiesSignal = signal<MasterLookupDto[]>([]);
  private readonly loadingSignal = signal<boolean>(false);
  private readonly currentFiltersSignal = signal<SearchFilter[]>([]);
  private readonly lastSearchRequestSignal = signal<SearchRequest>({
    filters: [],
    sorts: [{ field: 'lookupKey', direction: 'ASC' }],
    page: 0, size: 20
  });
  private readonly detailsSignal = signal<LookupDetailDto[]>([]);

  // Public computed
  readonly entities = computed(() => this.entitiesSignal());
  readonly loading = computed(() => this.loadingSignal());
  readonly currentPage = computed(() => this.lastSearchRequestSignal().page);  // DERIVED
  readonly pageSize = computed(() => this.lastSearchRequestSignal().size);      // DERIVED
  readonly details = computed(() => this.detailsSignal());

  // Child mutation — LOCAL update + takeUntilDestroyed
  createDetail(request: CreateLookupDetailRequest, onSuccess?: (d: LookupDetailDto) => void): void {
    this.apiService.createDetail(request).pipe(
      tap(detail => {
        this.detailsSignal.update(items => [...items, detail]);  // APPEND
        this.refreshUsageInfo();
        onSuccess?.(detail);
      }),
      catchError(error => { this.handleError(error, this.detailErrorSignal); return EMPTY; }),
      finalize(() => this.detailSavingSignal.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  deleteDetail(id: number, onSuccess?: () => void): void {
    this.apiService.deleteDetail(id).pipe(
      tap(() => {
        this.detailsSignal.update(items => items.filter(d => d.id !== id));  // FILTER
        this.refreshUsageInfo();
        onSuccess?.();
      }),
      ...
    ).subscribe();
  }
}
```
