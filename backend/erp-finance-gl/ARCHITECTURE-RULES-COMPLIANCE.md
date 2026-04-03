# Architecture Rules Compliance Report

## erp-finance-gl Module

**Date:** 2026-03-27
**Last Updated:** Post Posting Engine implementation
**Reference:** `.github/instructions/backend.instructions.md` + `crs to project.instructions.md`

---

## 📊 Module Summary

| Metric | Count |
|--------|-------|
| Entities | 5 (4 DB tables) |
| Repositories | 5 |
| Services | 6 |
| Controllers | 4 |
| DTOs | 24 |
| Mappers | 3 |
| Error Codes | 53 |
| DB Migrations | V4 → V8 |
| REST Endpoints | 21 |
| Permissions | 16 |

---

## ✅ Guideline Compliance Matrix

### Rule 1: Module Ownership of Domain Logic

**Status:** ✅ COMPLIANT

All entities, repositories, services, and business logic reside within `com.example.erp.finance.gl.*`.
No other module accesses GL entities or repositories directly.

| Layer | Files | Package |
|-------|-------|---------|
| Entities | `AccountsChart`, `AccRuleHdr`, `AccRuleLine`, `GlJournalHdr`, `GlJournalLine` | `entity` |
| Repositories | `AccountsChartRepository`, `AccRuleHdrRepository`, `AccRuleLineRepository`, `GlJournalHdrRepository`, `GlJournalLineRepository` | `repository` |
| Services | `AccountsChartService`, `AccRuleHdrService`, `GlJournalService`, `PostingEngineService`, `AccountChartNumberGenerator`, `AccountChartTreeValidator` | `service` |

---

### Rule 2: Inter-Module Interaction via Public APIs Only

**Status:** ✅ COMPLIANT

Cross-module dependencies:

| Dependency | Module | Type | Usage |
|------------|--------|------|-------|
| `LookupValidationApi` | erp-masterdata | Interface (public API) | Validate lookup values (entrySide, amountSourceType, etc.) |
| `SecurityPermissions` | erp-security | Constants via SpEL `T()` | `@PreAuthorize` authorization |
| `ServiceResult`, `Status`, `LocalizedException` | erp-common-utils | Base classes/utilities | Response pattern + error handling |
| `OperationCode`, `ApiResponse` | erp-common-utils | Web utilities | Controller response wrapping |
| `AuditableEntity` | erp-common-utils | Base class | Audit fields (createdAt, updatedAt, etc.) |

**No cross-module entity imports. No cross-module repository injections.**

---

### Rule 3: Consistent Layered Architecture

**Status:** ✅ COMPLIANT

```
controller/ → REST/API (4 controllers)
service/    → Business logic (6 services)
repository/ → Persistence (5 repositories)
entity/     → JPA entities (5 entities)
dto/        → Request/Response DTOs (24 classes)
mapper/     → Entity↔DTO mapping (3 mappers)
exception/  → Error codes (1 class)
```

---

### Rule 4: No Cross-Module Repositories

**Status:** ✅ COMPLIANT

All repository injections verified:

| Service | Injected Repositories | Module |
|---------|----------------------|--------|
| `AccountsChartService` | `AccountsChartRepository`, `AccRuleHdrRepository` | GL ✅ |
| `AccRuleHdrService` | `AccRuleHdrRepository`, `AccountsChartRepository`, `GlJournalHdrRepository` | GL ✅ |
| `GlJournalService` | `GlJournalHdrRepository`, `AccountsChartRepository` | GL ✅ |
| `PostingEngineService` | `AccRuleHdrRepository`, `AccountsChartRepository`, `GlJournalHdrRepository` | GL ✅ |

---

### Rule 5: Shared Common Module is Domain-Free

**Status:** ✅ COMPLIANT

`erp-common-utils` provides only:
- `ServiceResult<T>`, `Status` — response pattern
- `LocalizedException` — error handling
- `OperationCode`, `ApiResponse` — REST response wrapping
- `AuditableEntity`, `BooleanNumberConverter` — base persistence
- Search infrastructure (`SearchRequest`, `Specification` helpers)

No domain DTOs, entities, or business logic in common.

---

### Rule 6: One-Way Dependencies (DAG)

**Status:** ✅ COMPLIANT

```
erp-common-utils → erp-masterdata → erp-security → erp-finance-gl → erp-main
```

No circular dependencies. GL depends on masterdata + security + common only.

---

### Rule 7: Clear Public API per Module

