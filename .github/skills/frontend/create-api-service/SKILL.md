---
description: "Generates the feature API service extending BaseApiService with doGet/doPost/doPut/doDelete. Phase 2, Step 2.2 — AFTER models, BEFORE facade. NOT providedIn root. Uses environment.authApiUrl."
---

# Skill: create-api-service

## Name
`create-api-service`

## Description
Generates the feature API service that extends `BaseApiService` for HTTP communication. This is **Phase 2, Step 2.2** of the execution template. MUST be created AFTER models and BEFORE facade.

## When to Use
- When implementing a new frontend feature's HTTP layer
- When the execution template Phase 2, Step 2.2 is being started
- AFTER models are defined, BEFORE facade

## Variables (Must Be Defined First)

| Variable | Example | Description |
|----------|---------|-------------|
| `DOMAIN_DIR` | `master-data` | Module domain directory (kebab-case) |
| `FEATURE_DIR` | `master-lookups` | Feature directory (kebab-case plural) |
| `ENTITY_NAME` | `MasterLookup` | PascalCase entity name |
| `ENTITY_KEBAB` | `master-lookup` | kebab-case entity name |
| `MODULE_URL` | `masterdata` | Backend module URL segment |
| `ENTITY_URL` | `master-lookups` | Backend entity URL path (kebab-case plural) |
| `HAS_CHILD` | `true/false` | Whether entity has child entities |
| `CHILD_NAME` | `LookupDetail` | PascalCase child name (if applicable) |

## Responsibilities

- Generate API service class extending `BaseApiService`
- Implement CRUD methods using `doGet`, `doPost`, `doPut`, `doDelete` from base class
- Use `environment.authApiUrl` for base URL construction
- Include child entity API methods if `HAS_CHILD` is true

## Constraints

- MUST NOT generate models, facade, or component code
- MUST NOT use `HttpClient` directly — must extend `BaseApiService`
- MUST NOT use `providedIn: 'root'` — service is component-scoped
- MUST NOT use `.pipe(map(...))` for response unwrapping — base class handles it
- MUST NOT hardcode API URLs

## Output

- Single file: `frontend/src/app/modules/<DOMAIN_DIR>/<FEATURE_DIR>/services/<feature>-api.service.ts`

---

## Steps

### 1. Create Service File
- **Location:** `frontend/src/app/modules/<DOMAIN_DIR>/<FEATURE_DIR>/services/<ENTITY_KEBAB>-api.service.ts`

### 2. Service Class Structure
```typescript
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { BaseApiService } from '../../../../shared/base/base-api.service';
import {
  <ENTITY_NAME>Dto,
  <ENTITY_NAME>UsageDto,
  Create<ENTITY_NAME>Request,
  Update<ENTITY_NAME>Request,
  PagedResponse,
  SearchRequest
} from '../models/<ENTITY_KEBAB>.model';

@Injectable()  // NOT providedIn: 'root'
export class <ENTITY_NAME>ApiService extends BaseApiService {

  private readonly baseUrl = `${environment.authApiUrl}/api/<MODULE_URL>`;
  private readonly entityUrl = `${this.baseUrl}/<ENTITY_URL>`;

  // ---------- Parent CRUD ----------

  search(request: SearchRequest): Observable<PagedResponse<<ENTITY_NAME>Dto>> {
    return this.doPost<PagedResponse<<ENTITY_NAME>Dto>>(`${this.entityUrl}/search`, request);
  }

  getById(id: number): Observable<<ENTITY_NAME>Dto> {
    return this.doGet<<ENTITY_NAME>Dto>(`${this.entityUrl}/${id}`);
  }

  create(request: Create<ENTITY_NAME>Request): Observable<<ENTITY_NAME>Dto> {
    return this.doPost<<ENTITY_NAME>Dto>(this.entityUrl, request);
  }

  update(id: number, request: Update<ENTITY_NAME>Request): Observable<<ENTITY_NAME>Dto> {
    return this.doPut<<ENTITY_NAME>Dto>(`${this.entityUrl}/${id}`, request);
  }

  toggleActive(id: number, active: boolean): Observable<<ENTITY_NAME>Dto> {
    return this.doPut<<ENTITY_NAME>Dto>(`${this.entityUrl}/${id}/toggle-active`, { active });
  }

  delete(id: number): Observable<void> {
    return this.doDelete<void>(`${this.entityUrl}/${id}`);
  }

  getUsage(id: number): Observable<<ENTITY_NAME>UsageDto> {
    return this.doGet<<ENTITY_NAME>UsageDto>(`${this.entityUrl}/${id}/usage`);
  }
}
```

