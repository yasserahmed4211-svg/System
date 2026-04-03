# ERP System — Implementation Contract

> Non-negotiable rules extracted from the **MasterLookup / LookupDetail** canonical implementation.
> Any deviation from these rules is a contract violation.

---

## SECTION A: BACKEND CONTRACTS

---

### A.1 Entity Contracts

| # | Rule | Violation Example |
|---|------|-------------------|
| A.1.1 | Every entity extends `AuditableEntity` | Defining `createdAt`/`createdBy` directly in entity |
| A.1.2 | PK column is always `ID_PK` with `@Column(name = "ID_PK")` | Using `@Column(name = "ID")` or `@Column(name = "ENTITY_ID")` |
| A.1.3 | PK uses `GenerationType.SEQUENCE` with explicit `@SequenceGenerator` | Using `GenerationType.IDENTITY` or `GenerationType.AUTO` |
| A.1.4 | `allocationSize = 1` on all `@SequenceGenerator` | Using default allocationSize (50) |
| A.1.5 | FK columns end with `_ID_FK` suffix | Using `_ID` or `_FK` alone |
| A.1.6 | Boolean columns stored as NUMBER(1) via `BooleanNumberConverter` | Using native boolean column type |
| A.1.7 | Boolean field default is `@Builder.Default Boolean isActive = Boolean.TRUE` | Using `boolean` primitive or `null` default |
| A.1.8 | Every `@ManyToOne` uses `fetch = FetchType.LAZY` | Using default EAGER fetch |
| A.1.9 | `@OneToMany` uses `cascade = ALL, orphanRemoval = false, fetch = LAZY` | Using `orphanRemoval = true` without analysis |
| A.1.10 | Entity uses `@SuperBuilder` (not `@Builder`) due to `AuditableEntity` inheritance | Using `@Builder` which breaks superclass |
| A.1.11 | Table names use UPPER_SNAKE_CASE with module prefix | Using lowercase or camelCase table names |
| A.1.12 | `@UniqueConstraint` and `@Index` are declared in `@Table` annotation | Relying on unnamed auto-generated constraints |
| A.1.13 | Unique constraints named `UK_<TABLE>_<DESC>` | Unnamed or non-standard constraint names |
| A.1.14 | Indexes named `IDX_<TABLE>_<COLUMN>` | Unnamed or non-standard index names |
| A.1.15 | FK constraints named `FK_<TABLE>_<REF>` via `@ForeignKey(name = "...")` | Auto-generated FK constraint names |
| A.1.16 | Use `@Formula` for computed counts instead of loading collections | Using `entity.getChildren().size()` |
| A.1.17 | `@PrePersist` handles defaults and is the **sole canonical location** for uppercase/case normalization of natural keys | Duplicating uppercase in mapper or service entity fields |
| A.1.18 | Entity has `activate()` and `deactivate()` helper methods | Setting `isActive` directly from service |

---

### A.2 Repository Contracts

| # | Rule | Violation Example |
|---|------|-------------------|
| A.2.1 | Repository extends both `JpaRepository<E, Long>` AND `JpaSpecificationExecutor<E>` | Missing `JpaSpecificationExecutor` |
| A.2.2 | Repository annotated with `@Repository` | Missing annotation |
| A.2.3 | Repository is NEVER injected outside its own module | Another module importing repository |
| A.2.4 | Existence checks use `boolean existsBy<Field>(...)` | Using `findBy().isPresent()` for existence |
| A.2.5 | Update uniqueness checks use `existsBy<Field>AndIdNot(value, id)` | Not excluding current entity ID |
| A.2.6 | Child queries use `JOIN FETCH` in `@Query` to avoid N+1 | Using Spring Data derived queries that navigate paths |
| A.2.7 | Count queries for reference checks use JPQL `@Query("SELECT COUNT()")` | Loading full collections just to count |
| A.2.8 | Projection interfaces used for read-only multi-table queries | Returning full entities when only subset needed |

