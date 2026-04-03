---
name: enforce-reusability
description: "REUSABILITY & DEDUPLICATION ENFORCER — detects duplicated logic across feature modules, enforces extraction into shared utilities/components/services, validates that existing shared code is consumed instead of reinvented. Covers date formatting, status mapping, grid helpers, confirm actions, FormMapper patterns, action cell components, and all cross-cutting UI logic."
---

# Skill: enforce-reusability

## Name
`enforce-reusability`

## Description
Detects duplicated or near-duplicate logic across ERP frontend feature modules and enforces extraction into the shared layer (`shared/`, `core/`). Validates that **existing** shared utilities, components, services, and base classes are consumed instead of being reinvented per feature. This skill complements `enforce-frontend-architecture` (structural rules) and `enforce-ui-ux` (display rules) by focusing on **code reuse, consistency of implementation, and elimination of copy-paste patterns**.

## When to Use
- When creating a new feature module or page component
- When reviewing pull requests that add new utilities, helpers, or services
- When a feature implements logic that already exists in `shared/` or `core/`
- When adding a new grid config, form model, confirm action, or action cell component
- When refactoring to extract repeated patterns into shared code
- During feature validation alongside `enforce-frontend-architecture`

---

## CORE PRINCIPLE

> If the same logic appears in two or more feature modules, it belongs in `shared/` or `core/`.

Every piece of cross-cutting logic must live in exactly **one** canonical location. Feature modules consume; they do not duplicate.

---

## EXTRACTION CLASSIFICATION

Before applying checks, classify any candidate extraction:

| Category | Target Location | Naming Convention | Examples |
|----------|----------------|-------------------|----------|
| **UTIL** | `shared/utils/` | Pure function, no DI | `getFormFieldError()`, `formatDateValue()` |
| **SHARED COMPONENT** | `shared/components/` | `erp-*` selector, standalone, OnPush | `ErpFormFieldComponent`, `ErpCrudActionsCellComponent` |
| **SHARED SERVICE** | `shared/services/` | `@Injectable({ providedIn: 'root' })` | `ErpNotificationService`, `ErpDialogService` |
| **CORE SERVICE** | `core/services/` | `@Injectable({ providedIn: 'root' })` | `LookupService`, `PermissionService` |
| **BASE CLASS** | `shared/base/` | Abstract class, no selector | `ErpListComponent`, `BaseApiService` |
| **AG GRID HELPER** | `shared/ag-grid/` | Exported function or const | `createErpGridOptions()`, `ERP_DEFAULT_COL_DEF` |
| **MAPPER** | Feature-level `models/` | Static methods on model class | `FormMapper` pattern (`createEmpty`, `fromDomain`, `toCreateRequest`) |

---

## CHECK MATRIX

### Layer 1: Shared Base Class Consumption (5 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| RE.1.1 | List pages extend `ErpListComponent` | Every search/list page extends `ErpListComponent` for pagination, sort, filter state | Custom pagination state management per page |
| RE.1.2 | API services extend `BaseApiService` | Every API service extends `BaseApiService` and uses `doGet`/`doPost`/`doPut`/`doDelete` | Direct `HttpClient` injection in feature API service |
| RE.1.3 | Base class methods used, not overridden | `setPage()`, `setSort()`, `setFilters()`, `reload()` from base class — not reinvented | Custom `page` signal duplicating base state |
| RE.1.4 | Response unwrapping via base class | `unwrapResponse()` from `BaseApiService` — no per-service unwrap logic | `pipe(map(res => res.data))` in every API method |
| RE.1.5 | No duplicate base class | Only one `ErpListComponent` and one `BaseApiService` exist — no alternative base classes | `MyBaseListComponent` appearing alongside `ErpListComponent` |

