---
description: "Generates the complete DTO set: CreateRequest, UpdateRequest, Response, SearchRequest, UsageResponse, OptionResponse. Phase 1, Step 1.3 — AFTER repository, BEFORE mapper. Enforces @Schema, i18n validation, immutability rules."
---

# Skill: create-dto

## Name
`create-dto`

## Description
Generates the complete set of DTO classes for a feature in the ERP system. This is **Phase 1, Step 1.3** of the execution template. Creates EXACTLY the required DTOs — no more, no less.

## When to Use
- After `create-repository` is complete (Step 1.2)
- When Phase 1, Step 1.3 of the execution template is being executed
- BEFORE creating mapper, service, or controller

---

## Steps

### 1. Create All Required DTO Classes

For each entity, create **exactly** these DTOs in `backend/erp-<MODULE_NAME>/src/main/java/com/example/<module>/dto/`:

| # | DTO Class | Purpose |
|---|-----------|---------|
| 1 | `<Entity>CreateRequest` | POST request body |
| 2 | `<Entity>UpdateRequest` | PUT request body (excludes immutable fields) |
| 3 | `<Entity>Response` | All GET/POST/PUT responses |
| 4 | `<Entity>SearchRequest` | POST /search body (extends `BaseSearchContractRequest`) |
| 5 | `<Entity>UsageResponse` | GET /{id}/usage response |
| 6 | `<Entity>OptionResponse` | *(if needed)* Dropdown options (slim DTO) |

### 2. CreateRequest
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new <Entity> - طلب إنشاء <Entity> جديد")
public class <Entity>CreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Unique key - المفتاح الفريد", example = "SAMPLE_KEY")
    private String fieldName;

    @Schema(description = "Active status - حالة التفعيل", example = "true")
    @Builder.Default
    private Boolean isActive = true;
}
```

### 3. UpdateRequest (Excludes Immutable Fields)
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update <Entity> - طلب تحديث <Entity>")
public class <Entity>UpdateRequest {

    // ❌ NO natural keys (lookupKey, code) — they are IMMUTABLE
    // ❌ NO FK references (parentId) — they are IMMUTABLE

    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Description - الوصف", example = "Updated description")
    private String descriptionEn;
}
```

### 4. Response (ALL fields + audit)
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "<Entity> response - استجابة <Entity>")
public class <Entity>Response {

    @Schema(description = "Unique identifier - المعرف الفريد")
    private Long id;

    // ALL business fields...

    @Schema(description = "Active status - حالة التفعيل")
    private Boolean isActive;

    // Computed counts (if parent)
    @Schema(description = "Number of child records - عدد السجلات الفرعية")
    private Integer childCount;

    // Audit fields — ALWAYS included
    @Schema(description = "Created timestamp - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Created by - أنشئ بواسطة")
    private String createdBy;

    @Schema(description = "Updated timestamp - تاريخ التحديث")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "Updated by - حُدّث بواسطة")
    private String updatedBy;
}
```

### 5. SearchRequest
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Schema(description = "Search request for <Entity> - طلب بحث <Entity>")
public class <Entity>SearchRequest extends BaseSearchContractRequest {
    // Inherits: filters, sorts, page, size
    // Override toCommonSearchRequest() ONLY if child (to exclude parent ID filter)
}
```

#### Child SearchRequest Pattern:
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Schema(description = "Search request for <ChildEntity>")
public class <ChildEntity>SearchRequest extends BaseSearchContractRequest {

    @Override
    public SearchRequest toCommonSearchRequest() {
        return toCommonSearchRequest(Set.of("parentId"));
    }

    public Long getParentId() {
        return extractLongFilter("parentId");
    }
}
```

### 6. UsageResponse
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Usage information for <Entity> - معلومات الاستخدام")
public class <Entity>UsageResponse {

    @Schema(description = "Entity ID")
    private Long id;

    @Schema(description = "Number of child references - عدد المراجع الفرعية")
    private long childCount;

    @Schema(description = "Can entity be deleted - هل يمكن حذف العنصر")
    private boolean canBeDeleted;

    @Schema(description = "Can entity be deactivated - هل يمكن إلغاء التفعيل")
    private boolean canDeactivate;

    @Schema(description = "Reason if actions are blocked - سبب حظر الإجراء")
    private String reason;
}
```