---

### A.3 DTO Contracts

| # | Rule | Violation Example |
|---|------|-------------------|
| A.3.1 | DTOs use `@Data @Builder @NoArgsConstructor @AllArgsConstructor` | Missing any of these Lombok annotations |
| A.3.2 | DTOs annotated with `@Schema(description = "English - Arabic")` | Missing Swagger documentation |
| A.3.3 | Each field has `@Schema(description, example)` | Undocumented fields |
| A.3.4 | Validation messages use i18n keys: `message = "{validation.required}"` | Hardcoded English messages in production |
| A.3.5 | `CreateRequest` excludes `id` and audit fields | Including `id` in create request |
| A.3.6 | `UpdateRequest` excludes immutable fields (natural keys, FKs) | Including `code`/`lookupKey` in update request |
| A.3.7 | `Response` includes ALL entity fields + audit fields + computed counts | Missing audit fields or computed values |
| A.3.8 | Audit timestamps use `@JsonFormat(shape = STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")` | Inconsistent date formats |
| A.3.9 | `SearchRequest` extends `BaseSearchContractRequest` | Duplicating filter/sort parsing logic |
| A.3.10 | Child `SearchRequest` overrides `toCommonSearchRequest()` to exclude parent ID filter | Parent ID processed twice |
| A.3.11 | Child `SearchRequest` provides `getMasterLookupId()` (or equivalent parent ID extractor) | Direct field instead of filter extraction |
| A.3.12 | `UsageResponse` contains `canBeDeleted`/`canDeactivate` booleans + reason | Missing actionability information |
| A.3.13 | `OptionResponse` is a slim DTO for dropdowns (no audit fields) | Reusing full Response DTO for dropdowns |

---

### A.4 Mapper Contracts

| # | Rule | Violation Example |
|---|------|-------------------|
| A.4.1 | One `@Component` mapper class per entity | Inline mapping in service or controller |
| A.4.2 | `toEntity()` does NOT set FK relationships — service sets those. Does NOT apply uppercase/case normalization — entity `@PrePersist` handles it | Mapper calling repository or duplicating entity lifecycle logic |
| A.4.3 | `updateEntityFromRequest()` returns void and mutates entity in-place | Creating new entity on update |
| A.4.4 | `updateEntityFromRequest()` does NOT update natural keys or FK references | Changing lookupKey/code in update |
| A.4.5 | `toResponse()` maps booleans with `Boolean.TRUE.equals(entity.getIsActive())` | Direct null-unsafe `entity.getIsActive()` |
| A.4.6 | All mapper methods handle `null` input gracefully (return null) | Missing null checks |
| A.4.7 | `toUsageResponse()` computes deletion/deactivation eligibility from counts | Hardcoded eligibility logic |

---

### A.5 Service Contracts

