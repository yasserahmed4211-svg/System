# ERP System — Architectural Blueprint

> Extracted from the canonical reference: **MasterLookup / LookupDetail** implementation.
> This document describes the actual, enforced architecture — not aspirational rules.

---

## 1. Project Structure

### 1.1 Backend Module Layout

```
backend/
├── erp-common-utils/          # Shared base classes, exceptions, utilities (domain-free)
├── erp-security/              # Authentication, authorization, tenant isolation
├── erp-masterdata/            # Master data domain module
├── erp-finance-gl/            # General Ledger domain module
├── erp-main/                  # Application entry point, i18n, global config
└── pom.xml                    # Parent Maven POM (multi-module)
```

### 1.2 Backend Per-Module Package Structure

Each domain module follows this exact internal package layout:

```
com.example.<module>/
├── controller/                # REST controllers (thin, delegates to service)
├── service/                   # Business logic, transaction management
├── repository/                # JPA repositories (module-internal, never shared)
│   └── projection/            # Interface-based projections for optimized queries
├── entity/                    # JPA entities (module-internal, never exposed)
├── dto/                       # Request/Response DTOs (public API contract)
├── mapper/                    # Entity ↔ DTO mapping (one mapper per entity)
└── exception/                 # Module-specific error codes
```

### 1.3 Frontend Module Layout

```
frontend/src/app/
├── core/                      # Singleton services, guards, models (app-wide)
│   ├── services/              # AuthenticationService, LookupService
│   ├── models/                # Shared domain models (LookupDetail, etc.)
│   └── guards/                # authGuard, permissionGuard
├── shared/                    # Reusable components, directives, utilities
│   ├── base/                  # BaseApiService, ErpListComponent
│   ├── components/            # ErpFormField, ErpSection, ErpEmptyState, etc.
│   ├── directives/            # ErpPermissionDirective
│   ├── services/              # ErpNotificationService, ErpDialogService, ErpErrorMapperService
│   ├── ag-grid/               # AG Grid shared config, helpers
│   ├── models/                # ErpGridState, SpecFilter, SpecFieldOption
│   └── utils/                 # backend-error-message, etc.
├── modules/
│   └── <domain>/              # e.g., master-data/
│       └── <feature>/         # e.g., master-lookups/
│           ├── components/    # Dumb/Presentational components
│           ├── facades/       # State management + API orchestration
│           ├── helpers/       # Confirm action functions
│           ├── models/        # Feature-specific DTOs, form models
│           ├── pages/         # Smart/Container components (Page A, Page B)
│           └── services/      # Feature API service
└── theme/                     # Layout, theming
```

### 1.4 Frontend Per-Feature File Inventory

For each feature, the following files exist:

```
<feature>/
├── models/
│   ├── <feature>.model.ts                   # All DTOs, interfaces, search types
│   └── <feature>-form.model.ts              # Form model + FormMapper (factory/mapper)
├── services/
│   └── <feature>-api.service.ts             # HTTP calls, extends BaseApiService
├── facades/
│   └── <feature>.facade.ts                  # Signal-based state, API orchestration
├── helpers/
│   └── <feature>-confirm-actions.ts         # Confirmation dialog wrappers
├── pages/
│   ├── <feature>-search/                    # Page A: List/Search
│   │   ├── <feature>-search.component.ts
│   │   ├── <feature>-search.component.html
│   │   ├── <feature>-search.component.scss
│   │   └── <feature>-grid.config.ts         # AG Grid column defs, filter options
│   └── <feature>-entry/                     # Page B: Create/Edit
│       ├── <feature>-entry.component.ts
│       ├── <feature>-entry.component.html
│       └── <feature>-entry.component.scss
└── components/                              # Presentational sub-components
    ├── <feature>-actions-cell/              # AG Grid actions cell renderer
    ├── <child>-section/                     # Detail/child display section
    └── <child>-form-modal/                  # Detail/child create/edit modal
```

---

## 2. Technology Stack

### 2.1 Backend