### Layer 2: AG Grid Shared Helpers (6 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| RE.2.1 | Grid uses `ERP_DEFAULT_COL_DEF` | Every AG Grid binds `[defaultColDef]="defaultColDef"` where `defaultColDef = ERP_DEFAULT_COL_DEF` | Custom `defaultColDef` object missing flex/minWidth/resizable |
| RE.2.2 | Grid uses `createErpGridOptions()` | Grid options created via `createErpGridOptions()` — not manually assembled | Inline `gridOptions = { pagination: true, animateRows: true, ... }` |
| RE.2.3 | Active column uses `createActiveColumnDef()` | Active/status boolean columns use the shared helper with translated labels | Per-feature boolean column with custom cellRenderer |
| RE.2.4 | Grid modules registered once | `registerErpAgGridModules()` called — no per-component `ModuleRegistry.registerModules()` | Multiple `ModuleRegistry` registration calls |
| RE.2.5 | Grid theme from shared factory | `createAgGridTheme()` from `shared/ag-grid/agGridTableStyle.ts` used | Inline theme params per component |
| RE.2.6 | Filter config follows shared pattern | Filter/column functions follow `create*FilterOptions()` + `create*ColumnDefs()` + `create*GridOptions()` pattern | Inline column defs inside component class |

### Layer 3: Date & Number Formatting (4 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| RE.3.1 | Date formatting uses shared utility | A single `formatDateValue()` or equivalent function lives in `shared/utils/` — all grids import it | `formatDateValue()` duplicated in `journals-grid.config.ts` AND `rules-grid.config.ts` |
| RE.3.2 | DateTime formatting uses shared utility | A single `formatDateTimeValue()` or equivalent for date+time — all grids import it | `formatDateTime()` redefined per grid config |
| RE.3.3 | Number formatting consistent | Amount/number valueFormatters use a shared function or `DecimalPipe` — not inline lambdas | `(params) => params.value != null ? Number(params.value).toLocaleString() : ''` repeated in 3 files |
| RE.3.4 | Locale resolution centralized | Language/locale string (`'ar-SA'`, `'en-GB'`) resolved from a single source (e.g., `LanguageService`) — not hardcoded per formatter | `(translate.currentLang || 'en') === 'ar' ? 'ar-SA' : 'en-GB'` in every grid config |

### Layer 4: Status & Label Resolution (4 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| RE.4.1 | `resolveLabel()` is shared | Label resolution for lookup codes lives in `shared/utils/` — not reimplemented per component | `resolveLabel()` method defined inside `rule-lines-section.component.ts` AND other components |
| RE.4.2 | Status badge helper is shared | Status badge rendering (CSS class mapping, HTML generation) lives in shared utils | `getJournalStatusBadgeClass()` duplicated for each entity's statuses |
| RE.4.3 | Lookup options consumed from `LookupService` | Feature modules use `LookupService.getOptions()` — no local static arrays for dynamic data | Hardcoded `[{ value: 'DRAFT', label: 'Draft' }]` in component |
| RE.4.4 | Active filter uses shared component | Active/Inactive filtering uses `ErpActiveFilterComponent` — not a custom toggle | Per-feature filter toggle written from scratch |

### Layer 5: Confirmation & Action Patterns (5 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| RE.5.1 | Confirm dialogs via `ErpDialogService` | All confirmation flows use `ErpDialogService.confirm()` or `confirmDelete()` — not `window.confirm()` | `if (confirm('Are you sure?'))` |
| RE.5.2 | Confirm action helpers follow pattern | Confirm helpers (`confirm*Action.ts`) follow the `ConfirmActionDeps` injection pattern | Inline confirmation+API call in component method |
| RE.5.3 | Notification via shared service | All toast/snackbar messages use `ErpNotificationService` — no custom toast implementations | `alert()` or custom notification component per feature |
| RE.5.4 | Error mapping via shared service | Backend error codes mapped via `ErpErrorMapperService` — not per-feature switch statements | `switch (err.code) { case 'NOT_FOUND': ... }` in component |
| RE.5.5 | Form error resolution centralized | Form validation errors resolved via `getFormFieldError()` or `ErpUiMessageResolverService` | Per-field `*ngIf="control.errors?.required"` with hardcoded messages |

### Layer 6: Component & Template Reuse (5 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| RE.6.1 | Page header uses `ErpPageHeaderComponent` | List pages use `<erp-page-header>` — not custom header divs | `<div class="d-flex justify-content-between"><h1>...</h1></div>` per page |
| RE.6.2 | Form actions use shared component | Form pages use `<erp-form-actions>` or `<erp-action-bar>` — not inline button markup | Custom save/cancel button HTML per form |
| RE.6.3 | CRUD cell uses `ErpCrudActionsCellComponent` | Grid action columns use or extend `<erp-crud-actions-cell>` as base | Completely custom action cell without delegating to shared component |
| RE.6.4 | Empty state uses `ErpEmptyStateComponent` | Empty table/list state uses `<erp-empty-state>` — not custom empty markup | `<div *ngIf="rows.length === 0">No data</div>` |
| RE.6.5 | Form fields use `ErpFormFieldComponent` | Form label + control + error wrapper uses `<erp-form-field>` | Per-field `<label>` + `<input>` + error `<span>` without shared wrapper |

