---
applyTo: 'frontend/**'
---

# Frontend Architecture Instructions

> Global Copilot instruction file for the ERP frontend.
> Enforces Angular architecture, state management, and component rules extracted from canonical governance documents.

---

## 1. Architecture Overview

### Data Flow (STRICT)

```
Component → Facade → ApiService → Backend
```

| Layer | Responsibility |
|---|---|
| **Component** | UI rendering, user interaction, template bindings. ZERO business logic. |
| **Facade** | State management (signals), API orchestration, error handling. |
| **ApiService** | HTTP communication. Extends `BaseApiService`. Handles response unwrapping. |

### Component Classification

| Type | Examples | Rules |
|---|---|---|
| **Smart (Page)** | Search page, Entry page | Injects facade. Manages routing and lifecycle. Provides facade + API service. |
| **Dumb (Presentational)** | Section, Actions cell | `@Input`/`@Output` only. MUST NOT inject services. |
| **Modal** | Child form modal | Self-contained. Manages own `FormGroup` and `NgbModal` lifecycle. |

---

## 2. Core Rules

### 2.1 Component Architecture

- MUST use `standalone: true` on all components.
- MUST use `ChangeDetectionStrategy.OnPush` on all components.
- MUST NOT use NgModule-based components.
- MUST NOT use default change detection strategy.

### 2.2 Facade Pattern

- MUST use facade pattern for all state management and API orchestration.
- MUST NOT call API service directly from components.
- MUST NOT perform business logic in components.
- Facade MUST be `@Injectable()` — NOT `providedIn: 'root'`.
- Facade and ApiService MUST be provided at page component level via `providers: [...]`.

### 2.3 Signal-Based State

- MUST use Angular `signal()` for all state — private writable signals.
- MUST use `computed()` for all public readonly state exposure.
- MUST NOT use `BehaviorSubject` or RxJS-based state management.
- MUST NOT expose writable signals publicly.

### 2.4 API Service

- MUST extend `BaseApiService`.
- MUST use `doGet`, `doPost`, `doPut`, `doDelete` from base class.
- MUST NOT use `HttpClient` directly in components or facades.
- MUST NOT use `providedIn: 'root'` — provided at component level.
- MUST use `environment.authApiUrl` for base URL — no hardcoded URLs.
- MUST NOT use `.pipe(map(...))` for response unwrapping — base class handles it.

### 2.5 Permission Enforcement (Triple Layer)

- **Route guards**: `canActivate: [authGuard, permissionGuard]` with `data: { permission: 'PERM_...' }`.
- **UI visibility**: `erpPermission="PERM_..."` directive on buttons and action elements.
- **Programmatic checks**: `authService.hasPermission('PERM_...')` before operations.
- All three layers MUST be applied — skipping any layer is a violation.

### 2.6 Internationalization

- ALL user-facing strings MUST use translation keys — never hardcoded text.
- MUST use `TranslateModule` + `translate.instant()` / `| translate` pipe.
- Grid column headers MUST be rebuilt on `translate.onLangChange`.

---

## 3. State Management Rules

### 3.1 Pagination State

- `lastSearchRequestSignal` is the SINGLE source of truth for search state (`{ filters, sorts, page, size }`).
- `currentPage` and `pageSize` MUST be `computed()` derived from `lastSearchRequestSignal` — NOT separate writable signals.
- MUST NOT create standalone signals for page, size, or sort that duplicate `ErpListComponent` state.

### 3.2 Filter State

- `currentFiltersSignal` is the ONLY independently managed search state signal.
- All other search state flows through `lastSearchRequestSignal`.

### 3.3 Facade Operation Pattern

Every API call in the facade MUST follow this sequence:

1. Set loading/saving signal.
2. Clear error signal.
3. Call API via `pipe(tap(...), catchError(...), finalize(...))`.
4. Update state signals in `tap()`.
5. Handle error in `catchError()` via `handleError()`.
6. Reset loading in `finalize()`.