| Layer        | Technology                    |
|-------------|-------------------------------|
| Framework    | Spring Boot 3.x              |
| ORM          | JPA / Hibernate              |
| Database     | Oracle (naming conventions)   |
| Security     | Spring Security + `@PreAuthorize` |
| Caching      | Spring Cache (`@Cacheable`, `@CacheEvict`) backed by Redis |
| Build        | Maven (multi-module)          |
| Validation   | Jakarta Validation (`@NotBlank`, `@Size`, `@NotNull`) |
| Code Gen     | Lombok (`@Data`, `@Builder`, `@SuperBuilder`, `@RequiredArgsConstructor`) |
| Response     | `ServiceResult<T>` + `OperationCode.craftResponse()` → `ApiResponse<T>` envelope (in `erp-common-utils`) |
| Testing      | JUnit 5 + Mockito             |
| API Docs     | Swagger / OpenAPI 3 (`@Operation`, `@Schema`, `@Tag`) |
| Search       | Custom `SearchRequest`/`SpecBuilder` in `erp-common-utils` |

### 2.2 Frontend

| Layer        | Technology                     |
|-------------|--------------------------------|
| Framework    | Angular 17+ (standalone components) |
| State        | Angular Signals (`signal`, `computed`, `effect`) |
| Grid         | AG Grid Angular                |
| Forms        | Reactive Forms (`FormBuilder`, `FormGroup`) |
| i18n         | `@ngx-translate/core`          |
| Modals       | `@ng-bootstrap/ng-bootstrap` (`NgbModal`) |
| HTTP         | Angular `HttpClient` via `BaseApiService` |
| Routing      | Angular Router + lazy loading (`loadComponent`) |
| Change Det.  | `ChangeDetectionStrategy.OnPush` |
| Auth         | `authGuard`, `permissionGuard`, `ErpPermissionDirective` |

---

## 3. Database Naming Conventions

| Element           | Convention                            | Example                          |
|-------------------|---------------------------------------|----------------------------------|
| Table name        | `<PREFIX>_<ENTITY>` (UPPER_SNAKE)     | `MD_MASTER_LOOKUP`               |
| Primary key col   | `ID_PK`                               | `ID_PK`                          |
| PK constraint     | `<TABLE>_PK`                          | `MD_MASTER_LOOKUP_PK` (implicit) |
| Foreign key col   | `<ENTITY>_ID_FK`                      | `MASTER_LOOKUP_ID_FK`            |
| FK constraint     | `FK_<TABLE>_<REF>`                    | `FK_MD_LOOKUP_DETAIL_MASTER`     |
| Unique constraint | `UK_<TABLE>_<DESC>`                   | `UK_MD_MASTER_LOOKUP_KEY`        |
| Index             | `IDX_<TABLE>_<COLUMN>`                | `IDX_MD_LOOKUP_DETAIL_MASTER_FK` |
| Boolean column    | `IS_<FIELD>` (stored as NUMBER 0/1)   | `IS_ACTIVE`                      |
| Sequence          | `<TABLE>_SEQ`                         | `MD_MASTER_LOOKUP_SEQ`           |
| Audit columns     | `CREATED_AT`, `CREATED_BY`, `UPDATED_AT`, `UPDATED_BY` | — |

---

## 4. Entity Architecture

### 4.1 Base Class

All business entities extend `AuditableEntity`:

```java
@MappedSuperclass
@EntityListeners(AuditEntityListener.class)
public abstract class AuditableEntity {
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "CREATED_BY", length = 100, nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "UPDATED_AT")
    private Instant updatedAt;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;
}
```

### 4.2 Entity Annotations Pattern

```java
@Entity
@Table(name = "<TABLE_NAME>",
    uniqueConstraints = { @UniqueConstraint(name = "UK_...", columnNames = {...}) },
    indexes = { @Index(name = "IDX_...", columnList = "...") }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Md<Entity> extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "<seq_name>")
    @SequenceGenerator(name = "<seq_name>", sequenceName = "<DB_SEQ>", allocationSize = 1)
    @Column(name = "ID_PK")
    private Long id;
    
    // Boolean fields use BooleanNumberConverter
    @Column(name = "IS_ACTIVE", nullable = false)
    @Builder.Default
    @Convert(converter = BooleanNumberConverter.class)
    private Boolean isActive = Boolean.TRUE;
}
```

### 4.3 Entity Lifecycle Hooks

- `@PrePersist`: Set defaults (isActive) and **canonical location for uppercase key normalization**
- `@PreUpdate`: Enforce immutable transformations (uppercase keys)
- Audit fields handled by `AuditEntityListener` (not in entity callbacks)

> **Governance Rule:** Uppercase/case normalization of natural keys (e.g., `lookupKey`, `code`) MUST happen exclusively in the entity's `@PrePersist`/`@PreUpdate`. Mappers and services must NOT duplicate this transformation. Services may use `.toUpperCase()` only for pre-save uniqueness checks (comparing against already-normalized DB values).

