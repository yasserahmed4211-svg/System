# Phase 1 Current Implementation Analysis

## 1. Backend Findings

### 1.1 Entity model

#### ACC_POSTING_MST

| Field | Column | Constraints / notes |
|---|---|---|
| postingId | POSTING_ID | PK, sequence `ACC_POSTING_MST_SEQ` |
| branchIdFk | BRANCH_ID_FK | nullable |
| companyIdFk | COMPANY_ID_FK | NOT NULL |
| currencyCode | CURRENCY_CODE | nullable, length 3 |
| docDate | DOC_DATE | NOT NULL |
| errorMessage | ERROR_MESSAGE | nullable, length 1000 |
| finJournalIdFk | FIN_JOURNAL_ID_FK | nullable, scalar FK-like field only |
| reversalPostingIdFk | REVERSAL_POSTING_ID_FK | nullable, scalar FK-like field only |
| sourceDocId | SOURCE_DOC_ID | NOT NULL |
| sourceDocNo | SOURCE_DOC_NO | nullable, length 100 |
| sourceDocType | SOURCE_DOC_TYPE | NOT NULL, length 50 |
| sourceModule | SOURCE_MODULE | NOT NULL, length 50 |
| status | STATUS | NOT NULL, length 20 |
| totalAmount | TOTAL_AMOUNT | nullable, precision 15, scale 2 |
| detailCount | formula | computed by `SELECT COUNT(*) FROM ACC_POSTING_DTL ...` |
| createdAt / createdBy / updatedAt / updatedBy | inherited | audit fields from `AuditableEntity` |

- Relationships:
  - `@OneToMany` to `AccPostingDtl` through `postingMst`
  - No JPA relation to `GL_JOURNAL_HDR`; `finJournalIdFk` is just a scalar field
- Indexes:
  - `IDX_ACC_POSTING_MST_STATUS`
  - `IDX_ACC_POSTING_MST_COMPANY_FK`
  - `IDX_ACC_POSTING_MST_MODULE`
  - `IDX_ACC_POSTING_MST_JOURNAL_FK`
- Lifecycle/state fields actually used in code:
  - `status`
  - `errorMessage`
  - `finJournalIdFk`
  - `reversalPostingIdFk`
- Normalization:
  - `sourceModule`, `sourceDocType`, `status` uppercased in `@PrePersist` and `@PreUpdate`

#### ACC_POSTING_DTL

| Field | Column | Constraints / notes |
|---|---|---|
| postingDtlId | POSTING_DTL_ID | PK, sequence `ACC_POSTING_DTL_SEQ` |
| postingMst | POSTING_ID_FK | NOT NULL, `FK_ACC_POSTING_DTL_MST` |
| lineNo | LINE_NO | NOT NULL |
| amount | AMOUNT | NOT NULL, precision 15, scale 2 |
| businessSide | BUSINESS_SIDE | NOT NULL, length 50 |
| sign | SIGN | NOT NULL |
| description | DESCRIPTION | nullable, length 500 |
| customerIdFk | CUSTOMER_ID_FK | nullable |
| supplierIdFk | SUPPLIER_ID_FK | nullable |
| costCenterIdFk | COST_CENTER_ID_FK | nullable |
| contractIdFk | CONTRACT_ID_FK | nullable |
| itemIdFk | ITEM_ID_FK | nullable |
| createdAt / createdBy / updatedAt / updatedBy | inherited | audit fields |

- Relationships:
  - `@ManyToOne(fetch = LAZY)` to `AccPostingMst`
- Indexes:
  - `IDX_ACC_POSTING_DTL_MST_FK`
  - `IDX_ACC_POSTING_DTL_SIDE`
- Normalization:
  - `businessSide` uppercased in `@PrePersist` and `@PreUpdate`

#### GL_JOURNAL_HDR