### 3.4 Child Entity State Updates

- Child create: `signal.update(items => [...items, newItem])` — append locally.
- Child update: `signal.update(items => items.map(d => d.id === id ? updated : d))` — map in-place.
- Child delete: `signal.update(items => items.filter(d => d.id !== id))` — filter locally.
- MUST NOT reload the entire grid after child mutations.
- After child create/delete: MUST call `refreshUsageInfo(parentId)`.

### 3.5 Error Handling

- MUST use `extractBackendErrorCode(error)` → `ErpErrorMapperService.mapError(code)` → translation key.
- MUST NOT display raw HTTP error messages to users.
- Error/save-error effects MUST display via `ErpNotificationService`.

### 3.6 Cleanup

- `ngOnDestroy` MUST call `facade.clearCurrentEntity()`.
- Facade MUST provide `clearCurrentEntity()` and `resetChildState()` methods.
- MUST NOT allow state to leak across navigations.

---

## 4. Component Rules

### 4.1 Search Page (Page A)

- MUST extend `ErpListComponent`.
- MUST implement `load(state: ErpGridState)` → delegates to `facade.applyGridStateAndLoad()`.
- AG Grid configuration MUST be in a separate `<feature>-grid.config.ts` file.
- Grid config functions MUST accept `TranslateService` and rebuild on language change.
- Actions cell MUST be a standalone AG Grid cell renderer component.
- MUST provide `[Facade, ApiService]` in component `providers`.

### 4.2 Entry Page (Page B)

- MUST use `signal<FormModel>()` + Reactive `FormGroup`.
- MUST use `FormMapper` for DTO ↔ FormModel conversion (`createEmpty`, `fromDomain`, `toCreateRequest`, `toUpdateRequest`).
- On create success → MUST switch to edit mode **in-place** using `Location.replaceState()`.
- MUST NOT use `router.navigate` after create — it destroys/recreates the component causing flicker.
- Edit mode MUST disable immutable form fields via `.get('field')?.disable()`.
- Permission check MUST happen in `ngOnInit` before loading data.
- `ngOnDestroy` MUST call `facade.clearCurrentEntity()`.

### 4.3 Presentational (Dumb) Components

- MUST use `@Input()` / `@Output()` exclusively for data flow.
- MUST NOT inject any services.
- MUST NOT fetch data or call APIs.

### 4.4 Modal Components

- MUST manage own `FormGroup` and `NgbModal` lifecycle.
- `open(entity?)` method — if entity provided → edit mode (patch form + disable immutable fields).
- MUST emit save event with request + modal reference.
- Parent component handles save via facade and closes modal in `onSuccess`.
- MUST use `ChangeDetectionStrategy.OnPush`.

### 4.5 Immutability Enforcement

- Natural keys and FK parent references are IMMUTABLE after creation.
- `UpdateRequest` interface MUST omit immutable fields.
- Edit mode MUST disable immutable form fields.
- `FormMapper.toUpdateRequest()` MUST exclude immutable fields.

---

## 5. API & Data Flow Rules

### 5.1 Request/Response Contract

- `SearchRequest` MUST use `{ filters, sorts, page, size }` structure.
- `FilterOperator` MUST be `'EQUALS' | 'CONTAINS' | 'STARTS_WITH'`.
- Toggle active MUST send `{ active: boolean }` body — not query param.
- Frontend DTO interfaces MUST match backend response DTOs in field names, types, and optionality.

### 5.2 File Organization

- All DTOs and interfaces in single `<feature>.model.ts` file.
- `FormModel` + `FormMapper` in separate `<feature>-form.model.ts`.
- API service in `services/<feature>-api.service.ts`.
- Facade in `facades/<feature>.facade.ts`.
- Confirm actions in `helpers/<feature>-confirm-actions.ts`.
- Grid config in `pages/<feature>-search/<feature>-grid.config.ts`.