### Layer 7: FormMapper & Model Patterns (4 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| RE.7.1 | FormMapper pattern consistent | Every form model implements `createEmpty()`, `fromDomain()`, `toCreateRequest()`, `toUpdateRequest()` | Missing `fromDomain()` — direct DTO binding instead |
| RE.7.2 | No DTO-to-form logic in component | All DTO → form mapping lives in `FormMapper` — not inline in component `ngOnInit` | `this.form.patchValue({ name: dto.name, code: dto.code })` in component |
| RE.7.3 | Form model in dedicated file | Form models live in `models/*-form.model.ts` — not embedded in component files | `interface AccountForm { ... }` inside `account-form.component.ts` |
| RE.7.4 | Request DTOs separate from display | Create/Update request DTOs are distinct from display models and form models | Single model used for API request, form binding, and grid display |

### Layer 8: Cross-Cutting Infrastructure (4 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| RE.8.1 | Permission checks via shared service | `PermissionService.hasPermission()` or `erpPermission` directive — not direct token parsing | `authService.currentUserValue.permissions.includes(...)` in every component |
| RE.8.2 | Feature flags via `FeatureFlagService` | UI visibility toggles use `FeatureFlagService.isEnabled()` — not hardcoded booleans | `showModule = true;` as component property |
| RE.8.3 | Token management via `TokenStoreService` | Token read/write through `TokenStoreService` — not direct `localStorage` access | `localStorage.getItem('accessToken')` in feature code |
| RE.8.4 | Barrel exports maintained | `shared/index.ts` and `core/index.ts` re-export all public APIs — feature modules import from barrel | Direct deep imports like `import { ... } from 'src/app/shared/services/erp-notification.service'` |

---

## AUTOMATIC REJECTION TRIGGERS

Any of the following causes **immediate rejection**:

| # | Trigger | Rules |
|---|---------|-------|
| 1 | Same function body appears in 2+ feature modules | RE.3.1, RE.3.2, RE.3.3, RE.4.1 |
| 2 | Feature API service does not extend `BaseApiService` | RE.1.2 |
| 3 | List page does not extend `ErpListComponent` | RE.1.1 |
| 4 | Grid does not use `ERP_DEFAULT_COL_DEF` | RE.2.1 |
| 5 | `window.confirm()` or `window.alert()` used anywhere | RE.5.1, RE.5.3 |
| 6 | Direct `localStorage` access outside `TokenStoreService` / `LanguageService` | RE.8.3 |
| 7 | Inline `ModuleRegistry.registerModules()` in a feature component | RE.2.4 |
| 8 | Hardcoded error message string instead of translation key or `ErpErrorMapperService` | RE.5.4, RE.5.5 |

---

## CANONICAL PATTERNS (from this codebase)

### Pattern A: ErpListComponent Base Class

```typescript
// From shared/base/erp-list.component.ts — every list page extends this
export abstract class ErpListComponent {
  protected readonly gridState = signal<ErpGridState>({
    page: 0, size: 20, sort: undefined, direction: undefined, filters: []
  });

  protected abstract load(state: ErpGridState): void;
  protected initErpList(options?: ErpListInitOptions): void { ... }
  protected setPage(page: number): void { ... }
  protected setSort(sort: string | undefined, direction: ErpSortDirection | undefined): void { ... }
  protected setFilters(filters: SpecFilter[]): void { ... }
  protected reload(): void { this.load(this.gridState()); }
}

// Feature usage:
@Component({ ... })
export class UserListComponent extends ErpListComponent implements OnInit {
  ngOnInit(): void { this.initErpList(); }
  protected load(state: ErpGridState): void {
    this.facade.applyGridStateAndLoad({ page: state.page, size: state.size, ... });
  }
}
```

### Pattern B: BaseApiService Extension