| # | Rule | Violation Example |
|---|------|-------------------|
| A.5.1 | `@Service @RequiredArgsConstructor @Slf4j` on every service | Missing any annotation |
| A.5.2 | Every public method has `@PreAuthorize` with permission constant from `SecurityPermissions` | Hardcoded permission string |
| A.5.3 | Every write method has `@Transactional` | Missing transaction boundary |
| A.5.4 | Every read method has `@Transactional(readOnly = true)` | Missing readOnly flag |
| A.5.5 | Write operations that affect cached data have `@CacheEvict(cacheNames = "...", allEntries = true)` | Missing cache invalidation |
| A.5.6 | `ALLOWED_SORT_FIELDS` whitelist defined as `private static final Set<String>` | Accepting arbitrary sort fields |
| A.5.7 | Search uses `SpecBuilder.build()` + `PageableBuilder.from()` from common-utils | Manual specification/page construction |
| A.5.8 | Services return `ServiceResult<T>` wrapping DTOs — never raw DTOs or entities. Use `ServiceResult.success(dto, Status.CREATED)` for create, `ServiceResult.success(dto, Status.UPDATED)` for update/toggle, `ServiceResult.success(dto)` for reads. Exception: `delete()` stays `void`. | Returning raw DTO or entity from service method |
| A.5.9 | `create()`: validate uniqueness → map → save → return `ServiceResult.success(dto, Status.CREATED)` | Saving before validation or returning raw DTO |
| A.5.10 | `update()`: findById → throws `LocalizedException(Status.NOT_FOUND, ...)` → map update → save → return `ServiceResult.success(dto, Status.UPDATED)` | Skipping existence check or returning raw DTO |
| A.5.11 | `delete()`: findById → check references → delete (no try-catch — `DataIntegrityViolationException` handled by `GlobalExceptionHandler`) | Delete without reference check |
| A.5.12 | `toggleActive()`: findById → business rule validation → activate/deactivate → save | Skip deactivation constraints |
| A.5.13 | Error codes from `<Module>ErrorCodes` constants, never inline strings | Hardcoded error message strings |
| A.5.14 | `log.info()` for write operations, `log.debug()` for reads | Using wrong log level |
| A.5.15 | ALL exceptions use `LocalizedException(Status, ErrorCode, ...args)` — including not-found errors: `LocalizedException(Status.NOT_FOUND, <Module>ErrorCodes.<ENTITY>_NOT_FOUND, id)`. `NotFoundException` is NOT used. | Throwing `NotFoundException`, raw `RuntimeException`, or any non-`LocalizedException` |
| A.5.16 | Child service's `search()` requires non-null parent ID, returns `Page.empty()` if null | Searching without parent context |
| A.5.17 | Child service's `search()` uses explicit `Specification` JOIN for parent-child filtering | Implicit path navigation causing N+1 |

---

### A.6 Controller Contracts

| # | Rule | Violation Example |
|---|------|-------------------|
| A.6.1 | `@RestController @RequestMapping("/api/<module>/<entities>") @RequiredArgsConstructor` | Inconsistent URL pattern |
| A.6.2 | `@Tag(name = "...", description = "Arabic - English")` Swagger tag | Missing API documentation |
| A.6.3 | Controller injects service(s) + `OperationCode` — never repositories, mappers, or entities | Repository injection or missing OperationCode |
| A.6.4 | All non-delete endpoints return `ResponseEntity<ApiResponse<T>>` via `operationCode.craftResponse(serviceResult)`. `@ResponseStatus(CREATED)` is NOT used — `Status.CREATED` in `ServiceResult` maps to HTTP 201 automatically | Using `@ResponseStatus(CREATED)` or returning raw DTOs |
| A.6.5 | `@DeleteMapping` for delete → `@ResponseStatus(HttpStatus.NO_CONTENT)` with `void` return — no ServiceResult/craftResponse | Returning 200 for delete or wrapping delete in ServiceResult |
| A.6.6 | `@PostMapping("/search")` for search (not GET with query params) | GET with query string for search |
| A.6.7 | `@PutMapping("/{id}/toggle-active")` for status toggle | Separate activate/deactivate endpoints |
| A.6.8 | `@GetMapping("/{id}/usage")` for usage/dependency info | Missing usage endpoint |
| A.6.9 | Child endpoints under same controller: `/details`, `/details/{id}`, `/details/search` | Separate controller for child entity |
| A.6.10 | Every method has `@Operation(summary, description)` | Missing OpenAPI annotations |
| A.6.11 | Request bodies use `@Valid @RequestBody` | Missing validation trigger |
| A.6.12 | No business logic in controller methods — pure delegation | Conditional logic in controller |

---

## SECTION B: FRONTEND CONTRACTS

---

### B.1 Model Contracts

