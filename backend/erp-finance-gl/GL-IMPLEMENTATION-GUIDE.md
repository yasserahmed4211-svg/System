# 📘 Financial Posting Engine & General Ledger Module

## Comprehensive CRS + Functional Specification

**Version:** 2.0
**Owner:** Shama
**Scope:** Finance (GL Lite) for ERP + Legacy Integration
**Last Updated:** 2026-03-27

---

## 0. Implementation Status Tracker

> هذا القسم يوثق الحالة الحالية للتنفيذ مقابل المواصفات المعرّفة في هذه الوثيقة.
> آخر تحديث بعد مراجعة شاملة للكود الفعلي.

### ✅ مكتمل (Implemented & Verified)

| Component | Files | Status |
|-----------|-------|--------|
| **ACCOUNTS_CHART** — شجرة الحسابات الهرمية | `AccountsChart.java`, `AccountsChartService.java`, `AccountsChartController.java` | ✅ CRUD + Tree + Search + Deactivation guards |
| **ACC_RULE_HDR / ACC_RULE_LINE** — محرك القواعد المحاسبية | `AccRuleHdr.java`, `AccRuleLine.java`, `AccRuleHdrService.java`, `AccRuleHdrController.java` | ✅ CRUD + Line validation (R-004→R-017) + Duplicate guard + Rule-in-use guards |
| **GL_JOURNAL_HDR / GL_JOURNAL_LINE** — دفتر اليومية | `GlJournalHdr.java`, `GlJournalLine.java`, `GlJournalService.java`, `GlJournalController.java` | ✅ CRUD + Workflow (approve/post/reverse/cancel) + Balance check |
| **PostingEngineService** — محرك الترحيل الديناميكي | `PostingEngineService.java`, `PostingEngineController.java` | ✅ Dynamic rule-driven engine: TOTAL/FIXED/PERCENT/REMAINING |
| **Amount Source Types** | Lookup: `AMOUNT_SOURCE_TYPE` | ✅ TOTAL, FIXED, PERCENT, REMAINING |
| **DTOs** — العقود العامة | 24 DTO class | ✅ Request/Response for all domains |
| **Error Codes** — أكواد الأخطاء | `GlErrorCodes.java` (53 codes) | ✅ All codes with i18n (EN + AR) |
| **Security Permissions** — الصلاحيات | 16 permissions + `@PreAuthorize` | ✅ Active (not TODO) |
| **DB Migrations** | V4 → V8 | ✅ Tables + Sequences + Indexes + Seeds |

### ⏳ محدد في المواصفات — لم يُنفَّذ بعد (Specified but Not Yet Implemented)

| Component | CRS Section | Notes |
|-----------|-------------|-------|
| **ACC_POSTING_MST / ACC_POSTING_DTL** — Subledger posting tables | §4.1, §4.2, §8.3, §8.4 | الجداول موجودة في DB. لا يوجد entity/service/controller بعد. المسار الحالي: `PostingRequest → PostingEngineService → GL_JOURNAL_HDR` مباشرة بدون المرور بـ ACC_POSTING |
| **GL_LEDGER_ENTRY** — دفتر الأستاذ | §4.5, §8.8 | DDL محدد. لا يوجد entity/service بعد |
| **GL_ACCOUNT_BALANCE** — أرصدة الحسابات | §4.6, §8.9 | DDL محدد. لا يوجد entity/service بعد |
| **GL_FISCAL_PERIOD** — الفترات المالية | §8.2 | DDL محدد. لا يوجد entity/service بعد |
| **GL_RECURRING_TEMPLATE** — القيود المتكررة | §5.6 | DDL محدد. لا يوجد entity/service بعد |
| **Scheduler/Batch Mode** — ترحيل دُفعي | §6.2 | لا يوجد scheduler حتى الآن |
| **Event/Listener Mode** — أحداث ERP | §6.1 | لا يوجد event listener حتى الآن |
| **Financial Reports** — التقارير المالية | §5.4, §7.5 | Trial Balance, P&L, Balance Sheet — لم يُنفَّذ |
| **Ledger API** | §7.3 | لم يُنفَّذ (يعتمد على GL_LEDGER_ENTRY) |
| **Balance API** | §7.4 | لم يُنفَّذ (يعتمد على GL_ACCOUNT_BALANCE) |
| **Approval Workflow — Segregation of Duties** | §5.5 | مبسّط — لا يوجد `PENDING_APPROVAL` status. الحالات: DRAFT→APPROVED→POSTED→REVERSED/CANCELLED |
| **Unit Tests** | — | `src/test/java/` فارغ |

### 🔄 الفرق بين المواصفات والتنفيذ الحالي

| الموضوع | المواصفات (CRS) | التنفيذ الفعلي |
|---------|-----------------|----------------|
| **مسار الترحيل** | `Event/Scheduler → ACC_POSTING_MST/DTL → PostingEngine → GL_JOURNAL` | `PostingRequest → PostingEngineService → GL_JOURNAL_HDR` (مباشر) |
| **Journal Statuses** | `DRAFT, PENDING_APPROVAL, POSTED, REVERSED` | `DRAFT, APPROVED, POSTED, REVERSED, CANCELLED` |
| **Journal Types** | `AUTO, MANUAL, REVERSAL` | `AUTOMATIC, MANUAL, REVERSAL` (via lookup) |
| **Journal Number Format** | `JV-{year}-{seq}`, `MJ-{company}-{year}-{seq}` | `JRN-{seq:06d}` (unified sequence) |
| **GL_JOURNAL_LINE structure** | `ENTRY_SIDE + AMOUNT` (single amount, direction via ENTRY_SIDE) | `DEBIT_AMOUNT + CREDIT_AMOUNT` (dual amount columns, nullable XOR) |
| **Amount Source REMAINING** | غير مذكور في المواصفات الأصلية | ✅ مُنفَّذ: `totalAmount − cumulative same-side` |
| **Fiscal Period validation** | إلزامي قبل الترحيل | ❌ لم يُنفَّذ (لا يوجد GL_FISCAL_PERIOD entity) |
| **Pessimistic Locking** | `SELECT FOR UPDATE` على ACC_POSTING_MST | لم يُنفَّذ (المسار الحالي لا يستخدم ACC_POSTING_MST) |
| **Seed Data** | `GlLookupDataInitializer` (ApplicationRunner) | ❌ حُذف — البيانات تُقرأ من migration SQL مباشرة |

---

## 1. Purpose & Audience

### الهدف من الوثيقة

هذه الوثيقة تصف:

* محرك الترحيل المالي **Financial Posting Engine**
* موديول **General Ledger (GL)** بما يشمل:
  * Journal (دفتر اليومية)
  * Ledger (دفتر الأستاذ)
  * Account Balances (أرصدة الحسابات)
  * Financial Reports (Trial Balance, P&L, Balance Sheet)
* آلية التكامل مع:
  * **ERP System جديد** (Event / Listener – Sync or Near Sync)
  * **نظام قديم (Legacy)** عبر قراءة من `ACC_POSTING_*` عن طريق Scheduler

### الجمهور المستهدف

* Business Analyst
* Solution Architect
* Backend Developer (Spring Boot)
* Frontend Developer (Angular)
* QA / Tester
* أي Tool مثل Copilot للمساعدة في توليد الكود

---

## 2. High-Level Concept

### 2.1 الفكرة الأساسية

1. **الموديولات التشغيلية** (Sales, Rental, Inventory…):
   * تسجّل فقط **الحدث المالي** (Business Event)
   * لا تعرف أرقام الحسابات المحاسبية

2. **محرك الترحيل المالي Posting Engine**:
   * يقرأ الحدث من جداول مركزية `ACC_POSTING_MST / ACC_POSTING_DTL`
   * يطبق قواعد المحاسبة من `ACC_RULE_HDR / ACC_RULE_LINE`
   * يبني **قيد يومية متزن** في `GL_JOURNAL_HDR / GL_JOURNAL_LINE`

3. **دفتر الأستاذ Ledger & Account Balances**:
   * يتم توليد **Ledger Entries** من قيود اليومية
   * يتم تحديث أرصدة الحسابات `GL_ACCOUNT_BALANCE`

4. **التقارير المالية**:
   * Trial Balance
   * Profit & Loss
   * Balance Sheet  
     تعتمد بشكل أساسي على **Account Balances + Ledger**.

---

## 3. Architecture Context

### 3.1 ERP Mode – Event / Listener (Sync أو Near Real-Time)

* عند اكتمال عملية مالية في الـ ERP (مثلاً فاتورة بيع تمت الموافقة عليها):
  1. يتم إطلاق **Domain Event** داخلي (مثال: `FinancialPostingRequestedEvent`)
  2. Listener يقوم بإنشاء سجلات في:
     * `ACC_POSTING_MST`
     * `ACC_POSTING_DTL`
  3. بعد ذلك:
     * إمّا يتم استدعاء `PostingEngine.postSingle(postingId)` فورًا (Sync)
     * أو يتم ترك الـ Posting بحالة `READY` ليتم ترحيله بواسطة Job داخلي يعمل كل X ثواني.

> **القلب المحاسبي واحد، الاختلاف فقط في طريقة Trigger.**

---

### 3.2 Legacy Mode – Scheduler / Batch

* نظام قديم لا يدعم Events.
* يقوم النظام القديم أو ETL بكتابة الأحداث في `ACC_POSTING_MST/DTL` بحالة `NEW` أو `READY`.
* في خدمة Finance (Spring Boot) يوجد **Scheduler Job**:
  * كل X دقيقة:
    * يقرأ كل `POSTING_ID` حيث `STATUS IN ('NEW', 'READY_FOR_GL')`
    * ينادي `PostingEngine.postSingle(postingId)` لكل واحدة.

---

## 4. Data Model (Summary)

### 4.1 ACC_POSTING_MST – Central Posting Master (Subledger Posting Header)

#### Functional Role

ACC_POSTING_MST serves as the **central posting header** within the Subledger layer. It captures the financial event initiated by any operational module (Sales, Rental, Inventory, etc.) and governs the **full posting lifecycle** from inception to GL integration.

This table is the **entry point** of the deterministic posting pipeline. Each record represents a single business transaction awaiting or having completed accounting treatment. It does **not** perform any account resolution — that responsibility belongs exclusively to the Rule Engine (ACC_RULE_HDR / ACC_RULE_LINE).

#### Architectural Significance

* **Lifecycle Controller:** ACC_POSTING_MST manages posting state transitions through a well-defined status workflow:
  `NEW → READY_FOR_GL → POSTED → ERROR`.
  These are the only four valid states, enforced by CHECK constraint at the database level.
* **GL Integration Link:** Upon successful posting, the `FIN_JOURNAL_ID_FK` column establishes a direct foreign key reference to the resulting GL Journal (`GL_JOURNAL_HDR`), ensuring full traceability from business event to accounting entry.
* **Balance Verification:** The `TOTAL_DEBIT` and `TOTAL_CREDIT` fields (`NUMBER(18,2)`) store the aggregate debit and credit amounts produced by the Rule Engine, enabling pre-commit balanced journal validation.
* **Audit & Traceability:** Source document metadata (`SOURCE_MODULE`, `SOURCE_DOC_TYPE`, `SOURCE_DOC_ID`, `SOURCE_DOC_NO`) provides complete backward traceability to the originating business event.
* **Concurrency Safety:** Pessimistic locking is applied during posting to prevent concurrent processing of the same record.

#### Key Attributes

* `POSTING_ID` — Unique posting identifier (PK, `NUMBER(19,0)`)
* `COMPANY_ID_FK` — Company reference (`NOT NULL`)
* `BRANCH_ID_FK` — Branch reference (optional)
* `SOURCE_MODULE` — Originating module (`NOT NULL`, e.g., `SALES`, `RENTAL`, `INVENTORY`)
* `SOURCE_DOC_TYPE` — Document classification (`NOT NULL`, e.g., `INVOICE`, `RECEIPT`, `CONTRACT_POSTING`)
* `SOURCE_DOC_ID` — Primary key reference to the source document (`NOT NULL`)
* `SOURCE_DOC_NO` — Human-readable document number
* `DOC_DATE` — Document date (`NOT NULL`, determines fiscal period)
* `CURRENCY_CODE` — Transaction currency (default `'SAR'`)
* `STATUS` — Posting lifecycle state (`NOT NULL`): `NEW`, `READY_FOR_GL`, `POSTED`, `ERROR`
* `TOTAL_DEBIT` — Aggregate debit total from all posting detail lines (default `0`)
* `TOTAL_CREDIT` — Aggregate credit total from all posting detail lines (default `0`)
* `FIN_JOURNAL_ID_FK` — Foreign key to `GL_JOURNAL_HDR` after successful posting
* `ERROR_MESSAGE` — Diagnostic message captured on posting failure

> **Design Principle:**
> ACC_POSTING_MST is a lifecycle envelope. It contains no accounting logic, no account references, and no debit/credit direction. All accounting decisions are resolved deterministically by the Rule Engine and recorded in ACC_POSTING_DTL.

---

### 4.2 ACC_POSTING_DTL – Central Posting Detail (Subledger Posting Lines)

#### Functional Role

ACC_POSTING_DTL represents the **deterministic accounting detail lines** generated by the Rule Engine for each posting master record. Each line is a fully resolved accounting instruction containing the **final account**, the **entry side** (DEBIT or CREDIT), and the **monetary amount**.

Unlike prior architectural approaches that relied on BUSINESS_SIDE for account resolution or SIGN for debit/credit determination, the current architecture treats each detail line as a **self-contained, pre-resolved accounting directive**. No further interpretation, mapping, or mathematical inversion is required at posting time.

#### Architectural Significance

* **Deterministic Output:** Each ACC_POSTING_DTL line contains `ACCOUNT_ID_FK` (the resolved GL account, `NOT NULL`) and `ENTRY_SIDE` (the explicit debit/credit direction, `NOT NULL`). These values are final — they are produced by the Rule Engine and consumed directly by the Posting Engine without transformation.
* **No BUSINESS_SIDE Column:** Unlike the legacy design, this table does **not** contain a `BUSINESS_SIDE` column. The analytical classification exists only in `ACC_RULE_LINE` as a descriptive label. The posting detail contains only deterministic accounting facts.
* **No SIGN Column:** The legacy `SIGN` column (`+1/-1`) has been eliminated. Debit/credit direction is controlled entirely and explicitly by `ENTRY_SIDE`, enforced by CHECK constraint `CK_POSTING_ENTRY`.
* **Subledger Reference:** `ENTITY_TYPE` (`VARCHAR2(30)`) and `ENTITY_ID` (`NUMBER(19,0)`) provide a generic polymorphic reference to subledger entities (Customer, Supplier, Contract, etc.), enabling flexible subledger drill-down. A composite index `IDX_POSTING_DTL_ENTITY` supports efficient queries.
* **Balanced Journal Foundation:** The sum of all DEBIT lines must equal the sum of all CREDIT lines within a single posting. This invariant is enforced at the posting master level before GL Journal creation.

#### Key Attributes

* `POSTING_DTL_ID` — Unique detail line identifier (PK, `NUMBER(19,0)`)
* `POSTING_ID_FK` — Foreign key to `ACC_POSTING_MST` (`NOT NULL`)
* `LINE_NO` — Sequential line number within the posting (`NOT NULL`)
* `ACCOUNT_ID_FK` — **Final resolved GL account** (`NOT NULL`, FK to `ACCOUNTS_CHART`). Determined by the Rule Engine, not at posting time.
* `ENTRY_SIDE` — Explicit debit/credit direction (`NOT NULL`): `DEBIT` or `CREDIT`. This is the **sole source of truth** for posting direction. Enforced by `CK_POSTING_ENTRY`.
* `AMOUNT` — Monetary amount (`NOT NULL`, always positive; direction determined by `ENTRY_SIDE`)
* `ENTITY_TYPE` — Generic subledger entity classifier (e.g., `CUSTOMER`, `SUPPLIER`, `CONTRACT`)
* `ENTITY_ID` — Identifier of the referenced subledger entity
* `DESCRIPTION` — Optional line-level description

> **Design Principle:**
> Each ACC_POSTING_DTL line is a **pre-resolved accounting fact**. The Posting Engine reads these lines and transcribes them directly into GL Journal Lines without applying any mapping, sign inversion, or dynamic account resolution.

---

### 4.3 ACC_RULE_HDR / ACC_RULE_LINE – Deterministic Accounting Rule Engine

#### Functional Role

The Accounting Rule Engine is the **single source of truth** for all accounting decisions within the Subledger layer. It is structured as a Header/Line model:

* **ACC_RULE_HDR** — Defines the posting template for a specific combination of `(COMPANY_ID, SOURCE_MODULE, SOURCE_DOC_TYPE)`. Only one active rule header is permitted per combination, ensuring deterministic and unambiguous rule resolution.

* **ACC_RULE_LINE** — Defines the individual accounting instructions within each rule template. Each line specifies:
  * `ACCOUNT_ID_FK` — The **final GL account** to be posted.
  * `ENTRY_SIDE` — The explicit debit/credit direction (`DEBIT` or `CREDIT`).
  * `AMOUNT_SOURCE_TYPE` — How the amount is calculated: `TOTAL` (full transaction amount), `FIXED` (static value), or `PERCENT` (percentage of total).
  * `PRIORITY` — Execution order of rule lines during posting.

#### Architectural Significance

* **No Runtime Resolution:** The Rule Engine pre-defines every account and direction. During posting, there is no dynamic lookup, no BUSINESS_SIDE-to-account mapping, and no conditional branching. The engine simply applies the rule lines in priority order.
* **Deterministic by Design:** Given the same `(COMPANY_ID, SOURCE_MODULE, SOURCE_DOC_TYPE)` input, the engine always produces the same accounting output. This guarantees auditability, repeatability, and IFRS-compliant consistency.
* **Template-Based Architecture:** Each rule header acts as a reusable posting template. Changing an accounting policy (e.g., redirecting revenue to a different account) requires only a rule configuration change — no code modification.
* **Single Active Rule Constraint:** The system enforces that only one rule header can be active for any given `(COMPANY_ID, SOURCE_MODULE, SOURCE_DOC_TYPE)` combination, preventing ambiguity during rule resolution.

#### Key Attributes — ACC_RULE_HDR

* `RULE_ID` — Unique rule header identifier (PK, `NUMBER(19,0)`)
* `COMPANY_ID_FK` — Company scope (`NOT NULL`)
* `SOURCE_MODULE` — Originating module (`NOT NULL`, e.g., `SALES`, `RENTAL`)
* `SOURCE_DOC_TYPE` — Document type (`NOT NULL`, e.g., `INVOICE`, `RECEIPT`)
* `IS_ACTIVE` — Active flag (`NUMBER(1,0)`, values `0` or `1`, `NOT NULL`). Only one active rule per combination.
* Unique constraint: `ACC_RULE_HDR_UK (COMPANY_ID_FK, SOURCE_MODULE, SOURCE_DOC_TYPE)`

#### Key Attributes — ACC_RULE_LINE

* `RULE_LINE_ID` — Unique rule line identifier (PK, `NUMBER(19,0)`)
* `RULE_ID_FK` — Foreign key to `ACC_RULE_HDR` (`NOT NULL`)
* `BUSINESS_SIDE` — Descriptive label for the rule line (e.g., `AR`, `REVENUE`, `VAT_OUT`). Does NOT drive account selection.
* `ACCOUNT_ID_FK` — **Final GL account** (`NOT NULL`, FK to `ACCOUNTS_CHART`)
* `ENTRY_SIDE` — `DEBIT` or `CREDIT` (`NOT NULL`, explicit and immutable per line)
* `PRIORITY` — Execution order during posting (`NOT NULL`)
* `AMOUNT_SOURCE_TYPE` — `TOTAL`, `FIXED`, or `PERCENT` (`NOT NULL`)
* `AMOUNT_SOURCE_VALUE` — Numeric value (`NUMBER(18,6)`): holds the fixed amount when `AMOUNT_SOURCE_TYPE = FIXED`, or the percentage factor when `AMOUNT_SOURCE_TYPE = PERCENT`. NULL when `AMOUNT_SOURCE_TYPE = TOTAL`.
* `PAYMENT_TYPE_CODE` — Conditional rule application based on payment method (optional)
* `ENTITY_TYPE` — Subledger entity classification to record in `ACC_POSTING_DTL` (optional, `VARCHAR2(20)`)

> **Design Principle:**
> The Rule Engine eliminates all ambiguity from the accounting process. Every GL account, every debit/credit direction, and every amount derivation is explicitly declared in rule configuration — not inferred, calculated, or dynamically resolved at runtime.

> **Example:**
> For `(COMPANY=1, MODULE=SALES, DOC_TYPE=INVOICE)`, the active rule header contains three rule lines:
>
> | Priority | Business Side | Account       | Entry Side | Amount Source Type | Amount Source Value |
> |----------|---------------|---------------|------------|--------------------|-----------------|
> | 1        | AR            | 120101 (A/R)  | DEBIT      | TOTAL              | NULL            |
> | 2        | REVENUE       | 410101 (Rev)  | CREDIT     | PERCENT            | 0.952381        |
> | 3        | VAT_OUT       | 220301 (VAT)  | CREDIT     | PERCENT            | 0.047619        |

---

### 4.4 GL_JOURNAL_HDR / GL_JOURNAL_LINE – Journal (دفتر اليومية)

**GL_JOURNAL_HDR**

* `JOURNAL_ID`
* `JOURNAL_NO` – مثل: `JV-2025-0001`
* `COMPANY_ID`
* `DOC_DATE`
* `DESCRIPTION`
* `STATUS` – `DRAFT`, `PENDING_APPROVAL`, `POSTED`, `REVERSED`
* `SOURCE_POSTING_ID`

**GL_JOURNAL_LINE** *(mirrors ACC_POSTING_DTL structure)*

