---
description: "ARCHITECTURE ENFORCER — validates frontend code against 73 checks across 7 layers: file organization, models, API service, facade, components, routing/i18n, and theme/layout architecture. Rejects BehaviorSubject, providedIn root, missing OnPush, direct HttpClient, ::ng-deep card positioning."
---

# Skill: enforce-frontend-architecture

## Name
`enforce-frontend-architecture`

## Description
Validates that frontend feature implementations comply with the ERP architectural contracts. Checks layer separation, component structure, file organization, API patterns, form handling, and theme/layout architecture. This is the frontend equivalent of `enforce-backend-contract`.

## When to Use
- After generating or reviewing any frontend feature code
- During code review of frontend pull requests
- When auditing existing frontend features for compliance
- When resolving frontend architectural issues

## Responsibilities

- Validate file organization (models, services, facades, components in correct folders)
- Validate component structure (standalone, OnPush, no business logic)
- Validate API patterns (BaseApiService extension, no direct HttpClient)
- Validate facade patterns (signal-based, no BehaviorSubject, component-scoped)
- Validate theme/layout architecture rules

## Constraints

- MUST NOT generate or modify application code — this skill only validates
- MUST NOT fix violations automatically — report them for the appropriate create-* skill to fix
- MUST NOT validate backend code — scope is frontend only
- MUST NOT skip any check — all layers must be evaluated

## Output

- Architecture compliance report with PASS/VIOLATION per check across 7 layers
- Specific violation descriptions for any failures

---

## CHECK MATRIX

### Layer 1: File Organization (8 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| F.1.1 | Model file location | `models/<feature>.model.ts` contains ALL DTOs/interfaces | DTOs split across multiple files |
| F.1.2 | Form model file location | `models/<feature>-form.model.ts` contains FormModel + FormMapper | FormModel in main model file or component |
| F.1.3 | API service file location | `services/<feature>-api.service.ts` | API calls scattered in components |
| F.1.4 | Facade file location | `facades/<feature>.facade.ts` | State management in components |
| F.1.5 | Confirm actions file location | `helpers/<feature>-confirm-actions.ts` | Inline confirmation logic |
| F.1.6 | Grid config file location | `pages/<feature>-search/<feature>-grid.config.ts` | Inline column definitions |
| F.1.7 | Search page location | `pages/<feature>-search/<feature>-search.component.ts` | Wrong directory structure |
| F.1.8 | Entry page location | `pages/<feature>-entry/<feature>-entry.component.ts` | Wrong directory structure |

### Layer 2: Model Contracts (8 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| F.2.1 | All DTOs in single model file | `<feature>.model.ts` has Entity, Usage, Create, Update DTOs | DTOs in separate files |
| F.2.2 | FormMapper is const object | `export const <Entity>FormMapper = { ... }` with 4 methods | Class-based or missing FormMapper |
| F.2.3 | FormMapper has all 4 methods | `createEmpty()`, `fromDomain()`, `toCreateRequest()`, `toUpdateRequest()` | Missing any method |
| F.2.4 | UpdateRequest omits immutable fields | No `code`, `key`, `parentId` in update interface | Immutable fields in UpdateRequest |
| F.2.5 | PagedResponse is generic | `PagedResponse<T>` reused, not feature-specific | Feature-specific paged type |
| F.2.6 | FilterOperator uses standard values | `'EQUALS' \| 'CONTAINS' \| 'STARTS_WITH'` | Custom operator types |
| F.2.7 | Frontend DTOs match backend | Field names, types, optionality match backend Response DTO | Mismatched contract |
| F.2.8 | Numeric form→DTO uses `??` | `sortOrder: formValue.sortOrder ?? undefined` | `\|\|` for numeric fields |