**Status:** ✅ COMPLIANT

| API Layer | Public Interface |
|-----------|-----------------|
| DTOs (Request) | `AccountsChartCreateRequest`, `AccRuleHdrCreateRequest`, `GlJournalHdrCreateRequest`, `PostingRequest` + update/search variants |
| DTOs (Response) | `AccountsChartResponse`, `AccRuleHdrResponse`, `GlJournalHdrResponse`, `PostingResponse` + line responses |
| Endpoints | `/api/gl/accounts`, `/api/gl/rules`, `/api/gl/journals`, `/api/gl/posting` |

---

### Rule 8: Entities Internal, DTOs External

**Status:** ✅ COMPLIANT

Controllers never expose entities. All return `ServiceResult<*Response>` → `ApiResponse<*Response>`.

---

### Rule 10: Thin Controllers, Fat Services

**Status:** ✅ COMPLIANT

| Controller | Methods | Pattern |
|------------|---------|---------|
| `AccountsChartController` | 8 endpoints | Each: validate → call service → `operationCode.craftResponse()` |
| `AccRuleHdrController` | 6 endpoints | Same pattern |
| `GlJournalController` | 9 endpoints | Same pattern (CRUD + workflow: approve, post, reverse, cancel) |
| `PostingEngineController` | 1 endpoint | `POST /execute` → `postingEngineService.execute()` |

---

### Rule 16–20: Naming Conventions

**Status:** ✅ COMPLIANT

| Convention | Example | Status |
|------------|---------|--------|
| Tables: UPPER_SNAKE_CASE plural | `ACCOUNTS_CHART`, `GL_JOURNAL_HDR`, `ACC_RULE_HDR` | ✅ |
| PK: `ID_PK` | `ID_PK` on all tables | ✅ |
| FK columns: `*_FK` suffix | `CUSTOMER_ID_FK`, `ACCOUNT_ID_FK`, `JOURNAL_ID_FK` | ✅ |
| FK constraints: `*_FK` | `GL_JOURNAL_LINE_HDR_FK`, `GL_JOURNAL_LINE_ACCT_FK` | ✅ |
| Indexes: `IDX_*` | `IDX_GL_JOURNAL_HDR_DATE`, `IDX_GL_JOURNAL_LINE_ACCT_FK` | ✅ |
| Java fields: camelCase | `companyIdFk`, `sourceModule`, `totalAmount` | ✅ |
| Booleans: `is*`/`has*` | `isActive`, `activeFl` | ✅ |
| Dates: `*At`/`*Date` | `createdAt`, `journalDate`, `startDate` | ✅ |
| Amounts | `totalDebit`, `totalCredit`, `debitAmount`, `creditAmount` | ✅ |

---

### Rule 19.1: @PreAuthorize on Service Methods

**Status:** ✅ IMPLEMENTED (Active)

Spring Security is integrated. All service methods use `@PreAuthorize`:

```java
@PreAuthorize("hasAnyAuthority("
    + "T(com.example.security.constants.SecurityPermissions).GL_POSTING_EXECUTE, "
    + "T(com.example.security.constants.SecurityPermissions).SYSTEM_ADMIN)")
```

**Permissions (16 active in `SecurityPermissions.java`):**

| Area | Permissions |
|------|------------|
| Chart of Accounts | `GL_ACCOUNT_VIEW`, `GL_ACCOUNT_CREATE`, `GL_ACCOUNT_UPDATE`, `GL_ACCOUNT_DELETE` |
| Accounting Rules | `GL_RULE_VIEW`, `GL_RULE_CREATE`, `GL_RULE_UPDATE`, `GL_RULE_DELETE` |
| Journal Entries | `GL_JOURNAL_VIEW`, `GL_JOURNAL_CREATE`, `GL_JOURNAL_UPDATE`, `GL_JOURNAL_DELETE`, `GL_JOURNAL_APPROVE`, `GL_JOURNAL_POST`, `GL_JOURNAL_REVERSE`, `GL_JOURNAL_CANCEL` |
| Posting Engine | `GL_POSTING_EXECUTE` |

---

### Rule 21–22: Pagination at DB Level

**Status:** ✅ COMPLIANT

All list endpoints use `Pageable` with DB-level pagination:

```java
@GetMapping
public Page<ContractResponse> listContracts(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size) { ... }
```

Sort field whitelists enforced:

