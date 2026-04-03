---
name: enforce-ui-ux
description: "UI/UX ENFORCER — validates data representation, display model mapping, feedback patterns, column readability, tooltip enforcement, bilingual i18n (AR/EN), and user-facing consistency. Ensures no raw IDs, ENUMs, or timestamps reach the UI. Enforces human-readable labels, status badges, date formatting, action feedback, column sizing, tooltip on truncation, and full Arabic/English support across all components."
---

# Skill: enforce-ui-ux

## Name
`enforce-ui-ux`

## Description
Validates that all frontend UI renders human-readable, consistent, and user-friendly data. Enforces display model mapping, status visibility, date formatting, action feedback, layout consistency, **column readability (sizing/tooltips)**, and **full bilingual support (Arabic + English)**. This skill complements `enforce-design-system` (CSS tokens) and `enforce-frontend-architecture` (code structure) by focusing on **what the user sees and experiences**.

## When to Use
- When creating or reviewing any component that displays backend data
- When binding grid columns, dropdown options, or form fields
- When adding action buttons or status indicators
- When reviewing pull requests that affect user-facing rendering
- When creating or modifying AG Grid column definitions
- When adding or reviewing translation keys (en.json / ar.json)
- During feature validation alongside `enforce-frontend-architecture`

---

## CORE PRINCIPLE

> If the user must think to understand a displayed value, the implementation is defective.

The UI is for **end users**, not developers. No database internals, raw codes, or system values may be visible.

---

## CHECK MATRIX

### Layer 1: Data Representation (7 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| UX.1.1 | No raw IDs displayed | IDs resolved to `display` label via `LookupItem` or dedicated mapper | Grid column shows `82` instead of `الصندوق الرئيسي (82)` |
| UX.1.2 | No raw ENUM values | ENUMs mapped to localized labels via `LookupSelectOption` or translation keys | UI shows `ACTIVE` instead of `نشط` |
| UX.1.3 | No raw timestamps | Dates formatted via `DatePipe` or equivalent with locale-aware pattern | UI shows `2024-04-01T10:30:00Z` |
| UX.1.4 | Booleans display as labels | `true`/`false` shown as localized text (e.g., نشط/غير نشط) or badge with color | Raw `true`/`false` in UI |
| UX.1.5 | Amounts include formatting | Numbers formatted with locale-appropriate separators and currency where applicable | Raw `1500.5` |
| UX.1.6 | Codes show name + code | Reference fields display `Name (Code)` pattern when both exist | Code-only display like `ACC-001` |
| UX.1.7 | Null/empty values have fallback | Missing values display `—` or localized `N/A`, never blank cell or `null`/`undefined` | Empty cell or literal `null` |

### Layer 2: Display Model Mapping (6 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| UX.2.1 | Grid binds display fields | AG Grid `ColDef.field` points to display-ready values, not raw DTO fields | `{ field: 'statusIdFk' }` instead of resolved label |
| UX.2.2 | Dropdowns use label/value | Dropdowns bind `LookupSelectOption` with `label` (display) and `value` (stored) | Dropdown shows raw codes |
| UX.2.3 | Lookup fields resolve display | `ErpLookupFieldComponent` resolves `LookupItem.display` — forms store ID, show label | Form input shows numeric ID |
| UX.2.4 | Status uses resolveLabel | Status columns use `resolveLabel(options, code)` or equivalent mapper | Raw status code in template |
| UX.2.5 | FormMapper separates concerns | `FormMapper.fromDomain()` maps DTO → form model; `toCreateRequest()`/`toUpdateRequest()` maps back | Direct DTO binding in template |
| UX.2.6 | Value formatters on grid | `valueFormatter` or `cellRenderer` used for dates, amounts, and booleans in AG Grid | Raw values in grid cells |

