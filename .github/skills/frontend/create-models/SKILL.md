---
description: "Generates TypeScript model files: DTOs/interfaces in <feature>.model.ts and FormModel + FormMapper in <feature>-form.model.ts. Phase 2, Step 2.1 — BEFORE API service, facade, or components."
---

# Skill: create-models

## Name
`create-models`

## Description
Generates all TypeScript model files for a frontend feature: DTOs/interfaces in a single model file, and FormModel + FormMapper in a separate form-model file. This is **Phase 2, Step 2.1** of the execution template.

## When to Use
- When implementing a new frontend feature
- When the execution template Phase 2, Step 2.1 is being started
- BEFORE creating API service, facade, or components

## Variables (Must Be Defined First)

| Variable | Example | Description |
|----------|---------|-------------|
| `DOMAIN_DIR` | `master-data` | Module domain directory (kebab-case) |
| `FEATURE_DIR` | `master-lookups` | Feature directory (kebab-case plural) |
| `ENTITY_NAME` | `MasterLookup` | PascalCase entity name |
| `ENTITY_KEBAB` | `master-lookup` | kebab-case entity name |
| `HAS_CHILD` | `true/false` | Whether entity has child entities |
| `CHILD_NAME` | `LookupDetail` | PascalCase child entity name (if applicable) |

## Responsibilities

- Generate all TypeScript DTO interfaces matching backend response contracts exactly
- Generate `FormModel` interface and `FormMapper` class for form-to-DTO conversion
- Define `SearchRequest`, `FilterOperator`, and pagination structures
- Ensure field names, types, and optionality match backend DTOs

## Constraints

- MUST NOT generate API service, facade, or component code
- MUST NOT assume field names or types — derive from backend response DTOs
- MUST NOT place models outside the `models/` folder
- MUST NOT split DTOs across multiple files — all in single `<feature>.model.ts`

## Output

- `frontend/src/app/modules/<DOMAIN_DIR>/<FEATURE_DIR>/models/<feature>.model.ts`
- `frontend/src/app/modules/<DOMAIN_DIR>/<FEATURE_DIR>/models/<feature>-form.model.ts`

---

## Steps

### 1. Create Model File
- **Location:** `frontend/src/app/modules/<DOMAIN_DIR>/<FEATURE_DIR>/models/<ENTITY_KEBAB>.model.ts`

### 2. Entity Response DTO
```typescript
export interface <ENTITY_NAME>Dto {
  id: number;
  // Business fields matching backend Response DTO exactly
  fieldName: string;
  description?: string;
  isActive: boolean;
  // Computed fields (if parent entity)
  childCount?: number;
  // Audit fields
  createdAt?: string;
  createdBy?: string;
  updatedAt?: string;
  updatedBy?: string;
}
```

### 3. Usage DTO
```typescript
export interface <ENTITY_NAME>UsageDto {
  entityId: number;
  // Reference counts
  childEntityCount: number;
  otherReferenceCount: number;
  // Computed eligibility
  canDelete: boolean;
  canDeactivate: boolean;
  deleteBlockedReason?: string;
  deactivateBlockedReason?: string;
}
```

### 4. Create Request DTO
```typescript
export interface Create<ENTITY_NAME>Request {
  // All required fields for creation
  fieldName: string;
  description?: string;
  isActive?: boolean;  // Optional — backend defaults to true
  // FK references (if child entity)
  parentEntityId?: number;
}
```

### 5. Update Request DTO
```typescript
export interface Update<ENTITY_NAME>Request {
  // OMIT immutable fields: natural keys (code, key), FK references (parentId)
  description?: string;
  // Only mutable fields
}
```

### 6. Child Entity DTOs (if `HAS_CHILD = true`)
```typescript
export interface <CHILD_NAME>Dto {
  id: number;
  // Business fields
  code: string;
  value: string;
  sortOrder?: number;
  isActive: boolean;
  // Parent reference
  <parentField>Id: number;
  // Audit
  createdAt?: string;
  createdBy?: string;
  updatedAt?: string;
  updatedBy?: string;
}

export interface Create<CHILD_NAME>Request {
  <parentField>Id: number;  // FK to parent — required
  code: string;
  value: string;
  sortOrder?: number;
  isActive?: boolean;
}

export interface Update<CHILD_NAME>Request {
  // OMIT: code, parentId (immutable)
  value: string;
  sortOrder?: number;
}
```

### 7. Shared Search/Pagination Types
```typescript
export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface SearchFilter {
  field: string;
  operator: FilterOperator;
  value?: string | number | boolean;
}

export type FilterOperator = 'EQUALS' | 'CONTAINS' | 'STARTS_WITH';

export interface SearchSort {
  field: string;
  direction: 'ASC' | 'DESC';
}

export interface SearchRequest {
  filters: SearchFilter[];
  sorts?: SearchSort[];
  page: number;
  size: number;
}
```