### 7. OptionResponse (if needed for dropdowns)
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Option for dropdown")
public class <Entity>OptionResponse {
    private Long id;
    private String label;
    private String code;
    // NO audit fields
}
```

---

## SHARED LAYER MANDATE (`erp-common-utils`)

Before creating DTOs, verify the following shared resources from `erp-common-utils` are consumed — do NOT reinvent:

| # | Requirement | Shared Class | Package |
|---|-------------|-------------|--------|
| SH.1 | `SearchRequest` DTO for search input already exists in common-utils | `SearchRequest` | `com.erp.common.search` |
| SH.2 | `SearchFilter` for filter criteria already exists in common-utils | `SearchFilter` | `com.erp.common.search` |
| SH.3 | `BaseSearchContractRequest` for API layer search mapping | `BaseSearchContractRequest` | `com.example.erp.common.dto` |
| SH.4 | API response envelope `ApiResponse<T>` — do NOT create per-module response wrappers | `ApiResponse` | `com.example.erp.common.web` |
| SH.5 | Field validation error items use `FieldErrorItem` | `FieldErrorItem` | `com.example.erp.common.web` |

**Rules:**
- NEVER redefine `SearchRequest`, `SearchFilter`, or `Op` in feature modules — import from `erp-common-utils`
- NEVER create a custom API response wrapper — use `ApiResponse<T>`
- `SearchRequest` in feature module `extends BaseSearchContractRequest` for API-layer conversion
- Validation messages MUST use i18n keys (`"{validation.required}"`) resolved by `LocaleConfig` from common-utils

> **Cross-reference:** After creating DTOs, run [`enforce-backend-contract`](../enforce-backend-contract/SKILL.md) to verify compliance.

---

## Rules (STRICT — from implementation-contract.md)

| Rule ID | Rule | MUST |
|---------|------|------|
| A.3.2 | Class-level `@Schema(description = "English - Arabic")` | YES |
| A.3.3 | Each field has `@Schema(description, example)` | YES |
| A.3.4 | Validation messages use i18n keys: `"{validation.required}"` | YES |
| A.3.5 | `CreateRequest` excludes `id` and audit fields | YES |
| A.3.6 | `UpdateRequest` excludes immutable fields (natural keys, FKs) | YES |
| A.3.7 | `Response` includes ALL fields + audit fields + computed counts | YES |
| A.3.8 | Audit timestamps: `@JsonFormat(shape = STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")` | YES |
| A.3.9 | `SearchRequest` extends `BaseSearchContractRequest` | YES |
| A.3.10 | Child `SearchRequest` overrides `toCommonSearchRequest()` to exclude parent ID | YES |
| A.3.11 | Child `SearchRequest` provides parent ID extractor method | YES |
| A.3.12 | `UsageResponse` has `canBeDeleted`/`canDeactivate` booleans + reason | YES |
| A.3.13 | `OptionResponse` is slim — no audit fields | YES |

---

## Violations (MUST NOT)

- ❌ Including `id` in `CreateRequest`
- ❌ Including immutable fields (`lookupKey`, `code`, `parentId`) in `UpdateRequest`
- ❌ Missing audit fields in `Response`
- ❌ Hardcoded English validation messages — use `"{validation.required}"`
- ❌ Missing `@Schema` documentation on class or fields
- ❌ Missing `@JsonFormat` on `Instant` audit fields
- ❌ Duplicating filter/sort parsing — must extend `BaseSearchContractRequest`
- ❌ Reusing full `Response` DTO for dropdowns — use `OptionResponse`
- ❌ Missing `canBeDeleted`/`canDeactivate` in `UsageResponse`
- ❌ Using `@SuperBuilder` on non-search DTOs (only `SearchRequest` needs it for `BaseSearchContractRequest`)

---

## Example (Real ERP)

```java
// MasterLookupCreateRequest
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Create a new master lookup - إنشاء قائمة مرجعية جديدة")
public class MasterLookupCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Unique lookup key - مفتاح البحث الفريد", example = "CURRENCY")
    private String lookupKey;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "English description", example = "Currency types")
    private String descriptionEn;

    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Arabic description", example = "أنواع العملات")
    private String descriptionAr;

    @Schema(description = "Active status", example = "true")
    @Builder.Default
    private Boolean isActive = true;
}

// MasterLookupUpdateRequest — NO lookupKey (immutable)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Update master lookup")
public class MasterLookupUpdateRequest {

    @Size(max = 200, message = "{validation.size}")
    private String descriptionEn;

    @Size(max = 200, message = "{validation.size}")
    private String descriptionAr;
}
```