```typescript
// From shared/base/base-api.service.ts
export abstract class BaseApiService {
  protected readonly http = inject(HttpClient);
  protected unwrapResponse<T>(response: unknown): T { ... }
  protected doGet<T>(url: string, params?: HttpParams): Observable<T> { ... }
  protected doPost<T>(url: string, body: unknown): Observable<T> { ... }
  protected doPut<T>(url: string, body: unknown): Observable<T> { ... }
  protected doDelete<T = void>(url: string): Observable<T> { ... }
}

// Feature usage:
@Injectable()
export class JournalApiService extends BaseApiService {
  private readonly baseUrl = `${environment.authApiUrl}/api/gl/journals`;
  getAll(params: HttpParams): Observable<PagedResponse<GlJournalHdrDto>> {
    return this.doGet(this.baseUrl, params);
  }
}
```

### Pattern C: Shared Grid Configuration Factories

```typescript
// Every grid config file follows this structure:
// <feature>-grid.config.ts

import { ERP_DEFAULT_COL_DEF, createErpGridOptions } from 'src/app/shared/ag-grid';

export function createXxxFilterOptions(translate: TranslateService): {
  fields: SpecFieldOption[]; operators: SpecOperatorOption[];
} { ... }

export function createXxxColumnDefs(
  translate: TranslateService, zone: NgZone, callbacks: { ... }
): ColDef[] { ... }

export function createXxxGridOptions(translate: TranslateService): {
  gridOptions: GridOptions; localeText: Record<string, string>;
} {
  return {
    gridOptions: createErpGridOptions({ enableRtl: translate.currentLang === 'ar' }),
    localeText: { ... }
  };
}
```

### Pattern D: Confirm Action Helper Pattern

```typescript
// From helpers/journal-confirm-actions.ts
export interface JournalConfirmActionDeps {
  dialog: ErpDialogService;
  notify: ErpNotificationService;
  auth: AuthenticationService;
  facade: JournalFacade;
}

export function confirmDeactivateJournal(
  deps: JournalConfirmActionDeps,
  journal: GlJournalHdrDto,
  onDone: () => void
): void {
  if (!deps.auth.hasPermission('PERM_GL_JOURNAL_DELETE')) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }
  deps.dialog.confirm({
    titleKey: 'COMMON.DEACTIVATE',
    messageKey: 'GL.CONFIRM_DEACTIVATE_JOURNAL',
    messageParams: { journalNo: journal.journalNo },
    type: 'warning'
  }).then((confirmed) => {
    if (confirmed) {
      deps.facade.toggleActiveJournal(journal.id, false, () => {
        deps.notify.success('MESSAGES.DEACTIVATE_SUCCESS');
        onDone();
      });
    }
  });
}
```

### Pattern E: Action Cell Delegation to Shared Component

```typescript
// Feature action cell delegates to ErpCrudActionsCellComponent for standard buttons
// and only adds domain-specific actions
@Component({
  template: `
    @if (rule; as r) {
      <erp-crud-actions-cell
        [showEdit]="true"
        [showDelete]="false"
        editPermission="PERM_GL_RULE_UPDATE"
        (editClicked)="onEditClick()"
      >
        <!-- Domain-specific extra button projected via ng-content -->
        <button type="button" ...>
          <i class="ti ti-toggle-right" aria-hidden="true"></i>
        </button>
      </erp-crud-actions-cell>
    }
  `
})
export class RuleActionsCellComponent implements ICellRendererAngularComp { ... }
```

### Pattern F: FormMapper Pattern

```typescript
// From models/<entity>-form.model.ts
export class AccountFormMapper {
  static createEmpty(): AccountFormModel { ... }
  static fromDomain(dto: AccountChartDto): AccountFormModel { ... }
  static toCreateRequest(form: AccountFormModel): CreateAccountRequest { ... }
  static toUpdateRequest(form: AccountFormModel): UpdateAccountRequest { ... }
}
```

### Pattern G: Barrel Exports

```typescript
// shared/index.ts — central export point
export * from './components';
export * from './services';
export * from './directives';
export * from './models';
export * from './base/erp-list.component';
export * from './base/base-api.service';

// Feature modules import from barrel:
import { ErpListComponent, ErpNotificationService } from 'src/app/shared';
```

### Pattern H: Form Error Resolution (Shared Utility)

```typescript
// From shared/utils/form-error-resolver.ts — single source of truth
export function getFormFieldError(
  control: AbstractControl | null,
  fieldLabel?: string
): FormErrorResult | null {
  if (!control || !control.errors || !(control.touched || control.dirty)) return null;
  const errors = control.errors;
  if (errors['required']) return { key: 'VALIDATION.REQUIRED' };
  if (errors['min']) return { key: 'VALIDATION.MIN_VALUE', params: { min: errors['min'].min } };
  // ... all validators mapped centrally
}
```

