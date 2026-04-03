# ERP System — Standard Feature Implementation Pattern

> Extracted from the canonical reference: **MasterLookup / LookupDetail** implementation.
> Every new feature MUST follow this exact pattern.

---

## 1. Backend Implementation Sequence

### Step 1: Entity (`entity/Md<Entity>.java`)

```
@Entity
@Table(name = "<PREFIX>_<ENTITY>",
    uniqueConstraints = { @UniqueConstraint(...) },
    indexes = { @Index(...) }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Md<Entity> extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "<entity>_seq")
    @SequenceGenerator(name = "<entity>_seq", sequenceName = "<PREFIX>_<ENTITY>_SEQ", allocationSize = 1)
    @Column(name = "ID_PK")
    private Long id;

    // Business fields with @Column, @NotBlank, @Size, etc.
    
    // Boolean: @Convert(converter = BooleanNumberConverter.class) + @Builder.Default = TRUE
    
    // FK relationships: @ManyToOne(fetch = LAZY) + @JoinColumn(name = "..._ID_FK")
    
    // Computed: @Formula("(SELECT COUNT(*) ...)")
    
    // @PrePersist: defaults + transformations
    // @PreUpdate: transformations
    
    // Helper methods: activate(), deactivate(), hasActiveChildren()
}
```

**Mandatory conventions:**
- Entity class prefix: `Md` (master data) or module-specific prefix
- PK column: always `ID_PK`
- FK columns: always `<REF>_ID_FK`
- Boolean columns: `IS_<FIELD>` with `BooleanNumberConverter`
- Sequence: `<TABLE>_SEQ` with `allocationSize = 1`
- All string key fields: forced UPPERCASE in `@PrePersist`/`@PreUpdate`

---

### Step 2: Repository (`repository/<Entity>Repository.java`)

```
@Repository
public interface <Entity>Repository
    extends JpaRepository<Md<Entity>, Long>,
            JpaSpecificationExecutor<Md<Entity>> {

    // Standard finders
    Optional<Md<Entity>> findBy<UniqueField>(<Type> value);
    boolean existsBy<UniqueField>(<Type> value);
    boolean existsBy<UniqueField>And<Field>AndIdNot(...);  // For update validation

    // Paginated queries
    Page<Md<Entity>> findBy<Filter>(<Type> value, Pageable pageable);

    // FETCH JOIN queries (for child entities — avoids N+1)
    @Query("SELECT e FROM Md<Entity> e JOIN FETCH e.<parent> WHERE ...")
    Page<Md<Entity>> searchBy<Parent>Id(@Param("parentId") Long parentId, Pageable pageable);

    // Count queries (for usage checks)
    @Query("SELECT COUNT(c) FROM <Child> c WHERE c.<entity>.id = :entityId")
    long countChildren(@Param("entityId") Long entityId);

    // Validation queries (single-query checks)
    @Query(value = "SELECT COUNT(*) FROM ... WHERE ... AND IS_ACTIVE = 1", nativeQuery = true)
    int countActiveByKeyAndCode(@Param(...) ..., @Param(...) ...);
}
```

**Mandatory conventions:**
- Always extend both `JpaRepository` AND `JpaSpecificationExecutor`
- Never expose outside the module
- Use `@Query` with `JOIN FETCH` for relationships
- Provide separate `count` queries for existence checks in service layer
- Use native query for complex multi-table validation

---

### Step 3: DTOs (`dto/`)

For each entity, create **exactly** these DTOs:

| DTO Class | Purpose | Immutable fields (excluded from UpdateRequest) |
|---|---|---|
| `<Entity>CreateRequest` | POST request body | — |
| `<Entity>UpdateRequest` | PUT request body | Natural keys, FK references |
| `<Entity>Response` | All GET/POST/PUT responses | — |
| `<Entity>SearchRequest` | POST /search body (extends `BaseSearchContractRequest`) | — |
| `<Entity>UsageResponse` | GET /{id}/usage response | — |
| `<Entity>OptionResponse` | Dropdown options (slim DTO) | — |