### Layer 3: Status & State Visibility (4 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| UX.3.1 | Status has visual indicator | Status displayed with badge, color, or icon — not plain text alone | Plain text `Draft` without visual cue |
| UX.3.2 | Active/Inactive uses `createActiveColumnDef` | Active column in grids uses shared `createActiveColumnDef()` helper | Custom boolean column |
| UX.3.3 | Loading state visible | Operations show spinner/skeleton during API calls | No loading indicator — UI freezes |
| UX.3.4 | Empty state communicated | Tables show `COMMON.NO_DATA` message; forms show placeholder guidance | Blank area with no context |

### Layer 4: Action Feedback (5 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| UX.4.1 | Every action has loading state | Button disabled or spinner shown during API call | Button clickable during in-flight request |
| UX.4.2 | Success notification | `notificationService.success()` with translation key after successful operation | Silent success — user unsure if action worked |
| UX.4.3 | Error notification | `notificationService.error()` with mapped error via `ErpErrorMapperService` | Raw HTTP error or no feedback |
| UX.4.4 | Confirmation before destructive action | Delete/deactivate uses confirmation dialog from confirm-actions helper | Direct delete without confirmation |
| UX.4.5 | Form validation feedback | Invalid fields show per-field error messages; `markAllAsTouched()` on submit | Save button disabled without explanation |

### Layer 5: Date & Number Formatting (4 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| UX.5.1 | Dates use locale format | Dates rendered as `dd MMM yyyy` or locale-appropriate via `DatePipe` | ISO-8601 string in UI |
| UX.5.2 | Date-time includes time | Timestamps show `dd MMM yyyy - hh:mm a` or equivalent | Date without time when time matters |
| UX.5.3 | Numbers use locale separators | `DecimalPipe` or equivalent for amounts: `1,500.50` not `1500.5` | Unformatted raw numbers |
| UX.5.4 | Grid date columns use valueFormatter | `valueFormatter` on date columns applies consistent formatting | Raw date string in grid |

### Layer 6: Layout & Consistency (5 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| UX.6.1 | Action bar is sticky | Page-level action buttons remain visible when scrolling | Buttons scroll out of view |
| UX.6.2 | Primary/secondary button hierarchy | Save = primary (filled), Cancel = secondary (outlined) — consistent across all forms | Inconsistent button styling per page |
| UX.6.3 | Grid columns have meaningful headers | All `headerName` values use `translate.instant()` with descriptive keys | Technical field names as headers |
| UX.6.4 | Responsive layout | No `overflow: hidden` on critical containers; content adapts to viewport | Fixed dimensions clip content |
| UX.6.5 | Consistent component patterns | Tables, forms, modals follow the same layout across all features | Custom one-off layouts per feature |

### Layer 7: Column Readability & Sizing (5 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|----------|
| UX.7.1 | Columns use `flex` or adequate `minWidth` | Every `ColDef` uses `flex` (preferred) or `minWidth >= 120` — no fixed narrow `width` | `{ width: 80 }` clips content |
| UX.7.2 | Default col def applied | Grid uses shared `ERP_DEFAULT_COL_DEF` (`flex: 1, minWidth: 150, resizable: true`) | Custom defaults missing `resizable` or `minWidth` |
| UX.7.3 | Long-text columns have tooltip | Columns likely to contain long text set `tooltipField` or `tooltipValueGetter` | Description column shows `...` with no tooltip |
| UX.7.4 | No meaningful data hidden | No column uses `overflow: hidden` or `text-overflow: ellipsis` without a tooltip mechanism | Ellipsis without recovery path |
| UX.7.5 | Column resizable by user | `resizable: true` on default or per-column `ColDef` | User cannot widen narrow column |

### Layer 8: Tooltip & Truncation (4 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|----------|
| UX.8.1 | Truncated text shows tooltip | Any cell/element with `text-overflow: ellipsis` has `title`, `ngbTooltip`, or AG Grid `tooltipField` | `...` with no way to see full value |
| UX.8.2 | Codes with labels show full info | Short display like `ACC-001` has tooltip showing `ACC-001 - Cash Account` | Code-only with no context on hover |
| UX.8.3 | Tooltip content is translated | Tooltip text uses `translate.instant()` or `\| translate` pipe — never hardcoded strings | English-only tooltip in Arabic UI |
| UX.8.4 | Grid enables built-in tooltips | `ERP_DEFAULT_COL_DEF` or grid-level config enables `tooltipShowDelay` for usability | Tooltip requires pixel-perfect hover |