| # | Rule | Violation Example |
|---|------|-------------------|
| B.1.1 | All DTOs and interfaces in single `<feature>.model.ts` file | Splitting DTOs across files |
| B.1.2 | `FormModel` + `FormMapper` in separate `<feature>-form.model.ts` | Inline DTO→form mapping in component |
| B.1.3 | `FormMapper` is a const object with `createEmpty()`, `fromDomain()`, `toCreateRequest()`, `toUpdateRequest()` | Manual mapping scattered in components |
| B.1.4 | `UpdateRequest` interface OMITS immutable fields (key, code, parentId) | Including immutable fields in update |
| B.1.5 | `PagedResponse<T>` is generic and reused | Feature-specific paged response type |
| B.1.6 | `SearchRequest` uses `FilterOperator = 'EQUALS' \| 'CONTAINS' \| 'STARTS_WITH'` | Custom operator types |

---

### B.2 API Service Contracts

| # | Rule | Violation Example |
|---|------|-------------------|
| B.2.1 | Extends `BaseApiService` | Direct `HttpClient` usage |
| B.2.2 | Uses `doGet/doPost/doPut/doDelete` from base (handles response unwrapping) | Manual `.pipe(map(...))` for unwrapping |
| B.2.3 | NOT `providedIn: 'root'` — provided at component `providers: [...]` level | Singleton API service |
| B.2.4 | Uses `environment.authApiUrl` for base URL | Hardcoded URLs |
| B.2.5 | Toggle active: `doPut(url, { active })` — sends `{ active: boolean }` body | Sending active as query param |

---

### B.3 Facade Contracts

| # | Rule | Violation Example |
|---|------|-------------------|
| B.3.1 | State managed via Angular `signal()` — private writable, public `computed()` readonly | Using BehaviorSubject or component state |
| B.3.2 | NOT `providedIn: 'root'` — provided at page component level | Singleton facade service |
| B.3.3 | Every API call follows: `set loading → call API → tap(success) → catchError → finalize(reset loading)` | Inconsistent loading state management |
| B.3.4 | `onSuccess` callback for post-action component behavior (navigation, notification) | Business logic (notifications) in facade |
| B.3.5 | Child mutations update signals locally (append/map/filter) — avoid full reload | Full grid reload on every child save |
| B.3.6 | After child create/delete: call `refreshUsageInfo(parentId)` | Stale usage data after child mutations |
| B.3.7 | Error handling via `extractBackendErrorCode()` → `ErpErrorMapperService` | Displaying raw HTTP error messages |
| B.3.8 | Provides `clearCurrentEntity()` for cleanup on destroy | Stale state between page navigations |
| B.3.9 | Default sort defined in `lastSearchRequestSignal` initialization | No default sort |
| B.3.10 | Pagination state consolidated into `lastSearchRequestSignal` (single `SearchRequest` signal). `currentPage`/`pageSize` are `computed()` derived from it | Separate writable signals for page, size, sort that duplicate ErpListComponent state |
| B.3.11 | Filters are the only independently managed state: `currentFiltersSignal`. All other search state flows through `lastSearchRequestSignal` | Scattering search state across many independent signals |

---

### B.4 Component Contracts

| # | Rule | Violation Example |
|---|------|-------------------|
| B.4.1 | All components use `standalone: true` | NgModule-based components |
| B.4.2 | All components use `ChangeDetectionStrategy.OnPush` | Default change detection |
| B.4.3 | Facade + ApiService provided via `providers: [...]` in component decorator | `providedIn: 'root'` |
| B.4.4 | Page A (Search) extends `ErpListComponent` | Custom pagination/sort handling |
| B.4.5 | Grid config in separate `<feature>-grid.config.ts` file | Inline column definitions |
| B.4.6 | Grid config functions accept `TranslateService` — rebuilt on language change | Hardcoded column headers |
| B.4.7 | Actions cell is a standalone AG Grid cell renderer component | Inline button templates in column def |
| B.4.8 | Page B (Entry) uses `signal<FormModel>()` + Reactive `FormGroup` | Template-driven forms |
| B.4.9 | On create success → switch to edit mode **in-place** (`isEditMode=true`, disable immutable fields, `Location.replaceState()` to update URL) — NOT `router.navigate` which destroys/recreates the component | `router.navigate` to edit route (causes form refresh + loading spinner) |
| B.4.10 | Edit mode: disable immutable form fields (`.get('field')?.disable()`) | Allowing immutable field editing |
| B.4.11 | `ngOnDestroy` calls `facade.clearCurrentEntity()` | State leaking across navigations |
| B.4.12 | Error/save-error effects display via `ErpNotificationService` | Inline error display logic |
| B.4.13 | Permission check in `ngOnInit` before loading data | Loading data then checking permission |
| B.4.14 | Presentational components: `@Input/@Output` only, no service injection | Smart child components |
| B.4.15 | Modal component manages own `FormGroup` and `NgbModal` lifecycle | Parent managing modal form state |
| B.4.16 | When mapping optional numeric form values to request DTOs, use `??` (nullish coalescing) NOT `||` (logical OR) — preserves `0` as valid | `sortOrder: formValue.sortOrder \|\| undefined` converting 0 to undefined |