* `JOURNAL_LINE_ID`
* `JOURNAL_ID_FK`
* `LINE_NO`
* `ACCOUNT_ID_FK` – الحساب المحاسبي المستهدف (FK → ACCOUNTS_CHART)
* `ENTRY_SIDE` – `DEBIT` أو `CREDIT` (اتجاه القيد الصريح)
* `AMOUNT` – المبلغ (دائماً موجب، الاتجاه يحدده ENTRY_SIDE)
* `ENTITY_TYPE` – نوع الكيان المرجعي (CUSTOMER, SUPPLIER, COST_CENTER, CONTRACT…)
* `ENTITY_ID` – معرّف الكيان (Polymorphic FK)
* `DESCRIPTION`

> **ملاحظة معمارية:** GL_JOURNAL_LINE يستخدم نفس نمط ACC_POSTING_DTL تماماً.  
> الـ Posting Engine ينسخ الأعمدة 1:1 بدون أي تحويل أو حساب.

---

### 4.5 GL_LEDGER_ENTRY – دفتر الأستاذ

* `ENTRY_ID`
* `ACCOUNT_ID_FK` – الحساب المحاسبي (FK → ACCOUNTS_CHART)
* `JOURNAL_ID_FK`, `JOURNAL_LINE_ID_FK` – ربط بقيد اليومية
* `DOC_DATE`
* `ENTRY_SIDE` – `DEBIT` أو `CREDIT` (نفس نمط GL_JOURNAL_LINE)
* `AMOUNT` – المبلغ (دائماً موجب)
* `ENTITY_TYPE`, `ENTITY_ID` – مرجع الكيان (Polymorphic)
* `DESCRIPTION`

> **ملاحظة:** `RUNNING_BALANCE` لا يُخزَّن في قاعدة البيانات.  
> يُحسب ديناميكياً عبر Window Function (`OVER()`) في الاستعلامات.  
> هذا يتجنب تعقيدات إعادة الحساب عند العكس أو التصحيح.

> دفتر الأستاذ هو منبع كل التحليل، وتتبع الحركات، والتدقيق.

---

### 4.6 GL_ACCOUNT_BALANCE – Account Balances

* `COMPANY_ID`
* `FISCAL_YEAR`
* `PERIOD_NO`
* `ACCOUNT_ID`
* `OPENING_BALANCE`
* `PERIOD_DEBIT`
* `PERIOD_CREDIT`
* `CLOSING_BALANCE`

> هذا الجدول هو **الملخص** الذي تبنى عليه التقارير:  
> Trial Balance, P&L, Balance Sheet.

---

### 4.7 Balance Convention — Debit-Positive (اتفاقية الأرصدة)

**القاعدة:**

يعتمد النظام على اتفاقية **Debit-Positive** في جميع جداول الأرصدة:

```text
Closing Balance = Opening Balance + Period Debit - Period Credit
```

هذا يعني:
* **الرصيد الموجب** = رصيد مدين صافي (مثال: أصول، مصروفات)
* **الرصيد السالب** = رصيد دائن صافي (مثال: التزامات، حقوق ملكية، إيرادات)

**العلاقة مع طبيعة الحساب (ACCOUNT_TYPE):**

| ACCOUNT_TYPE | الطبيعة العادية | Closing Balance الطبيعي | عرض التقارير |
|-------------|----------------|------------------------|-------------|
| ASSET       | مدين (DEBIT)   | موجب (+)               | يُعرض كما هو |
| EXPENSE     | مدين (DEBIT)   | موجب (+)               | يُعرض كما هو |
| LIABILITY   | دائن (CREDIT)  | سالب (-)               | يُعرض بالقيمة المطلقة |
| EQUITY      | دائن (CREDIT)  | سالب (-)               | يُعرض بالقيمة المطلقة |
| REVENUE     | دائن (CREDIT)  | سالب (-)               | يُعرض بالقيمة المطلقة |

**مثال:**
* حساب "الصندوق" (ASSET): مدين 10,000 - دائن 3,000 → Closing = +7,000 (صحيح)
* حساب "الموردين" (LIABILITY): مدين 2,000 - دائن 8,000 → Closing = -6,000 → يُعرض في التقارير كـ 6,000

> **مهم:** طبقة التقارير (Report Layer) هي المسؤولة عن عكس الإشارة (Sign Inversion) للحسابات ذات الطبيعة الدائنة. قاعدة البيانات تخزن دائماً بالاتفاقية المدينة الموجبة.

---

## 5. Business Logic (Detailed)

### 5.1 Posting Lifecycle

#### الحالات:

* `NEW` – حدث مالي تم تسجيله، لم يدخل بعد في المحاسبة.
* `READY_FOR_GL` – جاهز للترحيل (مثلاً بعد Approval).
* `POSTED` – تم إنشاء قيد محاسبي، وتم تحديث GL.
* `ERROR` – حدث خطأ أثناء الترحيل.

> **ملاحظة:** الحالات محصورة حصرياً في هذه الأربع قيم بموجب CHECK constraint `CK_POSTING_STATUS`. عملية العكس (Reversal) تتم عبر إنشاء posting جديد باتجاهات معكوسة، وليس عبر تغيير حالة السجل الأصلي.

#### منطق الترحيل (Posting) — Deterministic Rule-Based Engine:

The posting process follows a deterministic, rule-driven pipeline. No dynamic account resolution or sign-based logic is applied during posting. All accounting decisions are pre-resolved by the Rule Engine and stored in ACC_POSTING_DTL.

**Posting Algorithm:**

1. **Select eligible postings:** Retrieve all `ACC_POSTING_MST` records where `STATUS IN ('NEW', 'READY_FOR_GL')`.
2. **For each `POSTING_ID`:**
   * **Acquire pessimistic lock** on the posting record to prevent concurrent processing.
   * **Validate status:** Confirm the posting has not been processed by another thread.
   * **Validate fiscal period:** Ensure the target period is `OPEN` for posting.
   * **Read detail lines:** Load all `ACC_POSTING_DTL` records for this posting.
   * **Build GL Journal directly from detail lines:**
     * For each `ACC_POSTING_DTL` line:
       * Read `ACCOUNT_ID_FK` → this is the **final GL account** (no mapping required).
       * Read `ENTRY_SIDE` → this is the **explicit debit/credit direction** (no sign calculation).
       * Read `AMOUNT` → this is the **posting amount** (always positive).
     * Create `GL_JOURNAL_LINE` entries mirroring the detail lines exactly.
   * **Enforce balanced journal:**
     * Validate: `SUM(DEBIT lines) = SUM(CREDIT lines)`.
     * If unbalanced → reject with `STATUS = ERROR`.
   * **Persist GL records:**
     * Save `GL_JOURNAL_HDR` with `TOTAL_DEBIT` and `TOTAL_CREDIT`.
     * Save `GL_JOURNAL_LINE` entries.
   * **Post-commit operations:**
     * Create `GL_LEDGER_ENTRY` records from journal lines.
     * Update `GL_ACCOUNT_BALANCE` for affected accounts and periods.
   * **Finalize posting:**
     * Set `STATUS = POSTED`.
     * Set `FIN_JOURNAL_ID_FK = JOURNAL_ID`.
     * Clear `ERROR_MESSAGE`.

> **Key Invariant:** The Posting Engine performs no accounting logic. It is a **transcription engine** that converts pre-resolved ACC_POSTING_DTL lines into GL Journal entries and enforces balanced journal integrity.

> **Unified Column Pipeline (Zero-Transformation Architecture):**
> ```
> ACC_RULE_LINE (template: ENTRY_SIDE + AMOUNT_SOURCE_TYPE)
>     ↓ Rule Engine resolves amounts
> ACC_POSTING_DTL (instance: ACCOUNT_ID_FK, ENTRY_SIDE, AMOUNT, ENTITY_TYPE, ENTITY_ID)
>     ↓ Posting Engine copies verbatim
> GL_JOURNAL_LINE (posted: ACCOUNT_ID_FK, ENTRY_SIDE, AMOUNT, ENTITY_TYPE, ENTITY_ID)
>     ↓ Ledger Service copies verbatim
> GL_LEDGER_ENTRY (ledger: ACCOUNT_ID_FK, ENTRY_SIDE, AMOUNT, ENTITY_TYPE, ENTITY_ID)
>     ↓ Balance Service aggregates
> GL_ACCOUNT_BALANCE (aggregate: PERIOD_DEBIT, PERIOD_CREDIT)
> ```
> All four transactional tables share identical column semantics. No transformation, mapping, or sign calculation occurs at any stage.

لو حصل خطأ (Rule ناقصة، قيد غير متزن، فترة مغلقة…):

* لا ينشأ Journal.
* `STATUS = ERROR`.
* `ERROR_MESSAGE` توضح السبب.

---

### 5.2 Manual Journal Entry (إضافة يدوي في دفتر اليومية)

#### الهدف:

* تمكين المستخدم المالي من إدخال قيود محاسبية مباشرة مثل:
  * قيود تسوية
  * إهلاك
  * ترحيل أرباح
  * تصحيح خطأ محاسبي

#### DTO Structure:

**ManualJournalRequest:**
```java
{
  "companyId": Long (required, @Positive),
  "docDate": LocalDate (required),
  "description": String (required, @NotBlank),
  "lines": List<ManualJournalLineRequest> (required, @NotEmpty)
}
```

**ManualJournalLineRequest:**
```java
{
  "accountId": Long (required, @Positive),
  "entrySide": String (required, "DEBIT" or "CREDIT"),
  "amount": BigDecimal (required, @Positive),
  "entityType": String (optional, e.g., "CUSTOMER", "COST_CENTER"),
  "entityId": Long (optional),
  "description": String (optional)
}
```

> **ملاحظة UX:** واجهة المستخدم يمكنها عرض عمودين (مدين/دائن) وتحويلها تلقائياً إلى `entrySide` + `amount` عند الإرسال. هذا يحافظ على تجربة المحاسب المألوفة مع توحيد البنية الداخلية.

#### قواعد التحقق (Validation Rules):

1. **Header Validation:**
   * `companyId` يجب أن يكون موجب
   * `docDate` مطلوب
   * `description` لا يمكن أن يكون فارغاً
   * `lines` يجب أن تحتوي على سطرين على الأقل

2. **Line Validation:**
   * `accountId` يجب أن يكون موجب
   * `entrySide` يجب أن يكون `DEBIT` أو `CREDIT`
   * `amount` يجب أن يكون موجب (أكبر من صفر)
   * `entityType` إذا وُجد، يجب أن يكون من القيم المعتمدة (CUSTOMER, SUPPLIER, COST_CENTER, CONTRACT)

3. **Business Rules:**
   * مجموع سطور `DEBIT` = مجموع سطور `CREDIT` (Journal must be balanced)
   * إجمالي القيد لا يمكن أن يكون صفر
   * الحسابات يجب أن تكون موجودة وفعّالة (future validation)

4. **Posting Behavior:**
   * ينشأ `GL_JOURNAL_HDR` مع `JOURNAL_TYPE = 'MANUAL'`
   * ينشأ `GL_JOURNAL_LINE` لكل سطر (بنفس نمط `ENTRY_SIDE` + `AMOUNT`)
   * `STATUS = 'DRAFT'` عند الإنشاء (القيد اليدوي يحتاج موافقة)
   * يتم توليد `JOURNAL_NO` بصيغة: `MJ-{companyId}-{year}-{sequence}`
   * لا يوجد `SOURCE_POSTING_ID` (null)

5. **Approval Workflow (سير عمل الموافقة):**
   * `DRAFT` → `PENDING_APPROVAL` → `POSTED`
   * **DRAFT:** تم إنشاء القيد ويمكن تعديله أو حذفه
   * **PENDING_APPROVAL:** تم إرسال القيد للموافقة، لا يمكن تعديله
   * **POSTED:** تمت الموافقة والترحيل، لا يمكن تعديله (فقط عكس)
   * القيود الآلية (AUTO) تُرحَل مباشرة بحالة `POSTED` (لا تحتاج موافقة)
   * المستخدم المنشئ لا يمكنه الموافقة على قيده (Segregation of Duties)

6. **Post-Creation:**
   * القيد اليدوي يمكن عكسه فقط (Reversal)
   * لا يُسمح بتعديل قيم المدين/الدائن بعد POSTED
   * يتم تسجيل `createdBy` و `createdAt` من SecurityContext

---

### 5.3 Reversal Logic (قيد عكسي)

#### الحالات التي تحتاج Reversal:

* إلغاء فاتورة بعد ترحيلها.
* تصحيح خطأ كامل في حركة معينة.

#### المنطق — Deterministic Reversal via ENTRY_SIDE Inversion:

Reversal follows the same deterministic architecture. No SIGN column or mathematical inversion is used. Instead, the `ENTRY_SIDE` is explicitly inverted for each detail line.

1. **Create new `ACC_POSTING_MST`:**
   * `SOURCE_DOC_TYPE = {ORIGINAL_DOC_TYPE}_REVERSAL`
   * Same `COMPANY_ID_FK` and `DOC_DATE` (or current date per company policy).
   * `STATUS = NEW` (enters standard posting lifecycle).

2. **Create `ACC_POSTING_DTL` lines with inverted `ENTRY_SIDE`:**
   * For each original detail line:
     * Same `ACCOUNT_ID_FK` (same GL account).
     * Same `AMOUNT` (same absolute value).
     * **Inverted `ENTRY_SIDE`:** Original `DEBIT` → Reversal `CREDIT`, and vice versa.
     * Same `ENTITY_TYPE` and `ENTITY_ID` for subledger traceability.
   * No SIGN column is used. Direction is controlled entirely by `ENTRY_SIDE`.

3. **Posting Engine processes the reversal normally:**
   * The reversal posting enters the standard lifecycle (`NEW → READY_FOR_GL → POSTED`).
   * The Posting Engine transcribes the detail lines into a GL Journal with inverted directions.
   * The resulting reversal journal is guaranteed to produce equal and opposite entries.

4. **Traceability:**
   * The reversal posting references the original via `SOURCE_DOC_TYPE = {ORIGINAL_DOC_TYPE}_REVERSAL` and `SOURCE_DOC_ID` pointing to the original source document.
   * The original `ACC_POSTING_MST` record remains unchanged — its `STATUS` stays `POSTED`. The production CHECK constraint (`CK_POSTING_STATUS`) only permits `NEW`, `READY_FOR_GL`, `POSTED`, `ERROR`. There is no `REVERSED` status at the posting level.
   * Reversal visibility is achieved through the journal layer: `GL_JOURNAL_HDR.STATUS = 'REVERSED'` on the original journal and `JOURNAL_TYPE = 'REVERSAL'` on the reversal journal.

> **Audit Guarantee:** Both the original and reversal postings remain fully visible and traceable. No data is modified or deleted — only new records are created with explicit directional inversion. The original posting record is never altered.

---

### 5.4 Reporting Logic

> **اتفاقية الأرصدة:** النظام يخزن الأرصدة بنمط Debit-Positive (راجع القسم 4.7). طبقة التقارير مسؤولة عن عكس الإشارة للحسابات ذات الطبيعة الدائنة (LIABILITY, EQUITY, REVENUE) باستخدام `ABS(CLOSING_BALANCE)` أو `CLOSING_BALANCE * -1`.

1. **Trial Balance:**
   * من `GL_ACCOUNT_BALANCE`
   * تجميع حسب:
     * `COMPANY_ID`, `FISCAL_YEAR`, `PERIOD_RANGE`
   * الأعمدة:
     * Opening, Period Debit, Period Credit, Closing.
   * **عرض Closing:** يبقى كما هو (القيمة تعكس الاتجاه الفعلي).

2. **Profit & Loss:**
   * من `ACCOUNTS_CHART` حيث `ACCOUNT_TYPE in (REVENUE, EXPENSE)`
   * من `GL_ACCOUNT_BALANCE` (حركة الفترات المطلوبة)
   * **Sign Inversion:** الإيرادات تُعرض بالقيمة المطلقة (REVENUE × -1 لأن Closing سالب)
   * حساب صافي الربح: `ABS(Total Revenues) – Total Expenses`

3. **Balance Sheet:**
   * من `ACCOUNTS_CHART` حيث `ACCOUNT_TYPE in (ASSET, LIABILITY, EQUITY)`
   * من `GL_ACCOUNT_BALANCE` (Closing balances)
   * **Sign Inversion:** الالتزامات وحقوق الملكية تُعرض بالقيمة المطلقة
   * التأكد من المعادلة: `Assets = ABS(Liabilities) + ABS(Equity)`

---

### 5.5 Manual Journal Approval Workflow (سير عمل موافقة القيود اليدوية)

#### State Machine:

```text
DRAFT ──→ PENDING_APPROVAL ──→ POSTED
  │              │
  │              └──→ DRAFT (رفض مع ملاحظات)
  │
  └──→ DELETED (حذف قبل الإرسال)
```

#### القواعد:

1. **DRAFT → PENDING_APPROVAL:**
   * يمكن لمنشئ القيد إرسال القيد للموافقة
   * يجب أن يكون القيد متزناً (Debit = Credit)
   * بعد الإرسال لا يُسمح بتعديل القيد

2. **PENDING_APPROVAL → POSTED:**
   * يجب أن يكون المعتمد مختلفاً عن المنشئ (Segregation of Duties)
   * يجب أن يملك المعتمد صلاحية `GL_APPROVE_JOURNAL`
   * عند الموافقة:
     * يتم تحديث `STATUS = 'POSTED'`
     * يتم تسجيل `POSTED_BY` و `POSTED_AT`
     * يتم إنشاء `GL_LEDGER_ENTRY` لكل سطر
     * يتم تحديث `GL_ACCOUNT_BALANCE`

3. **PENDING_APPROVAL → DRAFT (رفض):**
   * يمكن للمعتمد رفض القيد مع ملاحظات
   * يعود القيد لحالة `DRAFT` ويمكن تعديله وإعادة إرساله

4. **DRAFT → DELETED:**
   * يمكن حذف القيد في حالة `DRAFT` فقط
   * لا يمكن حذف القيد بعد الإرسال أو الترحيل

#### APIs:

| Action | Endpoint | Method |
|--------|----------|--------|
| إنشاء مسودة | `/api/gl/journals/manual` | POST |
| إرسال للموافقة | `/api/gl/journals/{id}/submit` | PUT |
| الموافقة | `/api/gl/journals/{id}/approve` | PUT |
| الرفض | `/api/gl/journals/{id}/reject` | PUT |
| حذف مسودة | `/api/gl/journals/{id}` | DELETE |

> **ملاحظة:** القيود الآلية (`JOURNAL_TYPE = 'AUTO'`) تُنشأ بحالة `POSTED` مباشرة عبر محرك الترحيل ولا تمر بسير عمل الموافقة.

---

### 5.6 Recurring Journals (القيود المتكررة)

#### الهدف:

تمكين إنشاء قيود يومية متكررة تلقائياً (مثل: إيجار شهري، إهلاك، رواتب) دون تدخل يدوي في كل فترة.

#### DDL — GL_RECURRING_TEMPLATE:

```sql
CREATE SEQUENCE SEQ_GL_RECURRING_TEMPLATE START WITH 1 INCREMENT BY 1;

CREATE TABLE GL_RECURRING_TEMPLATE (
    TEMPLATE_ID          NUMBER          NOT NULL,
    TEMPLATE_NAME        VARCHAR2(200)   NOT NULL,
    COMPANY_ID_FK        NUMBER          NOT NULL,
    DESCRIPTION          VARCHAR2(500),
    FREQUENCY            VARCHAR2(20)    NOT NULL CHECK (FREQUENCY IN ('MONTHLY','QUARTERLY','SEMI_ANNUAL','ANNUAL')),
    START_DATE           DATE            NOT NULL,
    END_DATE             DATE,
    NEXT_RUN_DATE        DATE            NOT NULL,
    IS_ACTIVE            NUMBER(1)       DEFAULT 1 CHECK (IS_ACTIVE IN (0, 1)),
    AUTO_POST            NUMBER(1)       DEFAULT 0 CHECK (AUTO_POST IN (0, 1)),
    TOTAL_DEBIT          NUMBER(15,2)    DEFAULT 0,
    TOTAL_CREDIT         NUMBER(15,2)    DEFAULT 0,
    CREATED_BY           VARCHAR2(100),
    CREATED_AT           TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    UPDATED_BY           VARCHAR2(100),
    UPDATED_AT           TIMESTAMP,
    CONSTRAINT GL_RECURRING_TEMPLATE_PK PRIMARY KEY (TEMPLATE_ID)
);

CREATE TABLE GL_RECURRING_TEMPLATE_LINE (
    LINE_ID              NUMBER          NOT NULL,
    TEMPLATE_ID_FK       NUMBER          NOT NULL,
    LINE_NO              NUMBER          NOT NULL,
    ACCOUNT_ID_FK        NUMBER          NOT NULL,
    ENTRY_SIDE           VARCHAR2(6)     NOT NULL CHECK (ENTRY_SIDE IN ('DEBIT','CREDIT')),
    AMOUNT               NUMBER(15,2)    NOT NULL CHECK (AMOUNT > 0),
    ENTITY_TYPE          VARCHAR2(50),
    ENTITY_ID            NUMBER,
    DESCRIPTION          VARCHAR2(500),
    CONSTRAINT GL_RECURRING_TEMPLATE_LINE_PK PRIMARY KEY (LINE_ID),
    CONSTRAINT GL_RECURRING_LINE_TEMPLATE_FK FOREIGN KEY (TEMPLATE_ID_FK)
        REFERENCES GL_RECURRING_TEMPLATE (TEMPLATE_ID),
    CONSTRAINT GL_RECURRING_LINE_ACCOUNT_FK FOREIGN KEY (ACCOUNT_ID_FK)
        REFERENCES ACCOUNTS_CHART (ACCOUNT_CHART_PK)
);
```

#### قواعد العمل:

1. **الإنشاء:** يُنشئ المستخدم قالباً (Template) مع سطور القيد ونوع التكرار
2. **التوليد التلقائي:** Scheduler يفحص `NEXT_RUN_DATE <= TODAY` و `IS_ACTIVE = 1`:
   * يُنشئ `GL_JOURNAL_HDR` بنوع `MANUAL` من القالب
   * إذا `AUTO_POST = 1`: يُرحَل مباشرة (`STATUS = 'POSTED'`)
   * إذا `AUTO_POST = 0`: يُنشأ كمسودة (`STATUS = 'DRAFT'`) ويدخل سير عمل الموافقة