### Layer 3: API Service Contracts (7 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| F.3.1 | Extends BaseApiService | `extends BaseApiService` in class declaration | Direct HttpClient usage |
| F.3.2 | Uses base methods | `doGet()`, `doPost()`, `doPut()`, `doDelete()` | Manual `this.http.get()` |
| F.3.3 | NOT providedIn root | `@Injectable()` without `providedIn: 'root'` | Singleton API service |
| F.3.4 | Environment base URL | `environment.authApiUrl` for URL construction | Hardcoded URL |
| F.3.5 | Toggle active body | `doPut(url, { active })` | `{ isActive }` or query param |
| F.3.6 | No shareReplay | No `shareReplay` in CRUD API service | Caching in feature service |
| F.3.7 | Search via POST | `doPost('...search', request)` | GET with query params |

### Layer 4: Facade Contracts (14 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| F.4.1 | Signal-based state | All state via `signal()` | BehaviorSubject or component state |
| F.4.2 | Private signals | Signals declared as `private readonly` | Public writable signals |
| F.4.3 | Computed public | Public state via `computed()` readonly | Direct signal exposure |
| F.4.4 | NOT providedIn root | `@Injectable()` without `providedIn: 'root'` | Singleton facade |
| F.4.5 | onSuccess callback | Write methods accept `onSuccess?` callback | Notifications in facade |
| F.4.6 | Loading pattern | set loading → API call → tap → catchError → finalize | Missing loading management |
| F.4.7 | Child local updates | Child mutations use signal `.update()` (append/map/filter) | Full reload after child save |
| F.4.8 | refreshUsageInfo after child mutation | `refreshUsageInfo()` called after child create/delete | Stale usage data |
| F.4.9 | clearCurrentEntity | Method exists for cleanup | Missing cleanup |
| F.4.10 | Error mapping | `extractBackendErrorCode()` → `ErpErrorMapperService` | Raw HTTP errors displayed |
| F.4.11 | Consolidated pagination | `lastSearchRequestSignal` holds page/size/sort | Separate page/size/sort signals |
| F.4.12 | Derived page/size | `currentPage`/`pageSize` are `computed()` from lastSearchRequest | Writable page/size signals |
| F.4.13 | Independent filters | `currentFiltersSignal` is only independent search state | Multiple scattered filter states |
| F.4.14 | No manual cache | No `Map`, `{}`, or `shareReplay` for caching | In-memory cache in facade |
| F.4.15 | DestroyRef + takeUntilDestroyed | All `.subscribe()` calls preceded by `takeUntilDestroyed(this.destroyRef)` | Leaked subscriptions on destroy |

### Layer 5: Component Contracts (16 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| F.5.1 | Standalone components | `standalone: true` on all components | NgModule components |
| F.5.2 | OnPush change detection | `ChangeDetectionStrategy.OnPush` on all components | Default change detection |
| F.5.3 | Component-scoped providers | `providers: [Facade, ApiService]` on page | providedIn root |
| F.5.4 | Search extends ErpListComponent | `extends ErpListComponent` | Custom pagination handling |
| F.5.5 | Grid config external | Column defs from grid config file function | Inline column definitions |
| F.5.6 | Grid rebuilt on lang change | `translate.onLangChange.subscribe()` → rebuild | Hardcoded headers |
| F.5.7 | Actions cell renderer | Standalone AG Grid ICellRendererAngularComp | Inline template buttons |
| F.5.8 | Entry uses signal for ALL state | `signal()` for FormModel, isEditMode, entityId, loading | Any plain property for component state |
| F.5.9 | Entry uses Reactive Forms | `FormGroup` + `FormBuilder` | Template-driven forms |
| F.5.10 | Create→Edit in-place | `Location.replaceState()` after create success | `router.navigate` destroys component |
| F.5.11 | Immutable fields disabled | `.get('field')?.disable()` in edit mode | Allowing immutable editing |
| F.5.12 | ngOnDestroy cleanup | `facade.clearCurrentEntity()` called | State leak |
| F.5.13 | Error effect | `effect()` displays save errors via notification | Inline error display |
| F.5.14 | Permission check first | Check permission in ngOnInit before loading | Load then check |
| F.5.15 | Presentational components | `@Input/@Output` only, no services | Smart child components |
| F.5.16 | Modal self-contained | Own FormGroup + NgbModal lifecycle | Parent managing modal state |

