---
applyTo: 'backend/**'
---

# Backend Architecture Instructions

> Global Copilot instruction file for the ERP backend.
> Enforces architecture, naming, and behavioral rules extracted from the canonical governance documents.

---

## 1. Architecture Overview

### Layered Architecture (STRICT)

```
Controller → Service → Repository
```

| Layer | Responsibility |
|---|---|
| **Controller** | REST endpoint, input validation (`@Valid`), delegation to service. ZERO business logic. |
| **Service** | Business rules, transactions, security enforcement, caching, logging. |
| **Repository** | JPA data access. Module-internal only — NEVER shared across modules. |

### Supporting Layers

| Layer | Responsibility |
|---|---|
| **Entity** | JPA persistence model. Internal to the module — NEVER exposed outside. |
| **DTO** | Public API contract. ALL external communication uses DTOs. |
| **Mapper** | Converts Entity ↔ DTO. One `@Component` mapper per entity. |

### Response Envelope

- Services return `ServiceResult<T>` (except `delete()` which is `void`).
- Controllers convert via `operationCode.craftResponse(serviceResult)` → `ResponseEntity<ApiResponse<T>>`.
- HTTP status is derived from `ServiceResult.Status` automatically — do NOT use `@ResponseStatus(CREATED)`.

---

## 2. Core Rules

### 2.1 Layered Architecture

- MUST follow Controller → Service → Repository layering.
- MUST NOT inject repositories into controllers.
- MUST NOT inject mappers into controllers.
- MUST NOT reference entities in controllers.
- Controller injects ONLY service(s) + `OperationCode`.

### 2.2 DTO Contract

- MUST use DTOs for ALL request and response payloads.
- MUST NOT expose entities outside the module boundary.
- MUST NOT return entities from service public methods.
- `CreateRequest` MUST exclude `id` and audit fields.
- `UpdateRequest` MUST exclude immutable fields (natural keys, FK references).
- `Response` MUST include ALL entity fields + audit fields + computed counts.
- `UsageResponse` MUST include `canBeDeleted`, `canDeactivate`, and reason.
- `OptionResponse` MUST be a slim DTO (no audit fields) for dropdowns.
- `SearchRequest` MUST extend `BaseSearchContractRequest`.

### 2.3 ServiceResult Wrapper

- MUST return `ServiceResult<T>` from all service methods except `delete()`.
- MUST use `ServiceResult.success(dto, Status.CREATED)` for create.
- MUST use `ServiceResult.success(dto, Status.UPDATED)` for update and toggleActive.
- MUST use `ServiceResult.success(dto)` for read operations.
- `delete()` MUST return `void`.

### 2.4 Error Handling

- MUST use `LocalizedException(Status, ErrorCode, ...args)` for ALL errors.
- MUST NOT use `NotFoundException` — use `LocalizedException(Status.NOT_FOUND, ...)`.
- MUST NOT throw raw `RuntimeException` or any non-`LocalizedException`.
- MUST NOT hardcode error message strings.
- Error codes MUST come from centralized `<Module>ErrorCodes` constants.
- `DataIntegrityViolationException` MUST NOT be caught in services — handled by `GlobalExceptionHandler`.

### 2.5 Security

- MUST use `@PreAuthorize` on every public service method.
- MUST reference permission constants from `SecurityPermissions.java`.
- MUST NOT hardcode permission strings.
- Four permissions per entity: `VIEW`, `CREATE`, `UPDATE`, `DELETE`.
- Permission naming: `PERM_<ENTITY>_<ACTION>`.

### 2.6 Transaction Management

- MUST annotate every write method with `@Transactional`.
- MUST annotate every read method with `@Transactional(readOnly = true)`.
- MUST NOT omit transaction boundaries.

### 2.7 Database Naming