3. **تحديث NEXT_RUN_DATE:** بعد التوليد يُحسب تاريخ التنفيذ التالي حسب `FREQUENCY`
4. **الإيقاف:** إذا `END_DATE` مر أو `IS_ACTIVE = 0`، لا يتم توليد قيود جديدة

#### APIs:

| Action | Endpoint | Method |
|--------|----------|--------|
| إنشاء قالب | `/api/gl/recurring-templates` | POST |
| قائمة القوالب | `/api/gl/recurring-templates` | GET |
| تفاصيل قالب | `/api/gl/recurring-templates/{id}` | GET |
| تعديل قالب | `/api/gl/recurring-templates/{id}` | PUT |
| إيقاف/تفعيل | `/api/gl/recurring-templates/{id}/toggle` | PUT |
| توليد يدوي | `/api/gl/recurring-templates/{id}/generate` | POST |

---

## 6. Execution Modes (ERP Events vs Scheduler)

### 6.1 ERP Mode – Event / Listener

* بعد إتمام عملية (مثلاً Save & Approve Invoice):
  * يتم إطلاق Event.
  * Listener يترجم الحدث إلى `ACC_POSTING_MST/DTL`.
  * يمكن أن:
    * ينادي `PostingEngine.postSingle(postingId)` مباشرة.
    * أو يتركه لـ Job داخلي.

> مناسب لتجربة مستخدم لحظية، وإظهار رقم القيد بعد العملية مباشرة أو بعد ثوانٍ.

---

### 6.2 Legacy Mode – Scheduler

* نظام قديم يكتب في `ACC_POSTING_*`.
* Scheduler في Finance Service يقوم بـ:
  1. قراءة `POSTING_ID` حيث `STATUS IN ('NEW','READY_FOR_GL')`
  2. تطبيق Pessimistic Lock (`SELECT FOR UPDATE`) لمنع التضارب بين Instances
  3. استدعاء `PostingEngine.postSingle(postingId)`
  4. تحديث `STATUS = POSTED` أو `ERROR`

> **ملاحظة:** لا يوجد حالة `IN_PROGRESS` في الـ CHECK constraint. التضارب يُمنع عبر Pessimistic Locking.

---

## 7. API Specifications (Detailed)

> **ملاحظة:** هذه الـ APIs تعريف منطقي. التنفيذ الفعلي قد يستخدم DTOs، Pagination، Security (JWT) إلخ.

### 7.1 Posting APIs

#### 7.1.1 GET `/postings/new`

* **Description (EN):**  
  Retrieve all postings with status NEW or READY_FOR_GL for review/monitoring.

* **الوصف (AR):**  
  عرض جميع الحركات المالية التي لم تُرحّل بعد (NEW/READY_FOR_GL) لمراجعتها أو متابعتها.

* **Query Params (اختياري):**
  * `status` (default: `NEW,READY_FOR_GL`)
  * `companyId`
  * `sourceModule`
  * Pagination: `page`, `size`

* **Response (Example):**

```json
[
  {
    "postingId": 1001,
    "sourceModule": "SALES",
    "sourceDocType": "INVOICE",
    "sourceDocNo": "INV-2025-0001",
    "docDate": "2025-12-08",
    "companyId": 1,
    "status": "NEW",
    "totalDebit": 105.0,
    "totalCredit": 105.0
  }
]
```

---

#### 7.1.2 GET `/postings/{id}`

* **AR:** عرض تفاصيل حركة مالية محددة (Master + Detail).

* **EN:** Get a single posting with details.

* **Path Variable:**
  * `id` = `POSTING_ID`

* **Response (Example):**

```json
{
  "postingId": 1001,
  "sourceModule": "SALES",
  "sourceDocType": "INVOICE",
  "sourceDocId": 555,
  "sourceDocNo": "INV-2025-0001",
  "docDate": "2025-12-08",
  "companyId": 1,
  "status": "NEW",
  "totalDebit": 105.0,
  "totalCredit": 105.0,
  "details": [
    { "lineNo": 1, "accountId": 120101, "entrySide": "DEBIT", "amount": 105.0, "entityType": "CUSTOMER", "entityId": 100 },
    { "lineNo": 2, "accountId": 410101, "entrySide": "CREDIT", "amount": 100.0 },
    { "lineNo": 3, "accountId": 220301, "entrySide": "CREDIT", "amount": 5.0 }
  ]
}
```

---

#### 7.1.3 POST `/postings/{id}/post`

* **AR:** تنفيذ الترحيل المحاسبي لحركة واحدة.

* **EN:** Post a single financial posting into GL.

* **Path:**
  * `{id}` = `POSTING_ID`

* **Request Body:** (عادة لا يحتاج؛ يمكن أن يكون فارغًا)

* **Response (Example):**

```json
{
  "postingId": 1001,
  "status": "POSTED",
  "journalId": 2001,
  "journalNo": "JV-2025-0001",
  "message": "Posting successfully posted to GL."
}
```

* **Errors Examples:**
  * `400` – Missing accounting rule
  * `409` – Posting already POSTED
  * `500` – Unbalanced journal

---

#### 7.1.4 POST `/postings/post-all`

* **AR:** ترحيل Batch لكل الحركات NEW/READY.

* **EN:** Batch post all NEW/READY postings.

* **Request Body (اختياري):**

```json
{
  "companyId": 1,
  "maxItems": 100
}
```

* **Response Example:**

```json
{
  "totalFound": 25,
  "successCount": 23,
  "errorCount": 2,
  "errors": [
    { "postingId": 1020, "errorMessage": "No active accounting rule found for SALES/INVOICE" },
    { "postingId": 1035, "errorMessage": "Fiscal period is closed" }
  ]
}
```

---

#### 7.1.5 POST `/postings/{id}/reverse`

* **AR:** إنشاء قيد عكسي لحركة مُرحّلة.

* **EN:** Reverse a posted financial posting (create reversal journal).

* **Path:**
  * `{id}` = original `POSTING_ID` (status must be `POSTED`)

* **Response Example:**

```json
{
  "originalPostingId": 1001,
  "reversalPostingId": 1201,
  "reversalJournalId": 2201,
  "status": "REVERSED"
}
```

---

#### 7.1.6 PATCH `/postings/{id}/status`

* **AR:** تعديل حالة الحركة (للاستخدام الإداري فقط).

* **EN:** Manually update posting status (admin use).

* **Body Example:**

```json
{
  "status": "READY_FOR_GL"
}
```

---

### 7.2 Journal APIs (دفتر اليومية)

#### 7.2.1 GET `/journals`

* **AR:** بحث في قيود اليومية مع فلاتر.

* **EN:** Search journals with filters.

* **Query Params:**
  * `companyId`
  * `dateFrom`, `dateTo`
  * `sourcePostingId`
  * `journalNo`
  * `status` (DRAFT, PENDING_APPROVAL, POSTED, REVERSED)
  * Pagination

* **Response Example:**

```json
[
  {
    "journalId": 2001,
    "journalNo": "JV-2025-0001",
    "companyId": 1,
    "docDate": "2025-12-08",
    "description": "SALES INVOICE #INV-2025-0001",
    "status": "POSTED",
    "totalDebit": 105.0,
    "totalCredit": 105.0
  }
]
```

---

#### 7.2.2 GET `/journals/{id}`

* **AR:** عرض قيد اليومية مع السطور.

* **EN:** Get full journal with lines.

* **Response Example:**

```json
{
  "journalId": 2001,
  "journalNo": "JV-2025-0001",
  "companyId": 1,
  "docDate": "2025-12-08",
  "description": "SALES INVOICE #INV-2025-0001",
  "status": "POSTED",
  "lines": [
    { "lineNo": 1, "accountCode": "110101", "entrySide": "DEBIT", "amount": 105.0, "entityType": "CUSTOMER", "entityId": 100 },
    { "lineNo": 2, "accountCode": "410101", "entrySide": "CREDIT", "amount": 100.0 },
    { "lineNo": 3, "accountCode": "220301", "entrySide": "CREDIT", "amount": 5.0 }
  ]
}
```

---

#### 7.2.3 POST `/journals/manual`

* **AR:** إنشاء قيد يدوي في دفتر اليومية.

* **EN:** Create manual journal entry.

* **Endpoint:** `POST /api/gl/journals/manual`

* **Request Example:**

```json
{
  "companyId": 1,
  "docDate": "2025-12-31",
  "description": "Year-end adjustment",
  "lines": [
    { 
      "accountId": 500101, 
      "entrySide": "DEBIT", 
      "amount": 1000.0,
      "entityType": "COST_CENTER",
      "entityId": 10,
      "description": "Depreciation expense"
    },
    { 
      "accountId": 310201, 
      "entrySide": "CREDIT", 
      "amount": 1000.0,
      "description": "Accumulated depreciation" 
    }
  ]
}
```

* **Business Rules:**
  * مجموع سطور DEBIT = مجموع سطور CREDIT إلزامي
  * على الأقل سطرين في القيد
  * كل سطر يحدد `entrySide` (مدين أو دائن) و `amount` (موجب)
  * جميع المبالغ يجب أن تكون موجبة (أكبر من صفر)

* **Response (201 Created):**

```json
{
  "journalId": 12345,
  "journalNo": "MJ-1-2025-000001",
  "companyId": 1,
  "docDate": "2025-12-31",
  "description": "Year-end adjustment",
  "journalType": "MANUAL",
  "status": "DRAFT",
  "totalDebit": 1000.00,
  "totalCredit": 1000.00,
  "lineCount": 2,
  "createdAt": "2025-12-13T10:30:00",
  "createdBy": "admin"
}
```

* **Validation Errors (400 Bad Request):**

```json
{
  "status": 400,
  "message": "Journal is not balanced: totalDebit=1000.00, totalCredit=1500.00",
  "timestamp": "2025-12-13T10:30:00"
}
```

أو:

```json
{
  "status": 400,
  "message": "Journal must have at least two lines",
  "timestamp": "2025-12-13T10:30:00"
}
```

أو:

```json
{
  "status": 400,
  "message": "Line 1: Cannot have both debit and credit amounts. Use only one.",
  "timestamp": "2025-12-13T10:30:00"
}
```

---

#### 7.2.4 POST `/journals/{id}/reverse`

* **AR:** إنشاء قيد عكسي لقيد يدوي أو آلي.

* **EN:** Reverse a journal (manual or system).

* **Response Example:**

```json
{
  "originalJournalId": 2001,
  "reversalJournalId": 2101,
  "message": "Journal reversed successfully."
}
```

---

### 7.3 Ledger APIs – دفتر الأستاذ

#### 7.3.1 GET `/ledger/{accountId}`

* **AR:** عرض حركات الأستاذ لحساب معين (Running balance).

* **EN:** Get ledger entries for a specific account.

* **Query Params:**
  * `dateFrom`, `dateTo`
  * `companyId`
  * Pagination

* **Response Example:**

```json
{
  "accountId": 110101,
  "accountCode": "110101",
  "accountName": "Accounts Receivable",
  "entries": [
    {
      "docDate": "2025-12-08",
      "journalNo": "JV-2025-0001",
      "description": "Sales Invoice INV-2025-0001",
      "entrySide": "DEBIT",
      "amount": 105.0,
      "entityType": "CUSTOMER",
      "entityId": 100,
      "runningBalance": 105.0
    },
    {
      "docDate": "2025-12-09",
      "journalNo": "JV-2025-0005",
      "description": "Customer Receipt",
      "entrySide": "CREDIT",
      "amount": 50.0,
      "entityType": "CUSTOMER",
      "entityId": 100,
      "runningBalance": 55.0
    }
  ]
}
```

---

#### 7.3.2 GET `/ledger`

* **AR:** بحث مرن في دفتر الأستاذ مع فلاتر متعددة.
* **EN:** Flexible ledger search (by account range, date range, etc.).

---

### 7.4 Account Balances APIs

#### 7.4.1 GET `/balances`

* **AR:** عرض أرصدة الحسابات لفترة معينة.

* **EN:** Get account balances for a given period or range.

* **Query Params:**
  * `companyId`
  * `fiscalYear`
  * `periodFrom`
  * `periodTo`

* **Response Example:**

```json
[
  {
    "accountCode": "110101",
    "accountName": "Accounts Receivable",
    "openingBalance": 30.0,
    "periodDebit": 105.0,
    "periodCredit": 60.0,
    "closingBalance": 75.0
  }
]
```

---

#### 7.4.2 POST `/balances/recalculate`

* **AR:** إعادة حساب أرصدة الحسابات من Ledger (استخدام إداري/تصحيحي).
* **EN:** Recalculate account balances from ledger entries.

---

### 7.5 Reports APIs

#### 7.5.1 GET `/reports/trial-balance`

* **AR:** ميزان المراجعة لفترة محددة.

* **EN:** Trial balance report.

* **Query:**
  * `companyId`, `fiscalYear`, `periodFrom`, `periodTo`

* **Returns:** نفس هيكل `/balances` لكن مع أعمدة مدين/دائن لكل حساب.

---

#### 7.5.2 GET `/reports/profit-loss`

* **AR:** قائمة الأرباح والخسائر.

* **EN:** Profit & Loss statement.

* **Query:**
  * `companyId`, `fiscalYear`, `periodFrom`, `periodTo`

* **Logic Summary:**
  * تجميع الحسابات ذات النوع REVENUE / EXPENSE
  * حساب Net Profit

---

#### 7.5.3 GET `/reports/balance-sheet`

* **AR:** الميزانية العمومية.

* **EN:** Balance Sheet.

* **Query:**
  * `companyId`, `fiscalYear`, `periodNo` (أو تاريخ)

* **Logic Summary:**
  * تجميع الحسابات ASSET / LIABILITY / EQUITY
  * حساب Closing balances

---

## 8. Complete Database Schema (DDL Scripts)

### 8.1 ACCOUNTS_CHART – Chart of Accounts (Hierarchical Account Structure)

#### Functional Description (Architecture)

ACCOUNTS_CHART is the **hierarchical chart of accounts** that defines the complete accounting structure for each organization within the ERP system. Every GL account used in posting, journaling, or financial reporting must exist as an active record in this table.

**Architectural Role:**

* **Hierarchical Structure:** Accounts are organized in a parent-child tree using `ACCOUNT_CHART_FK` (self-referential foreign key). This supports multi-level account hierarchies required for consolidated financial reporting (e.g., Assets → Current Assets → Cash → Petty Cash).
* **Account Nature Classification:** The `ACCOUNT_TYPE` column (numeric) defines the fundamental nature of each account (طبيعة الحساب). This classification governs:
  * Balance Sheet vs. Profit & Loss classification
  * Year-end closing behavior (P&L accounts zero out; Balance Sheet accounts carry forward)
  * Normal balance direction (Assets/Expenses = Debit normal; Liabilities/Equity/Revenue = Credit normal)
* **Organizational Scoping:** `ORGANIZATION_FK` and `ORGANIZATION_SUB_FK` define the organizational ownership of each account, supporting multi-company and multi-branch structures.
* **Rule Engine Alignment:** Every `ACCOUNT_ID_FK` referenced in `ACC_RULE_LINE` and `ACC_POSTING_DTL` must point to a valid, active account (`ACTIVE = 1`) in this table. This prevents the Rule Engine from producing references to invalid or deactivated accounts.
* **IFRS Compliance:** The chart structure supports IFRS-compliant account categorization, enabling standard-conforming Trial Balance, P&L, and Balance Sheet generation directly from account hierarchy and type classification.

**Interaction with Other Tables:**

| Referencing Table     | FK Column        | Relationship |
|-----------------------|------------------|--------------|
| ACC_RULE_LINE         | ACCOUNT_ID_FK    | Defines target accounts in posting rules |
| ACC_POSTING_DTL       | ACCOUNT_ID_FK    | Records the resolved account per detail line |
| GL_JOURNAL_LINE       | ACCOUNT_ID_FK    | Records the posted account per journal line |
| GL_ACCOUNT_BALANCE    | ACCOUNT_ID_FK    | Stores period balances per account |
| GL_LEDGER_ENTRY       | ACCOUNT_ID_FK    | Records individual ledger movements per account |

**Column Mapping Reference:**

| Column               | Description                                |
|----------------------|--------------------------------------------|
| ACCOUNT_CHART_PK     | Primary key (account identifier)           |
| ACCOUNT_CHART_NO     | Account code visible to users (كود البند)     |
| ACCOUNT_CHART_NAME   | Account name (اسم البند)                    |
| ACCOUNT_CHART_FK     | Parent account (self-referential FK, رقم البند الاب) |
| ACTIVE               | Active flag (1 = active, 0 = inactive)     |
| ORGANIZATION_FK      | Organization (company) reference           |
| ORGANIZATION_SUB_FK  | Sub-organization (branch) reference        |
| ACCOUNT_TYPE         | Account nature (طبيعة الحساب)               |

```sql
--------------------------------------------------------
--  DDL for Table ACCOUNTS_CHART
--------------------------------------------------------
CREATE TABLE "ACCOUNTS_CHART" (
    "ACCOUNT_CHART_PK"       NUMBER,
    "ACCOUNT_CHART_NO"       VARCHAR2(50),
    "ACCOUNT_CHART_NAME"     VARCHAR2(500),
    "ACCOUNT_CHART_FK"       NUMBER,
    "ACTIVE"                 NUMBER,
    "ORGANIZATION_FK"        NUMBER,
    "ORGANIZATION_SUB_FK"    NUMBER,
    "CREATED_DATE"           TIMESTAMP(6),
    "CREATED_BY"             VARCHAR2(20),
    "MODIFIED_DATE"          TIMESTAMP(6),
    "MODIFIED_BY"            VARCHAR2(20),
    "ACCOUNT_TYPE"           NUMBER
);

COMMENT ON COLUMN "ACCOUNTS_CHART"."ACCOUNT_CHART_NO" IS 'كود البند';
COMMENT ON COLUMN "ACCOUNTS_CHART"."ACCOUNT_CHART_NAME" IS 'اسم البند';
COMMENT ON COLUMN "ACCOUNTS_CHART"."ACCOUNT_CHART_FK" IS 'رقم البند الاب';
COMMENT ON COLUMN "ACCOUNTS_CHART"."ACCOUNT_TYPE" IS 'طبيعة الحساب';

--------------------------------------------------------
--  Indexes
--------------------------------------------------------
CREATE UNIQUE INDEX "ACCOUNTS_CHART_PK" ON "ACCOUNTS_CHART" ("ACCOUNT_CHART_PK");

--------------------------------------------------------
--  Constraints
--------------------------------------------------------
ALTER TABLE "ACCOUNTS_CHART" MODIFY ("ACCOUNT_CHART_PK" NOT NULL ENABLE);
ALTER TABLE "ACCOUNTS_CHART" ADD CONSTRAINT "ACCOUNTS_CHART_PK" PRIMARY KEY ("ACCOUNT_CHART_PK") USING INDEX ENABLE;

--------------------------------------------------------
--  Ref Constraints (Self-Referential Hierarchy)
--------------------------------------------------------
ALTER TABLE "ACCOUNTS_CHART" ADD CONSTRAINT "ACCOUNTS_CHART_FK1"
    FOREIGN KEY ("ACCOUNT_CHART_FK") REFERENCES "ACCOUNTS_CHART" ("ACCOUNT_CHART_PK") ENABLE;
```

**Account Hierarchy Example:**

```
1000    Assets        (ACCOUNT_CHART_FK=NULL)
  1100    Current Assets  (ACCOUNT_CHART_FK=1000)
    1101    Cash            (ACCOUNT_CHART_FK=1100) ← Leaf / Posting Allowed
    1102    Bank            (ACCOUNT_CHART_FK=1100) ← Leaf / Posting Allowed
  1200    Fixed Assets    (ACCOUNT_CHART_FK=1000)
    1201    Buildings       (ACCOUNT_CHART_FK=1200) ← Leaf / Posting Allowed
```

---

### 8.2 GL_FISCAL_PERIOD – Fiscal Period Management

```sql
-- Sequence
CREATE SEQUENCE SEQ_GL_FISCAL_PERIOD START WITH 1 INCREMENT BY 1;

-- Table
CREATE TABLE GL_FISCAL_PERIOD (
    ID_PK                NUMBER          NOT NULL,
    COMPANY_ID_FK        NUMBER          NOT NULL,
    FISCAL_YEAR          NUMBER(4)       NOT NULL,
    PERIOD_NO            NUMBER(2)       NOT NULL,  -- 1-12 for monthly
    PERIOD_NAME          VARCHAR2(50)    NOT NULL,  -- 'Jan-2025', 'Period 1'
    START_DATE           DATE            NOT NULL,
    END_DATE             DATE            NOT NULL,
    STATUS               VARCHAR2(20)    DEFAULT 'OPEN' CHECK (STATUS IN ('OPEN','CLOSED','PERMANENTLY_CLOSED')),
    CLOSED_BY            VARCHAR2(100),
    CLOSED_AT            TIMESTAMP,
    CREATED_BY           VARCHAR2(100),
    CREATED_AT           TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    UPDATED_BY           VARCHAR2(100),
    UPDATED_AT           TIMESTAMP,
    
    CONSTRAINT GL_FISCAL_PERIOD_PK PRIMARY KEY (ID_PK),
    CONSTRAINT GL_FISCAL_PERIOD_UK UNIQUE (COMPANY_ID_FK, FISCAL_YEAR, PERIOD_NO),
    CONSTRAINT GL_FISCAL_PERIOD_DATES_CK CHECK (END_DATE >= START_DATE)
);

-- Index
CREATE INDEX IDX_GL_FISCAL_PERIOD_YEAR ON GL_FISCAL_PERIOD (COMPANY_ID_FK, FISCAL_YEAR);

-- Comments
COMMENT ON TABLE GL_FISCAL_PERIOD IS 'Manages fiscal periods and their open/closed status';
COMMENT ON COLUMN GL_FISCAL_PERIOD.STATUS IS 'OPEN: can post, CLOSED: cannot post, PERMANENTLY_CLOSED: cannot reopen';
```

