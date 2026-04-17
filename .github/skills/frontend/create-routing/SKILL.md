---
description: "Generates routing config with lazy-loaded routes (list/create/edit), authGuard + permissionGuard, AdminLayout wrapper, and confirm action helpers with permission-first pattern. Phase 2, Steps 2.4 + 2.10."
---

# Skill: create-routing

## Name
`create-routing`

## Description
Generates the routing configuration for a frontend feature including lazy-loaded routes with auth and permission guards, and confirm action helpers. This is **Phase 2, Steps 2.4 and 2.10** of the execution template.

## When to Use
- When setting up routes for a new frontend feature
- When the execution template Phase 2, Steps 2.4 + 2.10 are being started
- AFTER facade is defined (confirm actions depend on it), BEFORE or alongside components

## Variables (Must Be Defined First)

| Variable | Example | Description |
|----------|---------|-------------|
| `DOMAIN_DIR` | `master-data` | Module domain directory (kebab-case) |
| `FEATURE_DIR` | `master-lookups` | Feature directory (kebab-case plural) |
| `ENTITY_NAME` | `MasterLookup` | PascalCase entity name |
| `ENTITY_KEBAB` | `master-lookup` | kebab-case entity name |
| `ENTITY_URL` | `master-lookups` | URL path segment (kebab-case plural) |
| `ENTITY_PERM` | `MASTER_LOOKUP` | UPPER_SNAKE permission suffix |
| `HAS_CHILD` | `true/false` | Whether entity has child entities |
| `CHILD_NAME` | `LookupDetail` | PascalCase child name (if applicable) |

## Responsibilities

- Generate routing configuration with lazy-loaded routes (list, create, edit)
- Apply `authGuard` + `permissionGuard` on all routes
- Wrap routes in `AdminLayout` parent
- Generate confirm action helper functions with permission-first pattern

## Constraints

- MUST NOT generate models, API service, facade, or component code
- MUST NOT use eager `component` loading — must use `loadComponent`
- MUST NOT skip permission guards on any route
- MUST NOT hardcode permission strings — use `SecurityPermissions` constants

## Output

- `frontend/src/app/modules/<DOMAIN_DIR>/<FEATURE_DIR>/<feature>.routes.ts`
- `frontend/src/app/modules/<DOMAIN_DIR>/<FEATURE_DIR>/helpers/<feature>-confirm-actions.ts`

---

## PART 1: Confirm Actions

### File Location
`frontend/src/app/modules/<DOMAIN_DIR>/<FEATURE_DIR>/helpers/<ENTITY_KEBAB>-confirm-actions.ts`

