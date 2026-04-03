# ERP System — Execution Template

> Step-by-step checklist for implementing a new feature.
> Based on the canonical MasterLookup/LookupDetail pattern.
> Replace all `<placeholders>` with actual feature names.

---

## VARIABLES (fill before starting)

```
MODULE_NAME       = e.g., masterdata, finance-gl
MODULE_PREFIX     = e.g., MD, GL
ENTITY_NAME       = e.g., Activity, CostCenter  (Java class name, PascalCase)
ENTITY_TABLE      = e.g., MD_ACTIVITY, GL_COST_CENTER  (DB table name, UPPER_SNAKE)
ENTITY_SEQ        = e.g., MD_ACTIVITY_SEQ
ENTITY_URL        = e.g., activities, cost-centers  (URL path segment, kebab-case plural)
ENTITY_PERM       = e.g., ACTIVITY, COST_CENTER  (permission suffix, UPPER_SNAKE)
FEATURE_DIR       = e.g., activities, cost-centers  (frontend feature dir, kebab-case)
PARENT_ENTITY     = (optional) parent entity name if this is a child entity
PARENT_FK_COL     = (optional) e.g., ACTIVITY_ID_FK
HAS_CHILD_ENTITY  = true/false
CHILD_ENTITY_NAME = (optional) child entity name
```

---

## PHASE 1: BACKEND

### Step 1.1: Entity

- [ ] Create `backend/erp-<MODULE_NAME>/src/main/java/com/example/<module>/entity/Md<ENTITY_NAME>.java`
- [ ] Extend `AuditableEntity`
- [ ] Annotate: `@Entity @Table @Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder`
- [ ] Define `@Table(name = "<ENTITY_TABLE>", uniqueConstraints = {...}, indexes = {...})`
- [ ] PK: `@Id @GeneratedValue(SEQUENCE) @SequenceGenerator(name, sequenceName = "<ENTITY_SEQ>", allocationSize = 1) @Column(name = "ID_PK")`
- [ ] Business fields: `@NotBlank @Size @Column(name = "...", length = ..., nullable = ...)`
- [ ] Foreign keys: `@ManyToOne(fetch = LAZY) @JoinColumn(name = "<REF>_ID_FK", foreignKey = @ForeignKey(name = "FK_..."))`
- [ ] Boolean fields: `@Convert(converter = BooleanNumberConverter.class) @Builder.Default Boolean isActive = Boolean.TRUE`
- [ ] `@PrePersist` and `@PreUpdate` for defaults/transformations
- [ ] `activate()` / `deactivate()` helper methods
- [ ] If parent: `@OneToMany(mappedBy, cascade = ALL, orphanRemoval = false, fetch = LAZY)`, `@Formula` for computed count

### Step 1.2: Repository

- [ ] Create `repository/<ENTITY_NAME>Repository.java`
- [ ] Extend `JpaRepository<Md<ENTITY_NAME>, Long>, JpaSpecificationExecutor<Md<ENTITY_NAME>>`
- [ ] Add `@Repository`
- [ ] Add `findBy<UniqueField>()`, `existsBy<UniqueField>()`, `existsBy<UniqueField>AndIdNot()`
- [ ] Add paginated finders: `findBy<Filter>(..., Pageable)`
- [ ] If parent: add `countChildren()`, `countActiveChildren()` queries
- [ ] If child: add `searchByParentId()` with `JOIN FETCH`, `findByParentKeyAndActive()`
- [ ] If child: add reference count queries for delete validation

### Step 1.3: DTOs

- [ ] Create `dto/<ENTITY_NAME>CreateRequest.java` — `@Data @Builder @NoArgsConstructor @AllArgsConstructor @Schema`
  - [ ] `@NotBlank`/`@NotNull`/`@Size` validations with i18n message keys
  - [ ] `@Schema(description, example)` on every field
  - [ ] `@Builder.Default Boolean isActive = true`
