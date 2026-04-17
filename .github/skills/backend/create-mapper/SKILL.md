---
description: "Generates the @Component entity-to-DTO mapper. Phase 1, Step 1.4 — AFTER DTOs, BEFORE service. Enforces toEntity, updateEntityFromRequest (void, skips immutables), toResponse with Boolean.TRUE.equals, toUsageResponse."
---

# Skill: create-mapper

## Name
`create-mapper`

## Description
Generates the entity-to-DTO mapper class for the ERP system. This is **Phase 1, Step 1.4** of the execution template. One mapper per entity, manual mapping (no MapStruct).

## When to Use
- After `create-dto` is complete (Step 1.3)
- When Phase 1, Step 1.4 of the execution template is being executed
- BEFORE creating service or controller

## Responsibilities

- Generate a `@Component` mapper class with manual entity-to-DTO mapping (no MapStruct)
- Implement `toEntity()` for CreateRequest → Entity conversion
- Implement `updateEntityFromRequest()` as void method that mutates entity in-place, skipping immutable fields
- Implement `toResponse()` mapping booleans with `Boolean.TRUE.equals()`
- Implement `toUsageResponse()` for usage/dependency checks
- Handle null input gracefully in all methods

## Constraints

- MUST NOT generate entity, repository, DTO, service, or controller code
- MUST NOT set FK relationships in `toEntity()` — service sets those
- MUST NOT apply uppercase/case normalization — entity `@PrePersist` handles it
- MUST NOT update natural keys or FK references in `updateEntityFromRequest()`
- MUST NOT call repository or service from mapper — mappers are pure

## Output

- Single file: `backend/erp-<MODULE>/src/main/java/com/example/<module>/mapper/<Entity>Mapper.java`

---

## Steps

### 1. Create Mapper File
- **Location:** `backend/erp-<MODULE_NAME>/src/main/java/com/example/<module>/mapper/<ENTITY_NAME>Mapper.java`

### 2. Class Declaration
```java
@Component
public class <ENTITY_NAME>Mapper {
```

### 3. toEntity (CreateRequest → Entity)

**Root entity (no parent FK):**
```java
public Md<ENTITY_NAME> toEntity(<ENTITY_NAME>CreateRequest request) {
    if (request == null) return null;
    return Md<ENTITY_NAME>.builder()
            .fieldName(request.getFieldName())
            // Do NOT apply .toUpperCase() — entity @PrePersist handles it
            .isActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE)
            .build();
}
```

**Child entity (has parent FK — compile-time safety):**
```java
public Md<CHILD_NAME> toEntity(<CHILD_NAME>CreateRequest request, Md<PARENT_NAME> parent) {
    if (request == null) return null;
    return Md<CHILD_NAME>.builder()
            .fieldName(request.getFieldName())
            .isActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE)
            .masterEntity(parent)  // FK set at compile-time — caller cannot forget
            .build();
}
```

### 4. updateEntityFromRequest (Mutate In-Place)
```java
public void updateEntityFromRequest(Md<ENTITY_NAME> entity, <ENTITY_NAME>UpdateRequest request) {
    if (entity == null || request == null) return;
    // ❌ NEVER update natural keys (lookupKey, code)
    // ❌ NEVER update FK references (parentId)
    entity.setDescriptionEn(request.getDescriptionEn());
    entity.setDescriptionAr(request.getDescriptionAr());
}
```

### 5. toResponse (Entity → Response DTO)
```java
public <ENTITY_NAME>Response toResponse(Md<ENTITY_NAME> entity) {
    if (entity == null) return null;
    return <ENTITY_NAME>Response.builder()
            .id(entity.getId())
            .fieldName(entity.getFieldName())
            .isActive(Boolean.TRUE.equals(entity.getIsActive()))
            // Include computed counts if parent
            .childCount(entity.getChildCount() != null ? entity.getChildCount() : 0)
            // Audit fields — ALWAYS mapped
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
}
```

### 6. toOptionResponse (if applicable)
```java
public <ENTITY_NAME>OptionResponse toOptionResponse(Md<ENTITY_NAME> entity) {
    if (entity == null) return null;
    return <ENTITY_NAME>OptionResponse.builder()
            .id(entity.getId())
            .label(entity.getDescriptionEn())
            .code(entity.getCode())
            .build();
}
```

### 7. toUsageResponse
```java
public <ENTITY_NAME>UsageResponse toUsageResponse(Md<ENTITY_NAME> entity, long childCount) {
    if (entity == null) return null;
    boolean canDelete = childCount == 0;
    boolean canDeactivate = childCount == 0; // or based on active children
    String reason = null;
    if (!canDelete) {
        reason = "Entity has " + childCount + " child records";
    }
    return <ENTITY_NAME>UsageResponse.builder()
            .id(entity.getId())
            .childCount(childCount)
            .canBeDeleted(canDelete)
            .canDeactivate(canDeactivate)
            .reason(reason)
            .build();
}
```

---

