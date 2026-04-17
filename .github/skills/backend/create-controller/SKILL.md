---
description: "Generates a thin @RestController delegating ALL logic to the service. Phase 1, Step 1.8 — LAST step before tests. Enforces OperationCode.craftResponse, @Valid @RequestBody, @Operation, zero business logic, 204 for delete."
---

# Skill: create-controller

## Name
`create-controller`

## Description
Generates a thin REST controller that delegates ALL logic to the service layer. This is **Phase 1, Step 1.8** of the execution template — the LAST implementation step before tests.

## When to Use
- After `create-service` is complete (Step 1.7)
- When Phase 1, Step 1.8 of the execution template is being executed
- This is the final layer before unit tests

## Prerequisites
- Service with all CRUD methods returning `ServiceResult<T>`
- `OperationCode` bean available from `erp-common-utils`
- All DTOs defined (CreateRequest, UpdateRequest, Response, SearchRequest, UsageResponse)

## Responsibilities

- Generate a thin `@RestController` that delegates ALL logic to the service layer
- Use `OperationCode.craftResponse()` for all non-delete response wrapping
- Apply `@Valid` on all `@RequestBody` parameters
- Apply `@Operation` Swagger annotations on all endpoints
- Use `@ResponseStatus(HttpStatus.NO_CONTENT)` ONLY on `@DeleteMapping`
- ZERO business logic in the controller — pure delegation

## Constraints

- MUST NOT generate entity, repository, DTO, mapper, or service code
- MUST NOT inject repositories or mappers — controller injects ONLY service + `OperationCode`
- MUST NOT reference entity classes — use DTOs exclusively
- MUST NOT use `@ResponseStatus(CREATED)` — HTTP 201 is derived from `ServiceResult.Status`
- MUST NOT contain any business logic, validation, or conditional branching

## Output

- Single file: `backend/erp-<MODULE>/src/main/java/com/example/<module>/controller/<Entity>Controller.java`

---

## Steps

### 1. Create Controller File
- **Location:** `backend/erp-<MODULE_NAME>/src/main/java/com/example/<module>/controller/<ENTITY_NAME>Controller.java`

### 2. Class Declaration
```java
@RestController
@RequestMapping("/api/<module>/<entity-url>")
@RequiredArgsConstructor
@Tag(name = "<Entity> Management", description = "إدارة <Entity> - <Entity> Management API")
public class <ENTITY_NAME>Controller {

    private final <ENTITY_NAME>Service service;
    private final OperationCode operationCode;
```

### 3. Create Endpoint
```java
@PostMapping
@Operation(summary = "Create <Entity>", description = "إنشاء <Entity> جديد")
public ResponseEntity<ApiResponse<<ENTITY>Response>> create(
        @Valid @RequestBody <ENTITY>CreateRequest request) {
    return operationCode.craftResponse(service.create(request));
}
```

### 4. Update Endpoint
```java
@PutMapping("/{id}")
@Operation(summary = "Update <Entity>", description = "تحديث <Entity>")
public ResponseEntity<ApiResponse<<ENTITY>Response>> update(
        @PathVariable Long id,
        @Valid @RequestBody <ENTITY>UpdateRequest request) {
    return operationCode.craftResponse(service.update(id, request));
}
```

### 5. GetById Endpoint
```java
@GetMapping("/{id}")
@Operation(summary = "Get <Entity> by ID", description = "جلب <Entity> بالمعرف")
public ResponseEntity<ApiResponse<<ENTITY>Response>> getById(@PathVariable Long id) {
    return operationCode.craftResponse(service.getById(id));
}
```

### 6. Search Endpoint
```java
@PostMapping("/search")
@Operation(summary = "Search <Entity>s", description = "بحث في <Entity>")
public ResponseEntity<ApiResponse<Page<<ENTITY>Response>>> search(
        @Valid @RequestBody <ENTITY>SearchRequest searchRequest) {
    return operationCode.craftResponse(service.search(searchRequest));
}
```