### Structure
```typescript
import { ErpDialogService } from '../../../../shared/services/erp-dialog.service';
import { ErpNotificationService } from '../../../../shared/services/erp-notification.service';
import { AuthenticationService } from '../../../../core/services/authentication.service';
import { TranslateService } from '@ngx-translate/core';
import { <ENTITY_NAME>Facade } from '../facades/<ENTITY_KEBAB>.facade';
import { <ENTITY_NAME>Dto } from '../models/<ENTITY_KEBAB>.model';

export interface ConfirmActionDeps {
  dialog: ErpDialogService;
  notify: ErpNotificationService;
  auth: AuthenticationService;
  facade: <ENTITY_NAME>Facade;
  translate?: TranslateService;
}

// ─── Toggle Active ───

export function confirmToggle<ENTITY_NAME>Active(
  deps: ConfirmActionDeps,
  entity: <ENTITY_NAME>Dto,
  onDone?: () => void
): void {
  // 1. Permission check FIRST
  if (!deps.auth.hasPermission('PERM_<ENTITY_PERM>_UPDATE')) {
    deps.notify.showWarning(deps.translate?.instant('ERRORS.NO_PERMISSION') ?? 'No permission');
    return;
  }

  const isActivating = !entity.isActive;
  const messageKey = isActivating
    ? '<FEATURE>S.CONFIRM_ACTIVATE'
    : '<FEATURE>S.CONFIRM_DEACTIVATE';

  // 2. Show confirm dialog (warning type for deactivate)
  deps.dialog.confirm({
    title: deps.translate?.instant(isActivating ? 'COMMON.ACTIVATE' : 'COMMON.DEACTIVATE') ?? '',
    message: deps.translate?.instant(messageKey, { name: entity.<identifierField> }) ?? '',
    type: isActivating ? 'info' : 'warning',
    confirmLabel: deps.translate?.instant('COMMON.CONFIRM') ?? 'Confirm'
  }).then(confirmed => {
    if (confirmed) {
      deps.facade.toggleActive(entity.id, isActivating, () => {
        deps.notify.showSuccess(
          deps.translate?.instant(
            isActivating ? '<FEATURE>S.ACTIVATED_SUCCESS' : '<FEATURE>S.DEACTIVATED_SUCCESS'
          ) ?? ''
        );
        onDone?.();
      });
    }
  });
}

// ─── Delete ───

export function confirmDelete<ENTITY_NAME>(
  deps: ConfirmActionDeps,
  entity: <ENTITY_NAME>Dto,
  onDone?: () => void
): void {
  // 1. Permission check FIRST
  if (!deps.auth.hasPermission('PERM_<ENTITY_PERM>_DELETE')) {
    deps.notify.showWarning(deps.translate?.instant('ERRORS.NO_PERMISSION') ?? 'No permission');
    return;
  }

  // 2. Fetch usage info BEFORE showing confirm dialog
  deps.facade.getUsageInfo?.(entity.id);

  // Wait for usage — then check canDelete
  // (In practice, usage is fetched and checked before rendering dialog)
  const usage = deps.facade.usageInfo?.();
  if (usage && !usage.canDelete) {
    deps.dialog.confirm({
      title: deps.translate?.instant('COMMON.DELETE_BLOCKED') ?? '',
      message: deps.translate?.instant('<FEATURE>S.DELETE_BLOCKED_REASON', {
        reason: usage.deleteBlockedReason
      }) ?? '',
      type: 'warning',
      showCancel: false
    });
    return;
  }

  // 3. Show confirm dialog (danger type)
  deps.dialog.confirm({
    title: deps.translate?.instant('COMMON.DELETE') ?? '',
    message: deps.translate?.instant('<FEATURE>S.CONFIRM_DELETE', { name: entity.<identifierField> }) ?? '',
    type: 'danger',
    confirmLabel: deps.translate?.instant('COMMON.DELETE') ?? 'Delete'
  }).then(confirmed => {
    if (confirmed) {
      deps.facade.deleteEntity(entity.id, () => {
        deps.notify.showSuccess(deps.translate?.instant('<FEATURE>S.DELETED_SUCCESS') ?? '');
        onDone?.();
      });
    }
  });
}
```

### Child Confirm Actions (if `HAS_CHILD = true`)
```typescript
export function confirmToggle<CHILD_NAME>Active(
  deps: ConfirmActionDeps,
  child: <CHILD_NAME>Dto,
  onDone?: () => void
): void {
  // Same pattern as parent — permission check → dialog → facade.toggleChildActive()
}

export function confirmDelete<CHILD_NAME>(
  deps: ConfirmActionDeps,
  child: <CHILD_NAME>Dto,
  onDone?: () => void
): void {
  // Same pattern — permission check → usage check → dialog → facade.deleteChild()
}
```

---

## PART 2: Routing Configuration

### File Location
Add routes to `frontend/src/app/modules/<DOMAIN_DIR>/<DOMAIN>-routing.module.ts`

### Route Structure
```typescript
import { Routes } from '@angular/router';
import { authGuard } from '../../../core/guards/auth.guard';
import { permissionGuard } from '../../../core/guards/permission.guard';
import { AdminLayout } from '../../../theme/admin-layout/admin-layout.component';

const <ENTITY_KEBAB>Routes: Routes = [
  {
    path: '<ENTITY_URL>',
    component: AdminLayout,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./<FEATURE_DIR>/pages/<ENTITY_KEBAB>-search/<ENTITY_KEBAB>-search.component')
            .then(m => m.<ENTITY_NAME>SearchComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_<ENTITY_PERM>_VIEW' }
      },
      {
        path: 'create',
        loadComponent: () =>
          import('./<FEATURE_DIR>/pages/<ENTITY_KEBAB>-entry/<ENTITY_KEBAB>-entry.component')
            .then(m => m.<ENTITY_NAME>EntryComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_<ENTITY_PERM>_CREATE' }
      },
      {
        path: 'edit/:id',
        loadComponent: () =>
          import('./<FEATURE_DIR>/pages/<ENTITY_KEBAB>-entry/<ENTITY_KEBAB>-entry.component')
            .then(m => m.<ENTITY_NAME>EntryComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_<ENTITY_PERM>_UPDATE' }
      }
    ]
  }
];
```

