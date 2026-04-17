---
description: "Generates a JPA repository interface extending JpaRepository + JpaSpecificationExecutor. Phase 1, Step 1.2 — AFTER entity, BEFORE DTOs. Enforces existsBy checks, JOIN FETCH queries, and JPQL count queries."
---

# Skill: create-repository

## Name
`create-repository`

## Description
Generates a JPA repository interface for the ERP system following the canonical pattern. This is **Phase 1, Step 1.2** of the execution template. MUST be created AFTER entity, BEFORE DTOs.

## When to Use
- After `create-entity` is complete (Step 1.1)
- When Phase 1, Step 1.2 of the execution template is being executed
- BEFORE creating DTOs, mapper, service, or controller

## Responsibilities

- Generate a JPA repository interface extending `JpaRepository` and `JpaSpecificationExecutor`
- Define `existsBy<Field>()` methods for uniqueness checks
- Define `existsBy<Field>AndIdNot()` ONLY if the unique field is mutable on update
- Add `countChildren()` and `countActiveChildren()` JPQL queries if parent entity
- Add `JOIN FETCH` queries for child entities loading parent relationships

## Constraints

- MUST NOT generate entity, DTO, mapper, service, or controller code
- MUST NOT modify existing repository files unless explicitly requested
- MUST NOT assume entity field names — derive from entity definition
- MUST NOT add `existsBy<Field>AndIdNot()` for immutable natural keys
- Repository is module-internal — MUST NOT be designed for cross-module use

## Output

- Single file: `backend/erp-<MODULE>/src/main/java/com/example/<module>/repository/<Entity>Repository.java`

---

## Steps

### 1. Create Repository File
- **Location:** `backend/erp-<MODULE_NAME>/src/main/java/com/example/<module>/repository/<ENTITY_NAME>Repository.java`

### 2. Interface Declaration
```java
@Repository
public interface <ENTITY_NAME>Repository
    extends JpaRepository<Md<ENTITY_NAME>, Long>,
            JpaSpecificationExecutor<Md<ENTITY_NAME>> {
```

### 3. Standard Finders
```java
Optional<Md<ENTITY_NAME>> findBy<UniqueField>(<Type> value);
boolean existsBy<UniqueField>(<Type> value);
boolean existsBy<UniqueField>AndIdNot(<Type> value, Long id); // ONLY if unique field is mutable on update
```

### 4. Paginated Queries
```java
Page<Md<ENTITY_NAME>> findBy<Filter>(<Type> value, Pageable pageable);
```

### 5. Child Entity Queries (if parent)
```java
@Query("SELECT COUNT(c) FROM Md<CHILD> c WHERE c.<entity>.id = :entityId")
long countChildren(@Param("entityId") Long entityId);

@Query("SELECT COUNT(c) FROM Md<CHILD> c WHERE c.<entity>.id = :entityId AND c.isActive = true")
long countActiveChildren(@Param("entityId") Long entityId);
```

### 6. JOIN FETCH Queries (if child entity)
```java
@Query(value = "SELECT d FROM Md<ENTITY> d JOIN FETCH d.<parent> WHERE d.<parent>.id = :parentId",
       countQuery = "SELECT COUNT(d) FROM Md<ENTITY> d WHERE d.<parent>.id = :parentId")
Page<Md<ENTITY>> searchByParentId(@Param("parentId") Long parentId, Pageable pageable);
```

### 7. Validation Queries (for complex checks)
```java
@Query(value = "SELECT COUNT(*) FROM <TABLE> WHERE <FIELD> = :field AND IS_ACTIVE = 1",
       nativeQuery = true)
int countActiveByField(@Param("field") String field);
```

---

## SHARED LAYER MANDATE (`erp-common-utils`)

Before creating a new repository, verify the following shared resources from `erp-common-utils` are consumed — do NOT reinvent:

| # | Requirement | Shared Class | Package |
|---|-------------|-------------|--------|
| SH.1 | Search specifications built via `SpecBuilder` — do NOT write manual `Specification<E>` | `SpecBuilder` | `com.erp.common.search` |
| SH.2 | Pagination built via `PageableBuilder.from()` with sort field validation | `PageableBuilder` | `com.erp.common.search` |
| SH.3 | Allowed sort/filter fields validated via `AllowedFields` / `SetAllowedFields` | `SetAllowedFields` | `com.erp.common.search` |
| SH.4 | Active flag filtering via `ActiveFlagQueryHelper` | `ActiveFlagQueryHelper` | `com.erp.common.search` |
| SH.5 | Boolean field conversions handled by `BooleanFieldValueConverter` in search specs | `BooleanFieldValueConverter` | `com.erp.common.search` |

