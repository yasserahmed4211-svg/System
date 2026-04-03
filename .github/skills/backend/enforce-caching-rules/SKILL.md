---
description: "CACHING GOVERNANCE ENFORCER — validates @Cacheable/@CacheEvict usage against the approved entity list (lookupValues, roleDefinitions, etc.). Prevents unauthorized caching, wrong annotation order, and missing eviction on write methods."
---

# Skill: enforce-caching-rules

## Name
`enforce-caching-rules`

## Description
**CACHING GOVERNANCE ENFORCER.** Validates that caching annotations, cache names, and caching patterns comply with the strict caching architecture. Prevents unauthorized caching, stale data risks, and annotation misplacement.

## When to Use
- After any service is created or modified
- When `@Cacheable` or `@CacheEvict` is added anywhere
- When reviewing code that touches cached entities
- When a developer proposes caching a new entity
- As part of the full `validate-backend-feature` pipeline

---

## Cache Eligibility Gate (FIRST CHECK)

Before allowing ANY caching annotation, verify the entity against the eligibility list:

### Approved Cacheable Entities (EXHAUSTIVE — NO ADDITIONS without governance)

| Entity | Cache Name | Justification |
|--------|------------|---------------|
| Master Lookup | `lookupValues` | Small ref dataset, cross-module |
| Lookup Details | `lookupDetailValues` | Dropdown options system-wide |
| Login reference data | `loginReferenceData` | Per-session, rarely changes |
| Menu structure | `menuStructure` | Static nav, admin-only mutations |
| Role definitions | `roleDefinitions` | Security ref, infrequent changes |
| Permission definitions | `permissionDefinitions` | Security ref, infrequent changes |

### Eligibility Criteria (ALL must be TRUE)

| # | Criterion | Threshold |
|---|-----------|-----------|
| 1 | Dataset size | < 500 records |
| 2 | Update frequency | Low (admin-initiated) |
| 3 | Financial transactional impact | NONE |
| 4 | Workflow / state lifecycle | NONE |
| 5 | Cross-module reuse | Used by 2+ modules |
| 6 | Usage pattern | Dropdowns, auth checks, menus |

> **If ANY criterion is false → Entity is NOT cacheable. Period.**

### Permanently Excluded (NEVER cacheable)

- ❌ GL entities (Journal Entries, GL Accounts with balances, Vouchers)
- ❌ Billing entities
- ❌ Financial module entities
- ❌ Approval-based / workflow entities
- ❌ High write frequency entities
- ❌ Per-user / session-scoped data in shared Redis
- ❌ Search result sets (any `search()` method)

---

## Enforcement Checklist

### CHECK 1: Cache Eligibility (D.1)

```
[ ] D.1.1 — Entity is in the governance-approved eligible list
[ ] D.1.2 — Entity satisfies ALL 6 eligibility criteria
[ ] D.1.3 — Entity is NOT a transactional/financial entity
[ ] D.1.4 — Entity is NOT an approval/workflow entity
[ ] D.1.5 — Entity is NOT per-user/session data in shared Redis
[ ] D.1.6 — @Cacheable is NOT on a search() method
```

### CHECK 2: Cache Naming (D.2)

```
[ ] D.2.1 — Cache name is domain-specific camelCase (NOT "cache1", "data")
[ ] D.2.2 — Cache name matches the approved name for this entity
[ ] D.2.3 — No alternative/alias cache names used
[ ] D.2.4 — @Cacheable and @CacheEvict use SAME cacheNames value
```

### CHECK 3: Annotation Placement (D.3)

```
[ ] D.3.1 — @Cacheable is on SERVICE-layer read methods ONLY
[ ] D.3.2 — @CacheEvict is on SERVICE-layer write methods ONLY
[ ] D.3.3 — @Cacheable is NOT on write methods
[ ] D.3.4 — @CacheEvict is NOT on read-only methods
[ ] D.3.5 — @CacheEvict uses allEntries = true
[ ] D.3.6 — Cached read order: @Cacheable → @Transactional(readOnly) → @PreAuthorize
[ ] D.3.7 — Cached write order: @CacheEvict → @Transactional → @PreAuthorize
```

### CHECK 4: Cache Eviction Completeness (D.4)