> **Note:** If `PagedResponse`, `SearchFilter`, `FilterOperator`, `SearchSort`, and `SearchRequest` already exist in `shared/models/` or `core/models/`, import them instead of redefining.

### 8. Option DTO (if used in dropdowns)
```typescript
export interface <ENTITY_NAME>OptionDto {
  id: number;
  code: string;
  name: string;
  isActive: boolean;
}
```

---

### 9. Create Form Model File
- **Location:** `frontend/src/app/modules/<DOMAIN_DIR>/<FEATURE_DIR>/models/<ENTITY_KEBAB>-form.model.ts`

### 10. Form Model Interface
```typescript
import { <ENTITY_NAME>Dto, Create<ENTITY_NAME>Request, Update<ENTITY_NAME>Request } from './<ENTITY_KEBAB>.model';

export interface <ENTITY_NAME>FormModel {
  // All editable fields (mirrors form controls)
  fieldName: string;
  description: string;
  isActive: boolean;
  // Include immutable fields for display (disabled in edit mode)
  code: string;
}
```

### 11. FormMapper Const Object
```typescript
export const <ENTITY_NAME>FormMapper = {

  createEmpty(): <ENTITY_NAME>FormModel {
    return {
      fieldName: '',
      description: '',
      isActive: true,
      code: ''
    };
  },

  fromDomain(dto: <ENTITY_NAME>Dto): <ENTITY_NAME>FormModel {
    return {
      fieldName: dto.fieldName,
      description: dto.description ?? '',
      isActive: dto.isActive,
      code: dto.code
    };
  },

  toCreateRequest(model: <ENTITY_NAME>FormModel): Create<ENTITY_NAME>Request {
    return {
      fieldName: model.fieldName,
      description: model.description || undefined,
      isActive: model.isActive
    };
  },

  toUpdateRequest(model: <ENTITY_NAME>FormModel): Update<ENTITY_NAME>Request {
    // OMIT immutable fields: code, key, parentId
    return {
      description: model.description || undefined
    };
  }
};
```

---

## SHARED LAYER MANDATE

Before defining model types, **check if shared types already exist** — do NOT redefine types that live in `shared/models/`:

| # | Type | Check Location | Action |
|---|------|---------------|--------|
| SH.1 | `PagedResponse<T>` | `shared/models/` | Import if exists — do NOT redefine per feature |
| SH.2 | `SearchRequest` | `shared/models/` | Import if exists — do NOT redefine per feature |
| SH.3 | `SearchFilter` | `shared/models/` | Import if exists — do NOT redefine per feature |
| SH.4 | `FilterOperator` | `shared/models/` | Import if exists — do NOT redefine per feature |
| SH.5 | `SearchSort` | `shared/models/` | Import if exists — do NOT redefine per feature |
| SH.6 | `FormMapper` pattern | Per-feature `models/` | MUST follow canonical 4-method const object pattern |

**Rules:**
- ALWAYS check `shared/models/` before defining search/pagination interfaces
- If shared types exist, import them — NEVER redefine in feature model file
- If shared types do NOT exist yet and this is the first feature, define them in the feature model file and flag for future extraction to `shared/models/`
- `FormMapper` pattern is per-feature but MUST follow the canonical `createEmpty()`, `fromDomain()`, `toCreateRequest()`, `toUpdateRequest()` const object pattern
- Numeric form→DTO mappings MUST use `??` (nullish coalescing) — NEVER `||` (which converts `0` to `undefined`)

> **Cross-reference:** After defining models, run [`enforce-reusability`](../enforce-reusability/SKILL.md) to verify no type duplication across modules.

---

## Contract Rules

| # | Rule | Source | Violation |
|---|------|--------|-----------|
| B.1.1 | All DTOs and interfaces in single `<feature>.model.ts` file | Contract B.1.1 | Splitting DTOs across files |
| B.1.2 | `FormModel` + `FormMapper` in separate `<feature>-form.model.ts` | Contract B.1.2 | Inline DTO→form mapping in component |
| B.1.3 | `FormMapper` is a const object with `createEmpty()`, `fromDomain()`, `toCreateRequest()`, `toUpdateRequest()` | Contract B.1.3 | Manual mapping scattered in components |
| B.1.4 | `UpdateRequest` interface OMITS immutable fields (key, code, parentId) | Contract B.1.4 | Including immutable fields in update |
| B.1.5 | `PagedResponse<T>` is generic and reused | Contract B.1.5 | Feature-specific paged response type |
| B.1.6 | `SearchRequest` uses `FilterOperator = 'EQUALS' \| 'CONTAINS' \| 'STARTS_WITH'` | Contract B.1.6 | Custom operator types |
| C.3.3 | Update DTOs EXCLUDE immutable fields | Contract C.3.3 | Including natural keys in UpdateRequest |
| B.4.16 | Numeric form→DTO mappings use `??` (nullish coalescing) NOT `\|\|` | Contract B.4.16 | `sortOrder: formValue.sortOrder \|\| undefined` converts 0 to undefined |