### 4.4 Relationship Rules

- Parent → Child: `@OneToMany(mappedBy, cascade = ALL, orphanRemoval = false, fetch = LAZY)`
- Child → Parent: `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "..._ID_FK", foreignKey = @ForeignKey(name = "FK_..."))`
- Computed aggregates: `@Formula("(SELECT COUNT(*) ...")` to avoid lazy loading collections

---

## 5. Security Architecture

### 5.1 Permission Naming

```
PERM_<ENTITY>_VIEW
PERM_<ENTITY>_CREATE
PERM_<ENTITY>_UPDATE
PERM_<ENTITY>_DELETE
```

### 5.2 Backend Enforcement

```java
@PreAuthorize("hasAuthority(T(com.example.security.constants.SecurityPermissions).<ENTITY>_<ACTION>)")
```

Constants defined in `SecurityPermissions.java` (centralized).

### 5.3 Frontend Enforcement

- **Route guards**: `canActivate: [authGuard, permissionGuard]` with `data: { permission: 'PERM_...' }`
- **Button/UI visibility**: `erpPermission="PERM_..."` directive
- **Programmatic checks**: `authService.hasPermission('PERM_...')` before operations

---

## 6. Error Handling Architecture

### 6.1 Backend

- **`LocalizedException(Status, ErrorCode, ...args)`** — the ONLY exception type for ALL business and not-found errors
  - `Status.NOT_FOUND` — entity not found (HTTP 404), uses domain error codes (e.g., `MASTER_LOOKUP_NOT_FOUND`)
  - `Status.ALREADY_EXISTS` — duplicate key/code (HTTP 409)
  - `Status.CONFLICT` — FK violations, cannot deactivate/delete (HTTP 409)
- `DataIntegrityViolationException` — handled globally by `GlobalExceptionHandler` (no try-catch in services)
- Error codes: centralized in `<Module>ErrorCodes.java` (e.g., `MasterDataErrorCodes`)
- i18n messages: `messages.properties` / `messages_ar.properties` in `erp-main`

> **Governance Rule:** `NotFoundException` is NOT used. All not-found scenarios MUST throw `LocalizedException(Status.NOT_FOUND, <Module>ErrorCodes.<ENTITY>_NOT_FOUND, id)` to ensure domain-specific error codes flow to the frontend for proper i18n mapping.

### 6.2 Frontend

- `extractBackendErrorCode(error)` → extracts error code from HTTP response
- `ErpErrorMapperService.mapError(code)` → maps to translation key
- `ErpNotificationService` → displays translated error/success/warning messages
- Facade `handleError()` method sets error signal; effects display notifications

---

## 7. API Contract Architecture

### 7.1 URL Pattern

```
/api/<module>/<entities>                    # CRUD root
/api/<module>/<entities>/search             # POST search
/api/<module>/<entities>/{id}               # GET/PUT/DELETE by ID
/api/<module>/<entities>/{id}/toggle-active  # PUT toggle active
/api/<module>/<entities>/{id}/usage          # GET usage info
/api/<module>/<entities>/details             # Child entity CRUD (unified controller)
/api/<module>/<entities>/details/search      # Child search
/api/<module>/<entities>/details/options/{key} # Dropdown options
```

### 7.2 Search Contract (POST)

```json
{
  "filters": [{ "field": "...", "operator": "EQUALS|CONTAINS|STARTS_WITH", "value": "..." }],
  "sorts": [{ "field": "...", "direction": "ASC|DESC" }],
  "page": 0,
  "size": 20
}
```

### 7.3 Toggle Active Contract (PUT)

```json
{ "active": true }
```

### 7.4 Response Codes

| Operation     | Success | Duplicate | FK Violation | Not Found |
|--------------|---------|-----------|--------------|-----------|
| Create        | 201     | 409       | —            | 404 (parent) |
| Update        | 200     | —         | —            | 404       |
| Delete        | 204     | —         | 409          | 404       |
| Toggle Active | 200     | —         | 409 (constraints) | 404  |
| Get / Search  | 200     | —         | —            | 404       |

### 7.5 Response Envelope Architecture (ServiceResult + OperationCode)