- Table names: `<MODULE_PREFIX>_<ENTITY>` in UPPER_SNAKE_CASE.
- Primary key column: always `ID_PK`.
- PK constraint: `<TABLE>_PK`.
- Foreign key columns: always `<REF>_ID_FK`.
- FK constraints: `FK_<TABLE>_<REF>`.
- Unique constraints: `UK_<TABLE>_<DESC>`.
- Indexes: `IDX_<TABLE>_<COLUMN>`.
- Boolean columns: `IS_<FIELD>` stored as NUMBER(1).
- Sequences: `<TABLE>_SEQ` with `allocationSize = 1`.
- Audit columns: `CREATED_AT`, `CREATED_BY`, `UPDATED_AT`, `UPDATED_BY`.

### 2.8 Entity Rules

- MUST extend `AuditableEntity`.
- MUST use `@SuperBuilder` (not `@Builder`).
- MUST use `GenerationType.SEQUENCE` with explicit `@SequenceGenerator` (`allocationSize = 1`).
- MUST NOT use `GenerationType.IDENTITY` or `AUTO`.
- MUST use `BooleanNumberConverter` for all boolean columns.
- Boolean default: `@Builder.Default Boolean isActive = Boolean.TRUE`.
- MUST use `@ManyToOne(fetch = FetchType.LAZY)` for all FK relationships.
- MUST use `@OneToMany(cascade = ALL, orphanRemoval = false, fetch = LAZY)` for parent collections.
- MUST use `@Formula` for computed counts — MUST NOT load collections to count.
- MUST declare `@UniqueConstraint` and `@Index` in `@Table` annotation with explicit names.
- `@PrePersist` is the SOLE canonical location for uppercase/case normalization of keys.
- MUST provide `activate()` / `deactivate()` helper methods.

### 2.9 Repository Rules

- MUST extend both `JpaRepository<E, Long>` AND `JpaSpecificationExecutor<E>`.
- MUST be annotated with `@Repository`.
- MUST NEVER be injected outside its own module.
- MUST use `existsBy<Field>()` for existence checks — not `findBy().isPresent()`.
- MUST use `existsBy<Field>AndIdNot()` for update uniqueness checks.
- MUST use `JOIN FETCH` in `@Query` for relationship queries to avoid N+1.
- MUST use JPQL `COUNT` queries for reference checks — not collection loading.

### 2.10 Mapper Rules

- One `@Component` mapper per entity.
- `toEntity()` MUST NOT set FK relationships (service sets those).
- `toEntity()` MUST NOT apply uppercase/case normalization (entity `@PrePersist` handles it).
- `updateEntityFromRequest()` MUST return void and mutate entity in-place.
- `updateEntityFromRequest()` MUST NOT update natural keys or FK references.
- `toResponse()` MUST map booleans with `Boolean.TRUE.equals()`.
- All mapper methods MUST handle null input gracefully.

### 2.11 Logging

- `log.info()` for write operations.
- `log.debug()` for read operations.

---

## 3. Execution Flow

The mandatory implementation order for every new feature:

```
Entity → Repository → DTOs → Mapper → Error Codes → Permissions → Service → Controller → Unit Tests
```

Skipping or reordering steps is NOT allowed.

### Per-Step Summary

1. **Entity** — JPA entity extending `AuditableEntity`, DB conventions, lifecycle hooks.
2. **Repository** — `JpaRepository` + `JpaSpecificationExecutor`, existence checks, FETCH JOIN queries.
3. **DTOs** — `CreateRequest`, `UpdateRequest`, `Response`, `SearchRequest`, `UsageResponse`, `OptionResponse`.
4. **Mapper** — `@Component`, `toEntity()`, `updateEntityFromRequest()`, `toResponse()`, `toUsageResponse()`.
5. **Error Codes** — Constants in `<Module>ErrorCodes.java`, i18n message entries.
6. **Permissions** — Constants in `SecurityPermissions.java`, seed in DB.
7. **Service** — Business logic, `@PreAuthorize`, `@Transactional`, `@CacheEvict`, `ServiceResult<T>`.
8. **Controller** — Thin REST layer, `OperationCode.craftResponse()`, `@Operation` docs.
9. **Unit Tests** — `@ExtendWith(MockitoExtension.class)`, assert `ServiceResult` fields.

