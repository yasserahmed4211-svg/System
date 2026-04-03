# ERP Secure Routing Implementation Guide

## Overview

This document describes the secure routing implementation for the ERP system using **AuthGuard** and **PermissionGuard** to protect routes at the application level.

---

## 🔐 Guards

### 1. **AuthGuard**
- **Purpose**: Ensures the user is authenticated (logged in)
- **Location**: `src/app/core/guards/auth.guard.ts`
- **Behavior**:
  - ✅ Allows navigation if `authService.isLoggedIn()` returns `true`
  - ❌ Redirects to `/login` if user is not authenticated
  - Preserves the intended URL in `returnUrl` query parameter

**Usage**: Apply to parent routes or route groups

```typescript
{
  path: '',
  component: AdminLayout,
  canActivateChild: [AuthGuard],  // ← Protects all child routes
  children: [...]
}
```

---

### 2. **PermissionGuard**
- **Purpose**: Ensures the user has the required permission to access a route
- **Location**: `src/app/core/guards/permission.guard.ts`
- **Behavior**:
  - Reads `route.data.permission` from the route configuration
  - If **no permission** is defined → ✅ Allows navigation
  - If **permission exists** → Checks using `authService.hasPermission(permission)`
    - ✅ Allows navigation if user has permission
    - ❌ Redirects to `/access-denied` if user lacks permission

**Usage**: Apply to individual routes that require specific permissions

```typescript
{
  path: 'users',
  component: UserListComponent,
  canActivate: [PermissionGuard],
  data: { permission: 'PERM_USER_VIEW' }  // ← Required permission
}
```

---

## 📋 Implementation Pattern

### Standard Protected Route

```typescript
{
  path: 'your-module',
  component: AdminLayout,
  canActivateChild: [AuthGuard],  // Step 1: Check authentication
  children: [
    {
      path: 'feature',
      component: FeatureComponent,
      canActivate: [PermissionGuard],  // Step 2: Check permission
      data: { permission: 'PERM_FEATURE_VIEW' }
    }
  ]
}
```

### Execution Flow

1. **AuthGuard** runs first (via `canActivateChild`)
   - If not logged in → redirect to `/login` ❌
   - If logged in → proceed ✅

2. **PermissionGuard** runs second (via `canActivate`)
   - If no permission defined → allow ✅
   - If permission defined → check user permissions
     - Has permission → allow ✅
     - No permission → redirect to `/access-denied` ❌

---

## 🎯 Permission Naming Convention

Permissions follow the pattern: **`PERM_<RESOURCE>_<ACTION>`**

### Examples:

| Module         | Permission Key           | Description                |
|----------------|--------------------------|----------------------------|
| Users          | `PERM_USER_VIEW`         | View user list             |
| Users          | `PERM_USER_CREATE`       | Create new user            |
| Users          | `PERM_USER_UPDATE`       | Edit existing user         |
| Users          | `PERM_USER_DELETE`       | Delete user                |
| Pages          | `PERM_PAGE_VIEW`         | View pages registry        |
| Pages          | `PERM_PAGE_UPDATE`       | Edit page configuration    |
| Role Access    | `PERM_ROLE_VIEW`         | View role access control   |
| Finance        | `PERM_INVOICE_VIEW`      | View invoices              |
| Finance        | `PERM_INVOICE_CREATE`    | Create invoice             |

---

## 🚫 Access Denied Page

### Location
`src/app/core/components/access-denied/`

### Features
- User-friendly message explaining access is denied
- No technical errors or stack traces
- Two action buttons:
  - **Go Back**: Returns to previous page
  - **Go to Home**: Navigates to home page

### Route
```typescript
{
  path: 'access-denied',
  loadComponent: () => 
    import('./core/components/access-denied/access-denied.component').then(c => c.AccessDeniedComponent)
}
```

---

## ✅ Example: Security Module Routes

**Before** (Role-based - ❌ Deprecated):

```typescript
{
  path: 'users',
  component: UserListComponent,
  data: { roles: [Role.Admin] }  // ❌ Old approach
}
```

**After** (Permission-based - ✅ Current):

```typescript
{
  path: 'users',
  component: UserListComponent,
  canActivate: [PermissionGuard],
  data: { permission: 'PERM_USER_VIEW' }  // ✅ New approach
}
```

---

## 🔧 How to Add Guards to a New Route

### Step 1: Import Guards
```typescript
import { AuthGuard, PermissionGuard } from 'src/app/core/guards';
```

### Step 2: Apply to Parent Layout
```typescript
{
  path: '',
  component: AdminLayout,
  canActivateChild: [AuthGuard],  // Protects all children
  children: [...]
}
```

### Step 3: Apply Permission Guard to Child Routes
```typescript
{
  path: 'my-feature',
  component: MyFeatureComponent,
  canActivate: [PermissionGuard],
  data: { permission: 'PERM_MY_FEATURE_VIEW' }
}
```

---

## 🧪 Testing Checklist

### ✅ Test Scenarios

1. **Unauthenticated User**
   - [ ] Cannot access protected routes
   - [ ] Redirected to `/login`
   - [ ] Return URL preserved in query params

2. **Authenticated User WITHOUT Permission**
   - [ ] Can access layout/shell
   - [ ] Cannot access specific feature
   - [ ] Redirected to `/access-denied`
   - [ ] Menu item hidden (if dynamic menu implemented)

3. **Authenticated User WITH Permission**
   - [ ] Can access all authorized routes
   - [ ] Menu item visible
   - [ ] No redirects

4. **Direct URL Access**
   - [ ] Typing URL manually triggers guards
   - [ ] Same behavior as clicking menu

---

## 📝 Important Notes

### ⚠️ DO NOT Modify

- **Dynamic Menu Logic**: Menu already hides items based on permissions
- **Permission Keys**: Use existing keys from backend
- **AuthenticationService**: Methods `isLoggedIn()` and `hasPermission()` must not be changed

### ✅ DO Use

- **Same Permission Keys**: In routes, menus, and UI directives
- **PermissionGuard**: For all feature routes requiring authorization
- **AuthGuard**: For all authenticated areas

### 🔄 Consistency

The **same permission key** must be used in:
1. **Route guard** → `data: { permission: 'PERM_USER_VIEW' }`
2. **Dynamic menu** → Checks same permission to show/hide menu item
3. **UI elements** → `*erpPermission="'PERM_USER_VIEW'"`
4. **Backend API** → Same permission enforced on endpoints

---

## 🏗️ Architecture Benefits

✅ **Single Source of Truth**: Permissions defined once, used everywhere
✅ **Security in Depth**: Both route-level and API-level protection
✅ **Clean Separation**: Authentication ≠ Authorization
✅ **Scalable**: Easy to add new permissions without modifying guards
✅ **User-Friendly**: Clear error messages, no technical jargon

---

## 📚 Related Files

| File | Purpose |
|------|---------|
| `core/guards/auth.guard.ts` | Authentication check |
| `core/guards/permission.guard.ts` | Permission check |
| `core/guards/index.ts` | Guard exports |
| `core/services/authentication.service.ts` | Auth methods |
| `core/components/access-denied/` | Access denied page |
| `app-routing.module.ts` | Main routes + access-denied route |
| `modules/*/routing.module.ts` | Module-specific routes |

---

## 🎓 Summary

- **AuthGuard**: "Are you logged in?"
- **PermissionGuard**: "Are you allowed to see this?"
- **Access Denied**: "Sorry, you can't access this page."

Apply both guards to all protected routes for maximum security! 🔒