All API responses (except DELETE 204) are wrapped in a unified `ApiResponse<T>` envelope:

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "error": null,
  "timestamp": "2024-01-15T10:30:00.000Z"
}
```

**Backend flow:**

```
Service → ServiceResult<T> → Controller → OperationCode.craftResponse() → ResponseEntity<ApiResponse<T>>
```

**Key components** (all in `erp-common-utils`):

| Component | Location | Purpose |
|-----------|----------|---------|
| `ServiceResult<T>` | `com.example.erp.common.domain.status` | Service return wrapper: carries data + `StatusCode` (e.g., `Status.CREATED`, `Status.UPDATED`, `Status.SUCCESS`) |
| `StatusCode` | `com.example.erp.common.domain.status` | Interface defining `getCode()`, `getMessage()`, `getCategory()` |
| `Status` enum | `com.example.erp.common.domain.status` | Implements `StatusCode` — `CREATED`, `UPDATED`, `DELETED`, `SUCCESS`, `NOT_FOUND`, `CONFLICT`, `ALREADY_EXISTS`, etc. |
| `OperationCode` | `com.example.erp.common.web` | Interface with `craftResponse(ServiceResult<T>)` method — maps `StatusCode` → `HttpStatus` |
| `OperationCodeImpl` | `com.example.erp.common.web` | Bean implementing the StatusCode → HttpStatus mapping |
| `ApiResponse<T>` | `com.example.erp.common.web` | The envelope DTO with `success`, `message`, `data`, `error`, `timestamp` |
| `GlobalExceptionHandler` | `com.example.erp.common.web` | `@RestControllerAdvice` — catches `LocalizedException` and wraps in `ApiResponse` |

**StatusCode → HttpStatus mapping:**

| StatusCode | HTTP Status |
|-----------|-------------|
| `Status.SUCCESS` | 200 OK |
| `Status.CREATED` | 201 Created |
| `Status.UPDATED` | 200 OK |
| `Status.DELETED` | 200 OK |
| `Status.NOT_FOUND` | 404 Not Found |
| `Status.CONFLICT` | 409 Conflict |
| `Status.ALREADY_EXISTS` | 409 Conflict |

**Service usage pattern:**
```java
// Create — maps to HTTP 201
return ServiceResult.success(mapper.toResponse(saved), Status.CREATED);

// Update/Toggle — maps to HTTP 200
return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED);

// Read — maps to HTTP 200 (default)
return ServiceResult.success(mapper.toResponse(entity));

// Delete — stays void, no ServiceResult wrapping
public void delete(Long id) { ... }
```

**Controller usage pattern:**
```java
private final OperationCode operationCode;

@PostMapping
public ResponseEntity<ApiResponse<EntityResponse>> create(@Valid @RequestBody CreateRequest request) {
    return operationCode.craftResponse(service.create(request));
}