### 5.3 Callbacks

- Write operations (`create`, `update`, `toggleActive`, `delete`) MUST accept optional `onSuccess?` callback.
- `onSuccess` is for component-specific post-action behavior (navigation, notification).
- Business logic (notifications, state updates) MUST NOT be placed in `onSuccess` — it belongs in the facade.

---

## 6. Validation Rules

Copilot MUST verify:

- No direct API calls from components — all go through facade.
- No business logic in components — pure UI delegation.
- No state duplication — single source of truth via signals.
- No `BehaviorSubject` usage — Angular signals only.
- No `providedIn: 'root'` on facade or feature API service.
- No hardcoded strings in templates — translation keys only.
- No inline AG Grid column definitions — external config file required.
- No `router.navigate` after create success — use `Location.replaceState()`.
- No `||` (logical OR) for numeric form-to-DTO mappings — use `??` (nullish coalescing) to preserve `0`.
- DTO structures match backend request/response contracts.
- Permission enforcement on all three layers (route, directive, programmatic).
- `OnPush` change detection on every component.
- `standalone: true` on every component.
- Immutable fields disabled in edit mode.
- `ngOnDestroy` performs cleanup via `facade.clearCurrentEntity()`.

---

## 7. Anti-Patterns (FORBIDDEN)

| Pattern | Why It's Wrong |
|---|---|
| Direct `HttpClient` usage in component | Bypasses ApiService abstraction and response unwrapping. |
| Missing facade — component calls API directly | Violates data flow architecture. Component talks only to facade. |
| `BehaviorSubject` for state management | Angular signals are the standard. Signals provide better OnPush integration. |
| Missing `ChangeDetectionStrategy.OnPush` | Default change detection causes unnecessary re-renders. |
| Business logic in components | Components are UI-only. Conditional logic, validation, and orchestration belong in facade/service. |
| `providedIn: 'root'` on facade or API service | Facade and API service MUST be scoped to the page component for isolation. |
| Separate writable signals for page/size/sort | Duplicates `ErpListComponent` state. Use `lastSearchRequestSignal` as single source. |
| `router.navigate` after create | Destroys and recreates component, causing loading flicker. Use `Location.replaceState()`. |
| `sortOrder \|\| undefined` for numeric values | Logical OR converts `0` to `undefined`. Use nullish coalescing `??`. |
| Inline column definitions in component | Grid config MUST be in separate `<feature>-grid.config.ts` file. |
| Hardcoded column headers | Headers MUST use translation keys and rebuild on language change. |
| Template-driven forms | Reactive Forms (`FormBuilder`, `FormGroup`) are the standard. |
| Full grid reload after child mutation | Update signals locally (append/map/filter) to avoid flicker. |
| Stale usage data after child mutations | MUST call `refreshUsageInfo(parentId)` after child create/delete. |
| Displaying raw HTTP errors | MUST map via `ErpErrorMapperService` to translation keys. |
| Smart child components (injecting services) | Presentational children use `@Input`/`@Output` only. |
| Missing permission guards on routes | Every route MUST have `canActivate: [authGuard, permissionGuard]`. |
| Singleton API service via `providedIn: 'root'` | Feature API services MUST be component-scoped. |
| Manual in-memory caches in facade | MUST NOT maintain `Map` or object caches that duplicate backend-cached data. |
| `shareReplay` on CRUD API services | `shareReplay(1)` is allowed ONLY in lookup services for governance-approved entities. |

---

## 8. Routing Conventions

- Three routes per feature: `''` (list), `'create'` (create), `'edit/:id'` (edit).
- MUST use `loadComponent` for lazy loading — not eager `component`.
- Every route MUST have `canActivate: [authGuard, permissionGuard]`.
- Every route MUST have `data: { permission: 'PERM_...' }` matching `SecurityPermissions` constants.
- Routes MUST be wrapped in `AdminLayout` parent.