**DTO conventions:**
- `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- `@Schema(description = "... - Arabic translation")` on class and fields
- `@NotBlank/@NotNull/@Size` validations with `message = "{validation.required}"` / `"{validation.size}"`
- Response DTOs include audit fields: `createdAt`, `createdBy`, `updatedAt`, `updatedBy`
- Audit timestamps: `@JsonFormat(shape = STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")`
- SearchRequest extends `BaseSearchContractRequest` (not manually parsed)

**Shared DTOs:**
- `ToggleActiveRequest` — reused across all features (`{ active: Boolean }`)
- `BaseSearchContractRequest` — base for all search DTOs (handles filter/sort conversion)

---

### Step 4: Mapper (`mapper/<Entity>Mapper.java`)

```
@Component
public class <Entity>Mapper {

    public Md<Entity> toEntity(<Entity>CreateRequest request) { ... }
    
    public void updateEntityFromRequest(Md<Entity> entity, <Entity>UpdateRequest request) {
        // NEVER update natural keys or FK references
    }
    
    public <Entity>Response toResponse(Md<Entity> entity) {
        // Map ALL fields including audit fields
        // Use Boolean.TRUE.equals() for boolean mapping
    }
    
    public <Entity>OptionResponse toOptionResponse(Md<Entity> entity) { ... }
    
    public <Entity>UsageResponse toUsageResponse(Md<Entity> entity, long ...counts) {
        // Compute canBeDeleted, canDeactivate from counts
    }
}
```

**Mandatory conventions:**
- One mapper per entity, annotated with `@Component`
- Manual mapping (no MapStruct) — keeps explicit control
- `toEntity()`: does NOT set FK relationships (service sets those); does NOT apply uppercase/case normalization (entity `@PrePersist` handles it)
- `updateEntityFromRequest()`: void return, immutable fields are NOT updated
- `toResponse()`: maps boolean with `Boolean.TRUE.equals(entity.getIsActive())`
- `toUsageResponse()`: computes `canBeDeleted`/`canDeactivate` from counts

---

### Step 5: Error Codes (`exception/<Module>ErrorCodes.java`)

```
public final class <Module>ErrorCodes {
    private <Module>ErrorCodes() { throw new UnsupportedOperationException(...); }
    
    // One constant per error scenario
    public static final String <ENTITY>_NOT_FOUND = "<ENTITY>_NOT_FOUND";
    public static final String <ENTITY>_KEY_DUPLICATE = "<ENTITY>_KEY_DUPLICATE";
    public static final String <ENTITY>_FK_VIOLATION = "<ENTITY>_FK_VIOLATION";
    public static final String <ENTITY>_ACTIVE_DETAILS_EXIST = "<ENTITY>_ACTIVE_DETAILS_EXIST";
    ...
}
```

---

### Step 6: Service (`service/<Entity>Service.java`)

```
@Service
@RequiredArgsConstructor
@Slf4j
public class <Entity>Service {

    private final <Entity>Repository repository;
    private final <Entity>Mapper mapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(...);

    @Transactional
    @PreAuthorize("hasAuthority(T(SecurityPermissions).<ENTITY>_CREATE)")
    public ServiceResult<<Entity>Response> create(<Entity>CreateRequest request) {
        // 1. Log entry
        // 2. Validate uniqueness
        // 3. Map to entity
        // 4. Save
        // 5. Log success
        // 6. Return ServiceResult.success(mapper.toResponse(saved), Status.CREATED)
    }

    @CacheEvict(cacheNames = "...", allEntries = true)
    @Transactional
    @PreAuthorize("hasAuthority(T(SecurityPermissions).<ENTITY>_UPDATE)")
    public ServiceResult<<Entity>Response> update(Long id, <Entity>UpdateRequest request) {
        // 1. Log entry
        // 2. Find or throw LocalizedException(Status.NOT_FOUND, <Module>ErrorCodes.<ENTITY>_NOT_FOUND, id)
        // 3. Update via mapper
        // 4. Save
        // 5. Return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED)
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(SecurityPermissions).<ENTITY>_VIEW)")
    public ServiceResult<<Entity>Response> getById(Long id) {
        // 1. Find or throw
        // 2. Return ServiceResult.success(mapper.toResponse(entity))
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(SecurityPermissions).<ENTITY>_VIEW)")
    public ServiceResult<Page<<Entity>Response>> search(SearchRequest searchRequest) {
        // 1. Build Specification via SpecBuilder.build(searchRequest, allowedFields, converter)
        // 2. Build Pageable via PageableBuilder.from(searchRequest, ALLOWED_SORT_FIELDS)
        // 3. Execute findAll(spec, pageable)
        // 4. Map page via page.map(mapper::toResponse)
        // 5. Return ServiceResult.success(mappedPage)
    }

    @CacheEvict(cacheNames = "...", allEntries = true)
    @Transactional
    @PreAuthorize("hasAuthority(T(SecurityPermissions).<ENTITY>_UPDATE)")
    public ServiceResult<<Entity>Response> toggleActive(Long id, Boolean active) {
        // 1. Find or throw
        // 2. Business rule validation (e.g., cannot deactivate if active children exist)
        // 3. entity.activate() or entity.deactivate()
        // 4. Save
        // 5. Return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED)
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(SecurityPermissions).<ENTITY>_DELETE)")
    public void delete(Long id) {
        // 1. Find or throw
        // 2. Check references (children, FK usage)
        // 3. Delete — DataIntegrityViolationException handled by GlobalExceptionHandler (no try-catch)
        // NOTE: delete() stays void — returns 204 No Content via @ResponseStatus
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(SecurityPermissions).<ENTITY>_VIEW)")
    public ServiceResult<<Entity>UsageResponse> getUsage(Long id) {
        // 1. Find entity
        // 2. Count references
        // 3. Build usage response via mapper
        // 4. Return ServiceResult.success(usageResponse)
    }
}
```

**Mandatory conventions:**
- Every public method has `@PreAuthorize`
- Every write method has `@Transactional`; reads have `@Transactional(readOnly = true)`
- Every write that affects cacheable data has `@CacheEvict`
- `log.info()` for writes, `log.debug()` for reads
- `ALLOWED_SORT_FIELDS` whitelist enforced for search
- **Services return `ServiceResult<T>` wrapping DTOs — never raw DTOs or entities.** Use `ServiceResult.success(dto, Status.CREATED)` for create, `ServiceResult.success(dto, Status.UPDATED)` for update/toggle, `ServiceResult.success(dto)` for reads. The only exception is `delete()` which stays `void`.
- All errors use `LocalizedException(Status, ErrorCode, ...args)` — `NotFoundException` is NOT used
- Not-found: `LocalizedException(Status.NOT_FOUND, <Module>ErrorCodes.<ENTITY>_NOT_FOUND, id)`
- Duplicate: `LocalizedException(Status.ALREADY_EXISTS, <Module>ErrorCodes.<ENTITY>_KEY_DUPLICATE, ...)`
- FK violation: `LocalizedException(Status.CONFLICT, <Module>ErrorCodes.<ENTITY>_FK_VIOLATION, ...)`

#### Service-Level Caching Rules

Caching applies **only** to governance-approved eligible entities (see Blueprint §8.2). The following rules govern annotation placement within the service pattern:

**Read Method Caching (`@Cacheable`)**

- The `@Cacheable` annotation is placed on **read-only service methods** that return stable, low-volatility reference data
- It must appear **above** `@Transactional(readOnly = true)` and **below** `@PreAuthorize`
- Annotation ordering on a cached read method: `@Cacheable` → `@Transactional(readOnly = true)` → `@PreAuthorize`
- The `cacheNames` attribute must use the governance-approved domain-specific cache name (e.g., `lookupValues`, `roleDefinitions`)
- `@Cacheable` MUST NOT be placed on search/paginated methods — only on `getById`, `getAll`, or dropdown option methods for eligible entities

**Write Method Eviction (`@CacheEvict`)**

- The `@CacheEvict` annotation is placed on **every write method** (create, update, toggleActive, delete) that modifies a cached entity
- It must appear **above** `@Transactional` and **below** `@PreAuthorize`
- Annotation ordering on a write method: `@CacheEvict` → `@Transactional` → `@PreAuthorize`
- `allEntries = true` is mandatory — no single-key eviction unless governance-approved
- The `cacheNames` attribute must match the corresponding `@Cacheable` cache name exactly

**Non-Eligible Entity Services**

- Services for transactional, financial, or workflow entities MUST NOT include `@Cacheable` or `@CacheEvict` annotations
- If unsure whether an entity qualifies, it does not qualify — default is no caching

**Frontend Caching at Service Level**

- `shareReplay(1)` is permitted **only** in lookup services that retrieve dropdown/reference data for governance-approved eligible entities
- Feature API services (those extending `BaseApiService` for CRUD operations) MUST NOT use `shareReplay` or any in-memory caching mechanism
- Facades MUST NOT maintain manual in-memory caches (e.g., `Map`, plain objects) that duplicate backend-cached data

---

### Step 7: Controller (`controller/<Entity>Controller.java`)

```
@RestController
@RequestMapping("/api/<module>/<entities>")
@RequiredArgsConstructor
@Tag(name = "...", description = "...")
public class <Entity>Controller {

    private final <Entity>Service service;
    private final OperationCode operationCode;

    @PostMapping                        → create()    — returns operationCode.craftResponse(result)
    @PutMapping("/{id}")                → update()    — returns operationCode.craftResponse(result)
    @GetMapping("/{id}")                → getById()   — returns operationCode.craftResponse(result)
    @PostMapping("/search")             → search()    — returns operationCode.craftResponse(result)
    @PutMapping("/{id}/toggle-active")  → toggleActive() — returns operationCode.craftResponse(result)
    @DeleteMapping("/{id}")             → delete()    @ResponseStatus(NO_CONTENT) — stays void
    @GetMapping("/{id}/usage")          → getUsage()  — returns operationCode.craftResponse(result)
}
```

**Mandatory conventions:**
- No business logic in controllers
- No repository injection
- No entity references
- Controller injects service(s) + `OperationCode` — never repositories, mappers, or entities
- All non-delete endpoints return `ResponseEntity<ApiResponse<T>>` via `operationCode.craftResponse(serviceResult)`
- `@ResponseStatus(CREATED)` is NOT needed on POST — the `Status.CREATED` in `ServiceResult` automatically maps to HTTP 201 via `OperationCode`
- `delete()` stays `void` with `@ResponseStatus(HttpStatus.NO_CONTENT)` — no ServiceResult wrapping
- All methods documented with `@Operation(summary, description)`
- Method parameters use `@Valid @RequestBody` for DTOs, `@PathVariable` for IDs
- Child entities unified under same controller (not separate controller)

---

### Step 8: Unit Tests (`test/.../service/<Entity>ServiceTest.java`)

```
@ExtendWith(MockitoExtension.class)
class <Entity>ServiceTest {

    @Mock private <Entity>Repository repository;
    @Mock private <Entity>Mapper mapper;
    @InjectMocks private <Entity>Service service;

    // @BeforeEach: setup test entities, requests, responses

    // Tests:
    // - create_Success → assertThat(result.isSuccess()).isTrue(); assertThat(result.getData()).isEqualTo(expected); assertThat(result.getStatusCode()).isEqualTo(Status.CREATED);
    // - create_ShouldThrowException_WhenDuplicate → assertThrows(LocalizedException.class)
    // - update_Success → assertThat(result.isSuccess()).isTrue(); assertThat(result.getStatusCode()).isEqualTo(Status.UPDATED);
    // - getById_ShouldThrow_WhenNotFound → assertThrows(LocalizedException.class)
    // - toggleActive_ShouldFail_WhenActiveChildrenExist
    // - delete_ShouldFail_WhenHasChildren
    // - delete_ShouldFail_WhenReferencedByOtherEntity
    
    // NOTE: All assertions on service return values use ServiceResult:
    //   ServiceResult<T> result = service.create(request);
    //   assertThat(result.isSuccess()).isTrue();
    //   assertThat(result.getData()).isNotNull();
}
```

---

## 2. Frontend Implementation Sequence

### Step 1: Models (`models/<feature>.model.ts`)

Define **all** TypeScript interfaces in one file:

```typescript
// Parent entity DTOs
export interface <Entity>Dto { id: number; ... isActive: boolean; createdAt?: string; ... }
export interface <Entity>UsageDto { ... canDelete: boolean; canDeactivate: boolean; }
export interface Create<Entity>Request { ... }
export interface Update<Entity>Request { ... }  // Omits immutable fields

// Child entity DTOs (if applicable)
export interface <Child>Dto { ... }
export interface Create<Child>Request { masterEntityId: number; ... }
export interface Update<Child>Request { ... }  // Omits code, parentId

// Shared types
export interface PagedResponse<T> { content: T[]; totalElements: number; ... }
export interface SearchFilter { field: string; operator: FilterOperator; value?: ... }
export type FilterOperator = 'EQUALS' | 'CONTAINS' | 'STARTS_WITH';
export interface SearchSort { field: string; direction: 'ASC' | 'DESC'; }
export interface SearchRequest { filters: SearchFilter[]; sorts?: SearchSort[]; page: number; size: number; }
```

### Step 2: Form Model (`models/<feature>-form.model.ts`)

```typescript
export interface <Entity>FormModel { ... }

export const <Entity>FormMapper = {
    createEmpty(): <Entity>FormModel { ... },
    fromDomain(dto: <Entity>Dto): <Entity>FormModel { ... },
    toCreateRequest(model: <Entity>FormModel): Create<Entity>Request { ... },
    toUpdateRequest(model: <Entity>FormModel): Update<Entity>Request { ... }
};
```

### Step 3: API Service (`services/<feature>-api.service.ts`)

```typescript
@Injectable()
export class <Entity>ApiService extends BaseApiService {
    private readonly baseUrl = `${environment.authApiUrl}/api/<module>`;
    private readonly entityUrl = `${this.baseUrl}/<entities>`;

    search(request: SearchRequest): Observable<PagedResponse<EntityDto>> { return this.doPost(...); }
    getById(id: number): Observable<EntityDto> { return this.doGet(...); }
    create(request: CreateRequest): Observable<EntityDto> { return this.doPost(...); }
    update(id: number, request: UpdateRequest): Observable<EntityDto> { return this.doPut(...); }
    toggleActive(id: number, active: boolean): Observable<EntityDto> { return this.doPut(..., { active }); }
    delete(id: number): Observable<void> { return this.doDelete(...); }
    getUsage(id: number): Observable<UsageDto> { return this.doGet(...); }
}
```

**Mandatory conventions:**
- Extends `BaseApiService` (provides `doGet`, `doPost`, `doPut`, `doDelete` with response unwrapping)
- NOT `providedIn: 'root'` — provided at component level
- Uses environment variable for base URL

### Step 4: Facade (`facades/<feature>.facade.ts`)

```typescript
@Injectable()
export class <Entity>Facade {
    private apiService = inject(<Entity>ApiService);
    private readonly errorMapper = inject(ErpErrorMapperService);

    // PRIVATE SIGNALS (state)
    private entitiesSignal = signal<EntityDto[]>([]);
    private loadingSignal = signal<boolean>(false);
    private errorSignal = signal<string | null>(null);
    private savingSignal = signal<boolean>(false);
    private currentFiltersSignal = signal<SearchFilter[]>([]);
    private lastSearchRequestSignal = signal<SearchRequest>({
        filters: [],
        sorts: [{ field: '<defaultSortField>', direction: 'ASC' }],
        page: 0,
        size: 20
    });

    // PUBLIC COMPUTED (readonly) — derived from lastSearchRequestSignal
    readonly entities = computed(() => this.entitiesSignal());
    readonly loading = computed(() => this.loadingSignal());
    readonly saving = computed(() => this.savingSignal());
    readonly currentPage = computed(() => this.lastSearchRequestSignal().page);
    readonly pageSize = computed(() => this.lastSearchRequestSignal().size);
    ...

    // OPERATIONS — each follows:
    // 1. Set loading/saving signal
    // 2. Clear error signal
    // 3. Call API via pipe(tap(...), catchError(...), finalize(...))
    // 4. Update state signals in tap()
    // 5. Handle error in catchError() via handleError()
    // 6. Reset loading in finalize()

    loadEntities(): void { ... }
    createEntity(request, onSuccess?): void { ... }
    updateEntity(id, request, onSuccess?): void { ... }
    toggleActive(id, active, onSuccess?): void { ... }
    deleteEntity(id, onSuccess?): void { ... }

    // CHILD OPERATIONS (local state update to avoid full reload + flicker)
    createChild(request, onSuccess?): void {
        // On success: signal.update(items => [...items, newItem])
    }
    updateChild(id, parentId, request, onSuccess?): void {
        // On success: signal.update(items => items.map(d => d.id === id ? updated : d))
    }
    deleteChild(id, parentId, onSuccess?): void {
        // On success: signal.update(items => items.filter(d => d.id !== id))
    }

    // ERROR HANDLING
    private handleError(error, errorSignal): void {
        const backendCode = extractBackendErrorCode(error);
        const mappedKey = backendCode && this.errorMapper.hasMapping(backendCode)
            ? this.errorMapper.mapError(backendCode).translationKey : null;
        errorSignal.set(mappedKey || 'ERRORS.OPERATION_FAILED');
    }
}
```

**Mandatory conventions:**
- All state via Angular Signals — private writable, public computed readonly
- Facade is `@Injectable()` NOT `providedIn: 'root'` — provided at page component level
- Callbacks (`onSuccess?`) for component-specific post-action behavior
- Child entity mutations update signals locally (append/update/filter) to avoid full grid reload
- **Pagination state consolidated** into `lastSearchRequestSignal` (single `SearchRequest` signal). `currentPage`/`pageSize` are `computed()` derived from it — NOT separate writable signals
- Filters managed via `currentFiltersSignal` (the only independent state signal for search)

### Step 5: Confirm Actions (`helpers/<feature>-confirm-actions.ts`)

```typescript
export interface ConfirmActionDeps {
    dialog: ErpDialogService;
    notify: ErpNotificationService;
    auth: AuthenticationService;
    facade: <Entity>Facade;
}

export function confirmToggle<Entity>Active(deps, entity, onDone): void {
    // 1. Check permission
    // 2. Show confirm dialog (warning type for deactivate)
    // 3. On confirm: facade.toggleActive(..., onDone)
}

export function confirmDelete<Entity>(deps, entity, onDone): void {
    // 1. Check permission
    // 2. Fetch usage info
    // 3. If !canDelete → show warning
    // 4. Show confirm dialog (danger type)
    // 5. On confirm: facade.delete(..., onDone)
}
```

### Step 6: Page A — Search Component (`pages/<feature>-search/`)

```typescript
@Component({
    standalone: true,
    imports: [..., AgGridAngular, SpecificationFilterComponent, ErpEmptyStateComponent, ErpPermissionDirective],
    providers: [<Entity>Facade, <Entity>ApiService],  // Provided here!
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class <Entity>SearchComponent extends ErpListComponent implements OnInit {
    // Inject: ThemeService, Router, TranslateService, facade, authService, ConfirmActionDeps
    
    // AG Grid config: columnDefs, defaultColDef, gridOptions, localeText
    // Rebuilt on language change
    
    protected load(state: ErpGridState): void {
        this.facade.applyGridStateAndLoad({ page, size, sortBy, sortDir, filters });
    }
}
```

**Mandatory conventions:**
- Extends `ErpListComponent`
- AG Grid configuration in separate `<feature>-grid.config.ts` file
- `ChangeDetectionStrategy.OnPush`
- Standalone component with `providers: [Facade, ApiService]`
- Grid config rebuilt on `translate.onLangChange`
- Actions cell component for edit/toggle/delete
- Specification filter toggle for advanced search

### Step 7: Page B — Entry Component (`pages/<feature>-entry/`)

```typescript
@Component({
    standalone: true,
    imports: [..., ReactiveFormsModule, ErpFormFieldComponent, ErpSectionComponent, ErpBackButtonComponent, ...],
    providers: [<Entity>Facade, <Entity>ApiService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class <Entity>EntryComponent implements OnInit, OnDestroy {
    readonly model = signal<<Entity>FormModel>(<Entity>FormMapper.createEmpty());
    form!: FormGroup;
    isEditMode = false;
    entityId: number | null = null;

    ngOnInit(): void {
        // Route param subscription
        // Edit mode: loadForEdit(id) → patchValue + disable immutable fields
        // Create mode: loadForCreate() → reset form
    }

    save(): void {
        // Validate form
        // isEditMode ? toUpdateRequest() + facade.update() : toCreateRequest() + facade.create()
        // On create success: switch to edit mode in-place (no router.navigate)
        // → set isEditMode=true, entityId, disable immutable fields
        // → Location.replaceState() to update URL without component re-creation
        // → load children (showLoading=false) + usage info
    }

    ngOnDestroy(): void { this.facade.clearCurrentEntity(); }
}
```

**Mandatory conventions:**
- Signal-based form model
- FormMapper for DTO ↔ FormModel conversion
- Immutable fields disabled in edit mode (`.get('field')?.disable()`)
- On create success → switch to edit mode **in-place** via `Location.replaceState()` (NOT `router.navigate` — avoids component destroy/recreate and loading flicker)
- `ngOnDestroy` clears facade state
- Permission check in `ngOnInit` before loading
- `effect()` for displaying save/detail errors via notification service

### Step 8: Routing (`<domain>-routing.module.ts`)

```typescript
const routes: Routes = [
    {
        path: '<entities>',
        component: AdminLayout,
        children: [
            { path: '', loadComponent: () => import('...search...'), canActivate: [authGuard, permissionGuard], data: { permission: 'PERM_..._VIEW' } },
            { path: 'create', loadComponent: () => import('...entry...'), canActivate: [authGuard, permissionGuard], data: { permission: 'PERM_..._CREATE' } },
            { path: 'edit/:id', loadComponent: () => import('...entry...'), canActivate: [authGuard, permissionGuard], data: { permission: 'PERM_..._UPDATE' } }
        ]
    }
];
```

### Step 9: Grid Config (`pages/<feature>-search/<feature>-grid.config.ts`)

```typescript
export function create<Entity>FilterOptions(translate): { fields: SpecFieldOption[]; operators: SpecOperatorOption[] } { ... }

export function create<Entity>ColumnDefs(translate, zone, callbacks): ColDef[] {
    return [
        { field: '...', headerName: translate.instant('...'), sortable: true, flex: N },
        createActiveColumnDef(activeLabels, { ... }),
        { headerName: translate.instant('COMMON.ACTIONS'), cellRenderer: ActionsCellComponent, cellRendererParams: { ... } }
    ];
}

export function create<Entity>GridOptions(translate): { gridOptions: GridOptions; localeText: Record<string, string> } { ... }
```

### Step 10: Components

| Component | Type | Purpose |
|---|---|---|
| `<feature>-actions-cell` | AG Grid Cell Renderer | Edit/ToggleActive/Delete buttons per row |
| `<child>-section` | Presentational (dumb) | Displays child table, emits events |
| `<child>-form-modal` | Self-contained | Modal form for child create/edit |

**Presentational component conventions:**
- `@Input()` for data, `@Output()` for events
- Does NOT inject services or fetch data

**Modal component conventions:**
- Manages own `FormGroup` and modal lifecycle (`NgbModal`)
- `open(entity?)` method — if entity provided → edit mode (patch + disable immutable fields)
- Emits `DetailFormSaveEvent` with request + modal reference
- Parent component handles save via facade + closes modal in `onSuccess`
- **Nullish coalescing for numeric fields:** When mapping optional numeric form values to request DTOs, use `??` (nullish coalescing), NOT `||` (logical OR). This preserves `0` as a valid value:
  ```typescript
  // ✅ Correct — preserves 0
  sortOrder: formValue.sortOrder ?? undefined
  // ❌ Wrong — converts 0 to undefined
  sortOrder: formValue.sortOrder || undefined
  ```
- `ChangeDetectionStrategy.OnPush`

**Modal component conventions:**
- Manages own `FormGroup` and modal lifecycle (`NgbModal`)
- `open(entity?)` method — if entity provided → edit mode (patch + disable immutable fields)
- Emits `DetailFormSaveEvent` with request + modal reference
- Parent component handles save via facade + closes modal in `onSuccess`