| Field | Column | Constraints / notes |
|---|---|---|
| id | ID_PK | PK, sequence `GL_JOURNAL_HDR_SEQ` |
| journalNo | JOURNAL_NO | NOT NULL, UNIQUE, length 50 |
| journalDate | JOURNAL_DATE | NOT NULL |
| journalTypeIdFk | JOURNAL_TYPE_ID_FK | NOT NULL, length 50 |
| statusIdFk | STATUS_ID_FK | NOT NULL, length 50 |
| description | DESCRIPTION | nullable, length 500 |
| sourceModuleIdFk | SOURCE_MODULE_ID_FK | nullable, length 50 |
| sourceDocTypeId | SOURCE_DOC_TYPE_ID | nullable, length 50 |
| sourceDocIdFk | SOURCE_DOC_ID_FK | nullable |
| sourcePostingIdFk | SOURCE_POSTING_ID_FK | nullable, scalar FK-like field only |
| totalDebit | TOTAL_DEBIT | NOT NULL, precision 18, scale 2, default `0` |
| totalCredit | TOTAL_CREDIT | NOT NULL, precision 18, scale 2, default `0` |
| activeFl | ACTIVE_FL | NOT NULL, boolean-number converter, default `true` |
| lines | relation | `@OneToMany` to `GlJournalLine` |
| createdAt / createdBy / updatedAt / updatedBy | inherited | audit fields |

- Relationships:
  - `@OneToMany` to `GlJournalLine`
  - No JPA relation back to posting; `sourcePostingIdFk` is scalar only
- DB constraints confirmed in `V7__gl_journal_tables.sql`:
  - PK `GL_JOURNAL_HDR_PK`
  - UK `UK_GL_JOURNAL_NO`
  - Check `CK_GL_JOURNAL_ACTIVE CHECK (ACTIVE_FL IN (0,1))`
- Indexes:
  - `IDX_GL_JOURNAL_HDR_DATE`
  - `IDX_GL_JOURNAL_HDR_TYPE_FK`
  - `IDX_GL_JOURNAL_HDR_STATUS_FK`
  - `IDX_GL_JOURNAL_HDR_SRC_MOD`
  - `IDX_GL_JOURNAL_HDR_ACTIVE`
  - `IDX_GL_JOURNAL_HDR_POST_FK`
- Lifecycle/state fields actually used in code:
  - `statusIdFk`
  - `journalTypeIdFk`
  - `activeFl`
  - `sourcePostingIdFk`

#### GL_JOURNAL_LINE

| Field | Column | Constraints / notes |
|---|---|---|
| id | ID_PK | PK, sequence `GL_JOURNAL_LINE_SEQ` |
| journalHdr | JOURNAL_ID_FK | NOT NULL, FK to `GL_JOURNAL_HDR(ID_PK)` |
| lineNo | LINE_NO | NOT NULL |
| accountIdFk | ACCOUNT_ID_FK | NOT NULL |
| debitAmount | DEBIT_AMOUNT | nullable, precision 18, scale 2 |
| creditAmount | CREDIT_AMOUNT | nullable, precision 18, scale 2 |
| customerIdFk | CUSTOMER_ID_FK | nullable |
| supplierIdFk | SUPPLIER_ID_FK | nullable |
| costCenterIdFk | COST_CENTER_ID_FK | nullable |
| description | DESCRIPTION | nullable, length 500 |
| createdAt / createdBy / updatedAt / updatedBy | inherited | audit fields |

- Relationships:
  - `@ManyToOne(fetch = LAZY)` to `GlJournalHdr`
- DB constraints confirmed in `V7__gl_journal_tables.sql`:
  - PK `GL_JOURNAL_LINE_PK`
  - FK `GL_JOURNAL_LINE_HDR_FK`
- Indexes:
  - `IDX_GL_JOURNAL_LINE_HDR_FK`
  - `IDX_GL_JOURNAL_LINE_ACCT_FK`
  - `IDX_GL_JOURNAL_LINE_CUST_FK`
  - `IDX_GL_JOURNAL_LINE_SUPP_FK`
  - `IDX_GL_JOURNAL_LINE_CC_FK`