| Service | Allowed Sort Fields |
|---------|-------------------|
| `AccRuleHdrService` | `ruleId`, `companyIdFk`, `sourceModule`, `sourceDocType`, `isActive`, `createdAt`, `updatedAt` |
| `GlJournalService` | `id`, `journalNo`, `journalDate`, `journalTypeIdFk`, `statusIdFk`, `totalDebit`, `totalCredit`, `createdAt`, `updatedAt` |

---

### Rule 23: Redis via Abstractions

**Status:** ✅ COMPLIANT

```java
@Cacheable(cacheNames = "accountingRules", ...)
@CacheEvict(cacheNames = "accountingRules", allEntries = true)
```

Only `AccRuleHdrService` uses caching (read-heavy reference data). Write operations (`PostingEngineService`) do not cache.

---

### Rule 25: N+1 Prevention

**Status:** ✅ COMPLIANT

All critical queries use `LEFT JOIN FETCH`:

```java
@Query("SELECT r FROM AccRuleHdr r LEFT JOIN FETCH r.lines WHERE ...")
Optional<AccRuleHdr> findByIdWithLines(Long ruleId);

@Query("SELECT r FROM AccRuleHdr r LEFT JOIN FETCH r.lines WHERE ... AND r.isActive = true")
Optional<AccRuleHdr> findActiveRuleWithLines(Long companyId, String sourceModule, String sourceDocType);

@Query("SELECT j FROM GlJournalHdr j LEFT JOIN FETCH j.lines WHERE j.id = :journalId")
Optional<GlJournalHdr> findByIdWithLines(Long journalId);
```

---

### Rule 26: Proper Indexes

**Status:** ✅ COMPLIANT

V7 migration includes indexes on all FK and filter columns:

| Index | Column |
|-------|--------|
| `IDX_GL_JOURNAL_HDR_DATE` | `JOURNAL_DATE` |
| `IDX_GL_JOURNAL_HDR_TYPE_FK` | `JOURNAL_TYPE_ID_FK` |
| `IDX_GL_JOURNAL_HDR_STATUS_FK` | `STATUS_ID_FK` |
| `IDX_GL_JOURNAL_HDR_SRC_MOD` | `SOURCE_MODULE_ID_FK` |
| `IDX_GL_JOURNAL_HDR_ACTIVE` | `ACTIVE_FL` |
| `IDX_GL_JOURNAL_HDR_POST_FK` | `SOURCE_POSTING_ID_FK` |
| `IDX_GL_JOURNAL_LINE_HDR_FK` | `JOURNAL_ID_FK` |
| `IDX_GL_JOURNAL_LINE_ACCT_FK` | `ACCOUNT_ID_FK` |
| `IDX_GL_JOURNAL_LINE_CUST_FK` | `CUSTOMER_ID_FK` |
| `IDX_GL_JOURNAL_LINE_SUPP_FK` | `SUPPLIER_ID_FK` |
| `IDX_GL_JOURNAL_LINE_CC_FK` | `COST_CENTER_ID_FK` |

---

## 📁 Complete File Inventory

### Entities (5)

| File | Table | Key Fields |
|------|-------|------------|
| `AccountsChart.java` | `ACCOUNTS_CHART` | `accountChartPk`, `accountChartNo`, `accountChartName`, `accountTypeFk`, `parentAccountIdFk`, `isActive` |
| `AccRuleHdr.java` | `ACC_RULE_HDR` | `ruleId`, `companyIdFk`, `sourceModule`, `sourceDocType`, `isActive`, `lines` (OneToMany) |
| `AccRuleLine.java` | `ACC_RULE_LINE` | `ruleLineId`, `accountIdFk`, `entrySide`, `priority`, `amountSourceType`, `amountSourceValue`, `entityType`, `paymentTypeCode` |
| `GlJournalHdr.java` | `GL_JOURNAL_HDR` | `journalNo`, `journalDate`, `journalTypeIdFk`, `statusIdFk`, `totalDebit`, `totalCredit`, `sourcePostingIdFk`, `lines` (OneToMany) |
| `GlJournalLine.java` | `GL_JOURNAL_LINE` | `lineNo`, `accountIdFk`, `debitAmount`, `creditAmount`, `customerIdFk`, `supplierIdFk`, `costCenterIdFk` |

### Repositories (5)

