---
description: "ERROR HANDLING ENFORCER — ensures ALL exceptions use LocalizedException with domain-specific ErrorCodes. Rejects NotFoundException, RuntimeException, hardcoded strings. Validates error code registration in messages.properties (EN/AR)."
---

# Skill: enforce-error-handling

## Name
`enforce-error-handling`

## Description
**ERROR HANDLING GOVERNANCE ENFORCER.** Ensures that ALL error handling follows the `LocalizedException` pattern with domain-specific error codes from `<Module>ErrorCodes`. This skill detects and rejects `NotFoundException`, raw `RuntimeException`, hardcoded error strings, and any non-standard error pattern.

## When to Use
- After any service or controller is created/modified
- When reviewing exception handling patterns
- When validating that error codes are properly registered
- As part of the full `validate-backend-feature` pipeline

---

## Core Rule (NON-NEGOTIABLE)

> **`NotFoundException` is NOT USED. EVER.**
>
> ALL not-found scenarios MUST throw:
> ```java
> new LocalizedException(Status.NOT_FOUND, <Module>ErrorCodes.<ENTITY>_NOT_FOUND, id)
> ```

---

## Enforcement Checklist

### CHECK 1: Exception Type Validation

```
[ ] No import of NotFoundException anywhere in the module
[ ] No usage of "new NotFoundException(" anywhere
[ ] No usage of "throw new RuntimeException(" for business errors
[ ] No usage of "throw new IllegalArgumentException(" for validation errors
[ ] ALL exceptions are LocalizedException(Status, ErrorCode, ...args)
```

### CHECK 2: LocalizedException Status Usage

| Scenario | Required Status | Error Code Pattern |
|----------|-----------------|-------------------|
| Entity not found | `Status.NOT_FOUND` | `<ENTITY>_NOT_FOUND` |
| Duplicate key/code | `Status.ALREADY_EXISTS` | `<ENTITY>_KEY_DUPLICATE` |
| FK constraint violation | `Status.CONFLICT` | `<ENTITY>_FK_VIOLATION` |
| Cannot deactivate (active children) | `Status.CONFLICT` | `<ENTITY>_ACTIVE_CHILDREN_EXIST` |
| Cannot delete (has children) | `Status.CONFLICT` | `<ENTITY>_CHILDREN_EXIST` |
| Cannot delete (referenced) | `Status.CONFLICT` | `<ENTITY>_REFERENCES_EXIST` |

```
[ ] Every not-found uses Status.NOT_FOUND
[ ] Every duplicate uses Status.ALREADY_EXISTS
[ ] Every FK/constraint violation uses Status.CONFLICT
[ ] Status maps correctly to HTTP codes (404, 409, etc.)
```

### CHECK 3: Error Code Registration

```
[ ] Error codes defined in <Module>ErrorCodes.java as static final String constants
[ ] NO inline error strings in service methods
[ ] Each error code constant follows: <ENTITY>_<SCENARIO> pattern
[ ] Error codes class has private constructor (utility class)
[ ] Error codes class throws UnsupportedOperationException in constructor
```

### CHECK 4: Error Code Message Registration

```
[ ] Each error code has entry in messages.properties (EN)
[ ] Each error code has entry in messages_ar.properties (AR)
[ ] Message supports parameter substitution: {0}, {1}
[ ] Messages are human-readable (not technical stack traces)
```

### CHECK 5: Service Error Patterns

```
[ ] Every findById uses .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, ...))
[ ] Every uniqueness check throws LocalizedException(Status.ALREADY_EXISTS, ...)
[ ] Delete does NOT try-catch DataIntegrityViolationException — it propagates to GlobalExceptionHandler
[ ] Every toggleActive validates constraints before mutating
[ ] No catch-all exception handlers that swallow errors
```

### CHECK 6: Frontend Error Integration

```
[ ] Each backend error code is mapped in ErpErrorMapperService.errorMappings
[ ] Frontend uses extractBackendErrorCode(error) to get error code
[ ] Error codes mapped to translation keys: ERRORS.<ENTITY>_<SCENARIO>
[ ] Translation keys exist in en.json and ar.json
```

---

## Automatic Rejection Patterns

These patterns trigger IMMEDIATE rejection:

| Pattern Found | Rejection Reason |
|---------------|-----------------|
| `import ...NotFoundException` | Prohibited class — use `LocalizedException` |
| `new NotFoundException(` | Prohibited constructor — use `LocalizedException(Status.NOT_FOUND, ...)` |
| `throw new RuntimeException(` | Unstructured error — use `LocalizedException` |
| `throw new IllegalArgumentException(` | Use `LocalizedException(Status.BAD_REQUEST, ...)` |
| `throw new Exception(` | Too generic — use `LocalizedException` |
| `"Entity not found"` (inline string) | Must use error code constant |
| `"Duplicate"` (inline string) | Must use error code constant |
| `catch (Exception e) { /* ignored */ }` | Error swallowing prohibited |

---

## Error Code File Template