### Layer 6: Routing and i18n Contracts (10 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| F.6.1 | Three routes | `''`, `'create'`, `'edit/:id'` | Missing routes |
| F.6.2 | Lazy loading | `loadComponent` on all routes | Eager `component:` |
| F.6.3 | Auth guard | `canActivate: [authGuard, permissionGuard]` on every route | Missing guards |
| F.6.4 | Permission data | `data: { permission: 'PERM_...' }` matches constants | Wrong permission string |
| F.6.5 | AdminLayout wrapper | Routes wrapped in AdminLayout parent | No layout wrapper |
| F.6.6 | All strings translated | No hardcoded user-facing strings | Hardcoded strings |
| F.6.7 | Translation keys exist | Keys in both `en.json` and `ar.json` | Missing translations |
| F.6.8 | Error codes mapped | Backend error codes in `ErpErrorMapperService` | Unmapped error codes |
| F.6.9 | Confirm actions use i18n | `translate.instant()` with messageParams | Hardcoded entity names |
| F.6.10 | Confirm actions check permission first | Permission check before dialog show | Dialog then permission denied |

### Layer 7: Theme & Layout Architecture (10 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| F.7.1 | Theme state via ThemeService | Theme color, dark mode, container mode managed by `ThemeService` signals | Component managing theme state locally |
| F.7.2 | Theme persistence via localStorage | ThemeService reads/writes `erp_theme_color`, `erp_dark_mode`, `erp_container_mode` | No persistence — theme resets on refresh |
| F.7.3 | DOM sync via effects | ThemeService uses `effect()` to sync signals to DOM (body classes, part attribute) | Manual DOM manipulation in components |
| F.7.4 | RTL owned by LanguageService | RTL/LTR management exclusively in `LanguageService` — not ThemeService or ConfigurationComponent | RTL classes set in ConfigurationComponent |
| F.7.5 | ConfigurationComponent is thin | Only applies font family from MantisConfig — all other concerns delegated to ThemeService | ConfigurationComponent manipulates dark mode, theme color, or container |
| F.7.6 | No direct body manipulation | Components use ThemeService methods — never `document.body.classList.add()` for theme | `document.body.classList.add('mantis-dark')` in component |
| F.7.7 | Card-header-right uses flexbox | `.card-header-right` layout is flexbox (from global `card.scss`) — not absolute | `position: absolute` on `.card-header-right` |
| F.7.8 | No ::ng-deep card positioning | Component SCSS does not use `::ng-deep` to override card-header-right layout | `::ng-deep { .card-header-right { position: absolute } }` |
| F.7.9 | Navigation fully translated | All text in nav-right and nav-content uses `\| translate` pipe | Hardcoded navigation labels |
| F.7.10 | Single Arabic font source | Arabic font defined only in `font-family.scss` — no conflicting rules | Duplicate `[lang="ar"]` font-family in styles.scss |

---

## AUTOMATIC REJECTION TRIGGERS

Any of the following causes **immediate rejection** without further review:

| # | Trigger | Rules |
|---|---------|-------|
| 1 | `@Injectable({ providedIn: 'root' })` on Facade or API Service | F.3.3, F.4.4 |
| 2 | `BehaviorSubject` or `ReplaySubject` for state management | F.4.1 |
| 3 | Missing `standalone: true` on any component | F.5.1 |
| 4 | Missing `ChangeDetectionStrategy.OnPush` on any component | F.5.2 |
| 5 | Direct `HttpClient` injection instead of BaseApiService | F.3.1 |
| 6 | `router.navigate` for create→edit mode switch | F.5.10 |
| 7 | Template-driven forms (`[(ngModel)]`) | F.5.9 |
| 8 | Missing route guards (`authGuard`, `permissionGuard`) | F.6.3 |
| 9 | Separate writable signals for `currentPage`, `pageSize`, `currentSort` | F.4.11, F.4.12 |
| 10 | `formValue.numericField \|\| undefined` for numeric conversion | F.2.8 |
| 11 | Missing `takeUntilDestroyed(this.destroyRef)` before `.subscribe()` in facade | F.4.15 |
| 12 | Plain property `isEditMode`, `entityId`, or `loading` instead of `signal()` | F.5.8 |
| 13 | `::ng-deep` overriding `.card-header-right` with absolute positioning | F.7.8 |
| 14 | Direct `document.body` manipulation for theme/dark mode in components | F.7.6 |
| 15 | Hardcoded navigation text in `nav-right` or `nav-content` | F.7.9 |

