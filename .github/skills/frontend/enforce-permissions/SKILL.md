---
description: "PERMISSION ENFORCER — validates triple-enforcement pattern: route guards (authGuard + permissionGuard), UI directives (erpPermission), and programmatic checks (hasPermission before dialog). 35 checks across 5 sections."
---

# Skill: enforce-permissions

## Name
`enforce-permissions`

## Description
Validates that frontend permission enforcement follows the triple-enforcement pattern: route guards, UI directives, and programmatic checks. Also validates backend-frontend permission alignment and confirm action permission ordering. Enforces contracts C.2.x, B.5.x, B.6.x, and B.4.13.

## When to Use
- After creating or modifying routing, components, or confirm actions
- When auditing permission enforcement completeness
- During security review of frontend pull requests
- When diagnosing permission bypass issues

---

## CHECK MATRIX

### Section 1: Route Guard Enforcement (8 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| P.1.1 | authGuard on list route | `canActivate: [authGuard, ...]` on `path: ''` | Missing authGuard |
| P.1.2 | authGuard on create route | `canActivate: [authGuard, ...]` on `path: 'create'` | Missing authGuard |
| P.1.3 | authGuard on edit route | `canActivate: [authGuard, ...]` on `path: 'edit/:id'` | Missing authGuard |
| P.1.4 | permissionGuard on list route | `canActivate: [..., permissionGuard]` on `path: ''` | Missing permissionGuard |
| P.1.5 | permissionGuard on create route | `canActivate: [..., permissionGuard]` on `path: 'create'` | Missing permissionGuard |
| P.1.6 | permissionGuard on edit route | `canActivate: [..., permissionGuard]` on `path: 'edit/:id'` | Missing permissionGuard |
| P.1.7 | Permission data on all routes | `data: { permission: 'PERM_<ENTITY>_<ACTION>' }` present | Missing permission data |
| P.1.8 | Permission constants match | Permission strings match backend `SecurityPermissions` naming | Mismatched permissions |

### Section 2: UI Directive Enforcement (8 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| P.2.1 | Create button permission | `[erpPermission]="'PERM_<ENTITY>_CREATE'"` on create button | Visible without permission |
| P.2.2 | Edit button permission | `[erpPermission]="'PERM_<ENTITY>_UPDATE'"` on edit action | Visible without permission |
| P.2.3 | Toggle active permission | `[erpPermission]="'PERM_<ENTITY>_UPDATE'"` on toggle button | Visible without permission |
| P.2.4 | Delete button permission | `[erpPermission]="'PERM_<ENTITY>_DELETE'"` on delete button | Visible without permission |
| P.2.5 | Save button permission | Save button in entry component respects create/update permission | Save visible without permission |
| P.2.6 | Actions cell uses directive | All action buttons in AG Grid cell use `erpPermission` directive | Missing directive in cell |
| P.2.7 | Child add button permission | Add child button uses parent or child permission | Visible without permission |
| P.2.8 | Child action permissions | Child edit/toggle/delete buttons use permissions | Child actions unprotected |

### Section 3: Programmatic Check Enforcement (8 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| P.3.1 | Permission before create load | `ngOnInit` checks CREATE permission before loading create form | Loading then checking |
| P.3.2 | Permission before edit load | `ngOnInit` checks UPDATE permission before loading edit data | Loading then checking |
| P.3.3 | Confirm toggle checks first | `confirmToggle*Active()` checks permission BEFORE dialog | Dialog then permission |
| P.3.4 | Confirm delete checks first | `confirmDelete*()` checks permission BEFORE dialog | Dialog then permission |
| P.3.5 | Delete checks canDelete | Delete confirm checks `usage.canDelete` BEFORE dialog | Confirm then server reject |
| P.3.6 | AuthenticationService injected | Confirm action deps include `auth: AuthenticationService` | Missing auth service |
| P.3.7 | hasPermission method used | `deps.auth.hasPermission('PERM_...')` called | Direct auth check bypass |
| P.3.8 | No permission after action | Permission is checked BEFORE action, not after | Post-action permission check |

### Section 4: Permission Naming Convention (6 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| P.4.1 | VIEW permission exists | `PERM_<ENTITY>_VIEW` used in list route | Missing VIEW permission |
| P.4.2 | CREATE permission exists | `PERM_<ENTITY>_CREATE` used in create route | Missing CREATE permission |
| P.4.3 | UPDATE permission exists | `PERM_<ENTITY>_UPDATE` used in edit route + toggle | Missing UPDATE permission |
| P.4.4 | DELETE permission exists | `PERM_<ENTITY>_DELETE` used in delete action | Missing DELETE permission |
| P.4.5 | PERM_ prefix | All permissions start with `PERM_` prefix | Missing prefix |
| P.4.6 | Child permissions documented | Child entities use parent permissions OR have own set, explicitly | Ambiguous child permissions |

### Section 5: Cross-Layer Consistency (5 checks)

| # | Check | Pass Criteria | Violation |
|---|-------|--------------|-----------|
| P.5.1 | Route ↔ Button alignment | Route permission matches button permission for same action | Mismatched across layers |
| P.5.2 | Route ↔ Confirm action alignment | Route permission matches programmatic check in confirm action | Mismatched across layers |
| P.5.3 | Frontend ↔ Backend alignment | Frontend permission strings match backend `SecurityPermissions` constants exactly | Frontend/backend mismatch |
| P.5.4 | Four permissions per entity | VIEW, CREATE, UPDATE, DELETE all defined for every entity | Missing any of the four |
| P.5.5 | Triple enforcement per action | Every action has route guard + directive + programmatic check | Missing any enforcement layer |