#### ACC_RULE_HDR

| Field | Column | Constraints / notes |
|---|---|---|
| ruleId | RULE_ID | PK, sequence `ACC_RULE_HDR_SEQ` |
| companyIdFk | COMPANY_ID_FK | NOT NULL |
| sourceModule | SOURCE_MODULE | NOT NULL, length 50 |
| sourceDocType | SOURCE_DOC_TYPE | NOT NULL, length 50 |
| isActive | IS_ACTIVE | NOT NULL, boolean-number converter, default `true` |
| lines | relation | `@OneToMany` to `AccRuleLine` |
| createdAt / createdBy / updatedAt / updatedBy | inherited | audit fields |

- Relationships:
  - `@OneToMany(mappedBy = "ruleHdr")` to `AccRuleLine`
- JPA constraints:
  - unique constraint `ACC_RULE_HDR_UK` on `(COMPANY_ID_FK, SOURCE_MODULE, SOURCE_DOC_TYPE)`
- Indexes:
  - `IDX_ACC_RULE_HDR_ACTIVE`
  - `IDX_ACC_RULE_HDR_COMPANY_FK`
  - `IDX_ACC_RULE_HDR_MODULE`
- Notes:
  - No create-table migration was found in `backend/db-migration` for `ACC_RULE_HDR`

#### ACC_RULE_LINE

| Field | Column | Constraints / notes |
|---|---|---|
| ruleLineId | RULE_LINE_ID | PK, sequence `ACC_RULE_LINE_SEQ` |
| ruleHdr | RULE_ID_FK | NOT NULL |
| accountIdFk | ACCOUNT_ID_FK | NOT NULL |
| entrySide | ENTRY_SIDE | NOT NULL, length 10 |
| priority | PRIORITY | NOT NULL |
| amountSourceType | AMOUNT_SOURCE_TYPE | NOT NULL, length 20 |
| amountSourceValue | AMOUNT_SOURCE_VALUE | nullable, precision 18, scale 6 |
| paymentTypeCode | PAYMENT_TYPE_CODE | nullable, length 20 |
| entityType | ENTITY_TYPE | nullable, length 20 |
| createdAt / createdBy / updatedAt / updatedBy | inherited | audit fields |

- Relationships:
  - `@ManyToOne(fetch = LAZY)` to `AccRuleHdr`
- Indexes:
  - `IDX_ACC_RULE_LINE_HDR_FK`
  - `IDX_ACC_RULE_LINE_ACCOUNT_FK`
- Notes:
  - No create-table migration was found in `backend/db-migration` for `ACC_RULE_LINE`
  - `test-data-posting.sql` inserts a `BUSINESS_SIDE` column into `ACC_RULE_LINE`, but the runtime entity and engine do not map or use that field

### 1.2 Posting logic

- No production code was found in `erp-finance-gl` that creates `ACC_POSTING_MST` or `ACC_POSTING_DTL` records.
- Production code only reads postings in `AccPostingService` and updates postings in `JournalGenerationService`.
- Actual journal generation from posting is:
  1. Load posting + details.
  2. Require `status = READY_FOR_GL`.
  3. Require `finJournalIdFk == null`.
  4. Require `details` not empty.
  5. Build `PostingRequest` from posting header fields:
     - `companyIdFk`
     - `sourceModule`
     - `sourceDocType`
     - `totalAmount`
     - `docDate -> journalDate`
     - `sourceDocId`
     - generated description
  6. Build `entityMap` from the first detail line containing `customerIdFk` and/or `supplierIdFk`.
  7. Delegate to `PostingEngineService.executeForPosting`.

### 1.3 Rule application and amount calculation