### 7. Toggle Active Endpoint
```java
@PutMapping("/{id}/toggle-active")
@Operation(summary = "Toggle <Entity> active status", description = "تبديل حالة التفعيل")
public ResponseEntity<ApiResponse<<ENTITY>Response>> toggleActive(
        @PathVariable Long id,
        @Valid @RequestBody ToggleActiveRequest request) {
    return operationCode.craftResponse(service.toggleActive(id, request.getActive()));
}
```

### 8. Delete Endpoint
```java
@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
@Operation(summary = "Delete <Entity>", description = "حذف <Entity>")
public void delete(@PathVariable Long id) {
    service.delete(id);
}
```

### 9. Usage Endpoint
```java
@GetMapping("/{id}/usage")
@Operation(summary = "Get <Entity> usage", description = "معلومات استخدام <Entity>")
public ResponseEntity<ApiResponse<<ENTITY>UsageResponse>> getUsage(@PathVariable Long id) {
    return operationCode.craftResponse(service.getUsage(id));
}
```

### 10. Child Entity Endpoints (if applicable)
```java
// Under same controller — NOT a separate controller
@PostMapping("/details")
@Operation(summary = "Create detail", description = "إنشاء تفصيل")
public ResponseEntity<ApiResponse<<Child>Response>> createDetail(
        @Valid @RequestBody <Child>CreateRequest request) {
    return operationCode.craftResponse(childService.create(request));
}

@PostMapping("/details/search")
@Operation(summary = "Search details", description = "بحث في التفاصيل")
public ResponseEntity<ApiResponse<Page<<Child>Response>>> searchDetails(
        @Valid @RequestBody <Child>SearchRequest searchRequest) {
    return operationCode.craftResponse(childService.search(searchRequest));
}

@PutMapping("/details/{id}")
public ResponseEntity<ApiResponse<<Child>Response>> updateDetail(
        @PathVariable Long id,
        @Valid @RequestBody <Child>UpdateRequest request) {
    return operationCode.craftResponse(childService.update(id, request));
}

@PutMapping("/details/{id}/toggle-active")
public ResponseEntity<ApiResponse<<Child>Response>> toggleDetailActive(
        @PathVariable Long id,
        @Valid @RequestBody ToggleActiveRequest request) {
    return operationCode.craftResponse(childService.toggleActive(id, request.getActive()));
}

@DeleteMapping("/details/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void deleteDetail(@PathVariable Long id) {
    childService.delete(id);
}

@GetMapping("/details/options/{lookupKey}")
@Operation(summary = "Get detail options", description = "خيارات التفاصيل")
public ResponseEntity<ApiResponse<List<<Child>OptionResponse>>> getDetailOptions(
        @PathVariable String lookupKey) {
    return operationCode.craftResponse(childService.getOptions(lookupKey));
}
```

---

## SHARED LAYER MANDATE (`erp-common-utils`)

Before creating a controller, verify the following shared resources from `erp-common-utils` are consumed — do NOT reinvent:

| # | Requirement | Shared Class | Package |
|---|-------------|-------------|--------|
| SH.1 | Response mapping via `OperationCode.craftResponse()` | `OperationCode` / `OperationCodeImpl` | `com.example.erp.common.web` |
| SH.2 | Response envelope is `ApiResponse<T>` — handled automatically by `ApiResponseWrapper` | `ApiResponse` / `ApiResponseWrapper` | `com.example.erp.common.web` / `web.advice` |
| SH.3 | Exception handling by `GlobalExceptionHandler` — do NOT catch exceptions in controllers | `GlobalExceptionHandler` | `com.example.erp.common.web` |
| SH.4 | Pagination validation by `PageableValidator` / `PageableUtils` | `PageableUtils` | `com.example.erp.common.web.util` |
| SH.5 | Jackson serialization configured by `CommonJacksonConfig` — do NOT add custom ObjectMapper | `CommonJacksonConfig` | `com.example.erp.common.web.config` |

