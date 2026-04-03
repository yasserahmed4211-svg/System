---
description: "STATE MANAGEMENT ENFORCER — validates Angular Signal-based patterns: private signals + computed public, consolidated lastSearchRequestSignal for pagination, loading/error/finalize pattern, child local updates, no BehaviorSubject. 43 checks."
---

# Skill: enforce-state-management

## Name
`enforce-state-management`

## Description
Validates that Angular Signal-based state management in facades and components follows the ERP governance contracts. Ensures no duplicate state, proper pagination consolidation, correct signal patterns, and no prohibited caching patterns. This skill enforces contracts B.3.x, B.4.8–B.4.12, and D.5.x.

## When to Use
- After creating or modifying a facade
- After modifying component state management
- When reviewing frontend pull requests for state-related changes
- When diagnosing state bugs (stale data, dual ownership, leaks)

---

## CHECK MATRIX

### Section 1: Signal Architecture (12 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| S.1.1 | All state uses `signal()` | No `BehaviorSubject`, `ReplaySubject`, or plain class properties for reactive state | RxJS subjects for state |
| S.1.2 | Signals are private | All writable signals declared `private readonly` | Public writable signal |
| S.1.3 | Public state uses `computed()` | All public state is `computed(() => privateSignal())` | Direct signal exposure |
| S.1.4 | No signal duplication | Each piece of state has exactly one signal source | Same data in two signals |
| S.1.5 | Facade is @Injectable() | `@Injectable()` without `providedIn` | providedIn: 'root' |
| S.1.6 | Facade scoped to component | Provided in page component `providers: [...]` | Singleton façade |
| S.1.7 | No BehaviorSubject | Zero `BehaviorSubject` instances in facades | BehaviorSubject anywhere in facade |
| S.1.8 | No ReplaySubject | Zero `ReplaySubject` instances in facades | ReplaySubject anywhere in facade |
| S.1.9 | No plain property state | ALL component state (`isEditMode`, `entityId`, `loading`) uses `signal()` | Plain property for component-level state |
| S.1.10 | Signal naming convention | Private: `<name>Signal`, Public computed: `<name>` (no Signal suffix) | Inconsistent naming |
| S.1.11 | clearCurrentEntity exists | Facade has cleanup method resetting entity + usage + error signals | Missing cleanup |
| S.1.12 | ngOnDestroy calls cleanup | Entry/detail components call `facade.clearCurrentEntity()` in `ngOnDestroy` | State leak between navigations |

### Section 2: Pagination State Consolidation (8 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| S.2.1 | Single search request signal | `lastSearchRequestSignal` holds `{ filters, sorts, page, size }` | Separate page/size/sort signals |
| S.2.2 | currentPage is computed | `computed(() => this.lastSearchRequestSignal().page)` | Writable `currentPageSignal` |
| S.2.3 | pageSize is computed | `computed(() => this.lastSearchRequestSignal().size)` | Writable `pageSizeSignal` |
| S.2.4 | Sort is in lastSearchRequest | Sorts stored inside lastSearchRequestSignal | Separate `currentSortSignal` |
| S.2.5 | currentFiltersSignal is independent | Filters managed via dedicated `currentFiltersSignal` | Filters inside lastSearchRequest only |
| S.2.6 | applyGridStateAndLoad() | Method receives grid state → builds SearchRequest → calls executeSearch | Direct signal mutation from component |
| S.2.7 | No dual state with ErpListComponent | ErpListComponent.gridState is UI, facade.lastSearchRequest is API | Both managing same page/size |
| S.2.8 | Default sort in initialization | `lastSearchRequestSignal` initial value includes sorts | No default sort |

### Section 3: API Call Pattern (8 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| S.3.1 | Loading set before call | `loadingSignal.set(true)` before `apiService.method()` | Loading not set |
| S.3.2 | Error cleared before call | `errorSignal.set(null)` before API call | Old error still visible |
| S.3.3 | Success in tap() | State updates in `tap()` operator | State in subscribe callback |
| S.3.4 | Error in catchError() | `handleError(error, errorSignal)` in `catchError()` | Unhandled errors |
| S.3.5 | Loading reset in finalize() | `finalize(() => loadingSignal.set(false))` | Loading stuck on error |
| S.3.6 | Return EMPTY on error | `catchError` returns `EMPTY` | Return throwError or of() |
| S.3.7 | onSuccess callback | Write methods pass result to `onSuccess?.(entity)` | Notifications inside facade |
| S.3.8 | No business logic in facade | Facade does not contain dialog, notification, or navigation logic | UI logic in facade |
| S.3.9 | takeUntilDestroyed | All `.subscribe()` preceded by `takeUntilDestroyed(this.destroyRef)` | Leaked subscriptions on destroy |

### Section 4: Child Entity State (8 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| S.4.1 | Local append on child create | `signal.update(items => [...items, newItem])` | Full reload after create |
| S.4.2 | Local map on child update | `signal.update(items => items.map(d => d.id === id ? updated : d))` | Full reload after update |
| S.4.3 | Local filter on child delete | `signal.update(items => items.filter(d => d.id !== id))` | Full reload after delete |
| S.4.4 | Usage refresh after child create | `refreshUsageInfo()` called after child create | Stale usage info |
| S.4.5 | Usage refresh after child delete | `refreshUsageInfo()` called after child delete | Stale usage info |
| S.4.6 | Separate child signals | Child state in own signals (childLoadingSignal, etc.) | Mixed with parent signals |
| S.4.7 | resetChildState() exists | Method to clear child signals for cleanup | No child cleanup |
| S.4.8 | Child default page size | Detail search uses `size: 50` (detail default) | Using master default 20 |