- [ ] Create `dto/<ENTITY_NAME>UpdateRequest.java` — omit immutable fields (keys, FK refs)
- [ ] Create `dto/<ENTITY_NAME>Response.java` — ALL fields + audit fields + computed counts
  - [ ] `@JsonFormat` on `Instant` fields
- [ ] Create `dto/<ENTITY_NAME>SearchRequest.java extends BaseSearchContractRequest`
  - [ ] If child: override `toCommonSearchRequest()` to exclude parent ID; add `getParentId()` extractor
- [ ] Create `dto/<ENTITY_NAME>UsageResponse.java` — counts + `canDelete`/`canDeactivate` + reason
- [ ] Create `dto/<ENTITY_NAME>OptionResponse.java` (if used in dropdowns) — slim DTO, no audit

### Step 1.4: Mapper

- [ ] Create `mapper/<ENTITY_NAME>Mapper.java` with `@Component`
- [ ] `toEntity(CreateRequest)` — build entity, do NOT set FK relationships, do NOT apply uppercase (entity `@PrePersist` handles it)
- [ ] `updateEntityFromRequest(entity, UpdateRequest)` — void, skip immutable fields
- [ ] `toResponse(entity)` — map all + `Boolean.TRUE.equals()` for booleans
- [ ] `toOptionResponse(entity)` (if applicable)
- [ ] `toUsageResponse(entity, ...counts)` — compute `canDelete`/`canDeactivate`
- [ ] All methods handle null input

### Step 1.5: Error Codes

- [ ] Add constants to `exception/<Module>ErrorCodes.java`:
  - [ ] `<ENTITY>_NOT_FOUND`
  - [ ] `<ENTITY>_KEY_DUPLICATE` (or `_CODE_DUPLICATE`)
  - [ ] `<ENTITY>_FK_VIOLATION`
  - [ ] `<ENTITY>_ACTIVE_CHILDREN_EXIST` (if parent)
  - [ ] `<ENTITY>_CHILDREN_EXIST` (if parent)
  - [ ] `<ENTITY>_REFERENCES_EXIST` (if referenced by other entities)
- [ ] Add message entries in `erp-main/src/main/resources/i18n/messages.properties`
- [ ] Add message entries in `erp-main/src/main/resources/i18n/messages_ar.properties`

### Step 1.6: Permissions

- [ ] Add to `SecurityPermissions.java`:
  - [ ] `<ENTITY>_VIEW = "PERM_<ENTITY_PERM>_VIEW"`
  - [ ] `<ENTITY>_CREATE = "PERM_<ENTITY_PERM>_CREATE"`
  - [ ] `<ENTITY>_UPDATE = "PERM_<ENTITY_PERM>_UPDATE"`
  - [ ] `<ENTITY>_DELETE = "PERM_<ENTITY_PERM>_DELETE"`
- [ ] Seed permissions in database migration/seed data

### Step 1.7: Service

- [ ] Create `service/<ENTITY_NAME>Service.java` — `@Service @RequiredArgsConstructor @Slf4j`
- [ ] Define `ALLOWED_SORT_FIELDS` as `private static final Set<String>`
- [ ] Implement: (each with `@PreAuthorize`, `@Transactional`, logging)
  - [ ] `create()` → validate uniqueness → map → save → return `ServiceResult.success(dto, Status.CREATED)`
  - [ ] `update()` → findById → throws `LocalizedException(Status.NOT_FOUND, ...)` → update via mapper → save → return `ServiceResult.success(dto, Status.UPDATED)`. Add `@CacheEvict` if cacheable.
  - [ ] `getById()` → findById → return `ServiceResult.success(dto)`. `@Transactional(readOnly = true)`
  - [ ] `search()` → SpecBuilder + PageableBuilder → findAll → map page → return `ServiceResult.success(page)`. `@Transactional(readOnly = true)`
  - [ ] `toggleActive()` → findById → validate constraints → activate/deactivate → save → return `ServiceResult.success(dto, Status.UPDATED)`. `@CacheEvict`
  - [ ] `delete()` → findById → check references → delete (no try-catch — `DataIntegrityViolationException` handled by `GlobalExceptionHandler`). Returns `void` (no ServiceResult). `@CacheEvict`
  - [ ] `getUsage()` → findById → count references → build usage response → return `ServiceResult.success(usageDto)`. `@Transactional(readOnly = true)`