**Period Validation Logic:**
```java
public void validatePeriodIsOpen(Long companyId, LocalDate docDate) {
    GlFiscalPeriod period = periodRepository
        .findByCompanyAndDate(companyId, docDate)
        .orElseThrow(() -> new BusinessException("No fiscal period found for date: " + docDate));
    
    if (!"OPEN".equals(period.getStatus())) {
        throw new BusinessException("Cannot post to " + period.getStatus() + " period: " + period.getPeriodName());
    }
}
```

---

### 8.3 ACC_POSTING_MST – Central Posting Master (Subledger Header)

#### Functional Description (Architecture)

ACC_POSTING_MST is the **central posting header** in the Subledger layer. It serves as the lifecycle controller for all financial events entering the accounting pipeline.

**Key Design Characteristics:**

* **Lifecycle Management:** The `STATUS` column governs the posting workflow through a deterministic state machine:
  `NEW → READY_FOR_GL → POSTED → ERROR`.
  The CHECK constraint enforces only valid states: `('NEW','READY_FOR_GL','POSTED','ERROR')`.
* **Balance Totals:** `TOTAL_DEBIT` and `TOTAL_CREDIT` (both `NUMBER(18,2)`) store the aggregate debit and credit amounts from all associated `ACC_POSTING_DTL` lines, enabling pre-commit balanced journal validation (`TOTAL_DEBIT = TOTAL_CREDIT`).
* **GL Linkage:** `FIN_JOURNAL_ID_FK` provides a direct foreign key to the resulting `GL_JOURNAL_HDR` record after successful posting, ensuring full traceability from source transaction to accounting entry.
* **Source Traceability:** `SOURCE_MODULE`, `SOURCE_DOC_TYPE`, `SOURCE_DOC_ID`, and `SOURCE_DOC_NO` provide complete backward reference to the originating business event.
* **Concurrency Control:** Pessimistic locking (`SELECT FOR UPDATE`) is applied during posting to prevent dual processing.
* **No Accounting Logic:** This table contains no account references, no debit/credit direction, and no accounting rules. It is purely a lifecycle and metadata container.

```sql
--------------------------------------------------------
--  DDL for Table ACC_POSTING_MST
--------------------------------------------------------
CREATE TABLE "ACC_POSTING_MST" (
    "POSTING_ID"             NUMBER(19,0),
    "COMPANY_ID_FK"          NUMBER(19,0),
    "BRANCH_ID_FK"           NUMBER(19,0),
    "SOURCE_MODULE"          VARCHAR2(50),
    "SOURCE_DOC_TYPE"        VARCHAR2(50),
    "SOURCE_DOC_ID"          NUMBER(19,0),
    "SOURCE_DOC_NO"          VARCHAR2(100),
    "DOC_DATE"               DATE,
    "CURRENCY_CODE"          VARCHAR2(3) DEFAULT 'SAR',
    "STATUS"                 VARCHAR2(20),
    "TOTAL_DEBIT"            NUMBER(18,2) DEFAULT 0,
    "TOTAL_CREDIT"           NUMBER(18,2) DEFAULT 0,
    "ERROR_MESSAGE"          VARCHAR2(1000),
    "FIN_JOURNAL_ID_FK"      NUMBER(19,0),
    "CREATED_AT"             TIMESTAMP(6),
    "CREATED_BY"             VARCHAR2(100),
    "UPDATED_AT"             TIMESTAMP(6),
    "UPDATED_BY"             VARCHAR2(100)
);

--------------------------------------------------------
--  Indexes
--------------------------------------------------------
CREATE UNIQUE INDEX "SYS_C0025589" ON "ACC_POSTING_MST" ("POSTING_ID");
CREATE INDEX "IDX_POSTING_SOURCE" ON "ACC_POSTING_MST" ("SOURCE_MODULE", "SOURCE_DOC_TYPE");
CREATE INDEX "IDX_POSTING_DOC" ON "ACC_POSTING_MST" ("SOURCE_DOC_ID");
CREATE INDEX "IDX_POSTING_STATUS" ON "ACC_POSTING_MST" ("STATUS");

--------------------------------------------------------
--  Constraints
--------------------------------------------------------
ALTER TABLE "ACC_POSTING_MST" MODIFY ("COMPANY_ID_FK" NOT NULL ENABLE);
ALTER TABLE "ACC_POSTING_MST" MODIFY ("SOURCE_MODULE" NOT NULL ENABLE);
ALTER TABLE "ACC_POSTING_MST" MODIFY ("SOURCE_DOC_TYPE" NOT NULL ENABLE);
ALTER TABLE "ACC_POSTING_MST" MODIFY ("SOURCE_DOC_ID" NOT NULL ENABLE);
ALTER TABLE "ACC_POSTING_MST" MODIFY ("DOC_DATE" NOT NULL ENABLE);
ALTER TABLE "ACC_POSTING_MST" MODIFY ("STATUS" NOT NULL ENABLE);
ALTER TABLE "ACC_POSTING_MST" ADD CONSTRAINT "CK_POSTING_STATUS"
    CHECK (STATUS IN ('NEW','READY_FOR_GL','POSTED','ERROR')) ENABLE;
ALTER TABLE "ACC_POSTING_MST" ADD PRIMARY KEY ("POSTING_ID") USING INDEX ENABLE;
```

---

### 8.4 ACC_POSTING_DTL – Central Posting Detail (Subledger Lines)

#### Functional Description (Architecture)

ACC_POSTING_DTL contains the **pre-resolved accounting lines** for each posting master record. Each line is a deterministic accounting instruction produced by the Rule Engine.

**Key Design Characteristics:**

* **Deterministic Account Assignment:** `ACCOUNT_ID_FK` (`NOT NULL`) stores the **final resolved GL account**. This account is determined by the Rule Engine at rule application time — not dynamically at posting time.
* **Explicit Entry Direction:** `ENTRY_SIDE` (`DEBIT` or `CREDIT`, `NOT NULL`) is the **sole source of truth** for posting direction. No SIGN column exists, no mathematical inversion, and no DEFAULT_SIDE lookup is involved. Enforced by CHECK constraint `CK_POSTING_ENTRY`.
* **Positive Amount Convention:** `AMOUNT` (`NOT NULL`) is always a positive value. The posting direction is controlled entirely by `ENTRY_SIDE`.
* **Generic Subledger Reference:** `ENTITY_TYPE` (`VARCHAR2(30)`) and `ENTITY_ID` (`NUMBER(19,0)`) provide a polymorphic reference to subledger entities (Customer, Supplier, Contract, etc.). A composite index `IDX_POSTING_DTL_ENTITY` supports efficient subledger drill-down queries.
* **No BUSINESS_SIDE Column:** This table does **not** contain a `BUSINESS_SIDE` column. The analytical classification exists only in `ACC_RULE_LINE` as a descriptive label. The posting detail is purely deterministic — account + direction + amount.
* **No SIGN Column:** The legacy `SIGN` column has been eliminated. Direction is controlled entirely by `ENTRY_SIDE`.

```sql
--------------------------------------------------------
--  DDL for Table ACC_POSTING_DTL
--------------------------------------------------------
CREATE TABLE "ACC_POSTING_DTL" (
    "POSTING_DTL_ID"     NUMBER(19,0),
    "POSTING_ID_FK"      NUMBER(19,0),
    "LINE_NO"            NUMBER(10,0),
    "ACCOUNT_ID_FK"      NUMBER(19,0),
    "ENTRY_SIDE"         VARCHAR2(10),
    "AMOUNT"             NUMBER(18,2),
    "ENTITY_TYPE"        VARCHAR2(30),
    "ENTITY_ID"          NUMBER(19,0),
    "DESCRIPTION"        VARCHAR2(500),
    "CREATED_AT"         TIMESTAMP(6),
    "CREATED_BY"         VARCHAR2(100),
    "UPDATED_AT"         TIMESTAMP(6),
    "UPDATED_BY"         VARCHAR2(100)
);

--------------------------------------------------------
--  Indexes
--------------------------------------------------------
CREATE UNIQUE INDEX "SYS_C0025596" ON "ACC_POSTING_DTL" ("POSTING_DTL_ID");
CREATE INDEX "IDX_POSTING_DTL_POSTING" ON "ACC_POSTING_DTL" ("POSTING_ID_FK");
CREATE INDEX "IDX_POSTING_DTL_ACCOUNT" ON "ACC_POSTING_DTL" ("ACCOUNT_ID_FK");
CREATE INDEX "IDX_POSTING_DTL_ENTITY" ON "ACC_POSTING_DTL" ("ENTITY_TYPE", "ENTITY_ID");

--------------------------------------------------------
--  Constraints
--------------------------------------------------------
ALTER TABLE "ACC_POSTING_DTL" MODIFY ("POSTING_ID_FK" NOT NULL ENABLE);
ALTER TABLE "ACC_POSTING_DTL" MODIFY ("LINE_NO" NOT NULL ENABLE);
ALTER TABLE "ACC_POSTING_DTL" MODIFY ("ACCOUNT_ID_FK" NOT NULL ENABLE);
ALTER TABLE "ACC_POSTING_DTL" MODIFY ("ENTRY_SIDE" NOT NULL ENABLE);
ALTER TABLE "ACC_POSTING_DTL" MODIFY ("AMOUNT" NOT NULL ENABLE);
ALTER TABLE "ACC_POSTING_DTL" ADD CONSTRAINT "CK_POSTING_ENTRY"
    CHECK (ENTRY_SIDE IN ('DEBIT','CREDIT')) ENABLE;
ALTER TABLE "ACC_POSTING_DTL" ADD PRIMARY KEY ("POSTING_DTL_ID") USING INDEX ENABLE;

--------------------------------------------------------
--  Ref Constraints
--------------------------------------------------------
ALTER TABLE "ACC_POSTING_DTL" ADD CONSTRAINT "FK_POSTING_DTL_MST"
    FOREIGN KEY ("POSTING_ID_FK") REFERENCES "ACC_POSTING_MST" ("POSTING_ID") ENABLE;
```

---

### 8.5 ACC_RULE_HDR / ACC_RULE_LINE – Deterministic Accounting Rule Engine

#### Functional Description (Architecture)

The Rule Engine is implemented as a proper **Header/Line (Master/Detail)** model consisting of two physical tables: `ACC_RULE_HDR` and `ACC_RULE_LINE`. This structure replaces the legacy single-table `ACC_ACCOUNTING_RULES` design.

**Key Design Characteristics:**

**ACC_RULE_HDR (Rule Header):**

* **Unique Rule Per Context:** Each combination of `(COMPANY_ID_FK, SOURCE_MODULE, SOURCE_DOC_TYPE)` resolves to exactly one rule header. The unique index `ACC_RULE_HDR_UK` enforces this invariant at the database level.
* **Active Flag:** `IS_ACTIVE` (`NUMBER(1,0)`, values `0` or `1`) ensures only one rule set is active per module/document combination. CHECK constraint `CK_ACC_RULE_HDR_ACTIVE` enforces valid values.
* **Template Architecture:** Each rule header acts as a reusable posting template. Changing an accounting policy requires only a rule configuration change — no code modification.

**ACC_RULE_LINE (Rule Lines):**

* **Direct Account Reference:** `ACCOUNT_ID_FK` (`NOT NULL`) defines the **final GL account** to be posted. No runtime account resolution.
* **Explicit Posting Direction:** `ENTRY_SIDE` (`DEBIT` or `CREDIT`, `NOT NULL`) specifies the entry direction. Enforced by CHECK constraint `CK_RULE_LINE_ENTRY`. This value is transcribed directly to `ACC_POSTING_DTL.ENTRY_SIDE` during rule application.
* **Amount Derivation:** `AMOUNT_SOURCE_TYPE` (`NOT NULL`) defines how the posting amount is calculated:
  * `TOTAL` — Full transaction amount
  * `FIXED` — Static value from `AMOUNT_SOURCE_VALUE`
  * `PERCENT` — Percentage of total, where `AMOUNT_SOURCE_VALUE` holds the percentage factor
  Enforced by CHECK constraint `CK_RULE_LINE_AMOUNT_TYPE`.
* **Priority-Based Execution:** `PRIORITY` (`NOT NULL`) determines the order in which rule lines are applied, ensuring deterministic and repeatable posting output.
* **Analytical Label:** `BUSINESS_SIDE` serves as a **descriptive label** for the rule line (e.g., “AR”, “REVENUE”, “VAT_OUT”). It does NOT drive account selection or posting direction.
* **Payment Type Support:** `PAYMENT_TYPE_CODE` enables conditional rule application based on payment method.
* **Entity Type Scoping:** `ENTITY_TYPE` allows rule lines to define the subledger entity classification that will be recorded in `ACC_POSTING_DTL`.

```sql
--------------------------------------------------------
--  DDL for Table ACC_RULE_HDR
--------------------------------------------------------
CREATE TABLE "ACC_RULE_HDR" (
    "RULE_ID"            NUMBER(19,0),
    "SOURCE_MODULE"      VARCHAR2(50),
    "SOURCE_DOC_TYPE"    VARCHAR2(50),
    "COMPANY_ID_FK"      NUMBER(19,0),
    "IS_ACTIVE"          NUMBER(1,0) DEFAULT 1,
    "CREATED_AT"         TIMESTAMP(6),
    "CREATED_BY"         VARCHAR2(100),
    "UPDATED_AT"         TIMESTAMP(6),
    "UPDATED_BY"         VARCHAR2(100)
);

--------------------------------------------------------
--  ACC_RULE_HDR Indexes
--------------------------------------------------------
CREATE UNIQUE INDEX "SYS_C0025570" ON "ACC_RULE_HDR" ("RULE_ID");
CREATE UNIQUE INDEX "ACC_RULE_HDR_UK" ON "ACC_RULE_HDR" ("COMPANY_ID_FK", "SOURCE_MODULE", "SOURCE_DOC_TYPE");

--------------------------------------------------------
--  ACC_RULE_HDR Constraints
--------------------------------------------------------
ALTER TABLE "ACC_RULE_HDR" MODIFY ("SOURCE_MODULE" NOT NULL ENABLE);
ALTER TABLE "ACC_RULE_HDR" MODIFY ("SOURCE_DOC_TYPE" NOT NULL ENABLE);
ALTER TABLE "ACC_RULE_HDR" MODIFY ("COMPANY_ID_FK" NOT NULL ENABLE);
ALTER TABLE "ACC_RULE_HDR" MODIFY ("IS_ACTIVE" NOT NULL ENABLE);
ALTER TABLE "ACC_RULE_HDR" ADD CONSTRAINT "CK_ACC_RULE_HDR_ACTIVE"
    CHECK (IS_ACTIVE IN (0,1)) ENABLE;
ALTER TABLE "ACC_RULE_HDR" ADD PRIMARY KEY ("RULE_ID") USING INDEX ENABLE;
ALTER TABLE "ACC_RULE_HDR" ADD CONSTRAINT "ACC_RULE_HDR_UK"
    UNIQUE ("COMPANY_ID_FK", "SOURCE_MODULE", "SOURCE_DOC_TYPE") USING INDEX ENABLE;

--------------------------------------------------------
--  DDL for Table ACC_RULE_LINE
--------------------------------------------------------
CREATE TABLE "ACC_RULE_LINE" (
    "RULE_LINE_ID"           NUMBER(19,0),
    "RULE_ID_FK"             NUMBER(19,0),
    "BUSINESS_SIDE"          VARCHAR2(50),
    "ACCOUNT_ID_FK"          NUMBER(19,0),
    "ENTRY_SIDE"             VARCHAR2(10),
    "PRIORITY"               NUMBER(10,0),
    "AMOUNT_SOURCE_TYPE"     VARCHAR2(20),
    "AMOUNT_SOURCE_VALUE"    NUMBER(18,6),
    "CREATED_AT"             TIMESTAMP(6),
    "CREATED_BY"             VARCHAR2(100),
    "UPDATED_AT"             TIMESTAMP(6),
    "UPDATED_BY"             VARCHAR2(100),
    "PAYMENT_TYPE_CODE"      NUMBER,
    "ENTITY_TYPE"            VARCHAR2(20)
);

--------------------------------------------------------
--  ACC_RULE_LINE Indexes
--------------------------------------------------------
CREATE UNIQUE INDEX "SYS_C0025579" ON "ACC_RULE_LINE" ("RULE_LINE_ID");

--------------------------------------------------------
--  ACC_RULE_LINE Constraints
--------------------------------------------------------
ALTER TABLE "ACC_RULE_LINE" MODIFY ("RULE_ID_FK" NOT NULL ENABLE);
ALTER TABLE "ACC_RULE_LINE" MODIFY ("ACCOUNT_ID_FK" NOT NULL ENABLE);
ALTER TABLE "ACC_RULE_LINE" MODIFY ("ENTRY_SIDE" NOT NULL ENABLE);
ALTER TABLE "ACC_RULE_LINE" MODIFY ("PRIORITY" NOT NULL ENABLE);
ALTER TABLE "ACC_RULE_LINE" MODIFY ("AMOUNT_SOURCE_TYPE" NOT NULL ENABLE);
ALTER TABLE "ACC_RULE_LINE" ADD CONSTRAINT "CK_RULE_LINE_ENTRY"
    CHECK (ENTRY_SIDE IN ('DEBIT','CREDIT')) ENABLE;
ALTER TABLE "ACC_RULE_LINE" ADD PRIMARY KEY ("RULE_LINE_ID") USING INDEX ENABLE;
ALTER TABLE "ACC_RULE_LINE" ADD CONSTRAINT "CK_RULE_LINE_AMOUNT_TYPE"
    CHECK (AMOUNT_SOURCE_TYPE IN ('TOTAL','FIXED','PERCENT')) ENABLE;

--------------------------------------------------------
--  Ref Constraints
--------------------------------------------------------
ALTER TABLE "ACC_RULE_LINE" ADD CONSTRAINT "FK_RULE_LINE_HDR"
    FOREIGN KEY ("RULE_ID_FK") REFERENCES "ACC_RULE_HDR" ("RULE_ID") ENABLE;
```

> **Example Rule Configuration:**
>
> **ACC_RULE_HDR:** `RULE_ID=1, COMPANY_ID_FK=1, SOURCE_MODULE='SALES', SOURCE_DOC_TYPE='INVOICE', IS_ACTIVE=1`
>
> **ACC_RULE_LINE entries:**
>
> | RULE_LINE_ID | BUSINESS_SIDE | ACCOUNT_ID_FK | ENTRY_SIDE | PRIORITY | AMOUNT_SOURCE_TYPE | AMOUNT_SOURCE_VALUE |
> |---|---|---|---|---|---|---|
> | 1 | AR | 120101 | DEBIT | 1 | TOTAL | NULL |
> | 2 | REVENUE | 410101 | CREDIT | 2 | PERCENT | 0.952381 |
> | 3 | VAT_OUT | 220301 | CREDIT | 3 | PERCENT | 0.047619 |

---

### 8.6 GL_JOURNAL_HDR – Journal Header

```sql
-- Sequence
CREATE SEQUENCE SEQ_GL_JOURNAL_HDR START WITH 1 INCREMENT BY 1;

-- Table
CREATE TABLE GL_JOURNAL_HDR (
    JOURNAL_ID           NUMBER          NOT NULL,
    JOURNAL_NO           VARCHAR2(50)    NOT NULL,
    COMPANY_ID_FK        NUMBER          NOT NULL,
    DOC_DATE             DATE            NOT NULL,
    FISCAL_PERIOD_ID_FK  NUMBER          NOT NULL,
    JOURNAL_TYPE         VARCHAR2(20)    DEFAULT 'AUTO' CHECK (JOURNAL_TYPE IN ('AUTO','MANUAL','REVERSAL')),
    DESCRIPTION          VARCHAR2(500),
    STATUS               VARCHAR2(20)    DEFAULT 'DRAFT' CHECK (STATUS IN ('DRAFT','PENDING_APPROVAL','POSTED','REVERSED')),
    SOURCE_POSTING_ID_FK NUMBER,
    REVERSAL_JOURNAL_ID_FK NUMBER,
    CURRENCY_CODE        VARCHAR2(3)     DEFAULT 'SAR',
    TOTAL_DEBIT          NUMBER(15,2)    DEFAULT 0,
    TOTAL_CREDIT         NUMBER(15,2)    DEFAULT 0,
    POSTED_BY            VARCHAR2(100),
    POSTED_AT            TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY           VARCHAR2(100),
    CREATED_AT           TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    UPDATED_BY           VARCHAR2(100),
    UPDATED_AT           TIMESTAMP,
    
    CONSTRAINT GL_JOURNAL_HDR_PK PRIMARY KEY (JOURNAL_ID),
    CONSTRAINT GL_JOURNAL_HDR_UK UNIQUE (COMPANY_ID_FK, JOURNAL_NO),
    CONSTRAINT GL_JOURNAL_HDR_PERIOD_FK 
        FOREIGN KEY (FISCAL_PERIOD_ID_FK) REFERENCES GL_FISCAL_PERIOD (ID_PK),
    CONSTRAINT GL_JOURNAL_HDR_POSTING_FK 
        FOREIGN KEY (SOURCE_POSTING_ID_FK) REFERENCES ACC_POSTING_MST (POSTING_ID),
    CONSTRAINT GL_JOURNAL_HDR_REVERSAL_FK 
        FOREIGN KEY (REVERSAL_JOURNAL_ID_FK) REFERENCES GL_JOURNAL_HDR (JOURNAL_ID),
    CONSTRAINT GL_JOURNAL_HDR_BALANCED_CK 
        CHECK (TOTAL_DEBIT = TOTAL_CREDIT)
);

-- Indexes
CREATE INDEX IDX_GL_JOURNAL_HDR_DATE ON GL_JOURNAL_HDR (DOC_DATE);
CREATE INDEX IDX_GL_JOURNAL_HDR_PERIOD_FK ON GL_JOURNAL_HDR (FISCAL_PERIOD_ID_FK);
CREATE INDEX IDX_GL_JOURNAL_HDR_POSTING_FK ON GL_JOURNAL_HDR (SOURCE_POSTING_ID_FK);
CREATE INDEX IDX_GL_JOURNAL_HDR_COMPANY_FK ON GL_JOURNAL_HDR (COMPANY_ID_FK);

-- Comments
COMMENT ON TABLE GL_JOURNAL_HDR IS 'Journal header - contains accounting journal entries';
COMMENT ON COLUMN GL_JOURNAL_HDR.JOURNAL_TYPE IS 'AUTO: from posting engine, MANUAL: user entry, REVERSAL: reversal entry';
COMMENT ON COLUMN GL_JOURNAL_HDR.STATUS IS 'DRAFT: manual entry saved, PENDING_APPROVAL: awaiting approval, POSTED: finalized, REVERSED: reversed';
COMMENT ON COLUMN GL_JOURNAL_HDR.SOURCE_POSTING_ID_FK IS 'NULL for manual journals, populated for AUTO journals';
```