---

## SHARED LAYER MANDATE

Before creating routing and confirm actions, verify the following shared resources are consumed — do NOT reinvent:

| # | Requirement | Shared Resource | Import Path |
|---|-------------|----------------|-------------|
| SH.1 | Authentication gate | `authGuard` | `core/guards/auth.guard` |
| SH.2 | Permission gate | `permissionGuard` | `core/guards/permission.guard` |
| SH.3 | Page layout wrapper | `AdminLayout` | `theme/admin-layout/admin-layout.component` |
| SH.4 | Confirmation dialogs | `ErpDialogService.confirm()` | `shared/services/erp-dialog.service` |
| SH.5 | Toast notifications | `ErpNotificationService` | `shared/services/erp-notification.service` |
| SH.6 | Permission programmatic checks | `AuthenticationService.hasPermission()` | `core/services/authentication.service` |

**Rules:**
- NEVER create custom route guards — use `authGuard` + `permissionGuard`
- NEVER create custom confirmation dialogs — use `ErpDialogService.confirm()`
- NEVER create custom notification toasts — use `ErpNotificationService`
- NEVER bypass the `ConfirmActionDeps` pattern for confirm action helpers
- Confirm actions MUST follow the permission-first pattern: check `hasPermission()` → check `canDelete` → show dialog → call facade

> **Cross-reference:** After creating routing, run [`enforce-reusability`](../enforce-reusability/SKILL.md) to verify no shared patterns were reinvented.

---

## Contract Rules

| # | Rule | Source | Violation |
|---|------|--------|-----------|
| B.5.1 | Three routes per feature: `''` (list), `'create'`, `'edit/:id'` | Contract B.5.1 | Combined create/edit without route param |
| B.5.2 | `loadComponent` lazy loading (not `component`) | Contract B.5.2 | Eager component loading |
| B.5.3 | `canActivate: [authGuard, permissionGuard]` on every route | Contract B.5.3 | Missing guards |
| B.5.4 | `data: { permission: 'PERM_...' }` matches `SecurityPermissions` constants | Contract B.5.4 | Mismatched permission strings |
| B.5.5 | Routes wrapped in `AdminLayout` parent | Contract B.5.5 | No layout wrapper |
| B.6.1 | Confirm actions extracted to `helpers/<feature>-confirm-actions.ts` | Contract B.6.1 | Inline confirmation logic |
| B.6.2 | Functions receive `ConfirmActionDeps` interface | Contract B.6.2 | Direct service injection |
| B.6.3 | Every action checks permission FIRST before showing dialog | Contract B.6.3 | Dialog shown then permission denied |
| B.6.4 | Delete checks `usage.canDelete` BEFORE showing confirm dialog | Contract B.6.4 | Confirm shown then server rejects |
| B.6.5 | Dialog type: `'warning'` for toggle, `'danger'` for delete | Contract B.6.5 | Wrong dialog severity |
| B.6.6 | Dialog uses translation keys with `messageParams` | Contract B.6.6 | Hardcoded entity names |
| C.2.3 | Frontend: route guard + directive + programmatic check (triple enforcement) | Contract C.2.3 | Missing any protection layer |
| C.5.4 | Frontend fetches usage info and blocks delete dialog if `!canDelete` | Contract C.5.4 | Delete without usage check |
| C.5.5 | Delete confirmation uses `type: 'danger'` | Contract C.5.5 | Wrong dialog type |

---

## Violations Requiring Immediate Rejection

| Pattern | Rule Violated |
|---------|--------------|
| Eager component loading with `component:` property | B.5.2 |
| Missing `canActivate` guards on any route | B.5.3 |
| Missing `data: { permission: '...' }` on any route | B.5.4 |
| Routes not wrapped in `AdminLayout` parent | B.5.5 |
| Single route for both create and edit | B.5.1 |
| Inline confirmation logic in component instead of helper file | B.6.1 |
| Confirm action that doesn't check permission first | B.6.3 |
| Delete action that doesn't check `canDelete` from usage | B.6.4 |
| Delete dialog using `type: 'warning'` instead of `'danger'` | B.6.5, C.5.5 |
| Permission string doesn't match `SecurityPermissions` constant naming | B.5.4 |