- [ ] All return types are `ServiceResult<T>` except `delete()` which is `void`

### Step 1.8: Controller

- [ ] Create `controller/<ENTITY_NAME>Controller.java`
- [ ] Annotate: `@RestController @RequestMapping("/api/<module>/<entity-url>") @RequiredArgsConstructor @Tag`
- [ ] Inject service + `OperationCode`
- [ ] Implement endpoints: (each with `@Operation`)
  - [ ] `POST /` → `operationCode.craftResponse(service.create(...))` — Status.CREATED in ServiceResult maps to HTTP 201
  - [ ] `PUT /{id}` → `operationCode.craftResponse(service.update(...))`
  - [ ] `GET /{id}` → `operationCode.craftResponse(service.getById(...))`
  - [ ] `POST /search` → `operationCode.craftResponse(service.search(...))`
  - [ ] `PUT /{id}/toggle-active` → `operationCode.craftResponse(service.toggleActive(...))`
  - [ ] `DELETE /{id}` → `service.delete(id)`, `@ResponseStatus(NO_CONTENT)` — stays void, no craftResponse
  - [ ] `GET /{id}/usage` → `operationCode.craftResponse(service.getUsage(...))`
- [ ] All non-delete endpoints return `ResponseEntity<ApiResponse<T>>` via `operationCode.craftResponse()`
- [ ] If has child: add child endpoints under same controller (`/details/...`)

### Step 1.9: Unit Tests

- [ ] Create `test/.../service/<ENTITY_NAME>ServiceTest.java`
- [ ] `@ExtendWith(MockitoExtension.class)` with `@Mock` repo + mapper, `@InjectMocks` service
- [ ] Test cases (assert `ServiceResult` for non-delete methods):
  - [ ] `create_Success` — `assertThat(result.isSuccess()).isTrue(); assertThat(result.getData()).isEqualTo(expected); assertThat(result.getStatusCode()).isEqualTo(Status.CREATED)`
  - [ ] `create_ShouldThrowException_WhenDuplicate`
  - [ ] `update_Success` — `assertThat(result.getStatusCode()).isEqualTo(Status.UPDATED)`
  - [ ] `update_ShouldThrow_WhenNotFound`
  - [ ] `getById_Success` — `assertThat(result.isSuccess()).isTrue()`
  - [ ] `getById_ShouldThrow_WhenNotFound`
  - [ ] `toggleActive_Success`
  - [ ] `toggleActive_ShouldFail_WhenConstraints` (if applicable)
  - [ ] `delete_Success`
  - [ ] `delete_ShouldFail_WhenHasChildren` (if applicable)
  - [ ] `delete_ShouldFail_WhenReferenced` (if applicable)

---

## PHASE 2: FRONTEND

### Step 2.1: Models

- [ ] Create `modules/<domain>/<FEATURE_DIR>/models/<feature>.model.ts`
  - [ ] `<Entity>Dto` interface (matches backend Response DTO)
  - [ ] `<Entity>UsageDto` interface
  - [ ] `Create<Entity>Request` interface
  - [ ] `Update<Entity>Request` interface (omit immutable fields)
  - [ ] If child: repeat for child DTOs
  - [ ] `PagedResponse<T>`, `SearchFilter`, `FilterOperator`, `SearchSort`, `SearchRequest`