---

### B.5 Routing Contracts

| # | Rule | Violation Example |
|---|------|-------------------|
| B.5.1 | Three routes per feature: `''` (list), `'create'` (create), `'edit/:id'` (edit) | Combined create/edit without route param |
| B.5.2 | `loadComponent` lazy loading (not `component`) | Eager component loading |
| B.5.3 | `canActivate: [authGuard, permissionGuard]` on every route | Missing guards |
| B.5.4 | `data: { permission: 'PERM_...' }` matches `SecurityPermissions` constants | Mismatched permission strings |
| B.5.5 | Routes wrapped in `AdminLayout` parent | No layout wrapper |

---

### B.6 Confirm Action Contracts

| # | Rule | Violation Example |
|---|------|-------------------|
| B.6.1 | Extracted to `helpers/<feature>-confirm-actions.ts` as standalone functions | Inline confirmation logic in components |
| B.6.2 | Functions receive `ConfirmActionDeps` interface (dialog, notify, auth, facade) | Direct service injection in functions |
| B.6.3 | Every action checks permission FIRST before showing dialog | Dialog shown then permission denied |
| B.6.4 | Delete checks `usage.canDelete` BEFORE showing confirm dialog | Confirm shown then server rejects |
| B.6.5 | Dialog type: `'warning'` for toggle, `'danger'` for delete | Wrong dialog severity |
| B.6.6 | Dialog uses translation keys with `messageParams` for entity identifiers | Hardcoded entity names |

---

## SECTION C: CROSS-CUTTING CONTRACTS

---

### C.1 i18n

| # | Rule |
|---|------|
| C.1.1 | ALL user-facing strings use translation keys — never hardcoded |
| C.1.2 | Backend validation messages use `{validation.required}`, `{validation.size}` |
| C.1.3 | Backend error codes have entries in `messages.properties` (EN) + `messages_ar.properties` (AR) |
| C.1.4 | Frontend uses `TranslateModule` + `translate.instant()` / `\| translate` pipe |
| C.1.5 | Swagger `@Schema(description)` includes both English and Arabic descriptions |

### C.2 Security

| # | Rule |
|---|------|
| C.2.1 | Permission constants centralized in `SecurityPermissions.java` |
| C.2.2 | Backend: `@PreAuthorize` on every service method (not controller) |
| C.2.3 | Frontend: route guard + directive + programmatic check (triple enforcement) |
| C.2.4 | Four permissions per entity: VIEW, CREATE, UPDATE, DELETE |
| C.2.5 | Child entities may reuse parent permissions (e.g., LookupDetail uses MASTER_LOOKUP_*) |

### C.3 Immutability

| # | Rule |
|---|------|
| C.3.1 | Natural keys (lookupKey, code) are IMMUTABLE after creation |
| C.3.2 | FK parent references are IMMUTABLE after creation |
| C.3.3 | Update DTOs EXCLUDE immutable fields |
| C.3.4 | Mapper's `updateEntityFromRequest()` skips immutable fields |
| C.3.5 | Frontend disables immutable form fields in edit mode |