---

### 8.7 GL_JOURNAL_LINE – Journal Lines

```sql
-- Sequence
CREATE SEQUENCE SEQ_GL_JOURNAL_LINE START WITH 1 INCREMENT BY 1;

-- Table
-- ✅ Mirrors ACC_POSTING_DTL structure: ENTRY_SIDE + AMOUNT + ENTITY_TYPE/ENTITY_ID
-- This makes the Posting Engine a pure 1:1 transcription from ACC_POSTING_DTL to GL_JOURNAL_LINE.
CREATE TABLE GL_JOURNAL_LINE (
    JOURNAL_LINE_ID      NUMBER          NOT NULL,
    JOURNAL_ID_FK        NUMBER          NOT NULL,
    LINE_NO              NUMBER          NOT NULL,
    ACCOUNT_ID_FK        NUMBER          NOT NULL,
    ENTRY_SIDE           VARCHAR2(10)    NOT NULL,
    AMOUNT               NUMBER(15,2)    NOT NULL,
    ENTITY_TYPE          VARCHAR2(30),
    ENTITY_ID            NUMBER(19,0),
    DESCRIPTION          VARCHAR2(500),
    CREATED_BY           VARCHAR2(100),
    CREATED_AT           TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT GL_JOURNAL_LINE_PK PRIMARY KEY (JOURNAL_LINE_ID),
    CONSTRAINT GL_JOURNAL_LINE_HDR_FK 
        FOREIGN KEY (JOURNAL_ID_FK) REFERENCES GL_JOURNAL_HDR (JOURNAL_ID) ON DELETE CASCADE,
    CONSTRAINT GL_JOURNAL_LINE_ACCOUNT_FK 
        FOREIGN KEY (ACCOUNT_ID_FK) REFERENCES ACCOUNTS_CHART (ACCOUNT_CHART_PK),
    CONSTRAINT CK_JLINE_ENTRY_SIDE 
        CHECK (ENTRY_SIDE IN ('DEBIT','CREDIT')),
    CONSTRAINT CK_JLINE_AMOUNT 
        CHECK (AMOUNT >= 0)
);

-- Indexes
CREATE INDEX IDX_GL_JOURNAL_LINE_HDR_FK ON GL_JOURNAL_LINE (JOURNAL_ID_FK);
CREATE INDEX IDX_GL_JOURNAL_LINE_ACCOUNT_FK ON GL_JOURNAL_LINE (ACCOUNT_ID_FK);
CREATE INDEX IDX_GL_JOURNAL_LINE_ENTITY ON GL_JOURNAL_LINE (ENTITY_TYPE, ENTITY_ID);

-- Comments
COMMENT ON TABLE GL_JOURNAL_LINE IS 'Journal lines - mirrors ACC_POSTING_DTL with ENTRY_SIDE + AMOUNT pattern';
COMMENT ON COLUMN GL_JOURNAL_LINE.ENTRY_SIDE IS 'DEBIT or CREDIT - explicit direction, no sign logic';
COMMENT ON COLUMN GL_JOURNAL_LINE.AMOUNT IS 'Always positive. Direction determined by ENTRY_SIDE';
COMMENT ON COLUMN GL_JOURNAL_LINE.ENTITY_TYPE IS 'Polymorphic entity reference: CUSTOMER, SUPPLIER, COST_CENTER, CONTRACT, etc.';
COMMENT ON COLUMN GL_JOURNAL_LINE.ENTITY_ID IS 'ID of the referenced entity (polymorphic FK)';
```

> **ملاحظة هندسية:** GL_JOURNAL_LINE تعكس ACC_POSTING_DTL عمودياً. محرك الترحيل ينسخ `ACCOUNT_ID_FK`, `ENTRY_SIDE`, `AMOUNT`, `ENTITY_TYPE`, `ENTITY_ID` حرفياً من DTL إلى Journal Line دون أي تحويل أو حساب إشارات.

---

### 8.8 GL_LEDGER_ENTRY – General Ledger

```sql
-- Sequence
CREATE SEQUENCE SEQ_GL_LEDGER_ENTRY START WITH 1 INCREMENT BY 1;

-- Table
-- ✅ Mirrors GL_JOURNAL_LINE / ACC_POSTING_DTL pattern: ENTRY_SIDE + AMOUNT
-- RUNNING_BALANCE is NOT stored — calculated dynamically via window functions.
CREATE TABLE GL_LEDGER_ENTRY (
    ENTRY_ID             NUMBER          NOT NULL,
    ACCOUNT_ID_FK        NUMBER          NOT NULL,
    JOURNAL_ID_FK        NUMBER          NOT NULL,
    JOURNAL_LINE_ID_FK   NUMBER          NOT NULL,
    COMPANY_ID_FK        NUMBER          NOT NULL,
    DOC_DATE             DATE            NOT NULL,
    FISCAL_PERIOD_ID_FK  NUMBER          NOT NULL,
    ENTRY_SIDE           VARCHAR2(10)    NOT NULL,
    AMOUNT               NUMBER(15,2)    NOT NULL,
    ENTITY_TYPE          VARCHAR2(30),
    ENTITY_ID            NUMBER(19,0),
    DESCRIPTION          VARCHAR2(500),
    CREATED_BY           VARCHAR2(100),
    CREATED_AT           TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT GL_LEDGER_ENTRY_PK PRIMARY KEY (ENTRY_ID),
    CONSTRAINT GL_LEDGER_ENTRY_ACCOUNT_FK 
        FOREIGN KEY (ACCOUNT_ID_FK) REFERENCES ACCOUNTS_CHART (ACCOUNT_CHART_PK),
    CONSTRAINT GL_LEDGER_ENTRY_JOURNAL_FK 
        FOREIGN KEY (JOURNAL_ID_FK) REFERENCES GL_JOURNAL_HDR (JOURNAL_ID),
    CONSTRAINT GL_LEDGER_ENTRY_LINE_FK 
        FOREIGN KEY (JOURNAL_LINE_ID_FK) REFERENCES GL_JOURNAL_LINE (JOURNAL_LINE_ID),
    CONSTRAINT GL_LEDGER_ENTRY_PERIOD_FK 
        FOREIGN KEY (FISCAL_PERIOD_ID_FK) REFERENCES GL_FISCAL_PERIOD (ID_PK),
    CONSTRAINT CK_LEDGER_ENTRY_SIDE 
        CHECK (ENTRY_SIDE IN ('DEBIT','CREDIT')),
    CONSTRAINT CK_LEDGER_AMOUNT 
        CHECK (AMOUNT >= 0)
);

-- Indexes (CRITICAL for performance)
CREATE INDEX IDX_GL_LEDGER_ENTRY_ACCOUNT_FK ON GL_LEDGER_ENTRY (ACCOUNT_ID_FK);
CREATE INDEX IDX_GL_LEDGER_ENTRY_DATE ON GL_LEDGER_ENTRY (DOC_DATE);
CREATE INDEX IDX_GL_LEDGER_ENTRY_PERIOD_FK ON GL_LEDGER_ENTRY (FISCAL_PERIOD_ID_FK);
CREATE INDEX IDX_GL_LEDGER_ENTRY_JOURNAL_FK ON GL_LEDGER_ENTRY (JOURNAL_ID_FK);
CREATE INDEX IDX_GL_LEDGER_ENTRY_COMPANY_FK ON GL_LEDGER_ENTRY (COMPANY_ID_FK);

-- Composite index for common queries
CREATE INDEX IDX_GL_LEDGER_ACCOUNT_DATE ON GL_LEDGER_ENTRY (ACCOUNT_ID_FK, DOC_DATE);
```

---

### 8.9 GL_ACCOUNT_BALANCE – Account Balances Summary

```sql
-- Table
CREATE TABLE GL_ACCOUNT_BALANCE (
    COMPANY_ID_FK        NUMBER          NOT NULL,
    FISCAL_YEAR          NUMBER(4)       NOT NULL,
    PERIOD_NO            NUMBER(2)       NOT NULL,
    ACCOUNT_ID_FK        NUMBER          NOT NULL,
    OPENING_BALANCE      NUMBER(15,2)    DEFAULT 0,
    PERIOD_DEBIT         NUMBER(15,2)    DEFAULT 0,
    PERIOD_CREDIT        NUMBER(15,2)    DEFAULT 0,
    CLOSING_BALANCE      NUMBER(15,2)    DEFAULT 0,
    CREATED_BY           VARCHAR2(100),
    CREATED_AT           TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    UPDATED_BY           VARCHAR2(100),
    UPDATED_AT           TIMESTAMP,
    
    CONSTRAINT GL_ACCOUNT_BALANCE_PK 
        PRIMARY KEY (COMPANY_ID_FK, FISCAL_YEAR, PERIOD_NO, ACCOUNT_ID_FK),
    CONSTRAINT GL_ACCOUNT_BALANCE_ACCOUNT_FK 
        FOREIGN KEY (ACCOUNT_ID_FK) REFERENCES ACCOUNTS_CHART (ACCOUNT_CHART_PK)
);

-- Indexes
CREATE INDEX IDX_GL_ACCOUNT_BALANCE_YEAR ON GL_ACCOUNT_BALANCE (FISCAL_YEAR, PERIOD_NO);
CREATE INDEX IDX_GL_ACCOUNT_BALANCE_ACCOUNT_FK ON GL_ACCOUNT_BALANCE (ACCOUNT_ID_FK);
```

---

## 9. Transaction Management Strategy

### 9.1 Transaction Boundaries

```java
@Service
@Transactional(rollbackFor = Exception.class)
public class PostingEngineService {
    
    /**
     * Main posting method - ALL or NOTHING transaction
     */
    public PostingResult postSingle(Long postingId) {
        try {
            // 1. Lock posting record (prevent concurrent processing)
            AccPostingMst posting = postingRepository.findByIdForUpdate(postingId)
                .orElseThrow(() -> new BusinessException("Posting not found"));
            
            // 2. Validate status
            if (!"NEW".equals(posting.getStatus()) && !"READY_FOR_GL".equals(posting.getStatus())) {
                throw new BusinessException("Invalid status for posting: " + posting.getStatus());
            }
            
            // 3. Read details (no IN_PROGRESS status — concurrency controlled via pessimistic lock)
            List<AccPostingDtl> details = postingDtlRepository.findByPostingId(postingId);
            
            // 5. Build journal
            GlJournalHdr journal = buildJournal(posting, details);
            
            // 6. Validate balanced
            if (!isBalanced(journal)) {
                throw new BusinessException("Journal is not balanced");
            }
            
            // 7. Save journal
            journalRepository.save(journal);
            
            // 8. Create ledger entries
            createLedgerEntries(journal);
            
            // 9. Update balances
            updateAccountBalances(journal);
            
            // 10. Update posting status
            posting.setStatus("POSTED");
            posting.setFinJournalId(journal.getJournalId());
            posting.setErrorMessage(null);
            postingRepository.save(posting);
            
            return PostingResult.success(posting, journal);
            
        } catch (Exception e) {
            // Transaction will rollback automatically
            // Update status to ERROR
            updatePostingError(postingId, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Separate transaction for error logging
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updatePostingError(Long postingId, String errorMessage) {
        AccPostingMst posting = postingRepository.findById(postingId).orElse(null);
        if (posting != null) {
            posting.setStatus("ERROR");
            posting.setErrorMessage(errorMessage.substring(0, Math.min(1000, errorMessage.length())));
            postingRepository.save(posting);
        }
    }
}
```

### 9.2 Ledger Entry Creation Logic

**AR:** آلية إنشاء سطور دفتر الأستاذ من قيود اليومية.

**EN:** Mechanism for creating ledger entries from journal entries.

```java
@Service
@RequiredArgsConstructor
public class LedgerService {
    
    private final GlLedgerEntryRepository ledgerRepository;
    private final GlFiscalPeriodRepository periodRepository;
    
    /**
     * Create ledger entries from a posted journal
     * Called automatically after journal posting
     */
    @Transactional
    public void createLedgerEntriesFromJournal(GlJournalHdr journal) {
        // 1. Determine fiscal period
        GlFiscalPeriod period = periodRepository
            .findByCompanyAndDate(journal.getCompanyId(), journal.getDocDate())
            .orElseThrow(() -> new BusinessException(
                "No fiscal period found for date: " + journal.getDocDate()));
        
        // 2. Create one ledger entry per journal line (1:1 transcription)
        for (GlJournalLine line : journal.getLines()) {
            GlLedgerEntry entry = new GlLedgerEntry();
            
            // Primary fields
            entry.setAccountId(line.getAccountId());
            entry.setJournalId(journal.getJournalId());
            entry.setJournalLineId(line.getJournalLineId());
            entry.setCompanyId(journal.getCompanyId());
            
            // ✅ ENTRY_SIDE + AMOUNT — same pattern as GL_JOURNAL_LINE and ACC_POSTING_DTL
            entry.setEntrySide(line.getEntrySide());
            entry.setAmount(line.getAmount());
            
            // ✅ Polymorphic entity reference — copied verbatim
            entry.setEntityType(line.getEntityType());
            entry.setEntityId(line.getEntityId());
            
            // Period tracking
            entry.setFiscalPeriodId(period.getId());
            entry.setDocDate(journal.getDocDate());
            
            // Description and traceability
            entry.setDescription(line.getDescription() != null 
                ? line.getDescription() 
                : journal.getDescription());
            
            // Audit fields
            entry.setCreatedBy(SecurityContextHelper.getUsernameOrSystem());
            entry.setCreatedAt(LocalDateTime.now());
            
            // ⚠️ IMPORTANT: No RUNNING_BALANCE stored!
            // Running balance calculated dynamically via window functions
            
            ledgerRepository.save(entry);
        }
    }
    
    /**
     * Get ledger entries with calculated running balance
     * Uses window function for efficient calculation
     */
    public Page<LedgerEntryWithBalance> getLedgerWithRunningBalance(
            Long accountId, 
            LocalDate dateFrom, 
            LocalDate dateTo,
            Pageable pageable) {
        
        return ledgerRepository.findWithRunningBalance(
            accountId, dateFrom, dateTo, pageable);
    }
}
```

**Repository with Running Balance Calculation:**

```java
public interface GlLedgerEntryRepository extends JpaRepository<GlLedgerEntry, Long> {
    
    /**
     * ⚠️ Running Balance calculated via Window Function
     * NOT stored in database to avoid update cascades
     * Uses CASE WHEN to derive debit/credit from ENTRY_SIDE + AMOUNT
     */
    @Query(value = """
        SELECT 
            e.ENTRY_ID,
            e.ACCOUNT_ID_FK,
            e.DOC_DATE,
            e.JOURNAL_ID_FK,
            e.ENTRY_SIDE,
            e.AMOUNT,
            e.ENTITY_TYPE,
            e.ENTITY_ID,
            e.DESCRIPTION,
            CASE WHEN e.ENTRY_SIDE = 'DEBIT' THEN e.AMOUNT ELSE 0 END as DEBIT,
            CASE WHEN e.ENTRY_SIDE = 'CREDIT' THEN e.AMOUNT ELSE 0 END as CREDIT,
            SUM(CASE WHEN e.ENTRY_SIDE = 'DEBIT' THEN e.AMOUNT 
                     ELSE -e.AMOUNT END) OVER (
                PARTITION BY e.ACCOUNT_ID_FK 
                ORDER BY e.DOC_DATE, e.ENTRY_ID
                ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
            ) as RUNNING_BALANCE
        FROM GL_LEDGER_ENTRY e
        WHERE e.ACCOUNT_ID_FK = :accountId
        AND e.DOC_DATE BETWEEN :dateFrom AND :dateTo
        ORDER BY e.DOC_DATE, e.ENTRY_ID
        """, nativeQuery = true)
    Page<LedgerEntryWithBalance> findWithRunningBalance(
        @Param("accountId") Long accountId,
        @Param("dateFrom") LocalDate dateFrom,
        @Param("dateTo") LocalDate dateTo,
        Pageable pageable
    );
}
```

**GlAccountBalanceRepository with Atomic Update:**

```java
public interface GlAccountBalanceRepository 
        extends JpaRepository<GlAccountBalance, GlAccountBalanceId> {
    
    /**
     * Atomic balance update — prevents race conditions in concurrent posting.
     * Uses SET col = col + :delta pattern (no Read-Modify-Write).
     * Closing = Opening + Debit - Credit (debit-positive convention).
     */
    @Modifying
    @Query(value = """
        UPDATE GL_ACCOUNT_BALANCE  
        SET PERIOD_DEBIT    = PERIOD_DEBIT    + :debitAmt,
            PERIOD_CREDIT   = PERIOD_CREDIT   + :creditAmt,
            CLOSING_BALANCE = OPENING_BALANCE 
                            + (PERIOD_DEBIT  + :debitAmt) 
                            - (PERIOD_CREDIT + :creditAmt),
            UPDATED_BY      = :updatedBy,
            UPDATED_AT      = :updatedAt
        WHERE COMPANY_ID_FK  = :companyId
          AND FISCAL_YEAR    = :fiscalYear
          AND PERIOD_NO      = :periodNo
          AND ACCOUNT_ID_FK  = :accountId
        """, nativeQuery = true)
    void atomicAddMovements(
        @Param("companyId") Long companyId,
        @Param("fiscalYear") Integer fiscalYear,
        @Param("periodNo") Integer periodNo,
        @Param("accountId") Long accountId,
        @Param("debitAmt") BigDecimal debitAmount,
        @Param("creditAmt") BigDecimal creditAmount,
        @Param("updatedBy") String updatedBy,
        @Param("updatedAt") LocalDateTime updatedAt
    );
    
    void deleteByCompanyYearPeriod(Long companyId, Integer fiscalYear, Integer periodNo);
}
```

> **ملاحظة أمان التزامن:** يمنع هذا النمط `SET col = col + :delta` حالة السباق (Race Condition) التي تحدث عند استخدام نمط Read-Modify-Write التقليدي. حيث أن Oracle يحقق ذرية (atomicity) على مستوى الصف تلقائياً.

**Why NO Running Balance in DB?**

| Approach | Pros | Cons | Recommendation |
|----------|------|------|----------------|
| **Store in DB** | Fast retrieval | Complex updates on reversal/edit, cascading recalculations | ❌ Not Recommended |
| **Calculate on Query** | Always accurate, no update complexity | Slightly slower query | ✅ **Recommended** |
| **Hybrid (Cache)** | Fast + accurate | Complex cache invalidation | ⚠️ Only if performance critical |

**Decision:** Use window functions (Oracle `OVER()`) for runtime calculation.

---

### 9.3 Account Balance Update Service

**AR:** آلية تحديث أرصدة الحسابات بعد ترحيل القيود.