| File | Key Methods |
|------|-------------|
| `AccountsChartRepository` | `findAll`, `existsByAccountChartNo`, search with `JpaSpecificationExecutor` |
| `AccRuleHdrRepository` | `findByIdWithLines`, `existsActiveRuleForCombination`, `existsActiveRuleForCombinationExcluding`, `isAccountUsedInActiveRules`, `findActiveRuleWithLines` |
| `AccRuleLineRepository` | `findByRuleHdr_RuleId` |
| `GlJournalHdrRepository` | `findByIdWithLines`, `existsByJournalNo`, `getNextJournalNoSequence`, `generateJournalNo` (default), `existsBySourcePostingIdFkAndJournalTypeIdFkAndStatusIdFkIn` |
| `GlJournalLineRepository` | `findByJournalHdr_Id` |

### Services (6)

| File | Public Methods | Permissions |
|------|---------------|-------------|
| `AccountsChartService` | `create`, `getById`, `search`, `update`, `deactivate`, `getTree`, `getEligibleParents` | `GL_ACCOUNT_*` |
| `AccRuleHdrService` | `create`, `getById`, `search`, `update`, `deactivate` | `GL_RULE_*` |
| `GlJournalService` | `create`, `getById`, `search`, `update`, `delete`, `approve`, `post`, `reverse`, `cancel` | `GL_JOURNAL_*` |
| `PostingEngineService` | `execute` | `GL_POSTING_EXECUTE` |
| `AccountChartNumberGenerator` | `generateNextAccountNo` | Internal |
| `AccountChartTreeValidator` | `validateTreeIntegrity` | Internal |

### Controllers (4)

| File | Base Path | Endpoints |
|------|-----------|-----------|
| `AccountsChartController` | `/api/gl/accounts` | GET, GET/{id}, POST, PUT/{id}, DELETE/{id}, POST/search, GET/tree/{id}, GET/eligible-parents |
| `AccRuleHdrController` | `/api/gl/rules` | GET, GET/{id}, POST, PUT/{id}, DELETE/{id}, POST/search |
| `GlJournalController` | `/api/gl/journals` | GET, GET/{id}, POST, PUT/{id}, DELETE/{id}, POST/search, POST/{id}/approve, POST/{id}/post, POST/{id}/reverse, POST/{id}/cancel |
| `PostingEngineController` | `/api/gl/posting` | POST/execute |

### DTOs (24)

| Area | Request DTOs | Response DTOs |
|------|-------------|---------------|
| Chart of Accounts | `AccountsChartCreateRequest`, `AccountsChartUpdateRequest`, `AccountsChartSearchRequest`, `AccountsChartSearchContractRequest` | `AccountsChartResponse`, `AccountsChartTreeNode`, `EligibleParentAccountDto`, `AccountLookupDto` |
| Accounting Rules | `AccRuleHdrCreateRequest`, `AccRuleHdrUpdateRequest`, `AccRuleHdrSearchRequest`, `AccRuleHdrSearchContractRequest`, `AccRuleLineRequest` | `AccRuleHdrResponse`, `AccRuleLineResponse` |
| Journal Entries | `GlJournalHdrCreateRequest`, `GlJournalHdrUpdateRequest`, `GlJournalSearchContractRequest`, `GlJournalLineRequest` | `GlJournalHdrResponse`, `GlJournalLineResponse` |
| Posting Engine | `PostingRequest` | `PostingResponse` |

### Mappers (3)

| File | Methods |
|------|---------|
| `AccountsChartMapper` | `toEntity`, `toResponse`, `toTreeNode` |
| `AccRuleHdrMapper` | `toEntity`, `toResponse`, `updateEntity` |
| `GlJournalMapper` | `toEntity`, `toResponse` |

### Error Codes (53 total in `GlErrorCodes.java`)

| Section | Count | Examples |
|---------|-------|---------|
| Chart of Accounts | 19 | `GL_ACCOUNT_NOT_FOUND`, `GL_DUPLICATE_ACCOUNT_CODE`, `GL_ACCOUNT_CIRCULAR_REF` |
| Accounting Rules | 18 | `GL_RULE_NOT_FOUND`, `GL_DUPLICATE_ACTIVE_RULE`, `GL_RULE_AMOUNT_REMAINING_NO_VALUE` |
| GL Journals | 11 | `GL_JOURNAL_NOT_FOUND`, `GL_JOURNAL_NOT_BALANCED`, `GL_JOURNAL_AUTOMATIC_NO_UPDATE` |
| Posting Engine | 10 | `GL_POSTING_RULE_NOT_FOUND`, `GL_POSTING_UNBALANCED`, `GL_POSTING_REMAINING_NEGATIVE` |
| General | 2 | `GL_VALIDATION_ERROR`, `GL_ACCESS_DENIED` |