@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete(@PathVariable Long id) {
    service.delete(id);  // No craftResponse — returns 204 directly
}
```

**Frontend compatibility:**
- `BaseApiService.unwrapResponse()` transparently handles the `ApiResponse<T>` envelope
- It checks for `.data` property — extracts if present, passes through if not
- **No frontend changes needed** when backend adopts this pattern

---

## 8. Caching Architecture

### 8.1 Cache Infrastructure

- **Backend cache provider**: Redis (distributed cache)
- **Abstraction layer**: Spring Cache (`@Cacheable`, `@CachePut`, `@CacheEvict`)
- **Frontend in-memory replay**: `shareReplay(1)` — restricted to lookup services only
- Redis-specific configuration resides in infrastructure config — never in domain code

### 8.2 Cache Eligibility Criteria

An entity qualifies for caching **only if ALL** of the following are true:

| Criterion | Threshold |
|-----------|-----------|
| Dataset size | < 500 records |
| Update frequency | Low (admin-initiated, infrequent) |
| Financial transactional impact | None |
| Workflow / state lifecycle | None |
| Cross-module reuse | Used by 2+ modules or system-wide |
| Usage pattern | Dropdowns, authorization checks, menu rendering |

**Eligible entities (exhaustive list):**

| Entity | Cache Name | Justification |
|--------|------------|---------------|
| Master Lookup | `lookupValues` | Small reference dataset, used across all modules |
| Lookup Details (low volatility) | `lookupDetailValues` | Dropdown options for forms system-wide |
| Login reference data | `loginReferenceData` | Loaded once per session, rarely changes |
| Menu structure | `menuStructure` | Static navigation, admin-only mutations |
| Role definitions | `roleDefinitions` | Security reference, infrequent changes |
| Permission definitions | `permissionDefinitions` | Security reference, infrequent changes |

> **Governance Rule:** No entity may be added to the eligible list without satisfying ALL criteria above and receiving explicit governance approval.

### 8.3 Non-Eligible Entities (Cache Prohibited)

An entity MUST NOT be cached if **any** of the following are true:

- Transactional entity (e.g., Journal Entries, Invoices)
- Financial impact entity (e.g., GL Accounts with balances, Vouchers)
- Approval-based / workflow entity (e.g., entities with status lifecycle)
- High write frequency
- Per-user data (e.g., user preferences, session tokens)
- Session-based data
- Large or dynamic search result sets

> **Non-negotiable:** GL, Billing, and any future financial module entities are permanently excluded from caching.

### 8.4 Cache Naming Convention

Cache names MUST be domain-specific, camelCase, and descriptive:

| Pattern | Example |
|---------|---------|
| `<domainConcept>` | `lookupValues` |
| `<domainConcept>` | `lookupDetailValues` |
| `<domainConcept>` | `roleDefinitions` |
| `<domainConcept>` | `menuStructure` |
| `<domainConcept>` | `permissionDefinitions` |
| `<domainConcept>` | `loginReferenceData` |

Prohibited naming: generic names (`cache1`, `data`, `entities`), module-prefixed (`md_lookupValues`), or technical names (`redisCache`).

### 8.5 TTL Policy

- **Default TTL**: No TTL — eviction is mutation-driven (explicit `@CacheEvict`)
- **Optional TTL**: May be configured at infrastructure level for safety (e.g., 24 hours max) as a fallback, but MUST NOT be relied upon as the primary eviction strategy
- **Session-scoped caches** (e.g., `loginReferenceData`): May use a shorter TTL aligned with session duration

### 8.6 Eviction Rules

Every mutation operation on a cached entity MUST evict the relevant cache:

| Operation | Eviction Required |
|-----------|------------------|
| Create | YES — `@CacheEvict(allEntries = true)` |
| Update | YES — `@CacheEvict(allEntries = true)` |
| Toggle Active | YES — `@CacheEvict(allEntries = true)` |
| Delete | YES — `@CacheEvict(allEntries = true)` |

- **No partial eviction** unless explicitly defined and governance-approved
- **`allEntries = true`** is the default eviction strategy for all eligible caches
- Eviction annotations are placed on the **service layer** — never on controllers or repositories

### 8.7 Cache Layer Responsibility

| Layer | Responsibility |
|-------|---------------|
| **Service (Backend)** | Sole owner of cache read (`@Cacheable`) and eviction (`@CacheEvict`) annotations |
| **Repository** | No caching annotations — ever |
| **Controller** | No caching annotations — ever |
| **Configuration** | Redis connection, serialization, TTL defaults |
| **Frontend Service** | `shareReplay(1)` allowed ONLY for lookup services |
| **Frontend Facade/Component** | No manual in-memory duplication of backend-cached data |

### 8.8 Consistency Rules

- Cache eviction MUST be co-located with the `@Transactional` write method (same method or same service call)
- If a transaction fails (rollback), the cache entry may still be evicted — this is acceptable (stale miss, not stale hit)
- The system tolerates cache-miss after eviction (next read repopulates) — this is by design
- No cross-service cache eviction: each service evicts only its own domain caches

### 8.9 Prohibited Caching Patterns

| # | Prohibited Pattern | Reason |
|---|-------------------|--------|
| 1 | Caching paginated search results | Combinatorial explosion, stale results |
| 2 | Caching per-user or session-scoped data in shared Redis | Security risk, memory bloat |
| 3 | Manual `RedisTemplate` calls in service/business code | Breaks abstraction, couples to Redis |
| 4 | Frontend manual `Map` or object-based caching of backend data | Duplicates backend cache, stale data risk |
| 5 | Caching entities with financial impact | Data integrity risk |
| 6 | Caching transactional or workflow entities | State lifecycle corruption risk |
| 7 | Partial key-based eviction without governance approval | Risk of stale entries |
| 8 | `@CachePut` without paired `@CacheEvict` on mutations | Inconsistent cache state |
| 9 | Caching at repository or controller layer | Breaks layered architecture |
| 10 | Using `@Cacheable` on write methods | Semantic violation |

---

## 9. Dependency Direction (DAG)

```
erp-common-utils → erp-security → erp-masterdata → erp-finance-gl
                                 → erp-main (aggregator)
```

- No circular dependencies
- No cross-module repository access
- Inter-module communication via public service interfaces + DTOs only