### C.4 Active/Inactive Lifecycle

| # | Rule |
|---|------|
| C.4.1 | Single `toggle-active` endpoint (not separate activate/deactivate) |
| C.4.2 | Request body: `{ "active": boolean }` via `ToggleActiveRequest` |
| C.4.3 | Deactivation blocked if entity has active children |
| C.4.4 | Frontend shows confirmation dialog before toggle |
| C.4.5 | Cache evicted on toggle |

### C.5 Delete Lifecycle

| # | Rule |
|---|------|
| C.5.1 | Check reference counts BEFORE attempting delete |
| C.5.2 | Return HTTP 409 CONFLICT with descriptive error code if delete blocked |
| C.5.3 | `DataIntegrityViolationException` propagates to `GlobalExceptionHandler` — no try-catch in service |
| C.5.4 | Frontend fetches usage info and blocks delete dialog if `!canDelete` |
| C.5.5 | Delete confirmation dialog uses `type: 'danger'` |

### C.6 Search/Pagination

| # | Rule |
|---|------|
| C.6.1 | All list endpoints use server-side pagination |
| C.6.2 | Search via `POST /search` with `BaseSearchContractRequest` body |
| C.6.3 | Sort fields validated against `ALLOWED_SORT_FIELDS` whitelist |
| C.6.4 | Default page size: 20 (master list), 50 (detail list) |
| C.6.5 | Frontend sends `{ filters, sorts, page, size }` — backend converts via `BaseSearchContractRequest.toCommonSearchRequest()` |
| C.6.6 | Child search requires parent ID filter — extracted separately, not passed to SpecBuilder |

---

## SECTION D: CACHING CONTRACTS

---

### D.1 Cache Eligibility Contracts

| # | Rule | Violation Example |
|---|------|-------------------|
| D.1.1 | Only governance-approved entities may be cached. Eligible list: Master Lookup, Lookup Details (low volatility), Login reference data, Menu structure, Role definitions, Permission definitions | Adding `@Cacheable` to `JournalEntryService` |
| D.1.2 | Entity must satisfy ALL eligibility criteria: dataset < 500 records, low update frequency, no financial impact, no workflow lifecycle, cross-module reuse, used in dropdowns or auth | Caching an entity that meets only 3 of 6 criteria |
| D.1.3 | Transactional entities are NEVER cacheable — GL, Billing, and financial module entities are permanently excluded | Adding `@Cacheable` to any GL service method |
| D.1.4 | Approval-based or workflow entities are NEVER cacheable | Caching entities with status lifecycle transitions |
| D.1.5 | Per-user data and session-scoped data MUST NOT be cached in shared Redis | Caching user preferences in a shared Redis cache name |
| D.1.6 | Large or dynamic search result sets are NEVER cacheable | Adding `@Cacheable` to a `search()` method |

---

### D.2 Cache Naming Contracts

| # | Rule | Violation Example |
|---|------|-------------------|
| D.2.1 | Cache names must be domain-specific camelCase identifiers | Using `cache1`, `data`, `entities` |
| D.2.2 | Each eligible entity has exactly one approved cache name — no aliases or duplicates | Two services using different names for the same data |
| D.2.3 | Approved cache names: `lookupValues`, `lookupDetailValues`, `loginReferenceData`, `menuStructure`, `roleDefinitions`, `permissionDefinitions` | Inventing `masterLookupCache` or `md_lookups` |
| D.2.4 | `@Cacheable` and `@CacheEvict` on the same entity's service MUST reference the same `cacheNames` value | `@Cacheable("lookupValues")` on read but `@CacheEvict("lookups")` on write |

---

### D.3 Cache Annotation Placement Contracts