## SHARED LAYER MANDATE (`erp-common-utils`)

Before creating a mapper, verify the following shared conventions from `erp-common-utils` are respected — do NOT bypass:

| # | Requirement | Shared Convention | Why |
|---|-------------|------------------|-----|
| SH.1 | Boolean mapping uses `Boolean.TRUE.equals()` | Safe null handling | Avoids `NullPointerException` on unboxing |
| SH.2 | Uppercase/case normalization NOT done in mapper | Entity `@PrePersist` handles it | Single canonical location for normalization |
| SH.3 | FK relationships NOT set in `toEntity()` | Service sets FK via repository lookup | Mapper should not contain query logic |
| SH.4 | Date formatting uses `TimestampUtils` from common-utils if needed | `TimestampUtils` | Consistent ISO-8601 formatting |
| SH.5 | Audit fields mapped via `AuditableEntity` getters — no manual audit mapping | `AuditableEntity` | Audit fields managed by `AuditEntityListener` |

**Rules:**
- NEVER format dates manually — use `TimestampUtils` from `erp-common-utils`
- NEVER duplicate boolean null-safety patterns — always use `Boolean.TRUE.equals()`
- NEVER set audit fields in mappers — they are managed by `AuditEntityListener`

> **Cross-reference:** After creating the mapper, run [`enforce-backend-contract`](../enforce-backend-contract/SKILL.md) to verify compliance.

---

## Rules (STRICT — from implementation-contract.md)

| Rule ID | Rule | MUST |
|---------|------|------|
| A.4.2 | `toEntity()` for child entities accepts parent entity as parameter (compile-time FK safety) | YES |
| A.4.3 | `updateEntityFromRequest()` returns `void`, mutates entity in-place | YES |
| A.4.4 | `updateEntityFromRequest()` does NOT update natural keys or FK refs | YES |
| A.4.5 | `toResponse()` maps booleans with `Boolean.TRUE.equals(entity.getIsActive())` | YES |
| A.4.6 | All mapper methods handle `null` input gracefully (return null) | YES |
| A.4.7 | `toUsageResponse()` computes `canBeDeleted`/`canDeactivate` from counts | YES |

---

## Violations (MUST NOT)

- ❌ Inline mapping in service or controller — must use mapper
- ❌ Mapper calling repository or any service
- ❌ Mapper applying `.toUpperCase()` to natural keys — entity `@PrePersist` handles it
- ❌ Implicit FK contract — child `toEntity()` must accept parent entity as parameter, not rely on service to call `setParent()` afterwards
- ❌ `updateEntityFromRequest()` returning a new entity — must mutate in-place
- ❌ Updating `lookupKey`, `code`, or `parentId` in `updateEntityFromRequest()`
- ❌ Using `entity.getIsActive()` directly without `Boolean.TRUE.equals()` — null-unsafe
- ❌ Missing null checks on mapper methods
- ❌ Hardcoded `canBeDeleted = true` — must be computed from counts
- ❌ Using MapStruct — manual mapping is the project standard
- ❌ Missing audit fields in `toResponse()` — `createdAt`, `createdBy`, `updatedAt`, `updatedBy` are mandatory

---

## Example (Real ERP — MasterLookupMapper)

```java
@Component
public class MasterLookupMapper {

    public MdMasterLookup toEntity(MasterLookupCreateRequest request) {
        if (request == null) return null;
        return MdMasterLookup.builder()
                .lookupKey(request.getLookupKey())  // NOT .toUpperCase() — @PrePersist does it
                .descriptionEn(request.getDescriptionEn())
                .descriptionAr(request.getDescriptionAr())
                .isActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE)
                .build();
    }

    public void updateEntityFromRequest(MdMasterLookup entity, MasterLookupUpdateRequest request) {
        if (entity == null || request == null) return;
        // ❌ NEVER: entity.setLookupKey(...) — IMMUTABLE
        entity.setDescriptionEn(request.getDescriptionEn());
        entity.setDescriptionAr(request.getDescriptionAr());
    }

    public MasterLookupResponse toResponse(MdMasterLookup entity) {
        if (entity == null) return null;
        return MasterLookupResponse.builder()
                .id(entity.getId())
                .lookupKey(entity.getLookupKey())
                .descriptionEn(entity.getDescriptionEn())
                .descriptionAr(entity.getDescriptionAr())
                .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .detailCount(entity.getDetailCount() != null ? entity.getDetailCount() : 0)
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public MasterLookupUsageResponse toUsageResponse(MdMasterLookup entity, long detailCount) {
        if (entity == null) return null;
        boolean canDelete = detailCount == 0;
        boolean canDeactivate = detailCount == 0;
        return MasterLookupUsageResponse.builder()
                .id(entity.getId())
                .detailCount(detailCount)
                .canBeDeleted(canDelete)
                .canDeactivate(canDeactivate)
                .reason(!canDelete ? "Lookup has " + detailCount + " detail records" : null)
                .build();
    }
}
```