---

## 4. Validation Rules

Copilot MUST verify:

- No business logic in controllers (pure delegation only).
- No repository usage outside service layer.
- No entity reference in controller or DTO layer.
- Immutable fields (natural keys, FK references) are NOT updated in `UpdateRequest` or mapper.
- Error codes come from centralized `<Module>ErrorCodes` constants — never inline strings.
- Every service method has `@PreAuthorize`.
- Every write method has `@Transactional` and `@CacheEvict` (if entity is cache-eligible).
- Every read method has `@Transactional(readOnly = true)`.
- Sort fields are validated against `ALLOWED_SORT_FIELDS` whitelist.
- Search uses `SpecBuilder.build()` + `PageableBuilder.from()` from common-utils.
- `delete()` returns `void` — no `ServiceResult` wrapping.
- Controller uses `operationCode.craftResponse()` for ALL non-delete endpoints.
- Controller does NOT use `@ResponseStatus(CREATED)`.
- Controller uses `@ResponseStatus(HttpStatus.NO_CONTENT)` ONLY on `@DeleteMapping`.
- DTO validation annotations use i18n message keys (`{validation.required}`).
- Swagger `@Schema` is present on all DTO classes and fields.
- `@Operation` is present on all controller methods.

---

## 5. Caching Rules

### Eligibility

- ONLY governance-approved entities may be cached.
- Approved cache names: `lookupValues`, `lookupDetailValues`, `loginReferenceData`, `menuStructure`, `roleDefinitions`, `permissionDefinitions`.
- Entity MUST satisfy ALL criteria: dataset < 500 records, low update frequency, no financial impact, no workflow lifecycle, cross-module reuse, used in dropdowns or auth.
- If unsure whether an entity qualifies — it does NOT qualify.

### Annotation Rules

- `@Cacheable` — ONLY on service-layer read methods for eligible entities.
- `@CacheEvict(allEntries = true)` — on EVERY write method (create, update, toggleActive, delete) for cached entities.
- Annotation order on cached read: `@Cacheable` → `@Transactional(readOnly = true)` → `@PreAuthorize`.
- Annotation order on cached write: `@CacheEvict` → `@Transactional` → `@PreAuthorize`.
- `@Cacheable` and `@CacheEvict` MUST reference the same `cacheNames` value.

### Prohibited

- MUST NOT cache search/paginated results.
- MUST NOT cache transactional or financial data (GL, Billing).
- MUST NOT cache approval/workflow entities.
- MUST NOT cache per-user or session-scoped data in shared Redis.
- MUST NOT use `@CachePut` without paired `@CacheEvict` on all mutation paths.
- MUST NOT place caching annotations on repositories or controllers.
- MUST NOT use manual `RedisTemplate` calls in service code.
- MUST NOT use single-key eviction without governance approval.

---

## 6. Anti-Patterns (FORBIDDEN)