| # | Rule | Violation Example |
|---|------|-------------------|
| D.3.1 | `@Cacheable` is placed ONLY on service-layer read methods | `@Cacheable` on a repository method or controller method |
| D.3.2 | `@CacheEvict` is placed ONLY on service-layer write methods | `@CacheEvict` on a controller or repository |
| D.3.3 | `@Cacheable` MUST NOT appear on write methods (create, update, toggle, delete) | `@Cacheable` on `create()` method |
| D.3.4 | `@CacheEvict` MUST NOT appear on read-only methods | `@CacheEvict` on `getById()` for a non-mutation call |
| D.3.5 | `@CacheEvict(allEntries = true)` is mandatory — no single-key eviction without governance approval | `@CacheEvict(key = "#id")` without approval |
| D.3.6 | Annotation order on cached read: `@Cacheable` → `@Transactional(readOnly = true)` → `@PreAuthorize` | Wrong annotation ordering |
| D.3.7 | Annotation order on cached write: `@CacheEvict` → `@Transactional` → `@PreAuthorize` | Wrong annotation ordering |

---

### D.4 Cache Eviction Contracts

| # | Rule | Violation Example |
|---|------|-------------------|
| D.4.1 | Every `create()` on a cached entity MUST have `@CacheEvict(allEntries = true)` | `create()` without eviction on a cached entity |
| D.4.2 | Every `update()` on a cached entity MUST have `@CacheEvict(allEntries = true)` | `update()` without eviction on a cached entity |
| D.4.3 | Every `toggleActive()` on a cached entity MUST have `@CacheEvict(allEntries = true)` | `toggleActive()` without eviction — stale active/inactive state served |
| D.4.4 | Every `delete()` on a cached entity MUST have `@CacheEvict(allEntries = true)` | `delete()` without eviction — deleted entity still served from cache |
| D.4.5 | No partial eviction (single-key based) unless explicitly governance-approved | `@CacheEvict(key = "#id")` without documented justification |
| D.4.6 | Cache eviction must be co-located with the `@Transactional` method — not delegated to a separate eviction helper | Separate `CacheEvictionService` called after commit |

---

### D.5 Frontend Caching Contracts

| # | Rule | Violation Example |
|---|------|-------------------|
| D.5.1 | `shareReplay(1)` is allowed ONLY in lookup services for governance-approved eligible entities | `shareReplay(1)` in a `JournalEntryApiService` |
| D.5.2 | Feature API services (CRUD services extending `BaseApiService`) MUST NOT use `shareReplay` or any in-memory caching | `shareReplay` on `contractApiService.search()` |
| D.5.3 | Facades MUST NOT maintain manual in-memory caches (`Map`, plain objects) that duplicate backend-cached data | `private lookupCache = new Map()` in a facade |
| D.5.4 | Frontend MUST NOT implement its own TTL or expiration logic for data already cached by Redis on the backend | Custom `setTimeout`-based cache expiration in a service |
| D.5.5 | If backend caching is enabled for an entity, the frontend must rely on the backend cache — no redundant frontend caching layer | Both `shareReplay` in service AND `@Cacheable` on backend for the same entity without governance review |

---

### D.6 Prohibited Caching Patterns

| # | Rule | Violation Example |
|---|------|-------------------|
| D.6.1 | NEVER cache paginated search results | `@Cacheable` on `search(SearchRequest)` |
| D.6.2 | NEVER use manual `RedisTemplate` calls in service or business code | `redisTemplate.opsForValue().set(...)` in a service |
| D.6.3 | NEVER cache entities with financial impact | `@Cacheable` on GL Account balance retrieval |
| D.6.4 | NEVER cache entities with approval/workflow lifecycle | `@Cacheable` on a method returning pending-approval entities |
| D.6.5 | NEVER add `@Cacheable` to a non-eligible entity without governance approval | Any `@Cacheable` on entities outside the approved list |
| D.6.6 | NEVER use `@CachePut` without a paired `@CacheEvict` on all mutation paths | Orphaned `@CachePut` with no eviction on delete |
| D.6.7 | NEVER place caching annotations on repositories or controllers | `@Cacheable` in `LookupDetailRepository` |