**Rules:**
- NEVER create custom response wrappers — use `operationCode.craftResponse(serviceResult)`
- NEVER catch exceptions in controllers — `GlobalExceptionHandler` handles all errors
- NEVER use `@ResponseStatus(CREATED)` — HTTP 201 is derived from `Status.CREATED` via `OperationCode`
- NEVER configure custom `ObjectMapper` — `CommonJacksonConfig` handles serialization globally
- NEVER implement pagination validation — `PageableUtils` enforces constraints automatically

> **Cross-reference:** After creating the controller, run [`enforce-backend-contract`](../enforce-backend-contract/SKILL.md) to verify compliance.

---

## Rules (STRICT — from implementation-contract.md)

| Rule ID | Rule | MUST |
|---------|------|------|
| A.6.2 | `@Tag(name, description)` with Arabic/English | YES |
| A.6.3 | Controller injects ONLY service(s) + `OperationCode` | YES |
| A.6.4 | Non-delete endpoints return `ResponseEntity<ApiResponse<T>>` via `operationCode.craftResponse()` | YES |
| A.6.5 | Delete: `@ResponseStatus(NO_CONTENT)` + `void` return — no ServiceResult | YES |
| A.6.6 | Search: `@PostMapping("/search")` — NOT GET with query params | YES |
| A.6.7 | Toggle: `@PutMapping("/{id}/toggle-active")` | YES |
| A.6.8 | Usage: `@GetMapping("/{id}/usage")` | YES |
| A.6.9 | Child endpoints under SAME controller (`/details/...`) | YES |
| A.6.10 | Every method has `@Operation(summary, description)` | YES |
| A.6.11 | Request bodies use `@Valid @RequestBody` | YES |
| A.6.12 | ZERO business logic — pure delegation | YES |

### HTTP Status Mapping (Automatic)

| ServiceResult Status | HTTP Status | How |
|---------------------|-------------|-----|
| `Status.CREATED` | 201 | Automatic via `OperationCode.craftResponse()` |
| `Status.UPDATED` | 200 | Automatic via `OperationCode.craftResponse()` |
| `Status.SUCCESS` | 200 | Automatic via `OperationCode.craftResponse()` |
| Delete (void) | 204 | `@ResponseStatus(NO_CONTENT)` on method |

> **`@ResponseStatus(CREATED)` is NOT used on POST** — the `Status.CREATED` in `ServiceResult` maps to 201 automatically.

---

## Violations (MUST NOT)

- ❌ Injecting repository, mapper, or entity in controller
- ❌ Any business logic (conditionals, validations, transformations) in controller
- ❌ Returning raw DTOs — must use `operationCode.craftResponse()`
- ❌ `@ResponseStatus(CREATED)` on POST — handled by `ServiceResult` mapping
- ❌ Wrapping `delete()` in `ServiceResult` or `craftResponse` — stays `void` with 204
- ❌ `GET` for search — must use `POST /search`
- ❌ Separate activate/deactivate endpoints — single `toggle-active`
- ❌ Separate controller for child entity — unified under parent controller
- ❌ Missing `@Valid` on `@RequestBody`
- ❌ Missing `@Operation` on any endpoint
- ❌ Missing `OperationCode` injection

---

## Example (Real ERP — MasterLookupController)

```java
@RestController
@RequestMapping("/api/masterdata/master-lookups")
@RequiredArgsConstructor
@Tag(name = "Master Lookup Management", description = "إدارة القوائم المرجعية - Master Lookup API")
public class MasterLookupController {

    private final MasterLookupService masterLookupService;
    private final LookupDetailService lookupDetailService;
    private final OperationCode operationCode;

    @PostMapping
    @Operation(summary = "Create master lookup")
    public ResponseEntity<ApiResponse<MasterLookupResponse>> create(
            @Valid @RequestBody MasterLookupCreateRequest request) {
        return operationCode.craftResponse(masterLookupService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update master lookup")
    public ResponseEntity<ApiResponse<MasterLookupResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody MasterLookupUpdateRequest request) {
        return operationCode.craftResponse(masterLookupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete master lookup")
    public void delete(@PathVariable Long id) {
        masterLookupService.delete(id);
    }

    // ... all other endpoints follow same thin pattern
}
```