```java
public final class <Module>ErrorCodes {

    private <Module>ErrorCodes() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // <ENTITY> errors
    public static final String <ENTITY>_NOT_FOUND = "<ENTITY>_NOT_FOUND";
    public static final String <ENTITY>_KEY_DUPLICATE = "<ENTITY>_KEY_DUPLICATE";
    public static final String <ENTITY>_CODE_DUPLICATE = "<ENTITY>_CODE_DUPLICATE";
    public static final String <ENTITY>_FK_VIOLATION = "<ENTITY>_FK_VIOLATION";
    public static final String <ENTITY>_ACTIVE_CHILDREN_EXIST = "<ENTITY>_ACTIVE_CHILDREN_EXIST";
    public static final String <ENTITY>_CHILDREN_EXIST = "<ENTITY>_CHILDREN_EXIST";
    public static final String <ENTITY>_REFERENCES_EXIST = "<ENTITY>_REFERENCES_EXIST";
}
```

## Message Properties Template

```properties
# messages.properties
<ENTITY>_NOT_FOUND=<Entity> with ID {0} was not found
<ENTITY>_KEY_DUPLICATE=<Entity> with key ''{0}'' already exists
<ENTITY>_FK_VIOLATION=Cannot delete <Entity> with ID {0} because it is referenced by other records
<ENTITY>_ACTIVE_CHILDREN_EXIST=Cannot deactivate <Entity> with ID {0} because it has active child records
<ENTITY>_CHILDREN_EXIST=Cannot delete <Entity> with ID {0} because it has child records

# messages_ar.properties
<ENTITY>_NOT_FOUND=لم يتم العثور على <Entity> بالمعرف {0}
<ENTITY>_KEY_DUPLICATE=<Entity> بالمفتاح ''{0}'' موجود بالفعل
<ENTITY>_FK_VIOLATION=لا يمكن حذف <Entity> بالمعرف {0} لأنه مرتبط بسجلات أخرى
<ENTITY>_ACTIVE_CHILDREN_EXIST=لا يمكن إلغاء تفعيل <Entity> بالمعرف {0} لأنه يحتوي على سجلات فرعية فعالة
<ENTITY>_CHILDREN_EXIST=لا يمكن حذف <Entity> بالمعرف {0} لأنه يحتوي على سجلات فرعية
```

---

## Canonical Not-Found Pattern

```java
// ✅ CORRECT — domain-specific error code flows to frontend for i18n
Md<Entity> entity = repository.findById(id)
    .orElseThrow(() -> new LocalizedException(
        Status.NOT_FOUND,
        <Module>ErrorCodes.<ENTITY>_NOT_FOUND,
        id));

// ❌ REJECTED — NotFoundException bypasses error code contract
Md<Entity> entity = repository.findById(id)
    .orElseThrow(() -> new NotFoundException("Entity not found: " + id));
```

---

## Violation Response

```
❌ ERROR HANDLING VIOLATION

Rule: [Rule description]
Location: [File:Line]
Found: [e.g., new NotFoundException("...")]
Expected: new LocalizedException(Status.NOT_FOUND, <Module>ErrorCodes.<ENTITY>_NOT_FOUND, id)
Severity: CRITICAL

Impact: Frontend cannot display domain-specific localized error messages.
The error code contract is broken — frontend i18n mapping fails.

Fix: Replace with LocalizedException using domain error code.
```

---

## Enforcement Report Format

```
## Error Handling Governance Report

### Module: [Module Name]
### Date: [Date]

| Check | Rules | Passed | Failed |
|-------|-------|--------|--------|
| Exception Types | 5 | ? | ? |
| Status Usage | 4 | ? | ? |
| Code Registration | 5 | ? | ? |
| Message Registration | 4 | ? | ? |
| Service Patterns | 5 | ? | ? |
| Frontend Integration | 4 | ? | ? |
| **TOTAL** | **27** | **?** | **?** |

### Critical Violations:
[List any NotFoundException / RuntimeException usage]

### Missing Error Codes:
[List any unregistered error scenarios]

### Verdict: COMPLIANT / NON-COMPLIANT
```

---

## `erp-common-utils` ERROR HANDLING CLASSES

All error handling MUST use classes from `erp-common-utils` — do NOT create per-module equivalents:

| Class | Package | Usage |
|-------|---------|-------|
| `LocalizedException` | `com.example.erp.common.exception` | All business errors with i18n support |
| `Status` | `com.example.erp.common.domain.status` | Status codes: `NOT_FOUND`, `CONFLICT`, `VALIDATION_ERROR`, etc. |
| `CommonErrorCodes` | `com.example.erp.common.exception` | Shared error code constants (module-specific codes go in `<Module>ErrorCodes`) |
| `GlobalExceptionHandler` | `com.example.erp.common.web` | Centralized exception → API error mapping — do NOT duplicate |
| `ApiError` | `com.example.erp.common.web` | Error envelope included in `ApiResponse` — do NOT create custom error DTOs |

---

## RELATED SKILLS

| Skill | Purpose |
|-------|---------|
| `enforce-backend-contract` | Validates overall layered architecture compliance including `erp-common-utils` consumption |
| `enforce-caching-rules` | Validates caching eligibility and annotation rules |
| `validate-backend-feature` | Master validation across all layers with scoring |