---

## 9. Confirm Action Rules

- MUST be extracted to `helpers/<feature>-confirm-actions.ts` as standalone functions.
- Functions MUST receive `ConfirmActionDeps` interface (dialog, notify, auth, facade).
- Every action MUST check permission FIRST before showing dialog.
- Delete MUST check `usage.canDelete` BEFORE showing confirm dialog.
- Dialog type: `'warning'` for toggle, `'danger'` for delete.
- Dialog MUST use translation keys with `messageParams` for entity identifiers.

---

## 10. Frontend Caching Rules

- `shareReplay(1)` is allowed ONLY in lookup services for governance-approved eligible entities.
- CRUD API services (extending `BaseApiService`) MUST NOT use `shareReplay` or any in-memory caching.
- Facades MUST NOT maintain manual in-memory caches (`Map`, plain objects) duplicating backend data.
- Frontend MUST NOT implement its own TTL or expiration logic for backend-cached data.
- If backend caching is enabled for an entity, frontend MUST rely on backend cache — no redundant layer.

## 11. Feature Folder Structure (MANDATORY)

Every feature MUST strictly follow the canonical folder structure defined in the Blueprint.

The structure is NON-NEGOTIABLE.

### Standard Feature Structure

Each feature MUST be organized as:

<feature-name>/
├── components/
├── facades/
├── helpers/
├── models/
├── pages/
└── services/

---

### Folder Responsibilities

| Folder     | Responsibility                                        |
| ---------- | ----------------------------------------------------- |
| components | UI components (actions cell, sections, modals, forms) |
| facades    | State management and orchestration                    |
| helpers    | Confirm actions and utility logic                     |
| models     | DTO interfaces and form models                        |
| pages      | Smart components (search and entry screens)           |
| services   | API services extending BaseApiService                 |

---

### Rules

* MUST NOT create additional folders outside this structure.
* MUST NOT merge responsibilities between folders.
* MUST NOT place facade inside services or components.
* MUST NOT place API service inside components.
* MUST NOT place models outside models folder.

---

### Naming Consistency

* Folder names MUST be lowercase and kebab-case.
* Feature folder name MUST match route and API naming.

---

### Validation (STRICT)

Copilot MUST verify:

* Folder structure matches blueprint exactly
* No missing folders
* No misplaced files
* No duplicated responsibilities

If any violation is detected:

* STOP generation
* Correct structure
* Then continue

---

### Blueprint Alignment Rule

If an existing feature structure is provided:

* It MUST be treated as the canonical reference
* All new features MUST replicate the same structure

Deviation from blueprint structure is NOT allowed.

---

## 12. ERP Design System (CSS Tokens)

The project uses `--erp-*` CSS custom properties defined in `src/scss/erp/erp-tokens.scss` as the styling control layer. For full token reference, examples, and how-to guides, see the `enforce-design-system` skill.

### Rules

- ALL new component SCSS MUST use `var(--erp-*)` tokens — never hardcoded `px`/`rem`/color values.
- Every `var(--erp-*)` MUST include a fallback matching the current visual output.
- New component tokens MUST be added to `erp-tokens.scss` referencing base tokens — NOT hardcoded values.
- MUST NOT create CSS classes that duplicate `erp-ui.scss`.
- MUST NOT remove or replace existing Mantis theme styles — tokens are additive only.
- MUST NOT use inline styles in templates.

### Validation (STRICT)

Copilot MUST verify in any new or modified SCSS:

- No hardcoded spacing, font-size, font-weight, border-radius, color, z-index, or shadow values.
- All values use `var(--erp-*)` with proper fallbacks.
- No duplicate CSS classes from `erp-ui.scss`.
- No removed or overridden Mantis base styles.
- New component tokens (if any) are defined in `erp-tokens.scss` using base token references.

If a violation is detected: STOP, fix the value to use the correct token, then continue.