**EN:** Account balance update mechanism after journal posting.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountBalanceService {
    
    private final GlAccountBalanceRepository balanceRepository;
    private final GlFiscalPeriodRepository periodRepository;
    
    /**
     * Update account balances from a posted journal
     * Called after journal and ledger creation
     */
    @Transactional
    public void updateBalancesFromJournal(GlJournalHdr journal) {
        log.info("Updating balances for journal: {}", journal.getJournalNo());
        
        // 1. Determine fiscal period
        GlFiscalPeriod period = periodRepository
            .findByCompanyAndDate(journal.getCompanyId(), journal.getDocDate())
            .orElseThrow(() -> new BusinessException(
                "No fiscal period found for date: " + journal.getDocDate()));
        
        // 2. Group amounts by account — derive debit/credit from ENTRY_SIDE + AMOUNT
        Map<Long, BigDecimal> accountDebits = new HashMap<>();
        Map<Long, BigDecimal> accountCredits = new HashMap<>();
        
        for (GlJournalLine line : journal.getLines()) {
            Long accountId = line.getAccountId();
            if ("DEBIT".equals(line.getEntrySide())) {
                accountDebits.merge(accountId, line.getAmount(), BigDecimal::add);
            } else {
                accountCredits.merge(accountId, line.getAmount(), BigDecimal::add);
            }
        }
        
        // 3. Update or create balance records
        Set<Long> affectedAccounts = new HashSet<>();
        affectedAccounts.addAll(accountDebits.keySet());
        affectedAccounts.addAll(accountCredits.keySet());
        
        for (Long accountId : affectedAccounts) {
            updateSingleAccountBalance(
                journal.getCompanyId(),
                period.getFiscalYear(),
                period.getPeriodNo(),
                accountId,
                accountDebits.getOrDefault(accountId, BigDecimal.ZERO),
                accountCredits.getOrDefault(accountId, BigDecimal.ZERO)
            );
        }
    }
    
    /**
     * Update single account balance
     * Uses atomic SQL UPDATE to prevent race conditions in concurrent posting.
     * Balance convention: Debit-Positive (Closing = Opening + Debit - Credit).
     * Report layer handles sign-inversion for CREDIT-normal accounts.
     */
    private void updateSingleAccountBalance(
            Long companyId,
            Integer fiscalYear,
            Integer periodNo,
            Long accountId,
            BigDecimal debitAmount,
            BigDecimal creditAmount) {
        
        GlAccountBalanceId balanceId = new GlAccountBalanceId(
            companyId, fiscalYear, periodNo, accountId
        );
        
        // Ensure record exists (INSERT IF NOT EXISTS)
        if (!balanceRepository.existsById(balanceId)) {
            GlAccountBalance newBalance = new GlAccountBalance();
            newBalance.setId(balanceId);
            newBalance.setOpeningBalance(BigDecimal.ZERO);
            newBalance.setPeriodDebit(BigDecimal.ZERO);
            newBalance.setPeriodCredit(BigDecimal.ZERO);
            newBalance.setClosingBalance(BigDecimal.ZERO);
            newBalance.setCreatedBy(SecurityContextHelper.getUsernameOrSystem());
            newBalance.setCreatedAt(LocalDateTime.now());
            balanceRepository.save(newBalance);
        }
        
        // Atomic update — no Read-Modify-Write race condition
        // Closing = Opening + (accumulated Debit) - (accumulated Credit)
        balanceRepository.atomicAddMovements(
            companyId, fiscalYear, periodNo, accountId,
            debitAmount, creditAmount,
            SecurityContextHelper.getUsernameOrSystem(),
            LocalDateTime.now()
        );
        
        log.debug("Atomically updated balance for account {}: debit+={}, credit+={}",
            accountId, debitAmount, creditAmount);
    }
    
    /**
     * Recalculate all balances for a period (admin only)
     * Used for data correction or migration
     */
    @Transactional
    public int recalculateBalances(
            Long companyId, 
            Integer fiscalYear, 
            Integer periodNo) {
        
        log.warn("Recalculating all balances for company={}, year={}, period={}",
            companyId, fiscalYear, periodNo);
        
        // Clear existing balances
        balanceRepository.deleteByCompanyYearPeriod(companyId, fiscalYear, periodNo);
        
        // Rebuild from ledger entries
        List<Object[]> accountSums = ledgerRepository
            .sumByAccountForPeriod(companyId, fiscalYear, periodNo);
        
        int count = 0;
        for (Object[] row : accountSums) {
            Long accountId = (Long) row[0];
            BigDecimal totalDebit = (BigDecimal) row[1];
            BigDecimal totalCredit = (BigDecimal) row[2];
            
            updateSingleAccountBalance(
                companyId, fiscalYear, periodNo, accountId,
                totalDebit, totalCredit
            );
            count++;
        }
        
        log.info("Recalculated {} account balances", count);
        return count;
    }
}
```

---

### 9.4 Opening Balance Management

**AR:** إدارة الأرصدة الافتتاحية وترحيل الأرصدة بين الفترات والسنوات.

**EN:** Opening balance management and rollover between periods/years.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class FiscalYearService {
    
    private final GlAccountBalanceRepository balanceRepository;
    private final AccountsChartRepository accountRepository;
    private final GlFiscalPeriodRepository periodRepository;
    
    /**
     * Roll forward closing balances to next year opening balances
     * Handles both Balance Sheet and P&L accounts
     */
    @Transactional
    public void rollForwardToNewYear(
            Long companyId, 
            Integer fromYear, 
            Integer toYear) {
        
        log.info("Rolling forward balances from year {} to {}", fromYear, toYear);
        
        // 1. Get all closing balances from period 12 of previous year
        List<GlAccountBalance> closingBalances = balanceRepository
            .findByCompanyAndYearAndPeriod(companyId, fromYear, 12);
        
        if (closingBalances.isEmpty()) {
            throw new BusinessException(
                "No closing balances found for year " + fromYear + " period 12");
        }
        
        // 2. Get all accounts to determine type
        Map<Long, AccountsChart> accountMap = accountRepository
            .findByCompanyId(companyId)
            .stream()
            .collect(Collectors.toMap(AccountsChart::getAccountChartPk, a -> a));
        
        // 3. Calculate P&L total for retained earnings
        BigDecimal profitLoss = BigDecimal.ZERO;
        
        for (GlAccountBalance balance : closingBalances) {
            AccountsChart account = accountMap.get(balance.getId().getAccountId());
            if (account != null && isProfitLossAccount(account.getAccountType())) {
                profitLoss = profitLoss.add(balance.getClosingBalance());
            }
        }
        
        log.info("Net Profit/Loss for year {}: {}", fromYear, profitLoss);
        
        // 4. Create period 1 opening balances for new year
        int balanceSheetCount = 0;
        int profitLossCount = 0;
        
        for (GlAccountBalance prevBalance : closingBalances) {
            AccountsChart account = accountMap.get(prevBalance.getId().getAccountId());
            if (account == null) continue;
            
            GlAccountBalanceId newId = new GlAccountBalanceId(
                companyId, toYear, 1, account.getId()
            );
            
            GlAccountBalance newBalance = new GlAccountBalance();
            newBalance.setId(newId);
            newBalance.setPeriodDebit(BigDecimal.ZERO);
            newBalance.setPeriodCredit(BigDecimal.ZERO);
            
            if (isProfitLossAccount(account.getAccountType())) {
                // Zero out P&L accounts
                newBalance.setOpeningBalance(BigDecimal.ZERO);
                newBalance.setClosingBalance(BigDecimal.ZERO);
                profitLossCount++;
            } else {
                // Carry forward Balance Sheet accounts
                newBalance.setOpeningBalance(prevBalance.getClosingBalance());
                newBalance.setClosingBalance(prevBalance.getClosingBalance());
                balanceSheetCount++;
            }
            
            newBalance.setCreatedBy(SecurityContextHelper.getUsernameOrSystem());
            newBalance.setCreatedAt(LocalDateTime.now());
            
            balanceRepository.save(newBalance);
        }
        
        // 5. Transfer P&L to Retained Earnings
        if (profitLoss.compareTo(BigDecimal.ZERO) != 0) {
            transferToRetainedEarnings(companyId, toYear, profitLoss);
        }
        
        log.info("Rolled forward {} balance sheet accounts, zeroed {} P&L accounts",
            balanceSheetCount, profitLossCount);
    }
    
    /**
     * Check if account type is P&L (REVENUE or EXPENSE)
     */
    private boolean isProfitLossAccount(String accountType) {
        return "REVENUE".equals(accountType) || "EXPENSE".equals(accountType);
    }
    
    /**
     * Transfer net P&L to retained earnings account
     */
    private void transferToRetainedEarnings(
            Long companyId, 
            Integer fiscalYear, 
            BigDecimal profitLoss) {
        
        // Find retained earnings account (typically 3102xx)
        AccountsChart retainedEarnings = accountRepository
            .findByCompanyIdAndAccountCode(companyId, "310200")
            .orElseThrow(() -> new BusinessException(
                "Retained Earnings account not found"));
        
        GlAccountBalanceId balanceId = new GlAccountBalanceId(
            companyId, fiscalYear, 1, retainedEarnings.getId()
        );
        
        GlAccountBalance balance = balanceRepository.findById(balanceId)
            .orElseGet(() -> {
                GlAccountBalance newBal = new GlAccountBalance();
                newBal.setId(balanceId);
                newBal.setOpeningBalance(BigDecimal.ZERO);
                newBal.setPeriodDebit(BigDecimal.ZERO);
                newBal.setPeriodCredit(BigDecimal.ZERO);
                newBal.setClosingBalance(BigDecimal.ZERO);
                return newBal;
            });
        
        // Add profit/loss to opening balance
        balance.setOpeningBalance(
            balance.getOpeningBalance().add(profitLoss)
        );
        balance.setClosingBalance(
            balance.getClosingBalance().add(profitLoss)
        );
        
        balanceRepository.save(balance);
        
        log.info("Transferred {} to Retained Earnings account {}",
            profitLoss, retainedEarnings.getAccountCode());
    }
    
    /**
     * Roll forward opening balance from previous period
     * (within same fiscal year)
     */
    @Transactional
    public void rollForwardToNextPeriod(
            Long companyId,
            Integer fiscalYear,
            Integer fromPeriod,
            Integer toPeriod) {
        
        List<GlAccountBalance> prevBalances = balanceRepository
            .findByCompanyAndYearAndPeriod(companyId, fiscalYear, fromPeriod);
        
        for (GlAccountBalance prevBalance : prevBalances) {
            GlAccountBalanceId newId = new GlAccountBalanceId(
                companyId, fiscalYear, toPeriod, 
                prevBalance.getId().getAccountId()
            );
            
            // Check if already exists
            if (balanceRepository.existsById(newId)) {
                continue;
            }
            
            GlAccountBalance newBalance = new GlAccountBalance();
            newBalance.setId(newId);
            newBalance.setOpeningBalance(prevBalance.getClosingBalance());
            newBalance.setPeriodDebit(BigDecimal.ZERO);
            newBalance.setPeriodCredit(BigDecimal.ZERO);
            newBalance.setClosingBalance(prevBalance.getClosingBalance());
            newBalance.setCreatedBy(SecurityContextHelper.getUsernameOrSystem());
            newBalance.setCreatedAt(LocalDateTime.now());
            
            balanceRepository.save(newBalance);
        }
    }
    
    /**
     * Initialize opening balances for first time setup
     * Used during implementation/migration
     */
    @Transactional
    public void initializeOpeningBalances(
            Long companyId,
            Integer fiscalYear,
            Map<String, BigDecimal> accountCodeBalances) {
        
        log.info("Initializing opening balances for {} accounts", 
            accountCodeBalances.size());
        
        for (Map.Entry<String, BigDecimal> entry : accountCodeBalances.entrySet()) {
            String accountCode = entry.getKey();
            BigDecimal openingBalance = entry.getValue();
            
            AccountsChart account = accountRepository
                .findByCompanyIdAndAccountCode(companyId, accountCode)
                .orElseThrow(() -> new BusinessException(
                    "Account not found: " + accountCode));
            
            GlAccountBalanceId balanceId = new GlAccountBalanceId(
                companyId, fiscalYear, 1, account.getId()
            );
            
            GlAccountBalance balance = new GlAccountBalance();
            balance.setId(balanceId);
            balance.setOpeningBalance(openingBalance);
            balance.setPeriodDebit(BigDecimal.ZERO);
            balance.setPeriodCredit(BigDecimal.ZERO);
            balance.setClosingBalance(openingBalance);
            balance.setCreatedBy(SecurityContextHelper.getUsernameOrSystem());
            balance.setCreatedAt(LocalDateTime.now());
            
            balanceRepository.save(balance);
        }
        
        log.info("Opening balances initialized successfully");
    }
}
```

---

### 9.5 Fiscal Period Validation

**AR:** التحقق من حالة الفترة المالية قبل الترحيل.

**EN:** Fiscal period validation before posting.

```java
@Service
@RequiredArgsConstructor
public class FiscalPeriodValidationService {
    
    private final GlFiscalPeriodRepository periodRepository;
    
    /**
     * Validate period is open before posting
     * Throws exception if period closed or not found
     */
    public GlFiscalPeriod validatePeriodIsOpen(
            Long companyId, 
            LocalDate docDate) {
        
        GlFiscalPeriod period = periodRepository
            .findByCompanyAndDate(companyId, docDate)
            .orElseThrow(() -> new BusinessException(
                "No fiscal period defined for date: " + docDate + 
                ". Please create fiscal periods first."));
        
        if ("CLOSED".equals(period.getStatus())) {
            throw new BusinessException(
                "Cannot post to CLOSED period: " + period.getPeriodName() +
                " (closed on " + period.getClosedAt() + " by " + 
                period.getClosedBy() + ")");
        }
        
        if ("PERMANENTLY_CLOSED".equals(period.getStatus())) {
            throw new BusinessException(
                "Cannot post to PERMANENTLY CLOSED period: " + 
                period.getPeriodName() + 
                ". This period cannot be reopened.");
        }
        
        return period;
    }
    
    /**
     * Check if period can be closed
     * Validates all postings are processed
     */
    public void validateCanClosePeriod(
            Long companyId,
            Integer fiscalYear,
            Integer periodNo) {
        
        // Get period date range
        GlFiscalPeriod period = periodRepository
            .findByCompanyYearPeriod(companyId, fiscalYear, periodNo)
            .orElseThrow(() -> new BusinessException(
                "Period not found: year=" + fiscalYear + ", period=" + periodNo));
        
        // Check for pending postings
        long pendingCount = postingRepository.countByCompanyAndDateRangeAndStatus(
            companyId,
            period.getStartDate(),
            period.getEndDate(),
            List.of("NEW", "READY_FOR_GL")
        );
        
        if (pendingCount > 0) {
            throw new BusinessException(
                "Cannot close period. " + pendingCount + 
                " posting(s) still pending.");
        }
        
        // Check for error postings
        long errorCount = postingRepository.countByCompanyAndDateRangeAndStatus(
            companyId,
            period.getStartDate(),
            period.getEndDate(),
            List.of("ERROR")
        );
        
        if (errorCount > 0) {
            throw new BusinessException(
                "Cannot close period. " + errorCount + 
                " posting(s) in ERROR status. Please resolve first.");
        }
    }
}
```

**Integration in PostingEngineService:**

```java
@Service
@RequiredArgsConstructor
public class PostingEngineService {
    
    private final FiscalPeriodValidationService periodValidation;
    // ... other dependencies
    
    @Transactional
    public PostingResult postSingle(Long postingId) {
        try {
            // 1. Lock posting
            AccPostingMst posting = loadPostingMasterWithLock(postingId);
            
            // 2. Validate status
            validatePostingStatus(posting);
            
            // ✅ 3. VALIDATE FISCAL PERIOD IS OPEN
            GlFiscalPeriod period = periodValidation.validatePeriodIsOpen(
                posting.getCompanyId(), 
                posting.getDocDate()
            );
            
            // 4. Proceed with posting (no IN_PROGRESS status; pessimistic lock prevents concurrent processing)
            
            // ... rest of posting logic
            
        } catch (Exception e) {
            updatePostingError(postingId, e.getMessage());
            throw e;
        }
    }
}
```

---

### 9.6 Document Number Generation (Thread-Safe)

**AR:** توليد أرقام المستندات بطريقة آمنة من التزامن.

**EN:** Thread-safe document number generation.

**Problem with Simple Approach:**
```java
// ❌ NOT THREAD-SAFE - Race condition!
private String generateJournalNumber(Long companyId, String journalType) {
    long count = journalRepository.countByJournalTypeAndCompanyId(
        journalType, companyId);
    return String.format("%s-%d-%d-%06d", 
        journalType, companyId, LocalDate.now().getYear(), count + 1);
}
```

**Solution 1: Dedicated Counter Table (Recommended)**

```sql
-- Table for document number counters
CREATE TABLE GL_DOCUMENT_COUNTER (
    COMPANY_ID_FK        NUMBER          NOT NULL,
    FISCAL_YEAR          NUMBER(4)       NOT NULL,
    PERIOD_NO            NUMBER(2)       NOT NULL,
    DOC_TYPE             VARCHAR2(20)    NOT NULL,
    CURRENT_VALUE        NUMBER          DEFAULT 0,
    UPDATED_AT           TIMESTAMP,
    
    CONSTRAINT GL_DOCUMENT_COUNTER_PK 
        PRIMARY KEY (COMPANY_ID_FK, FISCAL_YEAR, PERIOD_NO, DOC_TYPE)
);
```

```java
@Service
@RequiredArgsConstructor
public class DocumentNumberService {
    
    private final GlDocumentCounterRepository counterRepository;
    private final GlFiscalPeriodRepository periodRepository;
    
    /**
     * Get next journal number (thread-safe)
     * Uses separate transaction with pessimistic lock
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String getNextJournalNumber(
            Long companyId, 
            LocalDate docDate,
            String journalType) {
        
        // Determine fiscal period
        GlFiscalPeriod period = periodRepository
            .findByCompanyAndDate(companyId, docDate)
            .orElseThrow(() -> new BusinessException(
                "No fiscal period for date: " + docDate));
        
        // Get counter with pessimistic lock
        GlDocumentCounterId counterId = new GlDocumentCounterId(
            companyId,
            period.getFiscalYear(),
            period.getPeriodNo(),
            journalType
        );
        
        GlDocumentCounter counter = counterRepository
            .findByIdForUpdate(counterId)
            .orElseGet(() -> {
                GlDocumentCounter newCounter = new GlDocumentCounter();
                newCounter.setId(counterId);
                newCounter.setCurrentValue(0L);
                return newCounter;
            });
        
        // Increment
        counter.setCurrentValue(counter.getCurrentValue() + 1);
        counter.setUpdatedAt(LocalDateTime.now());
        counterRepository.save(counter);
        
        // Format: JV-1-2025-01-000001
        return String.format("%s-%d-%04d-%02d-%06d",
            getJournalTypePrefix(journalType),
            companyId,
            period.getFiscalYear(),
            period.getPeriodNo(),
            counter.getCurrentValue()
        );
    }
    
    private String getJournalTypePrefix(String journalType) {
        return switch (journalType) {
            case "MANUAL" -> "MJ";
            case "AUTOMATIC" -> "JV";
            default -> "JN";
        };
    }
}
```

```java
public interface GlDocumentCounterRepository 
        extends JpaRepository<GlDocumentCounter, GlDocumentCounterId> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM GlDocumentCounter c WHERE c.id = :id")
    Optional<GlDocumentCounter> findByIdForUpdate(@Param("id") GlDocumentCounterId id);
}
```

**Solution 2: Database Sequence (Simpler but less flexible)**

```sql
CREATE SEQUENCE SEQ_JOURNAL_NO START WITH 1 INCREMENT BY 1;
```

```java
@Service
public class DocumentNumberService {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public String getNextJournalNumber(Long companyId) {
        Long seqValue = ((Number) entityManager
            .createNativeQuery("SELECT SEQ_JOURNAL_NO.NEXTVAL FROM DUAL")
            .getSingleResult()).longValue();
        
        return String.format("JV-%d-%d-%08d",
            companyId,
            LocalDate.now().getYear(),
            seqValue
        );
    }
}
```

---

### 9.7 Error Recovery Strategy

**AR:** استراتيجية استرجاع الأخطاء والتعامل مع الحالات العالقة.

**EN:** Error recovery strategy for stuck postings.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PostingRecoveryService {
    
    private final AccPostingMstRepository postingRepository;
    
    /**
     * Recover stuck postings
     * Runs every 5 minutes via @Scheduled
     * 
     * Note: Production CHECK constraint only allows (NEW, READY_FOR_GL, POSTED, ERROR).
     * Stuck postings are identified by STATUS='NEW' or 'READY_FOR_GL' with stale UPDATED_AT.
     * Recovery resets them to READY_FOR_GL for retry.
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    @Transactional
    public void recoverStuckPostings() {
        log.info("Starting stuck postings recovery check...");
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);
        
        // Find postings that may be stuck (locked but never completed)
        List<AccPostingMst> stuckPostings = postingRepository
            .findByStatusInAndUpdatedAtBefore(
                List.of("NEW", "READY_FOR_GL"), cutoffTime);
        
        if (stuckPostings.isEmpty()) {
            log.info("No stuck postings found");
            return;
        }
        
        log.warn("Found {} stuck posting(s)", stuckPostings.size());
        
        for (AccPostingMst posting : stuckPostings) {
            try {
                log.warn("Recovering stuck posting: postingId={}, docNo={}, stuck since {}",
                    posting.getPostingId(),
                    posting.getSourceDocNo(),
                    posting.getUpdatedAt());
                
                // Reset to READY_FOR_GL for retry
                posting.setStatus("READY_FOR_GL");
                posting.setErrorMessage(
                    "Auto-recovered (stale for >30 min). " +
                    "Original message: " + posting.getErrorMessage()
                );
                posting.setUpdatedBy("SYSTEM-RECOVERY");
                posting.setUpdatedAt(LocalDateTime.now());
                
                postingRepository.save(posting);
                
                log.info("Successfully recovered posting: {}", posting.getPostingId());
                
            } catch (Exception e) {
                log.error("Failed to recover posting: " + posting.getPostingId(), e);
            }
        }
    }
    
    /**
     * Find and report postings in ERROR status
     * For admin monitoring
     */
    public List<PostingErrorReport> getErrorPostings(Long companyId) {
        List<AccPostingMst> errors = postingRepository
            .findByCompanyIdAndStatus(companyId, "ERROR");
        
        return errors.stream()
            .map(p -> new PostingErrorReport(
                p.getPostingId(),
                p.getSourceModule(),
                p.getSourceDocType(),
                p.getSourceDocNo(),
                p.getDocDate(),
                p.getErrorMessage(),
                p.getUpdatedAt()
            ))
            .collect(Collectors.toList());
    }
    
    /**
     * Retry all error postings for a company
     * Admin operation
     */
    @Transactional
    public BatchRetryResult retryErrorPostings(Long companyId, int maxRetries) {
        List<AccPostingMst> errors = postingRepository
            .findByCompanyIdAndStatus(companyId, "ERROR")
            .stream()
            .limit(maxRetries)
            .toList();
        
        int successCount = 0;
        int stillErrorCount = 0;
        
        for (AccPostingMst posting : errors) {
            // Reset to READY
            posting.setStatus("READY_FOR_GL");
            posting.setErrorMessage("Retry requested by admin");
            postingRepository.save(posting);
            successCount++;
        }
        
        return new BatchRetryResult(successCount, stillErrorCount);
    }
    
    /**
     * Cancel posting (admin only)
     * For situations where posting cannot be fixed.
     * Note: Sets STATUS to ERROR (CANCELLED is not a valid CHECK constraint value).
     */
    @Transactional
    public void cancelPosting(Long postingId, String reason) {
        AccPostingMst posting = postingRepository.findById(postingId)
            .orElseThrow(() -> new BusinessException("Posting not found"));
        
        if ("POSTED".equals(posting.getStatus())) {
            throw new BusinessException(
                "Cannot cancel POSTED transaction. Use reversal instead.");
        }
        
        posting.setStatus("ERROR");
        posting.setErrorMessage("Cancelled by admin: " + reason);
        posting.setUpdatedBy(SecurityContextHelper.getUsernameOrSystem());
        posting.setUpdatedAt(LocalDateTime.now());
        
        postingRepository.save(posting);
        
        log.warn("Posting {} cancelled by {}: {}",
            postingId,
            SecurityContextHelper.getUsernameOrSystem(),
            reason);
    }
}
```

**Repository Methods:**

```java
public interface AccPostingMstRepository extends JpaRepository<AccPostingMst, Long> {
    