- [ ] Create `models/<feature>-form.model.ts`
  - [ ] `<Entity>FormModel` interface
  - [ ] `<Entity>FormMapper` const with `createEmpty()`, `fromDomain()`, `toCreateRequest()`, `toUpdateRequest()`

### Step 2.2: API Service

- [ ] Create `services/<feature>-api.service.ts`
- [ ] `@Injectable()` (NOT `providedIn: 'root'`)
- [ ] Extend `BaseApiService`
- [ ] Define `baseUrl`, `entityUrl` (and `childUrl` if applicable)
- [ ] Implement: `search`, `getById`, `create`, `update`, `toggleActive`, `delete`, `getUsage`
- [ ] If has child: `searchChildren`, `createChild`, `updateChild`, `toggleChildActive`, `deleteChild`, `getChildOptions`

### Step 2.3: Facade

- [ ] Create `facades/<feature>.facade.ts`
- [ ] `@Injectable()` (NOT `providedIn: 'root'`)
- [ ] Private signals: entities, loading, error, saving, saveError, lastSearchRequest (consolidated page/size/sort), filters, currentEntity, usageInfo
- [ ] If has child: detail signals (detailEntities, detailLoading, detailSaving, etc.)
- [ ] Public computed readonly for each signal. `currentPage`/`pageSize` derived from `lastSearchRequestSignal`
- [ ] Operations: `loadEntities()`, `applyGridStateAndLoad()`, `setFilters()`, `getById()`, `getUsageInfo()`
- [ ] Write operations: `create()`, `update()`, `toggleActive()`, `delete()` — each with `onSuccess?` callback
- [ ] If has child: `loadChildren()`, `createChild()`, `updateChild()`, `toggleChildActive()`, `deleteChild()` — with local signal updates
- [ ] `clearCurrentEntity()`, `resetChildState()` for cleanup
- [ ] `handleError()` using `extractBackendErrorCode` + `ErpErrorMapperService`

### Step 2.4: Confirm Actions

- [ ] Create `helpers/<feature>-confirm-actions.ts`
- [ ] Define `ConfirmActionDeps` interface
- [ ] `confirmToggle<Entity>Active()` — permission check → confirm dialog → facade call
- [ ] `confirmDelete<Entity>()` — permission check → usage check → confirm dialog → facade call
- [ ] If has child: `confirmToggleChildActive()`, `confirmDeleteChild()`

### Step 2.5: Grid Config

- [ ] Create `pages/<feature>-search/<feature>-grid.config.ts`
- [ ] `create<Entity>FilterOptions(translate)` → fields and operators
- [ ] `create<Entity>ColumnDefs(translate, zone, callbacks)` → ColDef[] with actions cell
- [ ] `create<Entity>GridOptions(translate)` → gridOptions + localeText
- [ ] Export `ERP_DEFAULT_COL_DEF`

### Step 2.6: Actions Cell Component

- [ ] Create `components/<feature>-actions-cell/<feature>-actions-cell.component.ts`
- [ ] Implement `ICellRendererAngularComp`
- [ ] Standalone, imports `ErpPermissionDirective`, `TranslateModule`
- [ ] Template: Edit button (`PERM_..._UPDATE`), Toggle button (`PERM_..._UPDATE`), Delete button (`PERM_..._DELETE`)
- [ ] Each button uses `erpPermission` directive

### Step 2.7: Page A — Search Component

- [ ] Create `pages/<feature>-search/<feature>-search.component.ts|html|scss`
- [ ] Standalone, `ChangeDetectionStrategy.OnPush`
- [ ] `providers: [<Entity>Facade, <Entity>ApiService]`
- [ ] Extend `ErpListComponent`, implement `OnInit`
- [ ] Inject: ThemeService, Router, TranslateService, facade, AuthenticationService, ConfirmActionDeps
- [ ] AG Grid theme from `createAgGridTheme()`
- [ ] Rebuild grid config on `translate.onLangChange`
- [ ] Implement `load(state)` → `facade.applyGridStateAndLoad()`
- [ ] Toolbar: Create button (`erpPermission`), Refresh button, Filter toggle
- [ ] Error state: `ErpEmptyStateComponent`
- [ ] Empty state: `ErpEmptyStateComponent`
- [ ] Template: `<ag-grid-angular>` with `[columnDefs]`, `[rowData]`, `[gridOptions]`, `[theme]`, `[localeText]`