### Layer 9: Bilingual / i18n Completeness (6 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|----------|
| UX.9.1 | All labels use translation keys | Every user-facing string uses `translate.instant()` or `\| translate` — zero hardcoded text | `<label>Account Name</label>` without pipe |
| UX.9.2 | Keys exist in both locale files | Every key used in templates/TS exists in both `assets/i18n/en.json` AND `assets/i18n/ar.json` | Key in `en.json` missing from `ar.json` |
| UX.9.3 | Validation messages translated | All form error messages use translation keys via `ErpUiMessageResolverService` or `\| translate` | Hardcoded `"This field is required"` |
| UX.9.4 | Buttons & actions translated | Every `<button>` label uses translation key | `<button>Save</button>` without translate |
| UX.9.5 | Grid rebuilds on language change | `translate.onLangChange.subscribe()` triggers column/filter/grid option rebuild | Grid headers stay in old language after switch |
| UX.9.6 | RTL layout supported | Grid sets `enableRtl` based on current language; layout adapts for Arabic | Fixed LTR layout in Arabic mode |

---

## AUTOMATIC REJECTION TRIGGERS

Any of the following causes **immediate rejection**:

| # | Trigger | Rules |
|---|---------|-------|
| 1 | Raw database ID displayed as a column value or label | UX.1.1 |
| 2 | Raw ENUM string shown to user (e.g., `POSTED`, `ACTIVE`) | UX.1.2 |
| 3 | ISO timestamp displayed without formatting | UX.1.3 |
| 4 | No loading indicator during API operations | UX.4.1 |
| 5 | No success/error feedback after user action | UX.4.2, UX.4.3 |
| 6 | Destructive action without confirmation dialog | UX.4.4 |
| 7 | Grid column binding raw DTO field that needs label resolution | UX.2.1 |
| 8 | Literal `null`, `undefined`, or blank displayed in UI | UX.1.7 |
| 9 | Ellipsis / truncated text with no tooltip | UX.8.1 |
| 10 | Hardcoded user-facing string (not a translation key) | UX.9.1 |
| 11 | Translation key missing from `en.json` or `ar.json` | UX.9.2 |
| 12 | Grid does not rebuild columns on language change | UX.9.5 |

---

## CANONICAL PATTERNS (from this codebase)

### Pattern A: Lookup Resolution via `ErpLookupFieldComponent`

```typescript
// In template — stores ID, displays resolved label
<erp-lookup-field
  formControlName="accountIdFk"
  [config]="accountLookupConfig">
</erp-lookup-field>
```

The component internally calls `LookupDataService.fetchById()` to resolve `LookupItem.display`.

### Pattern B: Grid Status with `resolveLabel`

```typescript
// In presentational component
resolveLabel(options: LookupSelectOption[], code: string | null | undefined): string {
  if (!code) return '—';
  return options.find(o => o.value === code)?.label ?? code;
}
```

### Pattern C: Active Column via Shared Helper

```typescript
import { createActiveColumnDef } from '../../../../shared/ag-grid/active-column-def';

const activeLabels = {
  active: translate.instant('COMMON.ACTIVE'),
  inactive: translate.instant('COMMON.INACTIVE')
};
// In column defs array:
createActiveColumnDef(activeLabels, { flex: 1 })
```

### Pattern D: Date Formatting in Grid

```typescript
{
  field: 'createdAt',
  headerName: translate.instant('COMMON.CREATED_AT'),
  valueFormatter: (params: ValueFormatterParams) => {
    if (!params.value) return '—';
    return new Date(params.value).toLocaleDateString(currentLang, {
      day: '2-digit', month: 'short', year: 'numeric'
    });
  }
}
```

### Pattern E: Action Feedback in Facade