| Pattern | Why It's Wrong |
|---|---|
| Using `NotFoundException` | Bypasses domain error code contract. Use `LocalizedException(Status.NOT_FOUND, ...)`. |
| Returning entities from controllers | Breaks DTO isolation. Entities are module-internal. |
| Returning entities from services | Services MUST return `ServiceResult<T>` wrapping DTOs. |
| Direct repository injection in controller | Violates layered architecture. Controller talks only to service. |
| Missing `ServiceResult` wrapper | All service methods (except `delete`) MUST return `ServiceResult<T>`. |
| Hardcoded error messages | Error codes MUST come from `<Module>ErrorCodes` constants. |
| `@ResponseStatus(CREATED)` on POST endpoint | HTTP 201 is mapped automatically from `Status.CREATED` in `ServiceResult`. |
| Uppercase normalization in mapper or service | `@PrePersist` on entity is the sole canonical location. |
| `findBy().isPresent()` for existence checks | Use `existsBy<Field>()` repository methods. |
| Loading collections to count children | Use `@Formula` or JPQL `COUNT` queries. |
| In-memory pagination | Use `Pageable` and `Page<T>` at the database level. |
| Catching `DataIntegrityViolationException` in service | Let it propagate to `GlobalExceptionHandler`. |
| Sort fields without whitelist validation | Define `ALLOWED_SORT_FIELDS` in service and enforce it. |
| Using `@Builder` on entities | MUST use `@SuperBuilder` due to `AuditableEntity` inheritance. |
| `GenerationType.IDENTITY` or `AUTO` | MUST use `GenerationType.SEQUENCE` with explicit `@SequenceGenerator`. |
| `orphanRemoval = true` without analysis | Default is `orphanRemoval = false`. |
| Setting `isActive` directly from service | Use entity's `activate()` / `deactivate()` helper methods. |
| Missing `@Valid` on `@RequestBody` | Validation will not trigger without it. |
| Inline column definitions in controller | Grid and column configs belong in config files. |
| Separate activate/deactivate endpoints | Use single `toggle-active` endpoint. |

---

## 7. API Conventions

### URL Pattern

```
POST   /api/<module>/<entities>                 → create
PUT    /api/<module>/<entities>/{id}             → update
GET    /api/<module>/<entities>/{id}             → getById
POST   /api/<module>/<entities>/search           → search
PUT    /api/<module>/<entities>/{id}/toggle-active → toggleActive
DELETE /api/<module>/<entities>/{id}             → delete (204 No Content)
GET    /api/<module>/<entities>/{id}/usage       → getUsage
```

### Child Entity URLs (unified under parent controller)

```
POST   /api/<module>/<entities>/details          → createChild
PUT    /api/<module>/<entities>/details/{id}      → updateChild
POST   /api/<module>/<entities>/details/search    → searchChildren
DELETE /api/<module>/<entities>/details/{id}      → deleteChild
GET    /api/<module>/<entities>/details/options/{key} → childOptions
```

### Service Method Signatures

| Method | Return Type | Status | Transaction |
|---|---|---|---|
| `create()` | `ServiceResult<Response>` | `Status.CREATED` | `@Transactional` |
| `update()` | `ServiceResult<Response>` | `Status.UPDATED` | `@Transactional` |
| `getById()` | `ServiceResult<Response>` | `Status.SUCCESS` | `@Transactional(readOnly = true)` |
| `search()` | `ServiceResult<Page<Response>>` | `Status.SUCCESS` | `@Transactional(readOnly = true)` |
| `toggleActive()` | `ServiceResult<Response>` | `Status.UPDATED` | `@Transactional` |
| `delete()` | `void` | N/A (204) | `@Transactional` |
| `getUsage()` | `ServiceResult<UsageResponse>` | `Status.SUCCESS` | `@Transactional(readOnly = true)` |

---

## 8. Unit Test Rules

- MUST use `@ExtendWith(MockitoExtension.class)`.
- MUST mock repository and mapper with `@Mock`, inject service with `@InjectMocks`.
- MUST assert `ServiceResult` for non-delete methods:
  - `assertThat(result.isSuccess()).isTrue()`
  - `assertThat(result.getData()).isEqualTo(expected)`
  - `assertThat(result.getStatusCode()).isEqualTo(Status.CREATED)` (or `UPDATED`, etc.)
- MUST assert `LocalizedException.class` for error cases — not `NotFoundException`.
- Required test cases per service: `create_Success`, `create_Duplicate`, `update_Success`, `update_NotFound`, `getById_Success`, `getById_NotFound`, `toggleActive_Success`, `toggleActive_Constraints`, `delete_Success`, `delete_HasChildren`, `delete_Referenced`.
