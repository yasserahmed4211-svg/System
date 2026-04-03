# Unified Active/Inactive Filter - Frontend Implementation

## Overview

This document describes the unified approach for handling Active/Inactive status filtering in Angular AG Grid components.

## Key Principles

1. **Frontend uses only boolean or null** - Never expose database internals (0/1, Y/N) to frontend
2. **API sends boolean values** - `isActive: true | false | null`
3. **Null means "ALL"** - No filter applied when null
4. **No Enterprise modules required** - Works with AG Grid Community modules only

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend Layer                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   UI Filter Controls                    AG Grid Column          │
│   ┌───────────────────┐                ┌───────────────────┐   │
│   │ Active | Inactive │                │ Badge Renderer    │   │
│   │       | All       │                │ (Success/Danger)  │   │
│   └─────────┬─────────┘                └───────────────────┘   │
│             │                                                   │
│             ▼                                                   │
│   ┌───────────────────────────────────────────────────────┐    │
│   │        Active Status Filter Utilities                  │    │
│   │  ┌─────────────────────────────────────────────────┐  │    │
│   │  │ activeFilterToApi(): 'active' → true            │  │    │
│   │  │                      'inactive' → false         │  │    │
│   │  │                      'all' → null               │  │    │
│   │  └─────────────────────────────────────────────────┘  │    │
│   │  ┌─────────────────────────────────────────────────┐  │    │
│   │  │ normalizeActiveValue(): handles edge cases      │  │    │
│   │  └─────────────────────────────────────────────────┘  │    │
│   └─────────────────────────────────────────────────────────┘   │
│                              │                                   │
│                              ▼                                   │
│   ┌───────────────────────────────────────────────────────┐    │
│   │             API Request (HttpClient)                   │    │
│   │  { isActive: true | false | null }                     │    │
│   └───────────────────────────────────────────────────────┘    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Backend Layer                           │
│  Boolean ↔ NUMBER(1) conversion via BooleanNumberConverter      │
└─────────────────────────────────────────────────────────────────┘
```

## Files Created/Modified

### New Files

| File | Purpose |
|------|---------|
| `shared/ag-grid/erp-ag-grid-modules.ts` | Centralized AG Grid module registration |
| `shared/ag-grid/active-status-filter.utils.ts` | Core utilities for active status handling |
| `shared/ag-grid/active-filter.component.ts` | Reusable filter component (Bootstrap buttons) |
| `shared/ag-grid/active-status-filter.utils.spec.ts` | Unit tests |

### Modified Files

| File | Changes |
|------|---------|
| `shared/ag-grid/index.ts` | Added exports for new utilities |
| `role-access-control.component.ts` | Updated to use unified active column |
| `pages-search.component.ts` | Updated to use unified active column |
| `user-list.component.ts` | Updated to use unified active column |

## Usage Guide

### 1. Register AG Grid Modules (Once per app)

```typescript
import { registerErpAgGridModules } from 'src/app/shared/ag-grid';

// In your component or app initialization
registerErpAgGridModules();
```

### 2. Create Active Column Definition

```typescript
import { createActiveColumnDef, ActiveColumnLabels } from 'src/app/shared/ag-grid';

// Build labels (with i18n)
const activeLabels: ActiveColumnLabels = {
  active: this.translate.instant('COMMON.ACTIVE'),
  inactive: this.translate.instant('COMMON.INACTIVE'),
  all: this.translate.instant('COMMON.ALL')
};

// In column definitions
this.columnDefs = [
  // ... other columns
  createActiveColumnDef(activeLabels, {
    headerName: this.translate.instant('USERS.STATUS'),
    maxWidth: 120,
    floatingFilter: false  // Use external filter controls
  }),
  // ... more columns
];
```

### 3. Handle Filter Value Changes

```typescript
import {
  ActiveFilterValue,
  activeFilterToApi,
  createActiveSearchFilter
} from 'src/app/shared/ag-grid';

// In component
activeFilter: ActiveFilterValue = 'active'; // Default: show active only

onActiveFilterChange(filterValue: ActiveFilterValue): void {
  // For API requests
  const apiValue = activeFilterToApi(filterValue);
  // apiValue is: true | false | null

  // Or create a search filter object
  const searchFilter = createActiveSearchFilter(filterValue);
  // Returns { field: 'active', op: 'EQ', value: true/false } or null for 'all'
}
```

### 4. Use the Filter Component (Optional)

```html
<erp-active-filter
  [(value)]="activeFilter"
  (apiValueChange)="onActiveApiFilterChange($event)"
  [defaultShowActive]="true">
</erp-active-filter>
```

```typescript
import { ActiveFilterComponent, ActiveStatus } from 'src/app/shared/ag-grid';

@Component({
  imports: [ActiveFilterComponent, /* ... */],
  // ...
})
export class MyComponent {
  activeFilter: ActiveFilterValue = 'active';

  onActiveApiFilterChange(apiValue: ActiveStatus): void {
    // apiValue is boolean | null, ready for API
    this.facade.setActiveFilter(apiValue);
    this.reload();
  }
}
```

## Important: No SetFilterModule

**DO NOT USE** `agSetColumnFilter` or `agSetColumnFloatingFilter` in column definitions!

These require the Enterprise `SetFilterModule` which is NOT registered.

✅ **Correct:**
```typescript
filter: 'agTextColumnFilter'  // Community module
```

❌ **Wrong:**
```typescript
filter: 'agSetColumnFilter'   // Requires Enterprise license
```

## Type Definitions

```typescript
// UI Filter Values (what the component uses)
type ActiveFilterValue = 'active' | 'inactive' | 'all';

// API Values (what goes to backend)
type ActiveStatus = boolean | null;

// Labels for i18n
interface ActiveColumnLabels {
  active: string;
  inactive: string;
  all?: string;
}
```

## Value Mapping

| UI Value | API Value | Database Value |
|----------|-----------|----------------|
| `'active'` | `true` | `1` |
| `'inactive'` | `false` | `0` |
| `'all'` | `null` | (no filter) |

## Cell Rendering

The `createActiveColumnDef` function automatically sets up:

- **Cell Renderer**: Shows Bootstrap badge
  - Active: `<span class="badge bg-success">Active</span>`
  - Inactive: `<span class="badge bg-secondary">Inactive</span>`

- **Value Formatter**: Returns translated label for filters

## Testing

Run the unit tests:
```bash
cd frontend
npm test -- --include=**/active-status-filter.utils.spec.ts
```

## Migration Notes

When updating existing components:

1. Replace `ModuleRegistry.registerModules([...])` with `registerErpAgGridModules()`
2. Replace manual active column definitions with `createActiveColumnDef()`
3. Remove `agSetColumnFilter` if present
4. Use `ActiveColumnLabels` for i18n consistency