### Section 5: Frontend Caching Prohibition (7 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| S.5.1 | No shareReplay in CRUD service | Feature API services have zero `shareReplay` calls | `shareReplay()` in feature service |
| S.5.2 | shareReplay only in lookups | `shareReplay(1)` allowed ONLY in lookup/reference services | `shareReplay` in CRUD service |
| S.5.3 | No manual Map cache | No `private cache = new Map()` in facades | Map-based caching |
| S.5.4 | No object cache | No plain object caches (`private data = {}`) in facades | Object-based caching |
| S.5.5 | No custom TTL | No `setTimeout` or `Date.now()` based cache expiration | Custom TTL logic |
| S.5.6 | No redundant frontend cache | If backend caches entity (Redis), frontend has no parallel cache | Double-caching |
| S.5.7 | No cacheable search | No caching of paginated search results in frontend | Cached search results |

---

## AUTOMATIC REJECTION TRIGGERS

| # | Trigger | Rules |
|---|---------|-------|
| 1 | `BehaviorSubject` or `ReplaySubject` used for state | S.1.1, S.1.7, S.1.8 |
| 2 | `@Injectable({ providedIn: 'root' })` on facade | S.1.5 |
| 3 | Separate writable `currentPageSignal` or `pageSizeSignal` | S.2.2, S.2.3 |
| 4 | Missing `finalize()` on API calls (loading stuck on error) | S.3.5 |
| 5 | Full child reload instead of local signal update | S.4.1, S.4.2, S.4.3 |
| 6 | `shareReplay` in a feature CRUD API service | S.5.1 |
| 7 | Missing `clearCurrentEntity()` method | S.1.11 |
| 8 | Public writable signals exposed from facade | S.1.2, S.1.3 |

---

## DIAGNOSTIC PATTERNS

### Pattern: Stale Data After Child Mutation
**Symptoms:** Usage info shows wrong count after adding/removing child
**Root Cause:** Missing `refreshUsageInfo()` after child create/delete
**Fix:** Add `this.refreshUsageInfo()` in child create/delete `tap()` block

### Pattern: Loading Spinner Stuck
**Symptoms:** Loading indicator never disappears after API error
**Root Cause:** Missing `finalize()` to reset loading signal
**Fix:** Add `finalize(() => this.loadingSignal.set(false))` to pipe

### Pattern: State Leak Between Pages
**Symptoms:** Old entity data visible when navigating to create page
**Root Cause:** Missing `ngOnDestroy → facade.clearCurrentEntity()`
**Fix:** Add cleanup in component `ngOnDestroy` lifecycle hook

### Pattern: Duplicate Pagination State
**Symptoms:** Page resets unexpectedly or shows wrong page number
**Root Cause:** Separate writable signals for page/size duplicating ErpListComponent state
**Fix:** Consolidate into `lastSearchRequestSignal`, derive page/size via `computed()`

### Pattern: Full Grid Flicker on Child Save
**Symptoms:** Entire child table flickers and scrolls to top after save
**Root Cause:** Full reload via `loadChildren()` instead of local signal update
**Fix:** Use `signal.update(items => [...items, newItem])` for append, `items.map()` for update, `items.filter()` for delete

---

## HOW TO RUN THIS SKILL

### Input
Feature facade file + related components.

### Process
1. **Analyze facade** for signal patterns, pagination consolidation, API call structure
2. **Analyze components** for state usage, cleanup, provider declarations
3. **Check caching** for prohibited patterns
4. **Run all 43 checks** across 5 sections
5. **Flag violations** with specific signal/method names and locations

### Output Format
```
STATE MANAGEMENT ENFORCEMENT REPORT
=====================================
Feature: <feature-name>
Date: <date>

SECTION 1: SIGNAL ARCHITECTURE       [X/12 PASS]
SECTION 2: PAGINATION CONSOLIDATION  [X/8 PASS]
SECTION 3: API CALL PATTERN          [X/8 PASS]
SECTION 4: CHILD ENTITY STATE        [X/8 PASS]
SECTION 5: FRONTEND CACHING          [X/7 PASS]

TOTAL: XX/43 CHECKS PASSED

AUTOMATIC REJECTION: YES/NO
VIOLATIONS: [list with signal names and file locations]

VERDICT: APPROVED / APPROVED WITH WARNINGS / REJECTED
```

---

## RELATED SKILLS

| Skill | Purpose |
|-------|---------|
| `enforce-reusability` | Validates that shared base classes (`ErpListComponent`, `BaseApiService`) and utilities (`extractBackendErrorCode`, `ErpErrorMapperService`) are consumed — not reinvented |
| `enforce-frontend-architecture` | Validates overall architectural compliance |
| `enforce-permissions` | Validates triple-enforcement permission pattern |