---

## Real ERP Example: MasterLookup Routing

### `master-data-routing.module.ts` (excerpt)
```typescript
{
  path: 'master-lookups',
  component: AdminLayout,
  children: [
    {
      path: '',
      loadComponent: () =>
        import('./master-lookups/pages/master-lookup-search/master-lookup-search.component')
          .then(m => m.MasterLookupSearchComponent),
      canActivate: [authGuard, permissionGuard],
      data: { permission: 'PERM_MASTER_LOOKUP_VIEW' }
    },
    {
      path: 'create',
      loadComponent: () =>
        import('./master-lookups/pages/master-lookup-entry/master-lookup-entry.component')
          .then(m => m.MasterLookupEntryComponent),
      canActivate: [authGuard, permissionGuard],
      data: { permission: 'PERM_MASTER_LOOKUP_CREATE' }
    },
    {
      path: 'edit/:id',
      loadComponent: () =>
        import('./master-lookups/pages/master-lookup-entry/master-lookup-entry.component')
          .then(m => m.MasterLookupEntryComponent),
      canActivate: [authGuard, permissionGuard],
      data: { permission: 'PERM_MASTER_LOOKUP_UPDATE' }
    }
  ]
}
```

### `master-lookup-confirm-actions.ts` (excerpt)
```typescript
export function confirmDeleteMasterLookup(
  deps: ConfirmActionDeps,
  entity: MasterLookupDto,
  onDone?: () => void
): void {
  if (!deps.auth.hasPermission('PERM_MASTER_LOOKUP_DELETE')) {
    deps.notify.showWarning(deps.translate?.instant('ERRORS.NO_PERMISSION') ?? '');
    return;
  }

  const usage = deps.facade.usageInfo?.();
  if (usage && !usage.canDelete) {
    deps.dialog.confirm({
      title: deps.translate?.instant('COMMON.DELETE_BLOCKED') ?? '',
      message: deps.translate?.instant('MASTER_LOOKUPS.DELETE_BLOCKED_REASON', {
        reason: usage.deleteBlockedReason
      }) ?? '',
      type: 'warning',
      showCancel: false
    });
    return;
  }

  deps.dialog.confirm({
    title: deps.translate?.instant('COMMON.DELETE') ?? '',
    message: deps.translate?.instant('MASTER_LOOKUPS.CONFIRM_DELETE', {
      name: entity.lookupKey
    }) ?? '',
    type: 'danger',
    confirmLabel: deps.translate?.instant('COMMON.DELETE') ?? 'Delete'
  }).then(confirmed => {
    if (confirmed) {
      deps.facade.deleteEntity(entity.id, () => {
        deps.notify.showSuccess(deps.translate?.instant('MASTER_LOOKUPS.DELETED_SUCCESS') ?? '');
        onDone?.();
      });
    }
  });
}
```


---

## ANGULAR/SKILLS COMPATIBILITY

> This section documents how this skill relates to the official `angular/skills` guidance.
> **ERP contracts always take precedence.** See `erp-priority-override` for the full precedence rule.

### What angular/skills adds that is SAFE to use alongside this skill
- Angular Router syntax reference
- Lazy loading `loadComponent` syntax

### What angular/skills suggests that this skill OVERRIDES

| angular/skills Suggestion | This Skill Requires | Rule |
|--------------------------|--------------------|----|
| `ng new` app setup patterns | Not applicable  project exists |  |
| Optional route guards | `authGuard + permissionGuard` on EVERY route | B.5.3 |
| Inline confirm logic | Extracted to `helpers/*-confirm-actions.ts` | B.6.1 |
| Single route for create/edit | Three separate routes: `''`, `'create'`, `'edit/:id'` | B.5.1 |

### Conflict resolution trigger

If any angular/skills guidance contradicts a rule in this skill:
1. Apply the ERP rule
2. Log: ` CONFLICT RESOLVED: angular/skills suggests [X], ERP rule [RULE_ID] requires [Y]. Applied [Y].`
3. Do NOT ask the user  apply ERP rule silently