### 3. Child Entity Methods (if `HAS_CHILD = true`)
```typescript
  // ---------- Child CRUD ----------

  private readonly childUrl = `${this.entityUrl}/details`;

  searchChildren(request: SearchRequest): Observable<PagedResponse<<CHILD_NAME>Dto>> {
    return this.doPost<PagedResponse<<CHILD_NAME>Dto>>(`${this.childUrl}/search`, request);
  }

  createChild(request: Create<CHILD_NAME>Request): Observable<<CHILD_NAME>Dto> {
    return this.doPost<<CHILD_NAME>Dto>(this.childUrl, request);
  }

  updateChild(id: number, request: Update<CHILD_NAME>Request): Observable<<CHILD_NAME>Dto> {
    return this.doPut<<CHILD_NAME>Dto>(`${this.childUrl}/${id}`, request);
  }

  toggleChildActive(id: number, active: boolean): Observable<<CHILD_NAME>Dto> {
    return this.doPut<<CHILD_NAME>Dto>(`${this.childUrl}/${id}/toggle-active`, { active });
  }

  deleteChild(id: number): Observable<void> {
    return this.doDelete<void>(`${this.childUrl}/${id}`);
  }

  getChildOptions(key: string): Observable<<CHILD_NAME>Dto[]> {
    return this.doGet<<CHILD_NAME>Dto[]>(`${this.childUrl}/options/${key}`);
  }
```

---

## SHARED LAYER MANDATE

Before creating a new API service, verify the following shared resources are consumed — do NOT reinvent:

| # | Requirement | Shared Resource | Import Path |
|---|-------------|----------------|-------------|
| SH.1 | MUST extend `BaseApiService` — never inject `HttpClient` directly | `BaseApiService` | `shared/base/base-api.service` |
| SH.2 | MUST use `doGet/doPost/doPut/doDelete` from base class — never call `this.http.*` | Inherited methods | — |
| SH.3 | MUST use `environment.authApiUrl` for base URL — never hardcode URLs | `environment` | `environments/environment` |
| SH.4 | MUST import shared DTOs if they exist (`PagedResponse`, `SearchRequest`) | Shared models | `shared/models/` (if barrel export exists) |
| SH.5 | MUST NOT add `shareReplay` or caching logic — caching is backend-only (Redis) | — | — |
| SH.6 | MUST NOT duplicate response unwrapping — `BaseApiService` handles `ApiResponse<T>` unwrapping | `unwrapResponse()` | Inherited from base |

> **Cross-reference:** After creating the API service, run [`enforce-reusability`](../enforce-reusability/SKILL.md) to verify no shared code was duplicated.

---

## Contract Rules

| # | Rule | Source | Violation |
|---|------|--------|-----------|
| B.2.1 | Extends `BaseApiService` | Contract B.2.1 | Direct `HttpClient` usage |
| B.2.2 | Uses `doGet/doPost/doPut/doDelete` from base (handles response unwrapping) | Contract B.2.2 | Manual `.pipe(map(...))` for unwrapping `ApiResponse` |
| B.2.3 | NOT `providedIn: 'root'` — provided at component `providers: [...]` level | Contract B.2.3 | `@Injectable({ providedIn: 'root' })` singleton service |
| B.2.4 | Uses `environment.authApiUrl` for base URL | Contract B.2.4 | Hardcoded URL strings |
| B.2.5 | Toggle active sends `doPut(url, { active })` — body is `{ active: boolean }` | Contract B.2.5 | Sending active as query param |
| D.5.2 | Feature API services MUST NOT use `shareReplay` or in-memory caching | Contract D.5.2 | `shareReplay(1)` on CRUD service |
| D.5.4 | Frontend MUST NOT implement its own TTL or expiration logic | Contract D.5.4 | Custom `setTimeout`-based cache expiration |

---

## Violations Requiring Immediate Rejection

| Pattern | Rule Violated |
|---------|--------------|
| `@Injectable({ providedIn: 'root' })` on API service | B.2.3 |
| Direct `this.http.get(...)` or `this.http.post(...)` calls | B.2.1, B.2.2 |
| Hardcoded API base URL (e.g., `'http://localhost:8080'`) | B.2.4 |
| `shareReplay(1)` in a feature CRUD API service | D.5.2 |
| Manual `.pipe(map(res => res.data))` to unwrap `ApiResponse` | B.2.2 |
| Toggle active sending `{ isActive: boolean }` instead of `{ active: boolean }` | B.2.5 |
| Missing `search()` method using POST | Blueprint §7.1 |
| Not extending `BaseApiService` | B.2.1 |