    List<AccPostingMst> findByStatusAndUpdatedAtBefore(
        String status, 
        LocalDateTime cutoffTime
    );
    
    List<AccPostingMst> findByCompanyIdAndStatus(
        Long companyId, 
        String status
    );
}
```

**Scheduler Configuration:**

```java
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Enable @Scheduled annotations
}
```

---

### 9.8 Concurrency Control

```java
public interface AccPostingMstRepository extends JpaRepository<AccPostingMst, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM AccPostingMst p WHERE p.postingId = :id")
    Optional<AccPostingMst> findByIdForUpdate(@Param("id") Long id);
}
```

**Optimistic Locking (for Balance Updates):**

```java
@Entity
@Table(name = "GL_ACCOUNT_BALANCE")
public class GlAccountBalance {
    
    @EmbeddedId
    private GlAccountBalanceId id;
    
    @Version
    private Long version;  // Optimistic lock
    
    // ... fields
}
```

---

## 10. Performance Optimization Strategy

### 10.1 Pagination for Ledger Queries

**Repository:**

```java
public interface GlLedgerEntryRepository extends JpaRepository<GlLedgerEntry, Long> {
    
    @Query("""
        SELECT e FROM GlLedgerEntry e 
        WHERE e.accountId = :accountId 
        AND e.docDate BETWEEN :dateFrom AND :dateTo
        ORDER BY e.docDate, e.entryId
        """)
    Page<GlLedgerEntry> findByAccountAndDateRange(
        @Param("accountId") Long accountId,
        @Param("dateFrom") LocalDate dateFrom,
        @Param("dateTo") LocalDate dateTo,
        Pageable pageable
    );
}
```

**Controller:**

```java
@GetMapping("/ledger/{accountId}")
public Page<LedgerEntryResponse> getLedgerEntries(
        @PathVariable Long accountId,
        @RequestParam LocalDate dateFrom,
        @RequestParam LocalDate dateTo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size) {
    
    Pageable pageable = PageRequest.of(page, size);
    return ledgerService.getLedgerEntries(accountId, dateFrom, dateTo, pageable);
}
```

### 10.2 Batch Processing for Multiple Postings

```java
@Service
public class BatchPostingService {
    
    @Transactional
    public BatchPostingResult postBatch(Long companyId, int maxItems) {
        List<AccPostingMst> postings = postingRepository
            .findByCompanyAndStatusIn(companyId, List.of("NEW", "READY_FOR_GL"), 
                PageRequest.of(0, maxItems));
        
        int successCount = 0;
        int errorCount = 0;
        List<PostingError> errors = new ArrayList<>();
        
        for (AccPostingMst posting : postings) {
            try {
                // Each posting in separate transaction
                postingSingleInNewTransaction(posting.getPostingId());
                successCount++;
            } catch (Exception e) {
                errorCount++;
                errors.add(new PostingError(posting.getPostingId(), e.getMessage()));
            }
        }
        
        return new BatchPostingResult(successCount, errorCount, errors);
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void postingSingleInNewTransaction(Long postingId) {
        postingEngineService.postSingle(postingId);
    }
}
```

### 10.3 Index Strategy Summary

| Table | Critical Indexes | Purpose |
|-------|------------------|---------|
| `GL_LEDGER_ENTRY` | `(ACCOUNT_ID_FK, DOC_DATE)` | Ledger queries by account + date |
| `GL_JOURNAL_HDR` | `(COMPANY_ID_FK, DOC_DATE)` | Journal search by date |
| `ACC_POSTING_MST` | `(STATUS)` | Find NEW/READY_FOR_GL postings |
| `GL_ACCOUNT_BALANCE` | `(FISCAL_YEAR, PERIOD_NO)` | Period-based reports |

### 10.4 Caching Strategy

```java
@Service
public class AccountsChartService {
    
    @Cacheable(cacheNames = "accountsChart", key = "#accountId")
    public AccountsChart getAccount(Long accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new NotFoundException("Account not found"));
    }
    
    @Cacheable(cacheNames = "accountingRules", 
               key = "#companyId + '_' + #module + '_' + #docType")
    public AccRuleHdr getRuleHeader(Long companyId, String module, 
                                     String docType) {
        return ruleHdrRepository
            .findByCompanyIdFkAndSourceModuleAndSourceDocTypeAndIsActive(
                companyId, module, docType, 1)
            .orElseThrow(() -> new BusinessException(
                "No active accounting rule found for: " + module + "/" + docType));
    }
}
```

---

## 12. REST API Endpoints Specification

### الهدف من هذا القسم

هذا القسم يوضح جميع الـ REST APIs المطلوبة لبناء الشاشات في Angular.
كل API يتبع مبادئ:
- RESTful design
- DTOs للـ request/response
- Pagination للـ lists
- HTTP status codes صحيحة
- Exception handling موحد

---

### 12.1 Chart of Accounts APIs

#### **GET /api/gl/accounts**
Get list of all accounts with pagination and filtering.

**Query Parameters:**
- `page` (default: 0)
- `size` (default: 20)
- `companyId` (optional)
- `accountType` (optional: ASSET, LIABILITY, REVENUE, EXPENSE, EQUITY)
- `isActive` (optional: true/false)
- `parentAccountId` (optional: for filtering by parent)

**Response:**
```json
{
  "content": [
    {
      "accountId": 101001,
      "accountCode": "1010-001",
      "accountName": "Cash - Main",
      "accountNameAr": "النقدية - الرئيسي",
      "accountType": "ASSET",
      "parentAccountId": 101000,
      "level": 2,
      "isActive": true
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "number": 0,
  "size": 20
}
```

---

#### **GET /api/gl/accounts/{id}**
Get single account details.

**Response:**
```json
{
  "accountId": 101001,
  "accountCode": "1010-001",
  "accountName": "Cash - Main",
  "accountNameAr": "النقدية - الرئيسي",
  "accountType": "ASSET",
  "parentAccountId": 101000,
  "parentAccountName": "Cash and Cash Equivalents",
  "level": 2,
  "isActive": true,
  "normalBalance": "DEBIT",
  "createdAt": "2025-01-15T10:30:00",
  "createdBy": "admin"
}
```

---

#### **POST /api/gl/accounts**
Create new account.

**Request:**
```json
{
  "accountCode": "1010-002",
  "accountName": "Petty Cash",
  "accountNameAr": "العهدة النقدية",
  "accountType": "ASSET",
  "parentAccountId": 101000,
  "isActive": true
}
```

**Response:** `201 Created` with created account object

---

#### **PUT /api/gl/accounts/{id}**
Update existing account.

**Request:** Same as POST

**Response:** `200 OK` with updated account object

---

#### **GET /api/gl/accounts/tree**
Get accounts as hierarchical tree structure.

**Query Parameters:**
- `companyId` (required)
- `accountType` (optional)

**Response:**
```json
[
  {
    "accountId": 100000,
    "accountCode": "1000",
    "accountName": "Assets",
    "level": 0,
    "children": [
      {
        "accountId": 101000,
        "accountCode": "1010",
        "accountName": "Current Assets",
        "level": 1,
        "children": [...]
      }
    ]
  }
]
```

---

### 12.2 Manual Journal Entry APIs

#### **POST /api/gl/journals/manual**
Create manual journal entry.

**Endpoint:** `POST /api/gl/journals/manual`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <token> (if authentication enabled)
```

**Request Body:**
```json
{
  "companyId": 1,
  "docDate": "2025-12-13",
  "description": "Adjustment entry for depreciation",
  "lines": [
    {
      "accountId": 501001,
      "entrySide": "DEBIT",
      "amount": 1000.00,
      "entityType": "COST_CENTER",
      "entityId": 10,
      "description": "Depreciation expense"
    },
    {
      "accountId": 151002,
      "entrySide": "CREDIT",
      "amount": 1000.00,
      "description": "Accumulated depreciation"
    }
  ]
}
```

**Request Fields:**

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| companyId | Long | Yes | @Positive | Company identifier |
| docDate | LocalDate | Yes | @NotNull | Document date (yyyy-MM-dd) |
| description | String | Yes | @NotBlank | Journal header description |
| lines | Array | Yes | @NotEmpty, min 2 lines | Journal lines |

**Line Fields:**

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| accountId | Long | Yes | @Positive | GL account ID (FK → ACCOUNTS_CHART) |
| entrySide | String | Yes | DEBIT or CREDIT | Explicit debit/credit direction |
| amount | BigDecimal | Yes | @Positive | Amount (always positive) |
| entityType | String | No | CUSTOMER, SUPPLIER, COST_CENTER, CONTRACT | Polymorphic entity type |
| entityId | Long | No | - | Entity ID (polymorphic FK) |
| description | String | No | - | Line description (optional) |

**Validation Rules:**
1. Journal must have at least **2 lines**
2. SUM(DEBIT lines) must equal SUM(CREDIT lines) (balanced journal)
3. Each line must specify `entrySide` (DEBIT or CREDIT) and positive `amount`
4. All amounts must be positive (> 0)
5. Journal total cannot be zero

**Response:** `201 Created`
```json
{
  "journalId": 12345,
  "journalNo": "MJ-1-2025-000001",
  "companyId": 1,
  "docDate": "2025-12-13",
  "description": "Adjustment entry for depreciation",
  "journalType": "MANUAL",
  "status": "DRAFT",
  "totalDebit": 1000.00,
  "totalCredit": 1000.00,
  "lineCount": 2,
  "createdAt": "2025-12-13T09:41:00",
  "createdBy": "admin"
}
```

**Error Responses:**

**400 Bad Request - Unbalanced Journal:**
```json
{
  "status": 400,
  "message": "Journal is not balanced: totalDebit=1000.00, totalCredit=1500.00",
  "timestamp": "2025-12-13T09:41:00"
}
```

**400 Bad Request - Insufficient Lines:**
```json
{
  "status": 400,
  "message": "Journal must have at least two lines",
  "timestamp": "2025-12-13T09:41:00"
}
```

**400 Bad Request - Invalid Line:**
```json
{
  "status": 400,
  "message": "Line 1: entrySide must be DEBIT or CREDIT",
  "timestamp": "2025-12-13T09:41:00"
}
```

**400 Bad Request - Invalid Amount:**
```json
{
  "status": 400,
  "message": "Line 2: amount must be positive",
  "timestamp": "2025-12-13T09:41:00"
}
```

**400 Bad Request - Zero Total:**
```json
{
  "status": 400,
  "message": "Journal total cannot be zero",
  "timestamp": "2025-12-13T09:41:00"
}
```

**Notes:**
- Manual journals are created with `status = "DRAFT"` initially
- Manual journals require approval workflow: `DRAFT → PENDING_APPROVAL → POSTED` (see Section 5.5)
- No posting engine involvement (no ACC_POSTING tables used)
- Journal number format: `MJ-{companyId}-{year}-{sequence}`
- Ledger entries and balance updates occur only upon approval (`POSTED`)
- Manual journals can only be reversed after `POSTED`, not edited
- `createdBy` is automatically populated from SecurityContext

---

### 12.3 Journal Entries List APIs

#### **GET /api/gl/journals**
Get list of all journals (manual + automatic) with pagination.

**Query Parameters:**
- `page` (default: 0)
- `size` (default: 20)
- `companyId` (optional)
- `dateFrom` (optional: yyyy-MM-dd)
- `dateTo` (optional: DRAFT, PENDING_APPROVAL, yyyy-MM-dd)
- `journalType` (optional: MANUAL, AUTOMATIC)
- `status` (optional: DRAFT, PENDING_APPROVAL, POSTED, REVERSED)
- `journalNo` (optional: search by number)

**Response:**
```json
{
  "content": [
    {
      "journalId": 12345,
      "journalNo": "MJ-1-2025-000001",
      "companyId": 1,
      "docDate": "2025-12-13",
      "description": "Adjustment entry",
      "journalType": "MANUAL",
      "status": "POSTED",
      "totalDebit": 1000.00,
      "totalCredit": 1000.00,
      "createdBy": "admin"
    }
  ],
  "totalElements": 500,
  "totalPages": 25,
  "number": 0,
  "size": 20
}
```

---

#### **GET /api/gl/journals/{id}**
Get journal details including all lines.

**Response:**
```json
{
  "journalId": 12345,
  "journalNo": "MJ-1-2025-000001",
  "companyId": 1,
  "docDate": "2025-12-13",
  "description": "Adjustment entry for depreciation",
  "journalType": "MANUAL",
  "status": "POSTED",
  "sourcePostingId": null,
  "lines": [
    {
      "lineNo": 1,
      "accountId": 501001,
      "accountCode": "5010-001",
      "accountName": "Depreciation Expense",
      "entrySide": "DEBIT",
      "amount": 1000.00,
      "entityType": "COST_CENTER",
      "entityId": 10,
      "description": "Depreciation expense"
    },
    {
      "lineNo": 2,
      "accountId": 151002,
      "accountCode": "1510-002",
      "accountName": "Accumulated Depreciation",
      "entrySide": "CREDIT",
      "amount": 1000.00,
      "description": "Accumulated depreciation"
    }
  ],
  "createdAt": "2025-12-13T09:41:00",
  "createdBy": "admin"
}
```

---

#### **POST /api/gl/journals/{id}/reverse**
Reverse a journal entry (create reversal journal).

**Response:** `200 OK`
```json
{
  "originalJournalId": 12345,
  "reversalJournalId": 12346,
  "reversalJournalNo": "MJ-1-2025-000002",
  "message": "Journal reversed successfully"
}
```

---

### 12.4 Ledger Report APIs

#### **GET /api/gl/ledger/{accountId}**
Get ledger entries for specific account.

**Query Parameters:**
- `dateFrom` (required: yyyy-MM-dd)
- `dateTo` (required: yyyy-MM-dd)
- `page` (default: 0)
- `size` (default: 50)

**Response:**
```json
{
  "accountId": 101001,
  "accountCode": "1010-001",
  "accountName": "Cash - Main",
  "openingBalance": 50000.00,
  "entries": [
    {
      "entryId": 1001,
      "docDate": "2025-12-01",
      "journalId": 100,
      "journalNo": "JV-1-2025-000100",
      "description": "Customer payment",
      "entrySide": "DEBIT",
      "amount": 5000.00,
      "entityType": "CUSTOMER",
      "entityId": 100,
      "runningBalance": 55000.00
    },
    {
      "entryId": 1002,
      "docDate": "2025-12-02",
      "journalId": 101,
      "journalNo": "JV-1-2025-000101",
      "description": "Supplier payment",
      "entrySide": "CREDIT",
      "amount": 3000.00,
      "entityType": "SUPPLIER",
      "entityId": 200,
      "runningBalance": 52000.00
    }
  ],
  "closingBalance": 52000.00,
  "totalDebit": 5000.00,
  "totalCredit": 3000.00
}
```

---

### 12.5 Posting Engine APIs

#### **GET /api/gl/postings/pending**
Get list of pending postings (status: NEW or READY_FOR_GL).

**Query Parameters:**
- `page` (default: 0)
- `size` (default: 20)
- `companyId` (optional)
- `sourceModule` (optional: SALES, RENTAL, INVENTORY)
- `dateFrom` (optional)
- `dateTo` (optional)

**Response:**
```json
{
  "content": [
    {
      "postingId": 5001,
      "sourceModule": "SALES",
      "sourceDocType": "INVOICE",
      "sourceDocNo": "INV-2025-001",
      "docDate": "2025-12-13",
      "totalDebit": 11500.00,
      "totalCredit": 11500.00,
      "status": "READY_FOR_GL",
      "companyId": 1
    }
  ],
  "totalElements": 25,
  "totalPages": 2,
  "number": 0,
  "size": 20
}
```

---

#### **GET /api/gl/postings/{id}**
Get posting details with all detail lines.

**Response:**
```json
{
  "postingId": 5001,
  "sourceModule": "SALES",
  "sourceDocType": "INVOICE",
  "sourceDocId": 1234,
  "sourceDocNo": "INV-2025-001",
  "docDate": "2025-12-13",
  "companyId": 1,
  "totalDebit": 11500.00,
  "totalCredit": 11500.00,
  "status": "READY_FOR_GL",
  "details": [
    {
      "lineNo": 1,
      "accountId": 120101,
      "entrySide": "DEBIT",
      "amount": 11500.00,
      "entityType": "CUSTOMER",
      "entityId": 100
    },
    {
      "lineNo": 2,
      "accountId": 410101,
      "entrySide": "CREDIT",
      "amount": 10000.00
    },
    {
      "lineNo": 3,
      "accountId": 220301,
      "entrySide": "CREDIT",
      "amount": 1500.00
    }
  ]
}
```

---

#### **POST /api/gl/postings/{id}/post**
Post single financial transaction.

**Response:** `200 OK`
```json
{
  "postingId": 5001,
  "journalId": 12347,
  "journalNo": "JV-1-2025-000347",
  "success": true,
  "status": "POSTED",
  "totalDebit": 11500.00,
  "totalCredit": 11500.00,
  "processedAt": "2025-12-13T10:15:00"
}
```

**Error Response:** `200 OK` (not 4xx because posting process completed, but with error)
```json
{
  "postingId": 5001,
  "success": false,
  "status": "ERROR",
  "errorMessage": "No active accounting rule found for SALES/INVOICE",
  "processedAt": "2025-12-13T10:15:00"
}
```

---

#### **POST /api/gl/postings/post-all**
Post all pending transactions in batch.

**Request:**
```json
{
  "companyId": 1,
  "sourceModule": "SALES",
  "maxCount": 100
}
```

**Response:** `200 OK`
```json
{
  "totalProcessed": 25,
  "successCount": 23,
  "errorCount": 2,
  "results": [
    {
      "postingId": 5001,
      "success": true,
      "journalId": 12347
    },
    {
      "postingId": 5002,
      "success": false,
      "errorMessage": "No accounting rule found"
    }
  ]
}
```

---

#### **GET /api/gl/postings/errors**
Get list of postings with ERROR status.

**Query Parameters:** Same as pending postings

**Response:** Same structure as pending postings list

---

#### **POST /api/gl/postings/{id}/reverse**
Reverse a posted transaction (create reversal posting).

**Response:** `200 OK`
```json
{
  "originalPostingId": 5001,
  "reversalPostingId": 5026,
  "reversalJournalId": 12348,
  "message": "Posting reversed successfully"
}
```

---

### 12.6 Accounting Rules APIs

#### **GET /api/gl/rules**
Get list of accounting rule headers with pagination.

**Query Parameters:**
- `page` (default: 0)
- `size` (default: 20)
- `companyId` (optional)
- `sourceModule` (optional)
- `sourceDocType` (optional)
- `isActive` (optional: 0/1)

**Response:**
```json
{
  "content": [
    {
      "ruleId": 1,
      "companyId": 1,
      "sourceModule": "SALES",
      "sourceDocType": "INVOICE",
      "isActive": 1,
      "lines": [
        {
          "ruleLineId": 1,
          "businessSide": "AR",
          "accountId": 120101,
          "accountCode": "1201-001",
          "accountName": "Accounts Receivable",
          "entrySide": "DEBIT",
          "priority": 1,
          "amountSourceType": "TOTAL",
          "amountSourceValue": null
        },
        {
          "ruleLineId": 2,
          "businessSide": "REVENUE",
          "accountId": 410101,
          "accountCode": "4101-001",
          "accountName": "Sales Revenue",
          "entrySide": "CREDIT",
          "priority": 2,
          "amountSourceType": "PERCENT",
          "amountSourceValue": 0.952381
        }
      ]
    }
  ],
  "totalElements": 50,
  "totalPages": 3
}
```

---

#### **GET /api/gl/rules/{id}**
Get single rule header with all rule lines.

**Response:** Single rule header object with lines array

---

#### **POST /api/gl/rules**
Create new accounting rule (header + lines).

**Request:**
```json
{
  "companyId": 1,
  "sourceModule": "SALES",
  "sourceDocType": "INVOICE",
  "isActive": 1,
  "lines": [
    {
      "businessSide": "AR",
      "accountId": 120101,
      "entrySide": "DEBIT",
      "priority": 1,
      "amountSourceType": "TOTAL",
      "amountSourceValue": null
    },
    {
      "businessSide": "REVENUE",
      "accountId": 410101,
      "entrySide": "CREDIT",
      "priority": 2,
      "amountSourceType": "PERCENT",
      "amountSourceValue": 0.952381
    },
    {
      "businessSide": "VAT_OUT",
      "accountId": 220301,
      "entrySide": "CREDIT",
      "priority": 3,
      "amountSourceType": "PERCENT",
      "amountSourceValue": 0.047619
    }
  ]
}
```

**Response:** `201 Created` with created rule header + lines

---

#### **PUT /api/gl/rules/{id}**
Update accounting rule header and lines.

**Request:** Same as POST

**Response:** `200 OK` with updated rule

---

#### **DELETE /api/gl/rules/{id}**
Deactivate accounting rule (soft delete via `IS_ACTIVE = 0`).

**Response:** `204 No Content`

---

### 12.7 Financial Reports APIs

#### **GET /api/gl/reports/trial-balance**
Generate trial balance report.

**Query Parameters:**
- `companyId` (required)
- `fiscalYear` (required)
- `periodFrom` (required)
- `periodTo` (required)

**Response:**
```json
{
  "companyId": 1,
  "fiscalYear": 2025,
  "periodFrom": 1,
  "periodTo": 12,
  "reportDate": "2025-12-13",
  "accounts": [
    {
      "accountId": 101001,
      "accountCode": "1010-001",
      "accountName": "Cash - Main",
      "accountType": "ASSET",
      "openingBalance": 50000.00,
      "periodDebit": 100000.00,
      "periodCredit": 80000.00,
      "closingBalance": 70000.00
    }
  ],
  "totals": {
    "totalOpeningBalance": 500000.00,
    "totalPeriodDebit": 1000000.00,
    "totalPeriodCredit": 1000000.00,
    "totalClosingBalance": 500000.00
  }
}
```

---

#### **GET /api/gl/reports/profit-loss**
Generate profit & loss statement.

**Query Parameters:**
- `companyId` (required)
- `fiscalYear` (required)
- `periodFrom` (required)
- `periodTo` (required)
- `compareWithPreviousYear` (optional: true/false)

**Response:**
```json
{
  "companyId": 1,
  "fiscalYear": 2025,
  "periodFrom": 1,
  "periodTo": 12,
  "revenue": {
    "total": 1000000.00,
    "accounts": [
      {
        "accountCode": "4101",
        "accountName": "Sales Revenue",
        "amount": 1000000.00
      }
    ]
  },
  "expenses": {
    "total": 700000.00,
    "accounts": [
      {
        "accountCode": "5001",
        "accountName": "Cost of Goods Sold",
        "amount": 500000.00
      },
      {
        "accountCode": "5010",
        "accountName": "Operating Expenses",
        "amount": 200000.00
      }
    ]
  },
  "netProfit": 300000.00
}
```

---

#### **GET /api/gl/reports/balance-sheet**
Generate balance sheet.

**Query Parameters:**
- `companyId` (required)
- `fiscalYear` (required)
- `periodNo` (required)

**Response:**
```json
{
  "companyId": 1,
  "fiscalYear": 2025,
  "periodNo": 12,
  "reportDate": "2025-12-31",
  "assets": {
    "current": {
      "total": 300000.00,
      "accounts": [...]
    },
    "nonCurrent": {
      "total": 500000.00,
      "accounts": [...]
    },
    "total": 800000.00
  },
  "liabilities": {
    "current": {
      "total": 150000.00,
      "accounts": [...]
    },
    "nonCurrent": {
      "total": 250000.00,
      "accounts": [...]
    },
    "total": 400000.00
  },
  "equity": {
    "total": 400000.00,
    "accounts": [...]
  },
  "totalLiabilitiesAndEquity": 800000.00,
  "isBalanced": true
}
```

---

### 12.8 Account Balances APIs

#### **GET /api/gl/balances**
Get account balances with filtering.

**Query Parameters:**
- `companyId` (required)
- `fiscalYear` (required)
- `periodFrom` (optional, default: 1)
- `periodTo` (optional, default: 12)
- `accountIdFrom` (optional)
- `accountIdTo` (optional)
- `page` (default: 0)
- `size` (default: 50)

**Response:**
```json
{
  "content": [
    {
      "accountId": 101001,
      "accountCode": "1010-001",
      "accountName": "Cash - Main",
      "fiscalYear": 2025,
      "periodNo": 12,
      "openingBalance": 50000.00,
      "periodDebit": 100000.00,
      "periodCredit": 80000.00,
      "closingBalance": 70000.00
    }
  ],
  "totalElements": 150,
  "totalPages": 3
}
```

---

#### **POST /api/gl/balances/recalculate**
Recalculate account balances (admin only).

**Request:**
```json
{
  "companyId": 1,
  "fiscalYear": 2025,
  "periodFrom": 1,
  "periodTo": 12
}
```

**Response:** `200 OK`
```json
{
  "message": "Balances recalculated successfully",
  "accountsProcessed": 150,
  "periodsProcessed": 12
}
```

---

### 12.9 Fiscal Periods APIs

#### **GET /api/gl/fiscal-periods**
Get list of fiscal periods.

**Query Parameters:**
- `companyId` (optional)
- `fiscalYear` (optional)
- `status` (optional: OPEN, CLOSED)

**Response:**
```json
[
  {
    "periodId": 1,
    "companyId": 1,
    "fiscalYear": 2025,
    "periodNo": 1,
    "periodName": "January 2025",
    "startDate": "2025-01-01",
    "endDate": "2025-01-31",
    "status": "OPEN",
    "closedAt": null,
    "closedBy": null
  }
]
```

---

#### **POST /api/gl/fiscal-periods**
Create fiscal year with 12 periods.

**Request:**
```json
{
  "companyId": 1,
  "fiscalYear": 2026,
  "startDate": "2026-01-01"
}
```

**Response:** `201 Created`
```json
{
  "message": "Fiscal year created successfully",
  "fiscalYear": 2026,
  "periodsCreated": 12
}
```

---

#### **PATCH /api/gl/fiscal-periods/{id}/status**
Open or close fiscal period.

**Request:**
```json
{
  "status": "CLOSED"
}
```

**Response:** `200 OK`
```json
{
  "periodId": 1,
  "fiscalYear": 2025,
  "periodNo": 12,
  "status": "CLOSED",
  "closedAt": "2025-12-13T10:00:00",
  "closedBy": "admin"
}
```

---

### 12.10 Common Response Patterns

#### **Success Response**
```json
{
  "status": 200,
  "data": { ... },
  "message": "Operation successful"
}
```

#### **Error Response**
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "entrySide",
      "message": "entrySide must be DEBIT or CREDIT"
    }
  ],
  "timestamp": "2025-12-13T10:00:00"
}
```

#### **Pagination Response**
```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 5,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