- Rule lookup is by `(companyIdFk, sourceModule, sourceDocType)` and `isActive = true`.
- Rule lines are sorted by ascending `priority`.
- Amount derivation is completely rule-driven:
  - `TOTAL` => full `request.totalAmount`
  - `FIXED` => `amountSourceValue`, must be `> 0`
  - `PERCENT` => `totalAmount * amountSourceValue`, rounded to 2 decimals
  - `REMAINING` => `totalAmount - cumulative(side)` on the same entry side
- Debit/Credit derivation:
  - `ENTRY_SIDE = DEBIT` => `debitAmount = lineAmount`
  - any other side path used by code => `creditAmount = lineAmount`
- Balance validation for generated journal:
  - engine builds journal lines, recalculates totals, then requires `totalDebit == totalCredit`

### 1.4 What posting detail lines actually influence

- Used:
  - `details` existence check
  - first `customerIdFk`
  - first `supplierIdFk`
- Not used by generation logic:
  - `AccPostingDtl.amount`
  - `AccPostingDtl.sign`
  - `AccPostingDtl.businessSide`
  - `costCenterIdFk`
  - `contractIdFk`
  - `itemIdFk`
- Result:
  - generated journal amounts come from rule configuration plus posting header `totalAmount`, not from posting detail lines

### 1.5 Journal generation mapping and lifecycle

- Generated journal header values:
  - `journalTypeIdFk = AUTOMATIC`
  - `statusIdFk = DRAFT`
  - `sourceModuleIdFk = posting.sourceModule`
  - `sourceDocTypeId = posting.sourceDocType`
  - `sourceDocIdFk = posting.sourceDocId`
  - `sourcePostingIdFk = postingId` when called through posting bridge
- Generated journal line values:
  - `accountIdFk` from rule line
  - `debitAmount` / `creditAmount` from `ENTRY_SIDE`
  - `customerIdFk` only when `ENTITY_TYPE = CUSTOMER` and entity map contains it
  - `supplierIdFk` only when `ENTITY_TYPE = VENDOR` and entity map contains it
  - `description` generated from rule priority + account + amount type
- After successful generation from posting bridge:
  - posting `finJournalIdFk` is set to created journal id
  - posting `status` is set to `JOURNAL_CREATED`
  - posting `errorMessage` is cleared
- Later lifecycle synchronization now exists in `GlJournalService`:
  - journal `post()` updates linked posting `JOURNAL_CREATED -> POSTED`
  - journal `reverse()` updates linked posting `POSTED -> REVERSED`

### 1.6 Idempotency and duplicate prevention

- Posting bridge path (`/api/gl/postings/{id}/generate-journal`):
  - idempotency is enforced only by `posting.finJournalIdFk != null`
  - behavior on duplicate is success response with the existing linked journal returned
- Preview path uses the same duplicate check and also throws `409 CONFLICT`
- Direct engine path (`/api/gl/posting/execute`):
  - no idempotency check was found
  - `sourcePostingIdFk` remains `null` when no posting id is supplied
  - endpoint is now deprecated and restricted to `SYSTEM_ADMIN`

### 1.7 Validation layer actually present

#### Posting bridge / engine validations

- Posting exists
- Posting status must be `READY_FOR_GL`
- Posting must not already have `finJournalIdFk`
- Posting must have details
- `totalAmount > 0`
- Active rule must exist
- Rule must have lines
- Every referenced account must exist and be active
- Generated journal must balance
- `REMAINING` amount must not go negative
- Unknown amount source type is rejected

#### Journal validations

- Create/update requires at least one line
- Each line must satisfy XOR: exactly one positive debit or positive credit
- All referenced accounts must exist
- All referenced accounts must be active
- All referenced accounts must be leaf accounts
- Sum debit must equal sum credit
- Update requires journal status `DRAFT`
- Post requires journal status `APPROVED`
- Reverse requires journal status `POSTED`
- Cancel rejects already `POSTED`, `CANCELLED`, or `REVERSED`
- Deactivate rejects `POSTED`
- Automatic update path allows only header fields to change in service, not lines