```typescript
save(request: CreateRequest, onSuccess?: () => void): void {
  this.loadingSignal.set(true);
  this.apiService.create(request)
    .pipe(
      takeUntilDestroyed(this.destroyRef),
      tap(() => onSuccess?.()),
      catchError(err => {
        this.saveErrorSignal.set(this.errorMapper.extractBackendErrorCode(err));
        return EMPTY;
      }),
      finalize(() => this.loadingSignal.set(false))
    )
    .subscribe();
}
```

### Pattern F: Error Effect in Component

```typescript
constructor() {
  effect(() => {
    const saveError = this.facade.saveError();
    if (!saveError) return;
    untracked(() => this.notificationService.error(saveError));
  });
}
```

### Pattern G: Column Tooltip for Long Text

```typescript
{
  field: 'description',
  headerName: translate.instant('COMMON.DESCRIPTION'),
  flex: 3,
  minWidth: 200,
  tooltipField: 'description',   // AG Grid built-in tooltip
  cellStyle: { 'white-space': 'nowrap', 'text-overflow': 'ellipsis', overflow: 'hidden' }
}
```

### Pattern H: Default Column Definition with Tooltip & Resize

```typescript
// From shared/ag-grid/erp-ag-grid.config.ts
export const ERP_DEFAULT_COL_DEF: ColDef = {
  flex: 1,
  minWidth: 150,
  filter: true,
  sortable: true,
  resizable: true,     // UX.7.5: user can widen columns
  floatingFilter: true,
  filterParams: {
    debounceMs: 500,
    suppressAndOrCondition: true
  }
};
```

### Pattern I: Grid Rebuild on Language Change

```typescript
ngOnInit(): void {
  this.initErpList();

  this.translate.onLangChange
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe(() => {
      this.initializeFilterOptions();
      this.initializeColumnDefs();
      this.initializeGridOptions();
      this.recreateGrid();
      this.cdr.detectChanges();
    });
}
```

### Pattern J: Translation Key Usage

```typescript
// In component template
<label>{{ 'GL.ACCOUNT_NAME' | translate }}</label>
<button class="btn btn-primary">{{ 'BUTTONS.SAVE' | translate }}</button>

// In TypeScript
this.notificationService.success(
  this.translate.instant('MESSAGES.SAVE_SUCCESS')
);
```

### Pattern K: RTL-Aware Grid

```typescript
createErpGridOptions({
  enableRtl: this.translate.currentLang === 'ar',
  localeText: { ... },
  pageSize: 20
});
```

---

## HOW TO RUN THIS SKILL

### Input
List of frontend feature files to validate (components, templates, grid configs, facades).

### Process
1. **Scan templates and grid configs** for raw data bindings
2. **Check all 46 rules** across 9 layers
3. **Verify display model mapping** — ensure no raw DTO field reaches the template without transformation
4. **Check feedback patterns** — loading, success, error on every action
5. **Check column readability** — flex/minWidth, tooltip on truncation, resizable
6. **Check bilingual completeness** — every key exists in both `en.json` and `ar.json`
7. **Check automatic rejection triggers** — if any triggered, REJECT immediately
8. **Generate report** with pass/fail per check

### Output Format
```
UI/UX ENFORCEMENT REPORT
=========================================
Feature: <feature-name>
Date: <date>

LAYER 1: DATA REPRESENTATION         [X/7 PASS]
LAYER 2: DISPLAY MODEL MAPPING       [X/6 PASS]
LAYER 3: STATUS & STATE VISIBILITY   [X/4 PASS]
LAYER 4: ACTION FEEDBACK             [X/5 PASS]
LAYER 5: DATE & NUMBER FORMATTING    [X/4 PASS]
LAYER 6: LAYOUT & CONSISTENCY        [X/5 PASS]
LAYER 7: COLUMN READABILITY & SIZING [X/5 PASS]
LAYER 8: TOOLTIP & TRUNCATION        [X/4 PASS]
LAYER 9: BILINGUAL / I18N            [X/6 PASS]

TOTAL: XX/46 CHECKS PASSED

AUTOMATIC REJECTION: YES/NO
VIOLATIONS: [list of failed checks with file locations]

VERDICT: APPROVED / APPROVED WITH WARNINGS / REJECTED
```