**Rules:**
- NEVER build JPA `Specification<E>` manually — use `SpecBuilder.build()`
- NEVER build `Pageable` manually — use `PageableBuilder.from()` with `ALLOWED_SORT_FIELDS`
- NEVER write custom sort field validation logic — use `SetAllowedFields`

> **Cross-reference:** After creating the repository, run [`enforce-backend-contract`](../enforce-backend-contract/SKILL.md) to verify compliance.

---

## Rules (STRICT — from implementation-contract.md)

| Rule ID | Rule | MUST |
|---------|------|------|
| A.2.2 | Annotated with `@Repository` | YES |
| A.2.3 | NEVER injected outside its own module | YES |
| A.2.4 | Existence checks use `boolean existsBy<Field>(...)` | YES |
| A.2.5 | Update uniqueness uses `existsBy<Field>AndIdNot(value, id)` — ONLY if that field is mutable on update | YES |
| A.2.6 | Child queries use `JOIN FETCH` in `@Query` to avoid N+1 | YES |
| A.2.7 | Count queries for reference checks use JPQL `@Query("SELECT COUNT()")` | YES |
| A.2.8 | Projection interfaces used for read-only multi-table queries | YES |
| A.2.9 | No dead code — every repository method must have at least one caller in the service | YES |

---

## Violations (MUST NOT)

- ❌ Missing `JpaSpecificationExecutor` — blocks search/filter functionality
- ❌ Missing `@Repository` annotation
- ❌ Using `findBy().isPresent()` for existence checks — use `existsBy<Field>()`
- ❌ Not excluding current ID in update validation — use `existsBy<Field>AndIdNot()`
- ❌ Derived queries that navigate paths causing N+1 — use `JOIN FETCH`
- ❌ Loading full collections to count — use `@Query("SELECT COUNT()")`
- ❌ Injecting repository in another module's service
- ❌ Returning full entities when only a subset is needed for read-only queries
- ❌ Dead code: repository methods that are never called by any service
- ❌ Adding `existsBy<Field>AndIdNot()` when that field is immutable on update (dead code by design)

---

## Example (Real ERP — MasterLookupRepository)

```java
@Repository
public interface MasterLookupRepository
    extends JpaRepository<MdMasterLookup, Long>,
            JpaSpecificationExecutor<MdMasterLookup> {

    Optional<MdMasterLookup> findByLookupKey(String lookupKey);

    boolean existsByLookupKey(String lookupKey);

    // Note: existsByLookupKeyAndIdNot() NOT defined — lookupKey is immutable on update

    @Query("SELECT COUNT(d) FROM MdLookupDetail d WHERE d.masterLookup.id = :masterLookupId")
    long countLookupDetails(@Param("masterLookupId") Long masterLookupId);

    @Query("SELECT COUNT(d) FROM MdLookupDetail d WHERE d.masterLookup.id = :masterLookupId AND d.isActive = true")
    long countActiveLookupDetails(@Param("masterLookupId") Long masterLookupId);
}
```

```java
@Repository
public interface LookupDetailRepository
    extends JpaRepository<MdLookupDetail, Long>,
            JpaSpecificationExecutor<MdLookupDetail> {

    @Query(value = "SELECT d FROM MdLookupDetail d JOIN FETCH d.masterLookup WHERE d.masterLookup.id = :masterLookupId",
           countQuery = "SELECT COUNT(d) FROM MdLookupDetail d WHERE d.masterLookup.id = :masterLookupId")
    Page<MdLookupDetail> searchByMasterLookupId(@Param("masterLookupId") Long masterLookupId, Pageable pageable);

    boolean existsByMasterLookupIdAndCode(Long masterLookupId, String code);

    boolean existsByMasterLookupIdAndCodeAndIdNot(Long masterLookupId, String code, Long id);

    Optional<MdLookupDetail> findByMasterLookup_LookupKeyAndCodeAndIsActive(
        String lookupKey, String code, Boolean isActive);
}
```