### 1.8 API layer

#### Posting endpoints

| Endpoint | Input | Output | Actual behavior | Error handling |
|---|---|---|---|---|
| `POST /api/gl/postings/search` | `AccPostingSearchContractRequest` -> common search request (`filters`, `sorts`, `page`, `size`) | paged `AccPostingMstResponse` | search by `status`, `sourceModule`, `sourceDocType`, `companyIdFk`, `docDate` | localized exceptions from service/repository lookup path |
| `GET /api/gl/postings/{postingId}` | path id | `AccPostingMstResponse` with details | loads posting + details | `GL_POSTING_NOT_FOUND` |
| `GET /api/gl/postings/{postingId}/preview-journal` | path id | `JournalPreviewResponse` | validates posting, simulates rule engine, no DB write | `GL_POSTING_NOT_FOUND`, `GL_POSTING_NOT_READY`, `GL_POSTING_JOURNAL_ALREADY_EXISTS`, `GL_POSTING_NO_DETAILS_FOR_JOURNAL`, rule/account/balance errors |
| `POST /api/gl/postings/{postingId}/generate-journal` | path id | `PostingResponse` | validates posting, creates automatic draft journal, then sets posting to `JOURNAL_CREATED` and links journal id; duplicate calls return the existing journal | same errors as preview plus engine errors; on engine failure posting becomes `ERROR` with `errorMessage = messageKey` |

#### Direct posting-engine endpoint

| Endpoint | Input | Output | Actual behavior | Error handling |
|---|---|---|---|---|
| `POST /api/gl/posting/execute` | `PostingRequest` | `PostingResponse` | deprecated internal/admin endpoint; creates automatic draft journal directly from rule engine without `ACC_POSTING_MST` bridge | `GL_POSTING_ZERO_AMOUNT`, `GL_POSTING_RULE_NOT_FOUND`, `GL_POSTING_NO_RULE_LINES`, `GL_POSTING_ACCOUNT_INVALID`, `GL_POSTING_UNBALANCED`, amount-type errors |

#### Journal endpoints

| Endpoint | Input | Output | Actual behavior | Error handling |
|---|---|---|---|---|
| `POST /api/gl/journals/search` | `GlJournalSearchContractRequest` | paged `GlJournalHdrResponse` | search journals | localized exceptions if search contract invalid upstream |
| `GET /api/gl/journals/{journalId}` | path id | `GlJournalHdrResponse` with lines | loads journal + lines | `GL_JOURNAL_NOT_FOUND` |
| `PUT /api/gl/journals/{journalId}` | `GlJournalHdrUpdateRequest` | `GlJournalHdrResponse` | updates draft journal; automatic journals only update date/description | `GL_JOURNAL_NOT_FOUND`, `GL_JOURNAL_NOT_DRAFT`, `GL_JOURNAL_TYPE_IMMUTABLE`, `GL_JOURNAL_NOT_BALANCED`, line/account errors |
| `PUT /api/gl/journals/{journalId}/toggle-active` | `ToggleActiveRequest { active }` | `GlJournalHdrResponse` | toggles active flag unless journal is `POSTED` | `GL_JOURNAL_NOT_FOUND`, `GL_JOURNAL_POSTED_IMMUTABLE` |
| `PATCH /api/gl/journals/{journalId}/approve` | path id | `GlJournalHdrResponse` | `DRAFT -> APPROVED` | `GL_JOURNAL_NOT_FOUND`, `GL_JOURNAL_NOT_DRAFT`, `GL_JOURNAL_NOT_BALANCED` |
| `PATCH /api/gl/journals/{journalId}/post` | path id | `GlJournalHdrResponse` | `APPROVED -> POSTED`; also syncs linked posting `JOURNAL_CREATED -> POSTED`; no ledger write found | `GL_JOURNAL_NOT_FOUND`, `GL_JOURNAL_NOT_APPROVED`, `GL_JOURNAL_NOT_BALANCED` |
| `PATCH /api/gl/journals/{journalId}/reverse` | path id | reversal `GlJournalHdrResponse` | marks original `REVERSED`, creates a new reversal journal directly as `POSTED`, and syncs linked posting `POSTED -> REVERSED` | `GL_JOURNAL_NOT_FOUND`, `GL_JOURNAL_REVERSE_REQUIRES_POSTED` |
| `PATCH /api/gl/journals/{journalId}/cancel` | path id | `GlJournalHdrResponse` | sets status to `CANCELLED` unless final/post-based restrictions block it | `GL_JOURNAL_NOT_FOUND`, `GL_JOURNAL_POSTED_IMMUTABLE`, `GL_JOURNAL_ALREADY_FINALIZED` |
| `POST /api/gl/journals/manual` | `GlJournalManualCreateRequest` | `GlJournalHdrResponse` | creates a `MANUAL` draft journal, null source fields, null entity dimensions | journal validation errors |
| `PUT /api/gl/journals/manual/{journalId}` | `GlJournalManualUpdateRequest` | `GlJournalHdrResponse` | updates manual draft journal only | journal validation errors |

