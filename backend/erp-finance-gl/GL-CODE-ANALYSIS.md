# GL Finance Module — تحليل الكود المنفذ فعلياً

> **تاريخ التوليد:** يونيو 2025
> **المصدر:** قراءة مباشرة من الكود المصدري (Source Code) فقط — بدون الرجوع لأي مستند CRS أو مواصفات.
> **الموديول:** `erp-finance-gl`
> **الحزمة الأساسية:** `com.example.erp.finance.gl`

---

## الفهرس

| # | القسم |
|---|-------|
| 1 | [ملخص المكونات المنفذة](#1-ملخص-المكونات-المنفذة) |
| 2 | [الكيانات (Entities) وقاعدة البيانات](#2-الكيانات-entities-وقاعدة-البيانات) |
| 3 | [المستودعات (Repositories) والاستعلامات](#3-المستودعات-repositories-والاستعلامات) |
| 4 | [الخدمات (Services) ومنطق الأعمال](#4-الخدمات-services-ومنطق-الأعمال) |
| 5 | [المتحكمات (Controllers) ونقاط الوصول REST](#5-المتحكمات-controllers-ونقاط-الوصول-rest) |
| 6 | [كائنات نقل البيانات (DTOs)](#6-كائنات-نقل-البيانات-dtos) |
| 7 | [المحوّلات (Mappers)](#7-المحوّلات-mappers) |
| 8 | [أكواد الأخطاء والرسائل (Error Codes & i18n)](#8-أكواد-الأخطاء-والرسائل) |
| 9 | [الصلاحيات (Security Permissions)](#9-الصلاحيات-security-permissions) |
| 10 | [ترحيلات قاعدة البيانات (Migrations)](#10-ترحيلات-قاعدة-البيانات-migrations) |
| 11 | [الأنماط والممارسات المطبقة](#11-الأنماط-والممارسات-المطبقة) |
| 12 | [تدفقات الأعمال (Business Flows)](#12-تدفقات-الأعمال-business-flows) |
| 13 | [قواعد التحقق (Validation Rules)](#13-قواعد-التحقق-validation-rules) |

---

## 1. ملخص المكونات المنفذة

### إحصائيات سريعة

| المكون | العدد |
|--------|-------|
| Entities | 5 |
| Repositories | 5 |
| Services | 6 |
| Controllers | 4 |
| DTOs | 24 |
| Mappers | 3 |
| Error Codes | 61 |
| Security Permissions | 17 |
| DB Migrations (V4–V8) | 5 |
| i18n Messages (EN + AR) | 51 × 2 |
| REST Endpoints | 22 |
| DB Tables | 5 |
| DB Sequences | 6 |

### خريطة الحزم (Package Map)

```
com.example.erp.finance.gl
├── controller/
│   ├── AccountsChartController        ← /api/gl/accounts
│   ├── AccRuleHdrController            ← /api/gl/rules
│   ├── GlJournalController             ← /api/gl/journals
│   └── PostingEngineController          ← /api/gl/posting
├── service/
│   ├── AccountsChartService
│   ├── AccountChartNumberGenerator
│   ├── AccountChartTreeValidator
│   ├── AccRuleHdrService
│   ├── GlJournalService
│   └── PostingEngineService
├── repository/
│   ├── AccountsChartRepository
│   ├── AccRuleHdrRepository
│   ├── AccRuleLineRepository
│   ├── GlJournalHdrRepository
│   └── GlJournalLineRepository
├── entity/
│   ├── AccountsChart
│   ├── AccRuleHdr
│   ├── AccRuleLine
│   ├── GlJournalHdr
│   └── GlJournalLine
├── dto/
│   ├── AccountsChartCreateRequest, AccountsChartUpdateRequest, AccountsChartResponse
│   ├── AccountsChartTreeNode, EligibleParentAccountDto, AccountLookupDto
│   ├── AccRuleHdrCreateRequest, AccRuleHdrUpdateRequest, AccRuleHdrResponse
│   ├── AccRuleLineRequest, AccRuleLineResponse, AccRuleHdrSearchRequest
│   ├── GlJournalHdrCreateRequest, GlJournalHdrUpdateRequest, GlJournalHdrResponse
│   ├── GlJournalLineRequest, GlJournalLineResponse
│   ├── PostingRequest, PostingResponse
│   └── *SearchContractRequest (3 classes)
├── mapper/
│   ├── AccountsChartMapper
│   ├── AccRuleHdrMapper
│   └── GlJournalMapper
└── exception/
    └── GlErrorCodes
```

---

## 2. الكيانات (Entities) وقاعدة البيانات

### 2.1 AccountsChart — دليل الحسابات

| العمود (DB) | الحقل (Java) | النوع | القيود |
|-------------|-------------|-------|--------|
| `ACCOUNT_CHART_PK` | `accountChartPk` | `Long` | PK, Sequence: `ACCOUNTS_CHART_SEQ` |
| `ACCOUNT_CHART_NO` | `accountChartNo` | `String(50)` | NOT NULL, Auto-Generated |
| `ACCOUNT_CHART_NAME` | `accountChartName` | `String(500)` | NOT NULL |
| `ACCOUNT_CHART_FK` | `parent` | `ManyToOne(LAZY)` | Self-join, nullable (null = root) |
| — | `children` | `OneToMany(LAZY)` | Bidirectional, mappedBy `parent` |
| `ACCOUNT_TYPE` | `accountType` | `String(50)` | NOT NULL, LOV: `GL_ACCOUNT_TYPE` |
| `ORGANIZATION_FK` | `organizationFk` | `Long` | NOT NULL, Immutable after creation |
| `ORGANIZATION_SUB_FK` | `organizationSubFk` | `Long` | nullable |
| `ACTIVE` | `isActive` | `Boolean` | NOT NULL, default `true`, `BooleanNumberConverter` |

**Unique Constraint:** `UK_ACCOUNTS_CHART_NO_ORG` → (`ACCOUNT_CHART_NO`, `ORGANIZATION_FK`)

**Indexes:**
- `IDX_ACCOUNTS_CHART_ACTIVE` → `ACTIVE`
- `IDX_ACCOUNTS_CHART_ORG_FK` → `ORGANIZATION_FK`
- `IDX_ACCOUNTS_CHART_PARENT_FK` → `ACCOUNT_CHART_FK`
- `IDX_ACCOUNTS_CHART_TYPE` → `ACCOUNT_TYPE`
- `IDX_ACCOUNTS_CHART_NO` → `ACCOUNT_CHART_NO` (from V4)
- `IDX_ACCOUNTS_CHART_PARENT_NO` → (`ACCOUNT_CHART_FK`, `ACCOUNT_CHART_NO`) (from V4)
- `IDX_ACCOUNTS_CHART_ORG_TYPE` → (`ORGANIZATION_FK`, `ACCOUNT_TYPE`) (from V4)

**Helper:** `boolean isLeaf()` → `children == null || children.isEmpty()`

**وراثة:** يرث من `AuditableEntity` → حقول: `createdAt`, `createdBy`, `updatedAt`, `updatedBy`

---

### 2.2 GlJournalHdr — رأس قيد اليومية

| العمود (DB) | الحقل (Java) | النوع | القيود |
|-------------|-------------|-------|--------|
| `ID_PK` | `id` | `Long` | PK, Sequence: `GL_JOURNAL_HDR_SEQ` |
| `JOURNAL_NO` | `journalNo` | `String(50)` | NOT NULL, UNIQUE, Auto: `JRN-XXXXXX` |
| `JOURNAL_DATE` | `journalDate` | `LocalDate` | NOT NULL |
| `JOURNAL_TYPE_ID_FK` | `journalTypeIdFk` | `String(50)` | NOT NULL, LOV values: `MANUAL`, `AUTOMATIC`, `ADJUSTMENT`, `REVERSAL` |
| `STATUS_ID_FK` | `statusIdFk` | `String(50)` | NOT NULL, LOV values: `DRAFT`, `APPROVED`, `POSTED`, `REVERSED`, `CANCELLED` |
| `DESCRIPTION` | `description` | `String(500)` | nullable |
| `SOURCE_MODULE_ID_FK` | `sourceModuleIdFk` | `String(50)` | nullable |
| `SOURCE_DOC_TYPE_ID` | `sourceDocTypeId` | `String(50)` | nullable |
| `SOURCE_DOC_ID_FK` | `sourceDocIdFk` | `Long` | nullable |
| `SOURCE_POSTING_ID_FK` | `sourcePostingIdFk` | `Long` | nullable, points to `AccRuleHdr.ruleId` |
| `TOTAL_DEBIT` | `totalDebit` | `BigDecimal(18,2)` | NOT NULL, default `0` |
| `TOTAL_CREDIT` | `totalCredit` | `BigDecimal(18,2)` | NOT NULL, default `0` |
| `ACTIVE_FL` | `activeFl` | `Boolean` | NOT NULL, default `true`, `BooleanNumberConverter` |

**Relations:** `@OneToMany lines` → `GlJournalLine` (cascade ALL, orphanRemoval = false)

**Lifecycle:** `@PrePersist` → يحوّل `journalTypeIdFk`, `sourceModuleIdFk`, `statusIdFk` إلى UPPERCASE

**Helper Methods:**
- `addLine(GlJournalLine)` / `removeLine(GlJournalLine)` / `replaceLines(List)`
- `recalculateTotals()` — يعيد حساب المجموع من السطور
- `isBalanced()` — `totalDebit.compareTo(totalCredit) == 0`
- `activate()` / `deactivate()`

**Unique:** `UK_GL_JOURNAL_NO` → `JOURNAL_NO`

**Indexes:**
- `IDX_GL_JOURNAL_HDR_DATE`, `IDX_GL_JOURNAL_HDR_TYPE_FK`, `IDX_GL_JOURNAL_HDR_STATUS_FK`
- `IDX_GL_JOURNAL_HDR_SRC_MOD`, `IDX_GL_JOURNAL_HDR_ACTIVE`, `IDX_GL_JOURNAL_HDR_POST_FK`

---

### 2.3 GlJournalLine — سطر قيد اليومية

| العمود (DB) | الحقل (Java) | النوع | القيود |
|-------------|-------------|-------|--------|
| `ID_PK` | `id` | `Long` | PK, Sequence: `GL_JOURNAL_LINE_SEQ` |
| `JOURNAL_ID_FK` | `journalHdr` | `ManyToOne(LAZY)` | NOT NULL, FK → `GL_JOURNAL_HDR` |
| `LINE_NO` | `lineNo` | `Integer` | NOT NULL |
| `ACCOUNT_ID_FK` | `accountIdFk` | `Long` | NOT NULL |
| `DEBIT_AMOUNT` | `debitAmount` | `BigDecimal(18,2)` | nullable |
| `CREDIT_AMOUNT` | `creditAmount` | `BigDecimal(18,2)` | nullable |
| `CUSTOMER_ID_FK` | `customerIdFk` | `Long` | nullable |
| `SUPPLIER_ID_FK` | `supplierIdFk` | `Long` | nullable |
| `COST_CENTER_ID_FK` | `costCenterIdFk` | `Long` | nullable |
| `DESCRIPTION` | `description` | `String(500)` | nullable |

**Indexes:**
- `IDX_GL_JOURNAL_LINE_HDR_FK`, `IDX_GL_JOURNAL_LINE_ACCT_FK`
- `IDX_GL_JOURNAL_LINE_CUST_FK`, `IDX_GL_JOURNAL_LINE_SUPP_FK`, `IDX_GL_JOURNAL_LINE_CC_FK`

---

### 2.4 AccRuleHdr — رأس القاعدة المحاسبية

| العمود (DB) | الحقل (Java) | النوع | القيود |
|-------------|-------------|-------|--------|
| `RULE_ID` | `ruleId` | `Long` | PK, Sequence: `ACC_RULE_HDR_SEQ` |
| `COMPANY_ID_FK` | `companyIdFk` | `Long` | NOT NULL |
| `SOURCE_MODULE` | `sourceModule` | `String(50)` | NOT NULL |
| `SOURCE_DOC_TYPE` | `sourceDocType` | `String(50)` | NOT NULL |
| `IS_ACTIVE` | `isActive` | `Boolean` | NOT NULL, default `true` |

**Relations:** `@OneToMany lines` → `AccRuleLine` (cascade ALL, orphanRemoval = true)

**Unique:** `ACC_RULE_HDR_UK` → (`COMPANY_ID_FK`, `SOURCE_MODULE`, `SOURCE_DOC_TYPE`)

**Indexes:**
- `IDX_ACC_RULE_HDR_ACTIVE`, `IDX_ACC_RULE_HDR_COMPANY_FK`, `IDX_ACC_RULE_HDR_MODULE`

---

### 2.5 AccRuleLine — سطر القاعدة المحاسبية

| العمود (DB) | الحقل (Java) | النوع | القيود |
|-------------|-------------|-------|--------|
| `RULE_LINE_ID` | `ruleLineId` | `Long` | PK, Sequence: `ACC_RULE_LINE_SEQ` |
| `RULE_ID_FK` | `ruleHdr` | `ManyToOne(LAZY)` | NOT NULL, FK → `ACC_RULE_HDR` |
| `ACCOUNT_ID_FK` | `accountIdFk` | `Long` | NOT NULL |
| `ENTRY_SIDE` | `entrySide` | `String(10)` | NOT NULL, LOV: `DEBIT`, `CREDIT` |
| `PRIORITY` | `priority` | `Integer` | NOT NULL, positive, unique within rule |
| `AMOUNT_SOURCE_TYPE` | `amountSourceType` | `String(20)` | NOT NULL, LOV: `TOTAL`, `FIXED`, `PERCENT`, `REMAINING` |
| `AMOUNT_SOURCE_VALUE` | `amountSourceValue` | `BigDecimal(18,6)` | nullable |
| `PAYMENT_TYPE_CODE` | `paymentTypeCode` | `String(20)` | nullable |
| `ENTITY_TYPE` | `entityType` | `String(20)` | nullable, LOV: `CUSTOMER`, `VENDOR`, `BANK`, etc. |

**Indexes:**
- `IDX_ACC_RULE_LINE_HDR_FK`, `IDX_ACC_RULE_LINE_ACCOUNT_FK`

---

## 3. المستودعات (Repositories) والاستعلامات

### 3.1 AccountsChartRepository

**Extends:** `JpaRepository<AccountsChart, Long>`, `JpaSpecificationExecutor<AccountsChart>`

| الاستعلام | الوصف |
|----------|-------|
| `existsByAccountChartNoAndOrganizationFk(no, orgFk)` | فحص تكرار رقم الحساب |
| `existsByAccountChartNoAndOrganizationFkAndAccountChartPkNot(no, orgFk, pk)` | فحص التكرار مع استثناء الحساب الحالي |
| `findByAccountChartPk(pk)` | بحث بالمفتاح الأساسي |
| `findByIdWithParent(pk)` | FETCH JOIN مع الأب |
| `findByIdWithChildren(pk)` | FETCH JOIN مع الأبناء |
| `countChildrenByParentPk(pk)` | عدد الأبناء الكلي |
| `countActiveChildrenByParentPk(pk)` | عدد الأبناء النشطين |
| `hasChildren(pk)` | هل يوجد أبناء |
| `findRootAccountsByOrganization(orgFk)` | الحسابات الجذرية حسب المنشأة |
| `findRootAccountsByOrganizationAndType(orgFk, type)` | الجذرية حسب المنشأة والنوع |
| `findAllRootAccounts()` | كل الجذرية |
| `findAllRootAccountsByType(type)` | الجذرية حسب النوع |
| `findAllForTreeByOrganization(orgFk)` | كل الحسابات مع الأب (لبناء الشجرة بدون N+1) |
| `findAllForTreeByOrganizationAndType(orgFk, type)` | نفس السابق مع فلتر النوع |
| `findAllForTree()` | كل الحسابات مع الأب |
| `findAllForTreeByType(type)` | حسب النوع فقط |
| `isLeafAccount(pk)` | هل الحساب ورقي (بدون أبناء) |
| `findMaxRootAccountNo(orgFk, type)` | أقصى رقم جذري (للترقيم التلقائي) |
| `findMaxChildAccountNo(parentPk, prefix)` | أقصى رقم ابن (للترقيم التلقائي) |
| `findMaxAccountNoByPrefixAndLength(orgFk, prefix, len)` | بحث بالبادئة والطول |
| `findDirectChildren(parentPk)` | الأبناء المباشرين |
| `findEligibleParents(orgFk, excludePks, search, pageable)` | الحسابات المؤهلة كآباء (LOV) مع صفحات |
| `findAllByOrganization(orgFk)` | كل حسابات المنشأة |

---

### 3.2 GlJournalHdrRepository

**Extends:** `JpaRepository<GlJournalHdr, Long>`, `JpaSpecificationExecutor<GlJournalHdr>`

| الاستعلام | الوصف |
|----------|-------|
| `findByIdWithLines(journalId)` | FETCH JOIN مع السطور |
| `existsByJournalNo(journalNo)` | فحص تكرار رقم القيد |
| `getNextJournalNoSequence()` | Native: `GL_JOURNAL_NO_SEQ.NEXTVAL FROM DUAL` |
| `generateJournalNo()` | Default method: `String.format("JRN-%06d", seq)` |
| `existsBySourcePostingIdFkAndJournalTypeIdFkAndStatusIdFkIn(ruleId, type, statuses)` | فحص استخدام القاعدة في القيود |

---

### 3.3 GlJournalLineRepository

**Extends:** `JpaRepository<GlJournalLine, Long>`, `JpaSpecificationExecutor<GlJournalLine>`

| الاستعلام | الوصف |
|----------|-------|
| `findByJournalHdr_Id(journalHdrId)` | سطور قيد معين |

---

### 3.4 AccRuleHdrRepository

**Extends:** `JpaRepository<AccRuleHdr, Long>`, `JpaSpecificationExecutor<AccRuleHdr>`

| الاستعلام | الوصف |
|----------|-------|
| `findByIdWithLines(ruleId)` | FETCH JOIN مع السطور |
| `existsActiveRuleForCombination(companyId, module, docType)` | فحص تكرار قاعدة نشطة |
| `existsActiveRuleForCombinationExcluding(companyId, module, docType, excludeId)` | نفسه مع استثناء |
| `isAccountUsedInActiveRules(accountPk)` | هل الحساب مستخدم في قواعد نشطة |
| `findActiveRuleWithLines(companyId, module, docType)` | إيجاد القاعدة النشطة مع السطور (يستخدمها محرك الترحيل) |

---

### 3.5 AccRuleLineRepository

**Extends:** `JpaRepository<AccRuleLine, Long>`

| الاستعلام | الوصف |
|----------|-------|
| `findByRuleHdr_RuleId(ruleId)` | سطور قاعدة معينة |

---

## 4. الخدمات (Services) ومنطق الأعمال

### 4.1 AccountsChartService — خدمة دليل الحسابات

**النوع:** `@Service`, `@Transactional(readOnly = true)`

**الاعتماديات:**
- `AccountsChartRepository`, `AccRuleHdrRepository`
- `AccountsChartMapper`, `AccountChartNumberGenerator`, `AccountChartTreeValidator`
- `LookupValidationApi`

#### العمليات المنفذة:

**`create(AccountsChartCreateRequest)`**
- `@Transactional` + `@CacheEvict(accountsChart)` + `@PreAuthorize(GL_ACCOUNT_CREATE)`
- يتحقق من نوع الحساب عبر `LookupValidationApi`
- يتحقق من الأب (وجود، نشاط، لا مرجعية ذاتية، لا دائرية، تطابق النوع)
- يولّد رقم الحساب تلقائياً عبر `AccountChartNumberGenerator`
- يُرجع `201-CREATED`

**`update(Long, AccountsChartUpdateRequest)`**
- `@Transactional` + `@CacheEvict(accountsChart)` + `@PreAuthorize(GL_ACCOUNT_UPDATE)`
- `organizationFk` غير قابل للتعديل
- لا يمكن تغيير النوع إذا يوجد أبناء
- لا يُعدَّل `accountChartNo`
- يُرجع `200-UPDATED`

**`deactivate(Long)`**
- `@Transactional` + `@CacheEvict(accountsChart)` + `@PreAuthorize(GL_ACCOUNT_DELETE)`
- Soft delete: `isActive = false`
- لا يمكن إلغاء التنشيط إذا يوجد أبناء نشطون
- لا يمكن إلغاء التنشيط إذا مستخدم في قواعد محاسبية نشطة
- يُرجع `204-NO_CONTENT`

**`getById(Long)`** → `@PreAuthorize(GL_ACCOUNT_VIEW)` → `ServiceResult<AccountsChartResponse>`

**`search(SearchRequest)`** → `@PreAuthorize(GL_ACCOUNT_VIEW)` → صفحات

**`getTree(Long orgFk, String accountType)`**
- `@Cacheable(accountsChart)` + `@PreAuthorize(GL_ACCOUNT_VIEW)`
- استعلام واحد يجلب كل الحسابات (تجنب N+1)
- يبني خريطة أب←أبناء في الذاكرة
- يُرجع شجرة تراتبية

**`getSubTree(Long pk)`** → شجرة فرعية من حساب معين

**`getEligibleParents(...)`** → LOV مع صفحات، يستبعد الحساب ونسله

**`lookupAccounts(...)` / `lookupAccountById(...)`** → بحث بسيط للـ LOV

**ثوابت البحث:**
- `ALLOWED_SORT_FIELDS`: accountChartPk, accountChartNo, accountChartName, accountType, isActive, organizationFk, createdAt, updatedAt
- `ALLOWED_SEARCH_FIELDS`: accountChartNo, accountChartName, accountType, isActive, organizationFk, parent.accountChartPk
- `DEFAULT_PAGE_SIZE = 20`, `MAX_PAGE_SIZE = 100`

---

### 4.2 AccountChartNumberGenerator — مولّد أرقام الحسابات

**النوع:** `@Service`

**آلية الترقيم:**

**حسابات جذرية:**
1. يأخذ `sortOrder` من `GL_ACCOUNT_TYPE` lookup (مثلاً: ASSET=1, LIABILITY=2)
2. أول حساب جذري من نوع ASSET → `"10"`
3. الثاني → `"11"`, الثالث → `"12"`, ... حتى `"19"` (10 جذري كحد أقصى)

**حسابات فرعية:**
1. يأخذ رقم الأب ويضيف عليه رقم تسلسلي بعرض `CHILD_SEGMENT_WIDTH = 2`
2. أول ابن للحساب `"10"` → `"1001"`
3. الثاني → `"1002"`, ... حتى `"1099"` (99 ابن كحد أقصى)

**أمثلة:**
```
Root (ASSET, sortOrder=1): 10, 11, 12, ...
  Child of 10:              1001, 1002, 1003, ...
    Grandchild of 1001:     100101, 100102, ...
Root (LIABILITY, sortOrder=2): 20, 21, 22, ...
```

---

### 4.3 AccountChartTreeValidator — مدقق شجرة الحسابات

**النوع:** `@Service`

| الدالة | الوصف | خطأ الرفض |
|--------|-------|-----------|
| `validateParentExists(pk)` | وجود الأب ونشاطه | `GL_ACCOUNT_PARENT_NOT_FOUND`, `GL_ACCOUNT_PARENT_INACTIVE` |
| `validateNoSelfReference(pk, parentPk)` | ليس أباً لنفسه | `GL_ACCOUNT_SELF_REFERENCE` |
| `validateNoCircularReference(account, parent)` | لا دائرية (BFS لأعلى ولأسفل) | `GL_ACCOUNT_CIRCULAR_REF`, `GL_ACCOUNT_DESCENDANT_AS_PARENT` |
| `validateNotDescendant(pk, proposedParentPk)` | الأب المقترح ليس من الأحفاد | `GL_ACCOUNT_DESCENDANT_AS_PARENT` |
| `validateAccountTypeChange(pk, current, requested)` | لا تغيير نوع إذا يوجد أبناء | `GL_ACCOUNT_TYPE_CHANGE_WITH_CHILDREN` |
| `validateTypeMatchesParent(childType, parentType)` | تطابق نوع الابن مع الأب | `GL_ACCOUNT_TYPE_MISMATCH` |

---

### 4.4 AccRuleHdrService — خدمة القواعد المحاسبية

**النوع:** `@Service`, `@Transactional(readOnly = true)`

**الاعتماديات:**
- `AccRuleHdrRepository`, `AccountsChartRepository`, `GlJournalHdrRepository`
- `AccRuleHdrMapper`, `LookupValidationApi`

#### العمليات المنفذة:

**`create(AccRuleHdrCreateRequest)`**
- `@Transactional` + `@CacheEvict(accountingRules)` + `@PreAuthorize(GL_RULE_CREATE)`
- لا يسمح بقاعدة نشطة مكررة لنفس (company, module, docType)
- يتحقق من الحقول عبر LOV: `SOURCE_MODULE`, `SOURCE_DOC_TYPE`
- يتحقق من السطور (راجع §13)
- يُرجع `201-CREATED`

**`update(Long, AccRuleHdrUpdateRequest)`**
- `@Transactional` + `@CacheEvict(accountingRules)` + `@PreAuthorize(GL_RULE_UPDATE)`
- لا يمكن التعديل إذا استُخدمت في ترحيلات مكتملة (قيود POSTED أو REVERSED من نوع AUTOMATIC)
- يُرجع `200-UPDATED`

**`deactivate(Long)`**
- `@Transactional` + `@CacheEvict(accountingRules)` + `@PreAuthorize(GL_RULE_DELETE)`
- لا يمكن الإلغاء إذا يوجد ترحيلات معلقة (قيود DRAFT أو APPROVED من نوع AUTOMATIC)
- Soft delete: `isActive = false`
- يُرجع `200-UPDATED`

**`getById(Long)`** / **`search(SearchRequest)`** → بالصلاحيات المناسبة

**ثوابت البحث:**
- `ALLOWED_SORT_FIELDS`: ruleId, companyIdFk, sourceModule, sourceDocType, isActive, createdAt, updatedAt
- `ALLOWED_SEARCH_FIELDS`: companyIdFk, sourceModule, sourceDocType, isActive

---

### 4.5 GlJournalService — خدمة قيود اليومية

**النوع:** `@Service`, `@Transactional(readOnly = true)`

**الاعتماديات:**
- `GlJournalHdrRepository`, `AccountsChartRepository`
- `GlJournalMapper`, `LookupValidationApi`

#### العمليات المنفذة:

**`create(GlJournalHdrCreateRequest)`**
- `@PreAuthorize(GL_JOURNAL_CREATE)`
- يتحقق من النوع والمصدر عبر LOV
- AUTOMATIC يتطلب `sourcePostingIdFk`
- يتحقق من السطور (XOR debit/credit, حساب موجود ونشط)
- يولّد رقم القيد تلقائياً: `JRN-XXXXXX`
- الحالة الأولية = `DRAFT`
- يتحقق من التوازن قبل الحفظ
- يُرجع `201-CREATED`

**`update(Long, GlJournalHdrUpdateRequest)`**
- `@PreAuthorize(GL_JOURNAL_UPDATE)`
- فقط قيود `DRAFT` يمكن تعديلها
- قيود `AUTOMATIC` لا يمكن تعديلها يدوياً

**`approve(Long)`** → `DRAFT` → `APPROVED`
**`post(Long)`** → `APPROVED` → `POSTED`

**`reverse(Long)`**
- فقط لقيود `POSTED`
- يضع الأصلي كـ `REVERSED`
- ينشئ قيد انعكاس جديد (`REVERSAL`, `POSTED`)
- يعكس debit ↔ credit في كل سطر
- يُرجع القيد الجديد `201-CREATED`

**`cancel(Long)`**
- لا يمكن إلغاء `POSTED` (استخدم reverse)
- لا يمكن إلغاء `CANCELLED` أو `REVERSED`
- يضع الحالة = `CANCELLED`

**`toggleActive(Long, Boolean)`** → تفعيل/تعطيل، لا يمكن تعطيل `POSTED`

#### مخطط حالات القيد (State Machine):

```
                 ┌───────────┐
      create()   │           │  approve()
    ──────────►  │   DRAFT   │ ──────────►  APPROVED
                 │           │              │
                 └─────┬─────┘              │ post()
                       │                    ▼
                cancel()│              ┌──────────┐
                       │              │  POSTED   │
                       ▼              └─────┬─────┘
                 ┌───────────┐              │ reverse()
                 │ CANCELLED │              ▼
                 └───────────┘     ┌────────────────┐
                                   │   REVERSED      │
                                   └────────┬───────┘
                                            │ (creates)
                                            ▼
                                   ┌────────────────┐
                                   │ REVERSAL(POSTED)│
                                   └────────────────┘
```

**ثوابت البحث:**
- `ALLOWED_SORT_FIELDS`: id, journalNo, journalDate, journalTypeIdFk, statusIdFk, sourceModuleIdFk, totalDebit, totalCredit, activeFl, createdAt, updatedAt
- `ALLOWED_SEARCH_FIELDS`: journalNo, journalDate, journalTypeIdFk, statusIdFk, sourceModuleIdFk, activeFl

---

### 4.6 PostingEngineService — محرك الترحيل الآلي

**النوع:** `@Service`, `@Transactional(readOnly = true)`

**الاعتماديات:**
- `AccRuleHdrRepository`, `AccountsChartRepository`, `GlJournalHdrRepository`
- `GlJournalMapper`

#### تدفق التنفيذ `execute(PostingRequest)`:

```
1. التحقق: totalAmount > 0
2. البحث عن قاعدة نشطة: (companyIdFk, sourceModule, sourceDocType)
3. التحقق: القاعدة بها سطور
4. ترتيب السطور حسب priority تصاعدياً
5. التحقق: كل الحسابات موجودة ونشطة
6. معالجة كل سطر:
   ├── حساب المبلغ حسب amountSourceType
   ├── تحديد الكيان (CUSTOMER/VENDOR/BANK) من entityMap
   └── إنشاء GlJournalLine (debit أو credit حسب entrySide)
7. بناء GlJournalHdr:
   ├── journalTypeIdFk = AUTOMATIC
   ├── statusIdFk = DRAFT
   ├── journalNo = JRN-XXXXXX (auto)
   └── sourcePostingIdFk = rule.ruleId
8. التحقق من التوازن (totalDebit == totalCredit)
9. الحفظ
10. إرجاع PostingResponse
```

#### حساب المبلغ (`calculateAmount`):

| النوع | الحساب | القيد |
|-------|--------|-------|
| `TOTAL` | `totalAmount` | — |
| `FIXED` | `amountSourceValue` | يجب أن يكون > 0 |
| `PERCENT` | `totalAmount × amountSourceValue` | القيمة بين 0 و 1 حصرياً |
| `REMAINING` | `totalAmount − cumulative` | يجب ألا يكون سالباً |

**حل الكيانات (`resolveEntity`):**
- يطابق `entityType` من القاعدة مع `entityMap` من الطلب
- يضع القيمة في الحقل المناسب: `customerIdFk`, `supplierIdFk`, `costCenterIdFk`

---

## 5. المتحكمات (Controllers) ونقاط الوصول REST

### 5.1 AccountsChartController — `/api/gl/accounts`

| HTTP | المسار | الدالة | الصلاحية | الوصف |
|------|--------|--------|---------|-------|
| `POST` | `/search` | `search()` | `GL_ACCOUNT_VIEW` | بحث مع فلاتر وترتيب وصفحات |
| `GET` | `/eligible-parents` | `getEligibleParents()` | `GL_ACCOUNT_VIEW, CREATE, UPDATE` | LOV الآباء المؤهلين (مع صفحات) |
| `GET` | `/{pk}` | `getById()` | `GL_ACCOUNT_VIEW` | تفاصيل حساب واحد |
| `POST` | `/` | `create()` | `GL_ACCOUNT_CREATE` | إنشاء حساب (رقم تلقائي) |
| `PUT` | `/{pk}` | `update()` | `GL_ACCOUNT_UPDATE` | تعديل (الرقم لا يتغير) |
| `DELETE` | `/{pk}` | `deactivate()` | `GL_ACCOUNT_DELETE` | Soft delete → `204` |
| `GET` | `/tree` | `getTree()` | `GL_ACCOUNT_VIEW` | الشجرة الكاملة |
| `GET` | `/tree/{pk}` | `getSubTree()` | `GL_ACCOUNT_VIEW` | شجرة فرعية |
| `GET` | `/lookup` | `lookup()` | `GL_ACCOUNT_VIEW` | LOV بسيط (leafOnly default) |
| `GET` | `/lookup/{pk}` | `lookupById()` | `GL_ACCOUNT_VIEW` | LOV حساب واحد |

---

### 5.2 GlJournalController — `/api/gl/journals`

| HTTP | المسار | الدالة | الصلاحية | الوصف |
|------|--------|--------|---------|-------|
| `POST` | `/search` | `search()` | `GL_JOURNAL_VIEW` | بحث القيود |
| `GET` | `/{id}` | `getById()` | `GL_JOURNAL_VIEW` | تفاصيل القيد مع السطور |
| `POST` | `/` | `create()` | `GL_JOURNAL_CREATE` | إنشاء قيد يدوي |
| `PUT` | `/{id}` | `update()` | `GL_JOURNAL_UPDATE` | تعديل (DRAFT فقط) |
| `PUT` | `/{id}/toggle-active` | `toggleActive()` | `GL_JOURNAL_DELETE` | تفعيل/تعطيل |
| `PATCH` | `/{id}/approve` | `approve()` | `GL_JOURNAL_APPROVE` | DRAFT→APPROVED |
| `PATCH` | `/{id}/post` | `post()` | `GL_JOURNAL_POST` | APPROVED→POSTED |
| `PATCH` | `/{id}/reverse` | `reverse()` | `GL_JOURNAL_REVERSE` | POSTED→REVERSED + قيد انعكاس |
| `PATCH` | `/{id}/cancel` | `cancel()` | `GL_JOURNAL_CANCEL` | إلغاء (ليس POSTED) |

---

### 5.3 AccRuleHdrController — `/api/gl/rules`

| HTTP | المسار | الدالة | الصلاحية | الوصف |
|------|--------|--------|---------|-------|
| `POST` | `/search` | `search()` | `GL_RULE_VIEW` | بحث القواعد |
| `GET` | `/{ruleId}` | `getById()` | `GL_RULE_VIEW` | تفاصيل القاعدة مع السطور |
| `POST` | `/` | `create()` | `GL_RULE_CREATE` | إنشاء قاعدة |
| `PUT` | `/{ruleId}` | `update()` | `GL_RULE_UPDATE` | تعديل |
| `PATCH` | `/{ruleId}/deactivate` | `deactivate()` | `GL_RULE_DELETE` | إلغاء التنشيط |

---

### 5.4 PostingEngineController — `/api/gl/posting`

| HTTP | المسار | الدالة | الصلاحية | الوصف |
|------|--------|--------|---------|-------|
| `POST` | `/execute` | `execute()` | `GL_POSTING_EXECUTE` | تنفيذ ترحيل آلي |

---

## 6. كائنات نقل البيانات (DTOs)

### 6.1 دليل الحسابات

| DTO | الاستخدام | الحقول الرئيسية |
|-----|----------|----------------|
| `AccountsChartCreateRequest` | إنشاء | accountChartName*, accountType*, accountChartFk, organizationFk*, organizationSubFk, isActive |
| `AccountsChartUpdateRequest` | تعديل | accountChartName*, accountType*, accountChartFk, organizationFk*(immutable), organizationSubFk, isActive |
| `AccountsChartResponse` | استجابة | accountChartPk, accountChartNo, accountChartName, accountType, accountChartFk, parentAccountName, parentAccountNo, level, isActive, organizationFk, organizationSubFk, createdAt, createdBy, updatedAt, updatedBy |
| `AccountsChartTreeNode` | شجرة | accountChartPk, accountChartNo, accountChartName, accountType, level, isActive, isLeaf, parentPk, organizationFk, childCount, children[] |
| `EligibleParentAccountDto` | LOV آباء | accountChartPk, accountChartNo, accountChartName, accountType, isActive |
| `AccountLookupDto` | LOV بسيط | id, display ("CODE - NAME"), code, name, type, isActive |

### 6.2 قيود اليومية

| DTO | الاستخدام | الحقول الرئيسية |
|-----|----------|----------------|
| `GlJournalHdrCreateRequest` | إنشاء | journalDate*, journalTypeIdFk*, description, sourceModuleIdFk, sourceDocTypeId, sourceDocIdFk, sourcePostingIdFk, lines[]* |
| `GlJournalHdrUpdateRequest` | تعديل | نفس الحقول |
| `GlJournalLineRequest` | سطر | accountIdFk*, debitAmount, creditAmount, customerIdFk, supplierIdFk, costCenterIdFk, description |
| `GlJournalHdrResponse` | استجابة | id, journalNo, journalDate, journalTypeIdFk, statusIdFk, description, sourceModuleIdFk, sourceDocTypeId, sourceDocIdFk, sourcePostingIdFk, totalDebit, totalCredit, activeFl, lineCount, lines[], createdAt, createdBy, updatedAt, updatedBy |
| `GlJournalLineResponse` | سطر استجابة | id, journalIdFk, lineNo, accountIdFk, accountCode, accountName, debitAmount, creditAmount, customerIdFk, supplierIdFk, costCenterIdFk, description |

### 6.3 القواعد المحاسبية

| DTO | الاستخدام | الحقول الرئيسية |
|-----|----------|----------------|
| `AccRuleHdrCreateRequest` | إنشاء | companyIdFk*, sourceModule*, sourceDocType*, isActive, lines[]* |
| `AccRuleHdrUpdateRequest` | تعديل | companyIdFk*, sourceModule*, sourceDocType*, isActive, lines[]* |
| `AccRuleLineRequest` | سطر | accountIdFk*, entrySide*, priority*, amountSourceType*, amountSourceValue, paymentTypeCode, entityType |
| `AccRuleHdrResponse` | استجابة | ruleId, companyIdFk, companyName, sourceModule, sourceDocType, isActive, lineCount, lines[], createdAt, createdBy, updatedAt, updatedBy |
| `AccRuleLineResponse` | سطر استجابة | ruleLineId, accountIdFk, accountCode, accountName, entrySide, priority, amountSourceType, amountSourceValue, paymentTypeCode, entityType |

### 6.4 محرك الترحيل

| DTO | الاستخدام | الحقول الرئيسية |
|-----|----------|----------------|
| `PostingRequest` | طلب ترحيل | companyIdFk*, sourceModule*, sourceDocType*, totalAmount*(positive), journalDate*, description, sourceDocIdFk, entityMap (Map\<String,Long\>) |
| `PostingResponse` | استجابة ترحيل | ruleId, companyIdFk, sourceModule, sourceDocType, journal (GlJournalHdrResponse) |

---

## 7. المحوّلات (Mappers)

### 7.1 AccountsChartMapper

| الدالة | المدخل | التحويل |
|--------|--------|---------|
| `toEntity(CreateRequest)` | DTO → Entity | `accountChartNo` **لا** يُعيَّن (يُولّد تلقائياً) |
| `updateEntity(Entity, UpdateRequest)` | تعديل Entity | `organizationFk` و `accountChartNo` **لا** يُعدَّلان |
| `toResponse(Entity)` | Entity → DTO | يحسب `level` بالمشي لأعلى، يحل اسم الأب |
| `toTreeNode(Entity, level)` | Entity → Tree | بناء تراتبي مع `isLeaf`, `childCount`, `children[]` |

### 7.2 GlJournalMapper

| الدالة | المدخل | التحويل |
|--------|--------|---------|
| `toEntity(CreateRequest, journalNo)` | DTO → Entity | الحالة = DRAFT, يعيد حساب المجاميع |
| `updateEntity(Entity, UpdateRequest)` | تعديل | يستبدل السطور بالكامل, يعيد حساب المجاميع |
| `toResponse(Entity)` | Entity → DTO | يجلب تفاصيل الحسابات بالجملة (batch lookup) |
| `toListResponse(Entity)` | Entity → DTO خفيف | بدون تفاصيل السطور |

### 7.3 AccRuleHdrMapper

| الدالة | المدخل | التحويل |
|--------|--------|---------|
| `toEntity(CreateRequest)` | DTO → Entity | يحوّل `sourceModule`/`sourceDocType` لـ UPPERCASE |
| `updateEntity(Entity, UpdateRequest)` | تعديل | يستبدل السطور بالكامل |
| `toResponse(Entity)` | Entity → DTO | يجلب تفاصيل الحسابات بالجملة |
| `toListResponse(Entity)` | Entity → DTO خفيف | بدون تفاصيل السطور |

> **ملاحظة:** كل Mapper يفحص `Hibernate.isInitialized()` قبل الوصول للعلاقات الكسولة.

---

## 8. أكواد الأخطاء والرسائل

### `GlErrorCodes.java` — 61 كود خطأ

#### دليل الحسابات (19 كود):

| الكود | الوصف EN | الوصف AR |
|------|---------|---------|
| `GL_ACCOUNT_NOT_FOUND` | Account not found with ID: {0} | الحساب غير موجود بالمعرف: {0} |
| `GL_DUPLICATE_ACCOUNT_CODE` | Account code '{0}' already exists | رمز الحساب '{0}' موجود مسبقاً |
| `GL_ACCOUNT_IN_USE` | Cannot delete, account in use | لا يمكن الحذف، الحساب مستخدم |
| `GL_ACCOUNT_HAS_CHILDREN` | Has {1} active child account(s) | يحتوي على {1} حساب فرعي نشط |
| `GL_ACCOUNT_IN_ACTIVE_RULE` | Used in active accounting rules | مستخدم في قواعد محاسبية نشطة |
| `GL_ACCOUNT_HAS_BALANCE` | Has existing balance | يحتوي على رصيد |
| `GL_ACCOUNT_ORG_LOCKED` | Organization is immutable | المنشأة غير قابلة للتعديل |
| `GL_ACCOUNT_NOT_LEAF` | Account is not a leaf | الحساب ليس ورقياً |
| `GL_INACTIVE_ACCOUNT` | Account is inactive | الحساب غير نشط |
| `GL_ACCOUNT_CIRCULAR_REF` | Circular reference detected | دائرية في المراجع |
| `GL_ACCOUNT_TYPE_MISMATCH` | Type {0} ≠ parent type {1} | النوع لا يطابق الأب |
| `GL_ACCOUNT_PARENT_NOT_FOUND` | Parent not found | الأب غير موجود |
| `GL_ACCOUNT_PARENT_INACTIVE` | Parent is inactive | الأب غير نشط |
| `GL_ACCOUNT_SELF_REFERENCE` | Self-reference | مرجعية ذاتية |
| `GL_ACCOUNT_DESCENDANT_AS_PARENT` | Descendant as parent | حفيد كأب |
| `GL_ACCOUNT_TYPE_CHANGE_WITH_CHILDREN` | Type change with children | تغيير النوع والأبناء موجودون |
| `GL_ACCOUNT_NO_GENERATION_FAILED` | Auto-number generation failed | فشل توليد الرقم |
| `GL_INVALID_ROOT_ACCOUNT_TYPE` | Invalid root account type | نوع حساب جذري غير صالح |
| `GL_ACCOUNT_NO_MANUAL_OVERRIDE` | Manual override not allowed | التجاوز اليدوي ممنوع |

#### القواعد المحاسبية (18 كود):

| الكود | الوصف |
|------|-------|
| `GL_RULE_NOT_FOUND` | القاعدة غير موجودة |
| `GL_DUPLICATE_ACTIVE_RULE` | قاعدة نشطة مكررة لنفس (company, module, docType) |
| `GL_RULE_IN_USE` | القاعدة مستخدمة في ترحيلات |
| `GL_RULE_HAS_PENDING_POSTINGS` | يوجد ترحيلات معلقة |
| `GL_RULE_NO_LINES` | يجب وجود سطر واحد على الأقل |
| `GL_RULE_MISSING_SIDES` | يجب وجود DEBIT و CREDIT معاً |
| `GL_RULE_DUPLICATE_PRIORITY` | أولوية مكررة |
| `GL_RULE_INVALID_PRIORITY` | الأولوية يجب أن تكون موجبة |
| `GL_RULE_AMOUNT_TOTAL_NO_VALUE` | TOTAL: القيمة يجب أن تكون فارغة |
| `GL_RULE_AMOUNT_FIXED_POSITIVE` | FIXED: القيمة يجب أن تكون > 0 |
| `GL_RULE_AMOUNT_PERCENT_RANGE` | PERCENT: القيمة بين 0 و 1 حصرياً |
| `GL_RULE_AMOUNT_REMAINING_NO_VALUE` | REMAINING: القيمة يجب أن تكون فارغة |
| + 6 أكواد أخرى | تحقق المصدر والنوع والجانب |

#### قيود اليومية (11 كود):

| الكود | الوصف |
|------|-------|
| `GL_JOURNAL_NOT_FOUND` | القيد غير موجود |
| `GL_JOURNAL_NOT_BALANCED` | Debit ≠ Credit |
| `GL_JOURNAL_NO_LINES` | لا توجد سطور |
| `GL_JOURNAL_LINE_XOR` | كل سطر يجب أن يحتوي debit أو credit (ليس كليهما) |
| `GL_JOURNAL_NOT_DRAFT` | يمكن التعديل في DRAFT فقط |
| `GL_JOURNAL_NOT_APPROVED` | يمكن الترحيل في APPROVED فقط |
| `GL_JOURNAL_POSTED_IMMUTABLE` | POSTED لا يمكن تعديله (استخدم reverse) |
| `GL_JOURNAL_REVERSE_REQUIRES_POSTED` | الانعكاس يتطلب POSTED |
| `GL_JOURNAL_ALREADY_FINALIZED` | القيد منتهي (CANCELLED أو REVERSED) |
| `GL_JOURNAL_AUTOMATIC_NO_UPDATE` | القيود التلقائية لا تُعدَّل يدوياً |
| `GL_JOURNAL_AUTOMATIC_REQUIRES_SOURCE` | AUTOMATIC يتطلب sourcePostingIdFk |

#### محرك الترحيل (11 كود):

| الكود | الوصف |
|------|-------|
| `GL_POSTING_RULE_NOT_FOUND` | لا توجد قاعدة نشطة |
| `GL_POSTING_NO_RULE_LINES` | القاعدة بلا سطور |
| `GL_POSTING_UNBALANCED` | القيد المولّد غير متوازن |
| `GL_POSTING_ZERO_AMOUNT` | المبلغ يجب أن يكون > 0 |
| `GL_POSTING_NEGATIVE_AMOUNT` | مبلغ سالب |
| `GL_POSTING_REMAINING_NEGATIVE` | REMAINING أصبح سالباً |
| `GL_POSTING_UNKNOWN_AMOUNT_TYPE` | نوع مبلغ غير معروف |
| `GL_POSTING_ACCOUNT_INVALID` | حساب غير موجود أو غير نشط |
| `GL_POSTING_UNKNOWN_ENTITY_TYPE` | نوع كيان غير معروف |
| `GL_POSTING_ENTITY_NOT_PROVIDED` | الكيان غير موفر في entityMap |

#### عام (2 كود):

| الكود | الوصف |
|------|-------|
| `GL_VALIDATION_ERROR` | خطأ تحقق عام |
| `GL_ACCESS_DENIED` | وصول مرفوض |

> **الرسائل:** كل كود له ترجمة في `messages.properties` (EN) و `messages_ar.properties` (AR) مع placeholders `{0}`, `{1}`.

---

## 9. الصلاحيات (Security Permissions)

**ملف:** `SecurityPermissions.java` (في `com.example.security.constants`)

| الصلاحية | الثابت في الكود | الأماكن المستخدمة |
|----------|----------------|------------------|
| `PERM_GL_ACCOUNT_VIEW` | `GL_ACCOUNT_VIEW` | AccountsChartController: search, getById, tree, subTree, lookup, eligibleParents |
| `PERM_GL_ACCOUNT_CREATE` | `GL_ACCOUNT_CREATE` | AccountsChartController: create. + eligibleParents |
| `PERM_GL_ACCOUNT_UPDATE` | `GL_ACCOUNT_UPDATE` | AccountsChartController: update. + eligibleParents |
| `PERM_GL_ACCOUNT_DELETE` | `GL_ACCOUNT_DELETE` | AccountsChartController: deactivate |
| `PERM_GL_RULE_VIEW` | `GL_RULE_VIEW` | AccRuleHdrController: search, getById |
| `PERM_GL_RULE_CREATE` | `GL_RULE_CREATE` | AccRuleHdrController: create |
| `PERM_GL_RULE_UPDATE` | `GL_RULE_UPDATE` | AccRuleHdrController: update |
| `PERM_GL_RULE_DELETE` | `GL_RULE_DELETE` | AccRuleHdrController: deactivate |
| `PERM_GL_JOURNAL_VIEW` | `GL_JOURNAL_VIEW` | GlJournalController: search, getById |
| `PERM_GL_JOURNAL_CREATE` | `GL_JOURNAL_CREATE` | GlJournalController: create |
| `PERM_GL_JOURNAL_UPDATE` | `GL_JOURNAL_UPDATE` | GlJournalController: update |
| `PERM_GL_JOURNAL_DELETE` | `GL_JOURNAL_DELETE` | GlJournalController: toggleActive |
| `PERM_GL_JOURNAL_APPROVE` | `GL_JOURNAL_APPROVE` | GlJournalController: approve |
| `PERM_GL_JOURNAL_POST` | `GL_JOURNAL_POST` | GlJournalController: post |
| `PERM_GL_JOURNAL_REVERSE` | `GL_JOURNAL_REVERSE` | GlJournalController: reverse |
| `PERM_GL_JOURNAL_CANCEL` | `GL_JOURNAL_CANCEL` | GlJournalController: cancel |
| `PERM_GL_POSTING_EXECUTE` | `GL_POSTING_EXECUTE` | PostingEngineController: execute |

> **ملاحظة:** كل endpoint يستخدم `hasAnyAuthority(PERM_XXX, SYSTEM_ADMIN)` — أي أن `SYSTEM_ADMIN` يتجاوز كل الصلاحيات.

---

## 10. ترحيلات قاعدة البيانات (Migrations)

### V4 — `V4__accounts_chart_hierarchical_numbering.sql`
- إنشاء فهارس إضافية لدليل الحسابات
- Unique constraint على (ACCOUNT_CHART_NO, ORGANIZATION_FK)
- فهارس مركبة للأداء

### V5 — `V5__account_type_to_varchar.sql`
- تحويل نوع الحساب من رقمي إلى VARCHAR

### V6 — `V6__gl_lookup_seed_and_payment_type_varchar.sql`
- بذر بيانات LOV:
  - `GL_ACCOUNT_TYPE`: ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
  - `SOURCE_MODULE`: SALES, PURCHASE, INVENTORY, GL, HR
  - `SOURCE_DOC_TYPE`: INVOICE, PAYMENT, RECEIPT, CREDIT_NOTE, DEBIT_NOTE, JOURNAL
  - `ENTRY_SIDE`: DEBIT, CREDIT
  - `AMOUNT_SOURCE_TYPE`: TOTAL, FIXED, PERCENT
  - `ENTITY_TYPE`: CUSTOMER, VENDOR, BANK, EMPLOYEE, ASSET
  - `PAYMENT_TYPE`: CASH, CHECK, BANK_TRANSFER, CREDIT_CARD
- تحويل paymentTypeCode إلى VARCHAR
- صلاحيات الحسابات والقواعد

### V7 — `V7__gl_journal_tables.sql`
- إنشاء جداول: `GL_JOURNAL_HDR`, `GL_JOURNAL_LINE`
- Sequences: `GL_JOURNAL_HDR_SEQ`, `GL_JOURNAL_LINE_SEQ`, `GL_JOURNAL_NO_SEQ`
- بذر LOV: `GL_JOURNAL_TYPE`, `GL_JOURNAL_STATUS`
- صلاحيات القيود (8 صلاحيات)

### V8 — `V8__posting_engine_additions.sql`
- إضافة `REMAINING` إلى `AMOUNT_SOURCE_TYPE` lookup
- صلاحية `PERM_GL_POSTING_EXECUTE`

---

## 11. الأنماط والممارسات المطبقة

### نمط الاستجابة (Response Pattern)
```
Service  →  ServiceResult<T>  →  operationCode.craftResponse()  →  ResponseEntity<ApiResponse<T>>
```

### نمط الأخطاء (Error Pattern)
```java
throw new LocalizedException(HttpStatus.XXX, GlErrorCodes.XXX, arg1, arg2, ...);
```
→ يُترجم تلقائياً من `messages.properties` / `messages_ar.properties`

### نمط الصلاحيات (Security Pattern)
```java
@PreAuthorize("hasAnyAuthority(T(SecurityPermissions).PERM_XXX, T(SecurityPermissions).SYSTEM_ADMIN)")
```

### نمط التحقق من LOV (Lookup Validation)
```java
lookupValidationApi.validateOrThrow(lookupKey, value);
```
→ يتحقق من `MD_MASTER_LOOKUP` في قاعدة البيانات

### نمط التخزين المؤقت (Caching)
- `@Cacheable(cacheNames = "accountsChart", key = "...")` → `getTree()`
- `@CacheEvict(cacheNames = "accountsChart", allEntries = true)` → `create()`, `update()`, `deactivate()`
- `@CacheEvict(cacheNames = "accountingRules", allEntries = true)` → rule CRUD

### نمط الحذف (Deletion = Soft Delete)
- كل الكيانات تستخدم `isActive` / `activeFl` بدلاً من الحذف الفعلي
- `BooleanNumberConverter` للتحويل بين `Boolean` و `NUMBER(1)` في Oracle

### نمط الوراثة (Inheritance)
- كل Entity يرث من `AuditableEntity` ← `BaseEntity`
- يستخدم `@SuperBuilder` من Lombok

### تجنب N+1
- استعلامات `findAllForTree*` تجلب كل الحسابات مع الأب في استعلام واحد
- `findByIdWithLines` يستخدم `LEFT JOIN FETCH`
- Mapper يجلب الحسابات بالجملة (batch lookup بـ `findAllById`)

### معالجة الصفحات (Pagination)
- كل endpoints البحث تدعم `page`, `size`, `sort`
- `PageableBuilder` + `SpecBuilder` للبحث الديناميكي
- `DEFAULT_PAGE_SIZE = 20`, `MAX_PAGE_SIZE = 100`

---

## 12. تدفقات الأعمال (Business Flows)

### 12.1 إنشاء حساب جديد

```
Request → validateAccountType(LOV) → validateParent(exists, active, noCircular, typeMatch)
        → generateAccountNo(auto) → save → evictCache → return 201
```

### 12.2 إنشاء قاعدة محاسبية

```
Request → validateNoDuplicate(company,module,docType) → validateLOVs(module,docType)
        → validateLines(sides, accounts, priorities, amounts) → save → evictCache → return 201
```

### 12.3 دورة حياة القيد اليدوي

```
create(DRAFT) → approve(APPROVED) → post(POSTED) → reverse(REVERSED + REVERSAL journal)
                                                  ↘ (cannot cancel POSTED)
create(DRAFT) → cancel(CANCELLED)
```

### 12.4 الترحيل الآلي

```
PostingRequest(company, module, docType, totalAmount, entityMap)
  → findActiveRule → sortLinesByPriority
  → for each line: calculateAmount + resolveEntity → buildJournalLine
  → buildJournalHdr(AUTOMATIC, DRAFT) → validateBalance → save
  → return PostingResponse(ruleId, journal)
```

---

## 13. قواعد التحقق (Validation Rules)

### 13.1 دليل الحسابات

| الرمز | القاعدة | مكان التنفيذ |
|-------|---------|-------------|
| V-01 | نوع الحساب يجب أن يكون من LOV `GL_ACCOUNT_TYPE` | `AccountsChartService.create/update` |
| V-02 | رقم الحساب يُولّد تلقائياً ولا يُعدَّل | `AccountChartNumberGenerator` + `Mapper` |
| V-03 | الأب يجب أن يكون موجوداً ونشطاً | `AccountChartTreeValidator` |
| V-04 | لا مرجعية ذاتية (الحساب لا يكون أبا لنفسه) | `AccountChartTreeValidator` |
| V-05 | لا دائرية في الشجرة (BFS) | `AccountChartTreeValidator` |
| V-06 | نوع الابن يجب أن يطابق نوع الأب | `AccountChartTreeValidator` |
| V-07 | لا تغيير نوع إذا يوجد أبناء | `AccountChartTreeValidator` |
| V-08 | `organizationFk` غير قابل للتعديل بعد الإنشاء | `AccountsChartService.update` |
| V-09 | لا إلغاء تنشيط إذا يوجد أبناء نشطون | `AccountsChartService.deactivate` |
| V-10 | لا إلغاء تنشيط إذا مستخدم في قواعد نشطة | `AccountsChartService.deactivate` |

### 13.2 القواعد المحاسبية

| الرمز | القاعدة | مكان التنفيذ |
|-------|---------|-------------|
| R-01 | لا قاعدة نشطة مكررة لنفس (company, module, docType) | `AccRuleHdrService.create/update` |
| R-02 | يجب وجود سطر واحد على الأقل | `validateLines()` |
| R-03 | يجب وجود DEBIT و CREDIT معاً | `validateLines()` |
| R-04 | الحساب يجب أن يكون موجوداً ونشطاً وورقياً | `validateLines()` |
| R-05 | `entrySide` من LOV: DEBIT, CREDIT | `validateLines()` |
| R-06 | `amountSourceType` من LOV: TOTAL, FIXED, PERCENT, REMAINING | `validateLines()` |
| R-07 | TOTAL: القيمة فارغة | `validateLines()` |
| R-08 | FIXED: القيمة > 0 | `validateLines()` |
| R-09 | PERCENT: 0 < القيمة < 1 | `validateLines()` |
| R-10 | REMAINING: القيمة فارغة | `validateLines()` |
| R-11 | الأولوية موجبة وفريدة داخل القاعدة | `validateLines()` |
| R-12 | لا تعديل إذا استُخدمت في POSTED/REVERSED | `AccRuleHdrService.update` |
| R-13 | لا إلغاء إذا يوجد DRAFT/APPROVED | `AccRuleHdrService.deactivate` |

### 13.3 قيود اليومية

| الرمز | القاعدة | مكان التنفيذ |
|-------|---------|-------------|
| J-01 | يجب وجود سطر واحد على الأقل | `GlJournalService.validateLines()` |
| J-02 | كل سطر: debit XOR credit (ليس كليهما) | `GlJournalService.validateLines()` |
| J-03 | كل حساب يجب أن يكون موجوداً ونشطاً | `GlJournalService.validateLines()` |
| J-04 | القيد يجب أن يكون متوازناً (totalDebit = totalCredit) | `GlJournalService.create/update/approve/post` |
| J-05 | التعديل فقط في DRAFT | `GlJournalService.update` |
| J-06 | AUTOMATIC لا يُعدَّل يدوياً | `GlJournalService.update` |
| J-07 | AUTOMATIC يتطلب sourcePostingIdFk | `GlJournalService.create` |
| J-08 | الاعتماد فقط من DRAFT | `GlJournalService.approve` |
| J-09 | الترحيل فقط من APPROVED | `GlJournalService.post` |
| J-10 | الانعكاس فقط من POSTED | `GlJournalService.reverse` |
| J-11 | لا إلغاء POSTED (استخدم reverse) | `GlJournalService.cancel` |
| J-12 | لا تعطيل POSTED | `GlJournalService.toggleActive` |

### 13.4 محرك الترحيل

| الرمز | القاعدة | مكان التنفيذ |
|-------|---------|-------------|
| P-01 | `totalAmount` يجب أن يكون > 0 | `PostingEngineService.execute` |
| P-02 | يجب وجود قاعدة نشطة | `PostingEngineService.execute` |
| P-03 | القاعدة يجب أن تحتوي سطور | `PostingEngineService.execute` |
| P-04 | كل حساب يجب أن يكون موجوداً ونشطاً | `PostingEngineService.execute` |
| P-05 | FIXED: القيمة > 0 | `PostingEngineService.calculateAmount` |
| P-06 | PERCENT: القيمة ليست null | `PostingEngineService.calculateAmount` |
| P-07 | REMAINING: المتبقي ≥ 0 | `PostingEngineService.calculateAmount` |
| P-08 | القيد المولّد يجب أن يكون متوازناً | `PostingEngineService.execute` |

---

> **نهاية التحليل** — هذا المستند مُولّد بالكامل من قراءة الكود المصدري الفعلي بدون الرجوع لأي مستند CRS أو مواصفات.