---

---

## 13. Acceptance Criteria (ملخص)

1. كل `POSTING_ID` في حالة NEW/READY يمكن ترحيله لقيد متزن.
2. لا يتم ترحيل نفس `POSTING_ID` مرتين.
3. يتم إنشاء Journal + Ledger + تحديث Balances لكل عملية ترحيل ناجحة.
4. في حالة خطأ، يتم تحديث STATUS = ERROR مع رسالة واضحة.
5. لا يُسمح بتعديل أو حذف قيود بعد POSTED، فقط Reversal.
6. التقارير (Trial Balance, P&L, Balance Sheet) تعتمد على نفس بيانات GL.
7. الإدخال اليدوي في دفتر اليومية يتحقق من توازن القيد ويُعامل مثل القيود الآلية.

---

## 15. Proposed Architectural Enhancements – General Ledger Module

### Scope
This section documents **recommended architectural improvements** to enhance
scalability, auditability, and future compliance of the General Ledger (GL) module.
  
These enhancements are:
- Optional and non-breaking
- Can be implemented incrementally
- Not required for initial Go-Live

---

### 15.1 Unifying Manual Journal with Posting Engine

#### 15.1.1 Current State

**Manual Journal behavior:**
- Directly creates records in:
  - `GL_JOURNAL_HDR`
  - `GL_JOURNAL_LINE`
- Journal is created with:
  - `STATUS = POSTED`
- Does **not** use:
  - `ACC_POSTING_MST`
  - `ACC_POSTING_DTL`
- Bypasses Posting Engine completely

#### 15.1.2 Identified Limitation (Future Risk)

- Two separate accounting paths:
  - Automatic postings (via Posting Engine)
  - Manual journals (direct GL write)
- Audit trail is split across two mechanisms
- Difficult to introduce:
  - Approval workflows
  - Posting simulation
  - Unified compliance controls

---

#### 15.1.3 Proposed Enhancement

Treat Manual Journal as a **special case of Posting Engine input**.

**Proposed classification:**
```text
SOURCE_MODULE   = GL
SOURCE_DOC_TYPE = MANUAL
```

**Flow (Optional Mode):**

1. Manual journal request creates:
   - `ACC_POSTING_MST`
   - `ACC_POSTING_DTL`
2. Posting Engine processes it normally
3. Resulting journal is created via standard flow

> ⚠️ This behavior can be controlled via a feature flag
> (e.g. `manualJournal.usePostingEngine=true`)

---

#### 15.1.4 Benefits

- Single accounting pipeline for all postings
- Unified audit and reversal logic
- Easier compliance (IFRS / audit requirements)
- Ready for approvals and simulations

> **Note:**
> This enhancement is **recommended**, not mandatory for Phase 1.

---

### 15.2 Posting Simulation (Preview Before Commit)

#### 15.2.1 Concept

Introduce a simulation endpoint to preview posting results **without database commit**.

**Proposed API:**

```http
POST /api/gl/postings/{postingId}/simulate
```

---

#### 15.2.2 Simulation Output

The API returns:

- Target GL accounts
- Debit and credit amounts
- Balance validation result
- Missing or inactive accounting rules
- No records are persisted

**Example Response (Conceptual):**

```json
{
  "postingId": 1001,
  "balanced": true,
  "lines": [
    { "accountCode": "110101", "entrySide": "DEBIT", "amount": 105.00 },
    { "accountCode": "410101", "entrySide": "CREDIT", "amount": 100.00 }
  ],
  "missingRules": []
}
```

---

#### 15.2.3 Benefits

- Fast debugging for finance administrators
- Increased trust before posting
- Reduction of postings ending in `ERROR`
- Useful for rule validation and testing

---

### 15.3 Accounting Rules — Split & Percentage Allocation

#### 15.3.1 Current Rule Structure

The `ACC_RULE_LINE` table implements split accounting and percentage-based allocation via `AMOUNT_SOURCE_TYPE` and `AMOUNT_SOURCE_VALUE`:

```text
ACC_RULE_LINE:
  BUSINESS_SIDE → ACCOUNT_ID_FK + ENTRY_SIDE + AMOUNT_SOURCE_TYPE + AMOUNT_SOURCE_VALUE
```

**Supported Amount Source Types:**

| AMOUNT_SOURCE_TYPE | AMOUNT_SOURCE_VALUE | Behavior |
|--------------------|--------------------|----------|
| `TOTAL` | NULL | Full amount from ACC_POSTING_MST |
| `PERCENT` | 0.952381 | Percentage of total amount |
| `FIXED` | 500.00 | Fixed amount (regardless of total) |

A single `BUSINESS_SIDE` can map to multiple accounts through multiple `ACC_RULE_LINE` entries with `PERCENT` type. The total percentage across all lines for a given rule header must equal 100%.

> **Note:** No additional tables or schema changes are required for split/percentage allocation. The `ACC_RULE_HDR` / `ACC_RULE_LINE` header-detail architecture covers all allocation scenarios.

---

---

### 15.4 Multi-Currency Support (دعم العملات المتعددة)

#### 15.4.1 Current State

حالياً يوجد `CURRENCY_CODE` فقط مع قيمة افتراضية `'SAR'`. لا يوجد:
- تتبع سعر الصرف
- تحويل المبالغ للعملة الأساسية
- جدول أسعار صرف مرجعي

---

#### 15.4.2 Enhanced DDL — Currency Exchange Rate Table

```sql
CREATE SEQUENCE SEQ_GL_EXCHANGE_RATE START WITH 1 INCREMENT BY 1;

CREATE TABLE GL_EXCHANGE_RATE (
    RATE_ID              NUMBER          NOT NULL,
    COMPANY_ID_FK        NUMBER          NOT NULL,
    FROM_CURRENCY        VARCHAR2(3)     NOT NULL,
    TO_CURRENCY          VARCHAR2(3)     NOT NULL,
    RATE_DATE            DATE            NOT NULL,
    EXCHANGE_RATE        NUMBER(18,6)    NOT NULL CHECK (EXCHANGE_RATE > 0),
    RATE_TYPE            VARCHAR2(20)    DEFAULT 'SPOT' CHECK (RATE_TYPE IN ('SPOT','AVERAGE','CLOSING')),
    CREATED_BY           VARCHAR2(100),
    CREATED_AT           TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT GL_EXCHANGE_RATE_PK PRIMARY KEY (RATE_ID),
    CONSTRAINT UK_GL_EXCHANGE_RATE UNIQUE (COMPANY_ID_FK, FROM_CURRENCY, TO_CURRENCY, RATE_DATE, RATE_TYPE)
);
```

---

#### 15.4.3 Enhanced Columns on Existing Tables

إضافة الأعمدة التالية على الجداول المتأثرة:

**GL_JOURNAL_HDR:**
```sql
ALTER TABLE GL_JOURNAL_HDR ADD (
    BASE_CURRENCY_CODE   VARCHAR2(3)     DEFAULT 'SAR',
    EXCHANGE_RATE        NUMBER(18,6)    DEFAULT 1
);
```

**GL_JOURNAL_LINE:**
```sql
ALTER TABLE GL_JOURNAL_LINE ADD (
    BASE_AMOUNT          NUMBER(15,2)
);

COMMENT ON COLUMN GL_JOURNAL_LINE.BASE_AMOUNT IS 'Amount converted to base currency: AMOUNT * EXCHANGE_RATE';
```

**GL_LEDGER_ENTRY:**
```sql
ALTER TABLE GL_LEDGER_ENTRY ADD (
    CURRENCY_CODE        VARCHAR2(3)     DEFAULT 'SAR',
    EXCHANGE_RATE        NUMBER(18,6)    DEFAULT 1,
    BASE_AMOUNT          NUMBER(15,2)
);
```

---

#### 15.4.4 Conversion Logic

```text
القاعدة الأساسية:
  BASE_AMOUNT = AMOUNT × EXCHANGE_RATE

مثال:
  AMOUNT = 1,000 USD
  EXCHANGE_RATE = 3.75 (USD → SAR)
  BASE_AMOUNT = 3,750 SAR

ملاحظات:
  - GL_ACCOUNT_BALANCE يُخزَّن دائماً بالعملة الأساسية (BASE_CURRENCY)
  - التقارير تُعرض بالعملة الأساسية ما لم يُطلب التفصيل بالعملة الأصلية
  - فروق تحويل العملات تُسجَّل كقيد تسوية منفصل
```

#### 15.4.5 Approach Recommendations

| Aspect | Recommendation |
|--------|---------------|
| Base Currency | يُحدد على مستوى الشركة (`COMPANY.BASE_CURRENCY`) |
| Exchange Rate Source | جدول `GL_EXCHANGE_RATE` مع تاريخ السعر |
| Rate Type | `SPOT` للعمليات الفورية، `CLOSING` لنهاية الفترة، `AVERAGE` للتقارير |
| FX Gain/Loss | قيد تسوية آلي عند نهاية كل فترة لحساب فروقات العملة |
| Balance Storage | `GL_ACCOUNT_BALANCE` يخزن المبالغ بالعملة الأساسية فقط |

---

### 15.5 Summary

| Enhancement                       | Priority  | Phase                    |
|-----------------------------------|-----------|--------------------------|
| Manual Journal via Posting Engine | Optional  | Phase 2                 |
| Posting Simulation API            | Optional  | Phase 2                 |
| Split Accounting Rules            | Available | Production (ACC_RULE_LINE) |
| Multi-Currency Support            | Required  | Phase 1 (structure) + Phase 2 (logic) |
| Recurring Journals                | Required  | Phase 1 (see Section 5.6) |
| Manual Journal Approval Workflow  | Required  | Phase 1 (see Section 5.5) |

> جميع التحسينات متوافقة مع البنية الحالية ومصممة لرفع موديول الحسابات العامة لمستوى Enterprise-grade.

---

## 16. UI/UX Architecture & Backend Contract Addendum

### Purpose

This section defines the **official UI/UX screen architecture** for the General
Ledger module and establishes **mandatory backend contracts** required to
support these screens.

This addendum ensures:
- UI and backend remain fully aligned
- No hidden logic exists outside defined endpoints
- Auditability, reversibility, and traceability are enforced by design

---

### 16.1 Screen-to-Backend Responsibility Model

Each GL screen must:
- Represent a single business responsibility
- Rely exclusively on backend APIs
- Never calculate accounting logic on the frontend
- Display real-time status from backend only

---

### 16.2 Mandatory Screens Overview

| Screen | Purpose | CRUD | History Log |
|--------|---------|------|-------------|
| Chart of Accounts | Account structure | C/R/U/D* | Yes |
| Accounting Rules | Business → Account mapping | C/R/U | Yes |
| Manual Journal | Manual accounting entry | C/R | Yes |
| Posting Monitor | Operational posting control | R | Yes |
| Journals Inquiry | Audit & review | R | Yes |
| Ledger Inquiry | Account movements | R | No |
| Account Balances | Period balances | R | No |
| Fiscal Periods | Period control | State-based | Yes |

\* Delete is soft-delete only.

---

### 16.3 History Log (Audit Trail) – Mandatory Requirement

#### 16.3.1 General Rule

Every screen that allows **Create / Update / Delete / State Change**
must expose a **History Log tab**.

#### 16.3.2 Backend Obligation

Backend must provide:

```http
GET /api/audit/{entity-name}/{entity-id}
```

Returned data must include:

- Action type
- Changed field (if applicable)
- Old value
- New value
- User
- Timestamp

**Frontend must not infer history from current state.**

---

### 16.4 Status-Driven UI Contract

#### 16.4.1 Status Is Source of Truth

Frontend behavior must depend only on backend status values:

**Posting Status (ACC_POSTING_MST):**

| Status | UI Behavior |
|--------|-------------|
| NEW | Editable |
| READY_FOR_GL | Editable |
| POSTED | Read-only |
| ERROR | Retry allowed |

**Journal Status (GL_JOURNAL_HDR):**

| Status | UI Behavior |
|--------|-------------|
| POSTED | Read-only |
| REVERSED | Read-only |

#### 16.4.2 Backend Must

- Enforce status transitions
- Reject invalid state changes

---

### 16.5 Mandatory Backend Endpoints per Screen

#### 16.5.1 Chart of Accounts

```http
GET    /api/gl/accounts
GET    /api/gl/accounts/{id}
POST   /api/gl/accounts
PUT    /api/gl/accounts/{id}
DELETE /api/gl/accounts/{id}
```

#### 16.5.2 Accounting Rules

```http
GET    /api/gl/accounting-rules
POST   /api/gl/accounting-rules
PUT    /api/gl/accounting-rules/{id}
PATCH  /api/gl/accounting-rules/{id}/deactivate
```

#### 16.5.3 Manual Journal

```http
POST /api/gl/manual-journals
GET  /api/gl/manual-journals/{id}
POST /api/gl/manual-journals/{id}/post
POST /api/gl/manual-journals/{id}/reverse
```

#### 16.5.4 Posting Engine

```http
GET  /api/gl/postings
GET  /api/gl/postings/{id}
POST /api/gl/postings/{id}/simulate
POST /api/gl/postings/{id}/post
POST /api/gl/postings/{id}/retry
```

---

### 16.6 UI Validation Contract

Backend must enforce:

- Debit = Credit validation
- Open fiscal period validation
- Active account validation
- Active accounting rule validation

**Frontend validation is cosmetic only.**

---

### 16.7 Non-Negotiable Rules

1. **No posted record can be updated or deleted**
2. **All reversals must generate new accounting entries**
3. **No frontend-calculated balances**
4. **No silent failures** (every error must be explicit)

---

### 16.8 Backend Enforcement Requirements (Critical Addendum)

#### 16.8.1 Explicit Read-Only Enforcement

**Requirement:**
Backend must reject any `PUT` / `DELETE` request if `STATUS = POSTED`

**Error Response:**

```http
HTTP 409 Conflict

{
  "errorCode": "GL_RECORD_POSTED",
  "message": "Record is posted and cannot be modified",
  "details": ["journalId: 12345", "status: POSTED"]
}
```

---

#### 16.8.2 Centralized Permission Validation

**Requirement:**
Backend must validate:
- Role
- Permission
- Action

> ⚠️ **UI ≠ Security boundary**

Backend must enforce access control regardless of UI state.

---

#### 16.8.3 Idempotency for Posting

**Requirement:**
Posting Engine must be idempotent.

**Behavior:**
- Same posting request = same result
- No double-posting
- Return existing journal if already posted

**Implementation Pattern:**

```java
if (posting.getStatus() == PostingStatus.POSTED) {
    return PostingResult.alreadyPosted(posting.getJournalId());
}
```

---

#### 16.8.4 History on State Change

**Requirement:**
Every state change must generate an audit record:

- `POST` operation
- `REVERSE` operation
- `CLOSE PERIOD` operation

**Example Audit Record:**

```json
{
  "entityType": "JOURNAL_HDR",
  "entityId": 12345,
  "action": "POST",
  "oldStatus": "NEW",
  "newStatus": "POSTED",
  "userId": "john.doe",
  "timestamp": "2026-01-15T10:30:00"
}
```

---

#### 16.8.5 Unified Error Model

**Requirement:**
All backend errors must follow a unified contract:

```json
{
  "errorCode": "GL_PERIOD_CLOSED",
  "message": "Fiscal period is closed",
  "details": [
    "companyId: 1",
    "fiscalYear: 2026",
    "periodNo: 1"
  ],
  "timestamp": "2026-01-15T10:30:00"
}
```

**Standard Error Codes:**

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `GL_PERIOD_CLOSED` | 409 | Fiscal period is closed |
| `GL_RECORD_POSTED` | 409 | Record already posted |
| `GL_UNBALANCED_JOURNAL` | 400 | Debit ≠ Credit |
| `GL_INACTIVE_ACCOUNT` | 400 | Account is inactive |
| `GL_MISSING_RULE` | 404 | Accounting rule not found |
| `GL_DUPLICATE_POSTING` | 409 | Posting already exists |

---

#### 16.8.6 Soft Delete Rule

**Requirement:**
All delete operations must be soft deletes.

**Implementation:**

```sql
UPDATE GL_ACCOUNT
SET IS_ACTIVE = 'N',
    UPDATED_BY = :userId,
    UPDATED_AT = SYSTIMESTAMP
WHERE ACCOUNT_ID = :accountId
```

**Backend must:**
- Never execute `DELETE FROM` statements on master data
- Always set `IS_ACTIVE = 'N'`
- Filter inactive records in queries by default
- Provide explicit `includeInactive=true` flag when needed

---

### 16.9 Backend Contract Compliance Checklist

| Requirement | Status | Notes |
|-------------|--------|-------|
| Read-only enforcement on POSTED | ✅ Required | Reject PUT/DELETE |
| Permission validation | ✅ Required | Backend-enforced |
| Idempotent posting | ✅ Required | Prevent duplicates |
| State change audit | ✅ Required | All status transitions |
| Unified error model | ✅ Required | Standard JSON format |
| Soft delete only | ✅ Required | IS_ACTIVE = 'N' |

---

### 16.10 Conclusion

This addendum formally binds the UI/UX layer to the General Ledger backend logic
and guarantees:

- **Consistency:** UI behavior driven by backend state
- **Auditability:** Complete history for all changes
- **Enterprise readiness:** Proper error handling and access control
- **Data integrity:** No frontend logic, no silent failures

**All backend teams must implement these contracts before UI development begins.**

---