## 2. Frontend Findings

### 2.1 Routes and screens actually present

- Postings:
  - `/finance/gl/postings` -> search/list page
  - `/finance/gl/postings/view/:id` -> read-only posting detail page
- Journals:
  - `/finance/gl/journals` -> search/list page
  - `/finance/gl/journals/create` -> manual create form
  - `/finance/gl/journals/edit/:id` -> shared edit form
  - `/finance/gl/journals/view/:id` -> shared view form
- No posting create/edit screen was found in frontend.

### 2.2 Posting screen actual behavior

#### Search/list page

- Actions per row:
  - View: always visible
  - Generate Journal: visible only when `posting.status === READY_FOR_GL`
  - View Journal: visible when `posting.status === JOURNAL_CREATED` or `posting.status === POSTED`, and `finJournalIdFk` exists
- Generate Journal button on grid row is not disabled; it is only conditionally shown/hidden.
- Advanced filters exist for:
  - `status`
  - `sourceModule`
  - `sourceDocType`
  - `sourceDocNo`

#### Posting detail page

- Shows posting header, linked journal, error message, and detail lines.
- Generate Journal button:
  - visible only when posting status is `READY_FOR_GL`
  - disabled only while `facade.saving()` is true
- View Journal button:
  - visible when posting status is `JOURNAL_CREATED` or `POSTED`, and `finJournalIdFk` exists
- Detail lines render `amount`, `businessSide`, and `sign`, but generation UI does not use them for preview or posting.

### 2.3 Confirmation UX

- Generate Journal is not immediate.
- Flow is:
  1. call preview endpoint
  2. open preview modal
  3. user confirms from modal
  4. then call generate endpoint
- Preview modal includes:
  - posting summary
  - rule id
  - source module / doc type
  - preview journal lines
  - total debit and total credit
  - balanced / not balanced banner
  - warning text
- Confirm Generate button is disabled when `preview.isBalanced === false`.
- No inline page preview exists; preview is modal-only.

### 2.4 Journal screen actual behavior

#### Journals search page

- Create Manual Journal button exists.
- Row actions depend on journal status:
  - `DRAFT` -> View, Edit, Approve, Cancel, Deactivate
  - `APPROVED` -> View, Post, Cancel
  - `POSTED` -> View, Reverse
  - `REVERSED` / `CANCELLED` -> View only

#### Journal form behavior

- One shared component handles create, edit, and view.
- Manual create mode:
  - starts with one empty line
  - journal type control is disabled and preset to `MANUAL`
  - source fields are disabled
  - add/remove line is allowed