---

## TRIPLE ENFORCEMENT PATTERN

Each user action must be protected at THREE levels:

```
┌──────────────────────────────────────────────────────┐
│ Layer 1: ROUTE GUARD                                  │
│ canActivate: [authGuard, permissionGuard]              │
│ data: { permission: 'PERM_<ENTITY>_<ACTION>' }        │
│ → Prevents navigation to unauthorized pages            │
├──────────────────────────────────────────────────────┤
│ Layer 2: UI DIRECTIVE                                  │
│ [erpPermission]="'PERM_<ENTITY>_<ACTION>'"            │
│ → Hides buttons/actions user cannot perform            │
├──────────────────────────────────────────────────────┤
│ Layer 3: PROGRAMMATIC CHECK                            │
│ authService.hasPermission('PERM_<ENTITY>_<ACTION>')    │
│ → Guards action execution before API call              │
│ → Used in confirm action helpers FIRST, before dialog  │
└──────────────────────────────────────────────────────┘
```

### Per-Action Enforcement Map

| Action | Route Guard | UI Directive | Programmatic Check |
|--------|------------|-------------|-------------------|
| View/List | `PERM_*_VIEW` on `''` route | — (page is the control) | — |
| Create (navigate) | `PERM_*_CREATE` on `'create'` route | `erpPermission` on Create button | — |
| Create (save) | `PERM_*_CREATE` on route | — | `ngOnInit` permission check |
| Edit (navigate) | `PERM_*_UPDATE` on `'edit/:id'` route | `erpPermission` on Edit button | — |
| Edit (save) | `PERM_*_UPDATE` on route | — | `ngOnInit` permission check |
| Toggle Active | `PERM_*_UPDATE` on route | `erpPermission` on Toggle button | `confirmToggle*Active()` checks first |
| Delete | `PERM_*_DELETE` via route | `erpPermission` on Delete button | `confirmDelete*()` checks first |

---

## AUTOMATIC REJECTION TRIGGERS

| # | Trigger | Rules |
|---|---------|-------|
| 1 | Any route missing both `authGuard` AND `permissionGuard` | P.1.1–P.1.6 |
| 2 | Any route missing `data: { permission: '...' }` | P.1.7 |
| 3 | Action buttons without `erpPermission` directive | P.2.1–P.2.4 |
| 4 | Confirm action that shows dialog BEFORE checking permission | P.3.3, P.3.4 |
| 5 | Delete action skipping `canDelete` usage check | P.3.5 |
| 6 | Permission string doesn't match `PERM_<ENTITY>_<ACTION>` pattern | P.4.5 |
| 7 | Frontend permission doesn't exist in backend `SecurityPermissions` | P.5.3 |

---

## DIAGNOSTIC PATTERNS

### Pattern: Unauthorized Page Access
**Symptoms:** User sees page they shouldn't access
**Root Cause:** Missing `permissionGuard` or wrong permission data on route
**Fix:** Add `canActivate: [authGuard, permissionGuard]` + correct `data: { permission: '...' }`

### Pattern: Action Button Visible But Fails
**Symptoms:** User clicks button, gets 403 from backend
**Root Cause:** Missing `erpPermission` directive on button
**Fix:** Add `[erpPermission]="'PERM_<ENTITY>_<ACTION>'"` to button

### Pattern: Confirmation Dialog Then Access Denied
**Symptoms:** User confirms action, then sees "no permission" error
**Root Cause:** Confirm action checks permission AFTER showing dialog
**Fix:** Move `deps.auth.hasPermission(...)` check to BEFORE dialog display

### Pattern: Delete Succeeds But Was Blocked
**Symptoms:** User deletes entity that has references, gets server error
**Root Cause:** Missing `canDelete` check before showing confirm dialog
**Fix:** Fetch usage info and check `usage.canDelete` BEFORE showing dialog

---

## HOW TO RUN THIS SKILL

### Input
Feature routing file + confirm actions + search/entry component templates.

### Process
1. **Analyze routing** for guards and permission data
2. **Analyze templates** for `erpPermission` directive usage
3. **Analyze confirm actions** for permission check ordering
4. **Check naming** against `SecurityPermissions` constants
5. **Verify triple enforcement** for each action type
6. **Run all 35 checks** across 5 sections

### Output Format
```
PERMISSION ENFORCEMENT REPORT
===============================
Feature: <feature-name>
Date: <date>

SECTION 1: ROUTE GUARDS             [X/8 PASS]
SECTION 2: UI DIRECTIVES            [X/8 PASS]
SECTION 3: PROGRAMMATIC CHECKS      [X/8 PASS]
SECTION 4: NAMING CONVENTIONS       [X/6 PASS]
SECTION 5: CROSS-LAYER CONSISTENCY  [X/5 PASS]

TOTAL: XX/35 CHECKS PASSED

AUTOMATIC REJECTION: YES/NO
VIOLATIONS: [list with action, missing layer, file location]

VERDICT: APPROVED / APPROVED WITH WARNINGS / REJECTED
```

---

## RELATED SKILLS

| Skill | Purpose |
|-------|---------|
| `enforce-reusability` | Validates that shared permission utilities (`ErpPermissionDirective`, `AuthenticationService.hasPermission()`) are consumed and not reinvented |
| `enforce-frontend-architecture` | Validates overall architectural compliance including permission layer |
| `enforce-state-management` | Validates Signal-based state management patterns |