---

## HOW TO RUN THIS SKILL

### Input
List of frontend feature files to validate.

### Process
1. **Scan all files** in the feature directory
2. **Run all 73 checks** across 7 layers
3. **Flag violations** with rule reference and file location
4. **Check automatic rejection triggers** — if any triggered, REJECT immediately
5. **Generate report** with pass/fail per check

### Output Format
```
FRONTEND ARCHITECTURE ENFORCEMENT REPORT
=========================================
Feature: <feature-name>
Date: <date>

LAYER 1: FILE ORGANIZATION          [X/8 PASS]
LAYER 2: MODEL CONTRACTS            [X/8 PASS]
LAYER 3: API SERVICE CONTRACTS      [X/7 PASS]
LAYER 4: FACADE CONTRACTS           [X/14 PASS]
LAYER 5: COMPONENT CONTRACTS        [X/16 PASS]
LAYER 6: ROUTING & I18N CONTRACTS   [X/10 PASS]
LAYER 7: THEME & LAYOUT ARCH        [X/10 PASS]

TOTAL: XX/73 CHECKS PASSED

AUTOMATIC REJECTION: YES/NO
VIOLATIONS: [list of failed checks with file locations]

VERDICT: APPROVED / APPROVED WITH WARNINGS / REJECTED
```

---

## RELATED SKILLS

After running this architecture enforcement, also verify shared code consumption:

| Skill | Purpose |
|-------|---------|
| `enforce-reusability` | Validates that shared code (`shared/`, `core/`) is consumed — no duplicated logic across features |
| `enforce-ui-ux` | Validates UI/UX display patterns, readability, and i18n compliance |
| `enforce-permissions` | Validates triple-enforcement permission pattern |
| `enforce-state-management` | Validates Signal-based state management patterns |
| `enforce-design-system` | Validates CSS token consumption and design system compliance |


---

## ANGULAR/SKILLS COMPATIBILITY

> This section documents how this skill relates to the official `angular/skills` guidance.
> **ERP contracts always take precedence.** See `erp-priority-override` for the full precedence rule.

### What angular/skills adds that is SAFE to use alongside this skill
- Signal API syntax (`signal`, `computed`, `effect`)
- Standalone component patterns  fully aligned with F.5.1
- OnPush change detection  fully aligned with F.5.2
- Lazy loading syntax  fully aligned with F.6.2

### What angular/skills suggests that this skill OVERRIDES

| angular/skills Suggestion | This Skill Requires | Rejection Trigger |
|--------------------------|--------------------|--------------------|
| Signal Forms (Angular v21+) | Reactive Forms only  `FormGroup + FormBuilder` | #7  automatic rejection |
| NgModule compatibility | `standalone: true` always | #3  automatic rejection |
| `providedIn: 'root'` for services | Component-scoped providers only | #1  automatic rejection |
| BehaviorSubject for streams | `signal()` always | #2  automatic rejection |
| `router.navigate` for mode switch | `Location.replaceState()` | #6  automatic rejection |

### Amended Automatic Rejection  Trigger #7 (explicit)
The following patterns all trigger rejection under rule #7 (template-driven / Signal Forms):
- `[(ngModel)]`  template-driven forms
- `FormField` from `@angular/forms` experimental
- `SignalForm`, `signalForm()`  Signal Forms API
- Any form pattern that does NOT use `FormGroup + FormBuilder`

### Conflict resolution trigger

If any angular/skills guidance contradicts a rule in this skill:
1. Apply the ERP rule
2. Log: ` CONFLICT RESOLVED: angular/skills suggests [X], ERP rule [RULE_ID] requires [Y]. Applied [Y].`
3. Do NOT ask the user  apply ERP rule silently