- Edit/view mode:
  - loads journal by id
  - if non-draft or view mode, form is fully disabled
  - if draft automatic journal, form is fully disabled and then only `journalDate` and `description` are re-enabled
  - automatic journal lines are shown but not editable
- Client-side line behavior:
  - entering debit clears credit on same line
  - entering credit clears debit on same line
  - totals are recomputed in component
  - save is blocked if form invalid or journal is not balanced

### 2.5 State management actually used

- Both posting and journal features use:
  - Angular `signal()` for local feature state
  - `computed()` for public state exposure
  - API observables for requests
- UI status reactions are implemented via getters/computed values, not hardcoded template-only logic:
  - posting: `isReadyForGl`, `isJournalCreated`, `isPosted`
  - journal: `isDraft`, `isApproved`, `isPosted`, `isAutomatic`, `isEditable`
- Search state is partly computed, but not fully minimal:
  - `lastSearchRequestSignal` is present
  - separate `filtersSignal` and `sortSignal` are also kept

### 2.6 Backend/frontend alignment findings

- Aligned:
  - preview endpoint exists in both backend and frontend
  - generate endpoint exists in both backend and frontend
  - journal lifecycle endpoints exist in both backend and frontend
  - automatic journal edit restriction exists in both backend and frontend
- Misaligned:
  - posting `errorMessage` stores backend message keys, but posting detail view renders it directly without translation pipe
  - frontend list-page row actions still need confirmation against the newly introduced `JOURNAL_CREATED` status if the grid action logic is separate from the detail page

## 3. Actual Flow

1. No posting creation entry point was found in this module or frontend.
2. Existing postings are listed and viewed through `/api/gl/postings` and the postings screens.
3. User can generate journal only from postings with status `READY_FOR_GL`.
4. Frontend first calls preview.
5. Backend preview reloads posting, checks `READY_FOR_GL`, checks no linked journal, checks details exist, then runs the rule engine without saving.
6. User confirms in preview modal.
7. Backend generate repeats the same validations.
8. `PostingEngineService` creates an automatic journal with status `DRAFT`.
9. `JournalGenerationService` immediately updates the posting record to `JOURNAL_CREATED` and sets `finJournalIdFk`.
10. Duplicate generate requests return the already linked journal instead of failing the create flow.
11. Journal approval and journal post happen later from journal screens.
12. `GlJournalService.approve()` changes only journal status: `DRAFT -> APPROVED`.
13. `GlJournalService.post()` changes journal status `APPROVED -> POSTED` and syncs posting status `JOURNAL_CREATED -> POSTED`.
14. `GlJournalService.reverse()` changes original journal status to `REVERSED`, creates reversal journal, and syncs posting status `POSTED -> REVERSED`.
15. No ledger table write, balance rollup, or account movement persistence was found during `post()`.

## 4. Gaps

1. Posting lifecycle is only partially synchronized.
  - The main semantic gap was fixed by introducing `JOURNAL_CREATED`.
  - Posting status is now synchronized on journal `post()` and `reverse()`.
  - No posting-side synchronization was found for journal `cancel()` or `approve()`, which may be acceptable or may need explicit business clarification.

2. Posting creation is missing from this module.
   - No production service/controller/UI was found that creates `ACC_POSTING_MST` and `ACC_POSTING_DTL`.

3. Posting detail lines do not drive journal generation.
   - `amount`, `sign`, and `businessSide` from `ACC_POSTING_DTL` are displayed in UI but ignored by the generation engine.
   - Journal amounts are derived only from rule lines plus posting header `totalAmount`.

4. The direct engine endpoint still bypasses the posting workflow by design.
  - `/api/gl/posting/execute` creates automatic journals without `ACC_POSTING_MST` state checks.
  - It is now deprecated and restricted to `SYSTEM_ADMIN`, which reduces operational risk.
  - The incorrect `sourcePostingIdFk = ruleId` fallback was fixed; unlinked direct calls now keep `sourcePostingIdFk = null`.