---

## Real ERP Example: MasterLookup API Service

```typescript
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { BaseApiService } from '../../../../shared/base/base-api.service';
import {
  MasterLookupDto,
  MasterLookupUsageDto,
  CreateMasterLookupRequest,
  UpdateMasterLookupRequest,
  LookupDetailDto,
  CreateLookupDetailRequest,
  UpdateLookupDetailRequest,
  PagedResponse,
  SearchRequest
} from '../models/master-lookup.model';

@Injectable()
export class MasterLookupApiService extends BaseApiService {

  private readonly baseUrl = `${environment.authApiUrl}/api/masterdata`;
  private readonly entityUrl = `${this.baseUrl}/master-lookups`;
  private readonly detailUrl = `${this.entityUrl}/details`;

  // ---------- Master Lookup CRUD ----------

  search(request: SearchRequest): Observable<PagedResponse<MasterLookupDto>> {
    return this.doPost<PagedResponse<MasterLookupDto>>(`${this.entityUrl}/search`, request);
  }

  getById(id: number): Observable<MasterLookupDto> {
    return this.doGet<MasterLookupDto>(`${this.entityUrl}/${id}`);
  }

  create(request: CreateMasterLookupRequest): Observable<MasterLookupDto> {
    return this.doPost<MasterLookupDto>(this.entityUrl, request);
  }

  update(id: number, request: UpdateMasterLookupRequest): Observable<MasterLookupDto> {
    return this.doPut<MasterLookupDto>(`${this.entityUrl}/${id}`, request);
  }

  toggleActive(id: number, active: boolean): Observable<MasterLookupDto> {
    return this.doPut<MasterLookupDto>(`${this.entityUrl}/${id}/toggle-active`, { active });
  }

  delete(id: number): Observable<void> {
    return this.doDelete<void>(`${this.entityUrl}/${id}`);
  }

  getUsage(id: number): Observable<MasterLookupUsageDto> {
    return this.doGet<MasterLookupUsageDto>(`${this.entityUrl}/${id}/usage`);
  }

  // ---------- Lookup Detail CRUD ----------

  searchDetails(request: SearchRequest): Observable<PagedResponse<LookupDetailDto>> {
    return this.doPost<PagedResponse<LookupDetailDto>>(`${this.detailUrl}/search`, request);
  }

  createDetail(request: CreateLookupDetailRequest): Observable<LookupDetailDto> {
    return this.doPost<LookupDetailDto>(this.detailUrl, request);
  }

  updateDetail(id: number, request: UpdateLookupDetailRequest): Observable<LookupDetailDto> {
    return this.doPut<LookupDetailDto>(`${this.detailUrl}/${id}`, request);
  }

  toggleDetailActive(id: number, active: boolean): Observable<LookupDetailDto> {
    return this.doPut<LookupDetailDto>(`${this.detailUrl}/${id}/toggle-active`, { active });
  }

  deleteDetail(id: number): Observable<void> {
    return this.doDelete<void>(`${this.detailUrl}/${id}`);
  }

  getDetailOptions(key: string): Observable<LookupDetailDto[]> {
    return this.doGet<LookupDetailDto[]>(`${this.detailUrl}/options/${key}`);
  }
}
```


---

## ANGULAR/SKILLS COMPATIBILITY

> This section documents how this skill relates to the official `angular/skills` guidance.
> **ERP contracts always take precedence.** See `erp-priority-override` for the full precedence rule.

### What angular/skills adds that is SAFE to use alongside this skill
- General TypeScript and Observable patterns
- Understanding of Angular DI

### What angular/skills suggests that this skill OVERRIDES

| angular/skills Suggestion | This Skill Requires | Rule |
|--------------------------|--------------------|----|
| Direct `HttpClient` injection | `extends BaseApiService` always | B.2.1 |
| `shareReplay` for performance | PROHIBITED in CRUD services | D.5.2 |
| `providedIn: 'root'` | Component-scoped `providers: []` only | B.2.3 |
| Custom response unwrapping | `doGet/doPost/doPut/doDelete` from base | B.2.2 |

### Conflict resolution trigger

If any angular/skills guidance contradicts a rule in this skill:
1. Apply the ERP rule
2. Log: ` CONFLICT RESOLVED: angular/skills suggests [X], ERP rule [RULE_ID] requires [Y]. Applied [Y].`
3. Do NOT ask the user  apply ERP rule silently