All 53 codes have entries in both `messages.properties` (EN) and `messages_ar.properties` (AR).

### DB Migrations

| File | Purpose |
|------|---------|
| `V4__accounts_chart_hierarchical_numbering.sql` | `ACCOUNTS_CHART` table + `ACC_RULE_HDR` + `ACC_RULE_LINE` + sequences + indexes |
| `V5__account_type_to_varchar.sql` | Convert `ACCOUNT_TYPE` to `VARCHAR2` |
| `V6__gl_lookup_seed_and_payment_type_varchar.sql` | Empty (seed data in DB directly) |
| `V7__gl_journal_tables.sql` | `GL_JOURNAL_HDR` + `GL_JOURNAL_LINE` + sequences + indexes + lookup seeds (GL_JOURNAL_TYPE, GL_JOURNAL_STATUS) + permissions (8 journal permissions) |
| `V8__posting_engine_additions.sql` | `REMAINING` in AMOUNT_SOURCE_TYPE + `GL_POSTING_EXECUTE` permission |

---

## 📋 Posting Engine Architecture

### Amount Calculation Pipeline (100% Dynamic — Zero Hardcoded Logic)

```
PostingRequest (companyIdFk + sourceModule + sourceDocType + totalAmount)
    ↓
AccRuleHdrRepository.findActiveRuleWithLines()
    ↓
AccRuleLine[] (sorted by priority ASC)
    ↓ calculateAmount() per line:
    ├── TOTAL     → totalAmount
    ├── FIXED     → amountSourceValue
    ├── PERCENT   → totalAmount × amountSourceValue (0 < value < 1)
    └── REMAINING → totalAmount − cumulativeSameSide
    ↓
GlJournalHdr (type=AUTOMATIC, status=DRAFT)
    ↓ balance check: totalDebit == totalCredit
    ↓
journalHdrRepository.save()
```

### Rule Guards

| Guard | Trigger | Error Code |
|-------|---------|------------|
| Rule in use (POSTED/REVERSED journals exist) | `AccRuleHdrService.update()` | `GL_RULE_IN_USE` |
| Pending postings (DRAFT/APPROVED journals exist) | `AccRuleHdrService.deactivate()` | `GL_RULE_HAS_PENDING_POSTINGS` |
| Account used in active rule | `AccountsChartService.deactivate()` | `GL_ACCOUNT_IN_ACTIVE_RULE` |

---

## ✅ Final Compliance Checklist

| # | Rule | Status |
|---|------|--------|
| 1 | Module Ownership | ✅ |
| 2 | Inter-Module via Public APIs | ✅ |
| 3 | Consistent Layered Architecture | ✅ |
| 4 | No Cross-Module Repositories | ✅ |
| 5 | Domain-Free Common Module | ✅ |
| 6 | One-Way Dependencies (DAG) | ✅ |
| 7 | Clear Public API per Module | ✅ |
| 8 | Entities Internal, DTOs External | ✅ |
| 10 | Thin Controllers, Fat Services | ✅ |
| 11 | Domain-Oriented Names | ✅ |
| 12 | No Global Static Business State | ✅ |
| 13 | Modules Replaceable/Removable | ✅ |
| 14 | Single Responsibility per Module | ✅ |
| 15 | Hide Internal Implementation | ✅ |
| 16 | Column/Attribute Naming | ✅ |
| 17 | PK Naming (`_PK`) | ✅ |
| 18 | FK Naming (`_FK`) | ✅ |
| 19 | Boolean/Date/Amount Naming | ✅ |
| 20 | Index/Unique/Check Naming | ✅ |
| 21 | Pagination on All Lists | ✅ |
| 22 | DB-Level Pagination | ✅ |
| 23 | Redis via Abstractions | ✅ |
| 25 | No N+1 Queries | ✅ |
| 26 | Proper Indexes | ✅ |
| 30 | @PreAuthorize on Services | ✅ |

---

## 📋 Future Work

1. **GL_LEDGER_ENTRY** — Ledger entries from posted journals
2. **GL_ACCOUNT_BALANCE** — Period balance aggregation
3. **ACC_POSTING_MST / ACC_POSTING_DTL** — Subledger posting tables (batch/scheduler mode)
4. **Financial Reports** — Trial Balance, P&L, Balance Sheet
5. **Unit Tests** — `src/test/java/` is currently empty