---

## Violations Requiring Immediate Rejection

| Pattern | Rule Violated |
|---------|--------------|
| DTOs split across multiple model files | B.1.1 |
| FormMapper defined inline inside a component | B.1.2, B.1.3 |
| `UpdateRequest` includes `code`, `key`, or `parentId` | B.1.4, C.3.3 |
| Custom paged response type per feature | B.1.5 |
| `formValue.sortOrder \|\| undefined` for numeric fields | B.4.16 |
| FormMapper missing any of the 4 required methods | B.1.3 |
| Frontend DTO field names don't match backend Response DTO | Blueprint §1.4 |

---

## Real ERP Example: MasterLookup Models

### `master-lookup.model.ts`
```typescript
export interface MasterLookupDto {
  id: number;
  lookupKey: string;
  description: string;
  isActive: boolean;
  detailCount: number;
  createdAt?: string;
  createdBy?: string;
  updatedAt?: string;
  updatedBy?: string;
}

export interface MasterLookupUsageDto {
  entityId: number;
  detailCount: number;
  canDelete: boolean;
  canDeactivate: boolean;
  deleteBlockedReason?: string;
  deactivateBlockedReason?: string;
}

export interface CreateMasterLookupRequest {
  lookupKey: string;
  description: string;
}

export interface UpdateMasterLookupRequest {
  // lookupKey is IMMUTABLE — excluded
  description: string;
}

export interface LookupDetailDto {
  id: number;
  code: string;
  value: string;
  sortOrder?: number;
  isActive: boolean;
  masterLookupId: number;
  createdAt?: string;
  createdBy?: string;
  updatedAt?: string;
  updatedBy?: string;
}

export interface CreateLookupDetailRequest {
  masterLookupId: number;
  code: string;
  value: string;
  sortOrder?: number;
  isActive?: boolean;
}

export interface UpdateLookupDetailRequest {
  // code and masterLookupId are IMMUTABLE — excluded
  value: string;
  sortOrder?: number;
}
```

### `master-lookup-form.model.ts`
```typescript
import { MasterLookupDto, CreateMasterLookupRequest, UpdateMasterLookupRequest } from './master-lookup.model';

export interface MasterLookupFormModel {
  lookupKey: string;
  description: string;
}

export const MasterLookupFormMapper = {
  createEmpty(): MasterLookupFormModel {
    return { lookupKey: '', description: '' };
  },
  fromDomain(dto: MasterLookupDto): MasterLookupFormModel {
    return { lookupKey: dto.lookupKey, description: dto.description ?? '' };
  },
  toCreateRequest(model: MasterLookupFormModel): CreateMasterLookupRequest {
    return { lookupKey: model.lookupKey, description: model.description };
  },
  toUpdateRequest(model: MasterLookupFormModel): UpdateMasterLookupRequest {
    return { description: model.description };
  }
};
```


---

## ANGULAR/SKILLS COMPATIBILITY

> This section documents how this skill relates to the official `angular/skills` guidance.
> **ERP contracts always take precedence.** See `erp-priority-override` for the full precedence rule.

### What angular/skills adds that is SAFE to use alongside this skill
- `signal()` and `computed()` syntax for understanding reactive types
- TypeScript strict mode patterns

### What angular/skills suggests that this skill OVERRIDES

| angular/skills Suggestion | This Skill Requires | Rule |
|--------------------------|--------------------|----|
| Signal Forms (`FormField`, `SignalForm`) | `FormGroup + FormBuilder` always | B.1.2, B.1.3 |
| Flexible DTO naming | Field names MUST match backend Response DTO exactly | Blueprint 1.4 |

### Additional prohibition
Signal Forms experimental API is **permanently forbidden** in FormModel and FormMapper.
`FormMapper` MUST implement the canonical 4-method const object pattern regardless of Angular version.

### Conflict resolution trigger

If any angular/skills guidance contradicts a rule in this skill:
1. Apply the ERP rule
2. Log: ` CONFLICT RESOLVED: angular/skills suggests [X], ERP rule [RULE_ID] requires [Y]. Applied [Y].`
3. Do NOT ask the user  apply ERP rule silently