5. Journal posting is status-only.
  - `GlJournalService.post()` validates, flips journal status, and syncs posting status.
   - No ledger persistence or account-balance update was found.

6. ~~Permission model is not aligned.~~ **RESOLVED.**
   - `V11__consolidate_posting_permissions.sql` migrates role mappings from `PERM_GL_POSTING_GENERATE_JOURNAL` → `PERM_GL_POSTING_CREATE` and deletes the old permission.
   - Backend `@PreAuthorize` uses `SecurityPermissions.GL_POSTING_CREATE` (`PERM_GL_POSTING_CREATE`).
   - Frontend uses `PERM_GL_POSTING_CREATE` in all `erpPermission` directives and `hasPermission()` checks.
   - No Java constant for the old `GENERATE_JOURNAL` permission exists.

7. Rule schema/runtime mismatch exists.
   - `test-data-posting.sql` inserts `BUSINESS_SIDE` into `ACC_RULE_LINE`.
   - Runtime entity `AccRuleLine` and posting engine do not map or use it.

8. Posting/rule DDL is incomplete in `db-migration` from what was found.
   - `GL_JOURNAL_*` DDL is present.
   - Create-table migrations for `ACC_POSTING_*` and `ACC_RULE_*` were not found in `backend/db-migration`.

9. Posting error display is not localized in the detail page.
   - backend saves `errorMessage = e.getMessageKey()`
   - frontend posting detail page renders `p.errorMessage` directly

## 5. Final Verdict

- Completion status: **80%**
  - Core journal entities, rule-driven journal generation, preview UX, journal CRUD, approval/post/reverse/cancel APIs, and posting/journal screens are implemented.
  - The most critical lifecycle mismatch was fixed by introducing `JOURNAL_CREATED`, returning existing journals on duplicate generate, and syncing posting status on journal post/reverse.
  - Phase 1 is still **not fully complete** because posting creation ownership and ledger-posting behavior remain open.

- Missing parts that are real in code:
  - production posting creation flow
  - full lifecycle policy clarity between posting and journal, especially around cancel/approve semantics
  - use of posting detail semantics (`amount`, `sign`, `businessSide`) or explicit removal of those semantics from the flow
  - real ledger posting logic during journal post
  - missing posting/rule table DDL in migrations found

- Risk level: **MEDIUM**
  - The highest remaining risk is financial traceability: journal `post()` does not persist any ledger effect in code found.
  - Authorization drift is resolved: `V11` migration removed `PERM_GL_POSTING_GENERATE_JOURNAL` and all code uses `PERM_GL_POSTING_CREATE`.

- Exact next actions to close Phase 1:
  1. ~~Align permission enforcement~~ — **DONE** via `V11__consolidate_posting_permissions.sql`.
  2. Define and implement explicit posting behavior for journal `cancel()` and confirm whether `approve()` should affect posting state.
  3. Implement or expose the real posting creation flow for `ACC_POSTING_MST` and `ACC_POSTING_DTL` if Phase 1 includes posting generation inside this module.
  4. Implement real ledger/account-balance persistence during journal `post()` if that belongs to Phase 1 scope.
  5. Decide the canonical accounting source. Either use posting detail `amount/sign/businessSide` in generation, or remove those fields from the Phase 1 contract and UI expectations.
  6. If journal `post()` remains a workflow-only operation, rename it or document that distinction explicitly to avoid financial-process ambiguity.
  6. Remove or harden the direct `/api/gl/posting/execute` path so it cannot bypass the posting bridge semantics.
  7. ~~Align permission usage~~ — **DONE** via `V11__consolidate_posting_permissions.sql`.
  8. Add/locate authoritative Flyway DDL for `ACC_POSTING_*` and `ACC_RULE_*` tables so schema and runtime mappings are traceable.