### Step 2.8: Page B — Entry Component

- [ ] Create `pages/<feature>-entry/<feature>-entry.component.ts|html|scss`
- [ ] Standalone, `ChangeDetectionStrategy.OnPush`
- [ ] `providers: [<Entity>Facade, <Entity>ApiService]`
- [ ] Implement `OnInit`, `OnDestroy`
- [ ] Signal-based form model + Reactive `FormGroup`
- [ ] Route param subscription: determine create/edit mode
- [ ] Edit mode: load entity → patchValue → disable immutable fields → load children + usage
- [ ] Create mode: reset form
- [ ] `save()`: validate → create or update via FormMapper → facade call
- [ ] On create success: switch to edit mode in-place (`isEditMode=true`, `Location.replaceState()`) — no router navigation
- [ ] `ngOnDestroy`: `facade.clearCurrentEntity()`
- [ ] Permission checks before load
- [ ] Effects for save/detail error notifications
- [ ] Template: `<app-card>` with header buttons (back, cancel, save), `<form>` with `<erp-section>`, `<erp-form-field>` per field
- [ ] If has child: child section component + child form modal

### Step 2.9: Child Components (if applicable)

- [ ] Create `components/<child>-section/<child>-section.component.ts|html|scss`
  - [ ] Presentational: `@Input()` details/loading/sort, `@Output()` add/edit/toggle/delete/sort
  - [ ] No service injection
- [ ] Create `components/<child>-form-modal/<child>-form-modal.component.ts|html`
  - [ ] Self-contained: own FormGroup + NgbModal
  - [ ] `open(entity?)` — edit mode if entity provided (patch + disable code)
  - [ ] Emits `SaveEvent` with request + modalRef

### Step 2.10: Routing

- [ ] Add to `<domain>-routing.module.ts`:
  ```typescript
  {
    path: '<ENTITY_URL>',
    component: AdminLayout,
    children: [
      { path: '', loadComponent: () => import('.../search...'), canActivate: [authGuard, permissionGuard], data: { permission: 'PERM_<ENTITY_PERM>_VIEW' } },
      { path: 'create', loadComponent: () => import('.../entry...'), canActivate: [authGuard, permissionGuard], data: { permission: 'PERM_<ENTITY_PERM>_CREATE' } },
      { path: 'edit/:id', loadComponent: () => import('.../entry...'), canActivate: [authGuard, permissionGuard], data: { permission: 'PERM_<ENTITY_PERM>_UPDATE' } }
    ]
  }
  ```

### Step 2.11: Error Mappings

- [ ] Add backend error codes to `ErpErrorMapperService.errorMappings`:
  - [ ] `'<ENTITY>_NOT_FOUND': 'ERRORS.<ENTITY>_NOT_FOUND'`
  - [ ] `'<ENTITY>_KEY_DUPLICATE': 'ERRORS.<ENTITY>_DUPLICATE'`
  - [ ] `'<ENTITY>_FK_VIOLATION': 'ERRORS.<ENTITY>_IN_USE'`
  - [ ] etc.
- [ ] Add translation keys to `assets/i18n/en.json` and `assets/i18n/ar.json`

### Step 2.12: i18n