```
[ ] D.4.1 — create() has @CacheEvict (if cached entity)
[ ] D.4.2 — update() has @CacheEvict (if cached entity)
[ ] D.4.3 — toggleActive() has @CacheEvict (if cached entity)
[ ] D.4.4 — delete() has @CacheEvict (if cached entity)
[ ] D.4.5 — No partial key-based eviction without governance approval
[ ] D.4.6 — Eviction co-located with @Transactional method
```

### CHECK 5: Frontend Caching (D.5)

```
[ ] D.5.1 — shareReplay(1) ONLY in lookup services for eligible entities
[ ] D.5.2 — Feature API services do NOT use shareReplay
[ ] D.5.3 — Facades do NOT have manual Map/object caches
[ ] D.5.4 — Frontend does NOT implement custom TTL logic
[ ] D.5.5 — No redundant frontend cache for backend-cached data
```

### CHECK 6: Prohibited Patterns (D.6)

```
[ ] D.6.1 — No @Cacheable on search/paginated methods
[ ] D.6.2 — No manual RedisTemplate calls in service code
[ ] D.6.3 — No caching of financial entities
[ ] D.6.4 — No caching of workflow entities
[ ] D.6.5 — No @Cacheable on non-eligible entities
[ ] D.6.6 — No orphaned @CachePut without paired @CacheEvict
[ ] D.6.7 — No caching annotations on repositories or controllers
```

---

## Violation Response

When a caching violation is detected:

```
❌ CACHING VIOLATION

Rule: [Rule ID] — [Description]
Location: [File:Method]
Found: [What was found]
Problem: [Why this is dangerous]
Fix: [Exact correction]

Impact: [Stale data risk / Performance issue / Security risk]
```

---

## Non-Eligible Entity Service Pattern

For entities that are NOT in the approved cache list, the service MUST NOT have any caching annotations:

```java
// ✅ CORRECT — non-eligible entity, no caching
@Transactional
@PreAuthorize("hasAuthority(...)")
public ServiceResult<EntityResponse> create(EntityCreateRequest request) {
    // No @CacheEvict — entity is not cached
}

// ❌ VIOLATION — caching a non-eligible entity
@CacheEvict(cacheNames = "entities", allEntries = true) // NOT APPROVED
@Transactional
public ServiceResult<EntityResponse> create(EntityCreateRequest request) {
```

---

## Annotation Order Enforcement

### Cached Read (ONLY for eligible entities):
```java
@Cacheable(cacheNames = "lookupValues", key = "#id")  // 1. Cache
@Transactional(readOnly = true)                         // 2. Transaction
@PreAuthorize("hasAuthority(...)")                      // 3. Security
public ServiceResult<EntityResponse> getById(Long id) { }
```

### Cached Write (ONLY for eligible entities):
```java
@CacheEvict(cacheNames = "lookupValues", allEntries = true)  // 1. Evict
@Transactional                                                 // 2. Transaction
@PreAuthorize("hasAuthority(...)")                             // 3. Security
public ServiceResult<EntityResponse> update(Long id, ...) { }
```

### Non-Cached Method:
```java
@Transactional(readOnly = true)     // 1. Transaction
@PreAuthorize("hasAuthority(...)")  // 2. Security
public ServiceResult<EntityResponse> getById(Long id) { }
```

---

## Enforcement Report Format

```
## Caching Governance Report

### Entity: [Name]
### Cache Eligible: YES / NO
### Approved Cache Name: [name] / N/A

| Check | Rules | Passed | Failed |
|-------|-------|--------|--------|
| Eligibility | 6 | ? | ? |
| Naming | 4 | ? | ? |
| Placement | 7 | ? | ? |
| Eviction | 6 | ? | ? |
| Frontend | 5 | ? | ? |
| Prohibited | 7 | ? | ? |
| **TOTAL** | **35** | **?** | **?** |

### Verdict: COMPLIANT / NON-COMPLIANT
```

---

## RELATED SKILLS

| Skill | Purpose |
|-------|---------|
| `enforce-backend-contract` | Validates overall layered architecture compliance including `erp-common-utils` consumption |
| `enforce-error-handling` | Validates error handling patterns: `LocalizedException`, `Status`, error codes |
| `validate-backend-feature` | Master validation across all layers with scoring |