---

## DUPLICATION HOTSPOTS (known in this codebase)

The following are **known duplication patterns** to watch for during reviews:

| Hotspot | Duplicated In | Shared Target |
|---------|--------------|---------------|
| `formatDateValue()` | `journals-grid.config.ts`, `rules-grid.config.ts` | Extract to `shared/utils/date-format.utils.ts` |
| `formatDateTime()` | `rules-grid.config.ts` (local function) | Merge into shared date util |
| Locale resolution `lang === 'ar' ? 'ar-SA' : 'en-GB'` | Multiple grid configs | `LanguageService.locale` computed signal |
| `resolveLabel(options, code)` | Multiple presentational components | Extract to `shared/utils/lookup-label.utils.ts` |
| Status badge CSS class mapping | `getJournalStatusBadgeClass()` | Generalize to `shared/utils/status-badge.utils.ts` |
| Amount `toLocaleString()` lambda | `journals-grid.config.ts`, `postings-grid.config.ts` | Extract to `shared/utils/number-format.utils.ts` |
| Deactivate toggle button template | `account-actions-cell`, `rule-actions-cell` | Template fragment via `ErpCrudActionsCellComponent` extension |
| `ConfirmActionDeps` interface | `journal-confirm-actions.ts`, `user-confirm-actions.ts` | Shared base interface in `shared/models/` |

---

## SAFETY RULES FOR EXTRACTION

When extracting duplicated code into shared utilities:

1. **Behavior-preserving**: Extracted function must produce identical output for all existing call sites
2. **No feature-specific logic**: Shared code must not import from feature modules
3. **Stateless preferred**: Utility functions should be pure — input → output, no side effects
4. **Backward compatible**: Existing import paths should still work (or be updated in the same PR)
5. **Barrel-exported**: Every new shared export must be added to the appropriate `index.ts`
6. **Documented**: Extracted function must have JSDoc explaining purpose and usage example
7. **Tested**: Shared utilities should have unit tests covering edge cases (null, empty, locale variants)

---

## HOW TO RUN THIS SKILL

### Input
List of frontend feature files to validate (components, services, grid configs, form models, helpers).

### Process
1. **Inventory shared layer** — scan `shared/`, `core/`, `shared/ag-grid/` for existing utilities
2. **Scan feature modules** — identify all utilities, helpers, formatters, and patterns in feature code
3. **Detect duplication** — compare feature code against shared layer and against other features
4. **Check all 37 rules** across 8 layers
5. **Flag extraction candidates** — classify as UTIL / SHARED COMPONENT / SHARED SERVICE / BASE CLASS / AG GRID HELPER
6. **Check automatic rejection triggers** — if any triggered, REJECT immediately
7. **Generate report** with pass/fail per check and extraction recommendations

### Output Format
```
REUSABILITY & DEDUPLICATION REPORT
=========================================
Feature: <feature-name>
Date: <date>

LAYER 1: BASE CLASS CONSUMPTION       [X/5 PASS]
LAYER 2: AG GRID SHARED HELPERS       [X/6 PASS]
LAYER 3: DATE & NUMBER FORMATTING     [X/4 PASS]
LAYER 4: STATUS & LABEL RESOLUTION    [X/4 PASS]
LAYER 5: CONFIRMATION & ACTIONS       [X/5 PASS]
LAYER 6: COMPONENT & TEMPLATE REUSE   [X/5 PASS]
LAYER 7: FORMMAPPER & MODEL PATTERNS  [X/4 PASS]
LAYER 8: CROSS-CUTTING INFRASTRUCTURE [X/4 PASS]

TOTAL: XX/37 CHECKS PASSED

AUTOMATIC REJECTION: YES/NO
VIOLATIONS: [list of failed checks with file locations]

EXTRACTION CANDIDATES:
| # | Type | Source File(s) | Proposed Location | Description |
|---|------|---------------|-------------------|-------------|
| 1 | UTIL | journals-grid.config.ts, rules-grid.config.ts | shared/utils/date-format.utils.ts | formatDateValue() |

VERDICT: APPROVED / APPROVED WITH WARNINGS / REJECTED
```