- [ ] Add translation keys for:
  - [ ] Page titles: `<FEATURE>S.TITLE`, `<FEATURE>S.CREATE`, `<FEATURE>S.EDIT`
  - [ ] Field labels: `<FEATURE>S.<FIELD_NAME>` (one per field)
  - [ ] Action labels: `<FEATURE>S.ACTIVATE/DEACTIVATE/DELETE`
  - [ ] Confirm messages: `<FEATURE>S.CONFIRM_ACTIVATE/DEACTIVATE/DELETE`
  - [ ] Hint messages: `<FEATURE>S.<FIELD>_READONLY_HINT`
  - [ ] Empty state: `<FEATURE>S.NO_<ENTITIES>`
  - [ ] Error messages (in `ERRORS` namespace)

---

## PHASE 3: VALIDATION

### Step 3.1: Backend Validation

- [ ] Compile: `mvn clean compile -pl erp-<MODULE_NAME> -am`
- [ ] Test: `mvn test -pl erp-<MODULE_NAME>`
- [ ] All unit tests pass
- [ ] No compilation errors or warnings

### Step 3.2: Frontend Validation

- [ ] Build: `ng build`
- [ ] No TypeScript errors
- [ ] No Angular compilation errors

### Step 3.3: Integration Validation

- [ ] Create → returns 201 with `ApiResponse<T>` envelope wrapping response DTO
- [ ] Update → returns 200 with `ApiResponse<T>` envelope, immutable fields unchanged
- [ ] GetById → returns 200 with full response
- [ ] Search → returns paginated results with correct filters/sort
- [ ] ToggleActive → respects child constraints
- [ ] Delete → checks references, returns 409 if blocked
- [ ] Usage → returns accurate counts and eligibility
- [ ] Frontend search page loads data with AG Grid
- [ ] Frontend create → save → switches to edit mode in-place (no page reload)
- [ ] Frontend edit → updates correctly, immutable fields disabled
- [ ] Permissions enforced at route, button, and API levels

---

## VERIFICATION CHECKLIST

```
[ ] Entity extends AuditableEntity with @SuperBuilder
[ ] PK is ID_PK with SEQUENCE (allocationSize=1)
[ ] FKs end with _ID_FK
[ ] Booleans use BooleanNumberConverter
[ ] @PrePersist is sole canonical location for uppercase key normalization (NOT in mapper or service)
[ ] Repository extends both JpaRepository + JpaSpecificationExecutor
[ ] All DTOs have @Schema and @Valid annotations
[ ] UpdateRequest excludes immutable fields
[ ] Mapper has null checks, uses Boolean.TRUE.equals(), does NOT apply uppercase
[ ] Service has @PreAuthorize on every public method
[ ] Service has @Transactional (readOnly for reads)
[ ] Service has @CacheEvict on write operations
[ ] ALL errors use LocalizedException — NOT NotFoundException
[ ] Not-found: LocalizedException(Status.NOT_FOUND, <Module>ErrorCodes.<ENTITY>_NOT_FOUND, id)
[ ] Service methods return ServiceResult<T> (except delete which is void)
[ ] create() uses Status.CREATED, update()/toggleActive() use Status.UPDATED
[ ] Controller injects OperationCode and uses craftResponse() for all non-delete endpoints
[ ] Controller does NOT use @ResponseStatus(CREATED) — handled by ServiceResult Status mapping
[ ] Controller has @ResponseStatus(NO_CONTENT) ONLY on DELETE
[ ] Controller has @Operation on every endpoint
[ ] Unit tests assert ServiceResult: result.isSuccess(), result.getData(), result.getStatusCode()
[ ] Frontend models match backend DTOs exactly
[ ] API service extends BaseApiService
[ ] Facade pagination in lastSearchRequestSignal (not separate page/size/sort signals)
[ ] Facade uses signals (private) + computed (public)
[ ] Components are standalone + OnPush
[ ] Facade + ApiService provided at component level, not root
[ ] Numeric form→DTO mappings use ?? (nullish coalescing), NOT ||
[ ] Grid config in separate file, rebuilt on language change
[ ] Routing has auth + permission guards on every route
[ ] i18n keys added for both EN and AR
[ ] Error codes mapped in ErpErrorMapperService
```
