---
description: "MASTER FRONTEND VALIDATION — comprehensively scores a complete frontend feature (120 points across 5 stages). Combines all frontend enforcement skills. Auto-rejects for providedIn root, BehaviorSubject, missing OnPush, missing guards."
---

# Skill: validate-frontend-feature

## Name
`validate-frontend-feature`

## Description
Master validation skill that comprehensively scores a complete frontend feature implementation against ALL ERP governance contracts. This is the frontend equivalent of `validate-backend-feature`. It combines all frontend enforcement skills into a single weighted scoring framework with automatic rejection thresholds.

## When to Use
- After completing a full frontend feature implementation (Phase 2 complete)
- Before marking a frontend feature as "done"
- When performing a comprehensive audit of an existing frontend feature
- During final review before merge

## Responsibilities

- Score a complete frontend feature across 5 stages (120 points total)
- Verify file completeness (models, services, facade, components, routing)
- Validate architectural compliance across all governance rules
- Run all enforcement checks: architecture, permissions, state management, UI/UX, design system
- Apply automatic rejection for critical violations

## Constraints

- MUST NOT generate or modify application code — this skill only validates and scores
- MUST NOT accept partial features — all required artifacts must be present
- MUST NOT skip any validation stage — all 5 stages are mandatory
- MUST NOT fix violations automatically — report with specific skill references

## Output

- Weighted scoring report (120 points) with:
  - Stage-by-stage scores and check results
  - Auto-reject violations identified
  - Final verdict: APPROVED or REJECTED with reasons
  - Remediation guidance referencing specific skills

---

## SCORING FRAMEWORK

### Stage 1: File Completeness (15 points)

| # | Check | Points | Source |
|---|-------|--------|--------|
| V.1.1 | `models/<feature>.model.ts` exists with all DTOs | 2 | Exec 2.1 |
| V.1.2 | `models/<feature>-form.model.ts` exists with FormMapper | 2 | Exec 2.1 |
| V.1.3 | `services/<feature>-api.service.ts` exists | 1 | Exec 2.2 |
| V.1.4 | `facades/<feature>.facade.ts` exists | 2 | Exec 2.3 |
| V.1.5 | `helpers/<feature>-confirm-actions.ts` exists | 1 | Exec 2.4 |
| V.1.6 | `pages/<feature>-search/<feature>-grid.config.ts` exists | 1 | Exec 2.5 |
| V.1.7 | `components/<feature>-actions-cell/` exists | 1 | Exec 2.6 |
| V.1.8 | `pages/<feature>-search/<feature>-search.component.ts` exists | 2 | Exec 2.7 |
| V.1.9 | `pages/<feature>-entry/<feature>-entry.component.ts` exists | 2 | Exec 2.8 |
| V.1.10 | Routes added to domain routing module | 1 | Exec 2.10 |

### Stage 2: Model & FormMapper Quality (15 points)

| # | Check | Points | Source |
|---|-------|--------|--------|
| V.2.1 | All DTOs (Entity, Usage, Create, Update) in single model file | 2 | B.1.1 |
| V.2.2 | FormMapper is const object with all 4 methods | 2 | B.1.3 |
| V.2.3 | `createEmpty()` returns sensible defaults | 1 | B.1.3 |
| V.2.4 | `fromDomain()` correctly maps all fields | 2 | B.1.3 |
| V.2.5 | `toCreateRequest()` maps correctly | 1 | B.1.3 |
| V.2.6 | `toUpdateRequest()` OMITS immutable fields | 2 | B.1.4, C.3.3 |
| V.2.7 | Numeric fields use `??` not `\|\|` | 2 | B.4.16 |
| V.2.8 | Frontend DTOs match backend Response DTO fields | 2 | Blueprint §1.4 |
| V.2.9 | PagedResponse is generic (not feature-specific) | 1 | B.1.5 |

### Stage 3: API Service & Facade Quality (30 points)

| # | Check | Points | Source |
|---|-------|--------|--------|
| V.3.1 | API service extends BaseApiService | 2 | B.2.1 |
| V.3.2 | Uses doGet/doPost/doPut/doDelete (no manual unwrap) | 2 | B.2.2 |
| V.3.3 | API service NOT providedIn root | 2 | B.2.3 |
| V.3.4 | Environment base URL | 1 | B.2.4 |
| V.3.5 | Toggle active sends `{ active }` body | 1 | B.2.5 |
| V.3.6 | No shareReplay in CRUD API service | 2 | D.5.2 |
| V.3.7 | Facade uses signal() for all state | 3 | B.3.1 |
| V.3.8 | All signals private, all public computed | 2 | B.3.1 |
| V.3.9 | Facade NOT providedIn root | 2 | B.3.2 |
| V.3.10 | Loading pattern: set → call → tap → catchError → finalize | 2 | B.3.3 |
| V.3.11 | onSuccess callbacks on write methods | 1 | B.3.4 |
| V.3.12 | Child mutations use local signal updates | 2 | B.3.5 |
| V.3.13 | refreshUsageInfo after child create/delete | 1 | B.3.6 |
| V.3.14 | Error handling via extractBackendErrorCode + mapper | 1 | B.3.7 |
| V.3.15 | clearCurrentEntity exists and resets signals | 1 | B.3.8 |
| V.3.16 | Pagination in lastSearchRequestSignal (consolidated) | 2 | B.3.10 |
| V.3.17 | currentPage/pageSize are computed (derived) | 2 | B.3.10 |

### Stage 4: Component Quality (30 points)

| # | Check | Points | Source |
|---|-------|--------|--------|
| V.4.1 | All components standalone: true | 2 | B.4.1 |
| V.4.2 | All components OnPush | 2 | B.4.2 |
| V.4.3 | Facade + ApiService in component providers | 2 | B.4.3 |
| V.4.4 | Search extends ErpListComponent | 2 | B.4.4 |
| V.4.5 | Grid config in separate file | 1 | B.4.5 |
| V.4.6 | Grid rebuilt on language change | 1 | B.4.6 |
| V.4.7 | Actions cell is ICellRendererAngularComp | 1 | B.4.7 |
| V.4.8 | Entry uses signal<FormModel>() | 1 | B.4.8 |
| V.4.9 | Entry uses Reactive Forms (FormGroup) | 2 | B.4.8 |
| V.4.10 | Create→Edit via Location.replaceState() | 3 | B.4.9 |
| V.4.11 | Immutable fields disabled in edit mode | 2 | B.4.10 |
| V.4.12 | ngOnDestroy → facade.clearCurrentEntity() | 2 | B.4.11 |
| V.4.13 | Error effect displays via notification service | 1 | B.4.12 |
| V.4.14 | Permission check before loading in ngOnInit | 2 | B.4.13 |
| V.4.15 | Presentational components: @Input/@Output only | 2 | B.4.14 |
| V.4.16 | Modal manages own FormGroup + NgbModal | 1 | B.4.15 |
| V.4.17 | Grid column defs accept TranslateService | 1 | B.4.6 |
| V.4.18 | Filter options from grid config function | 1 | B.4.5 |

### Stage 5: Routing, Permissions & i18n (30 points)

| # | Check | Points | Source |
|---|-------|--------|--------|
| V.5.1 | Three routes: '', 'create', 'edit/:id' | 2 | B.5.1 |
| V.5.2 | loadComponent lazy loading on all routes | 2 | B.5.2 |
| V.5.3 | authGuard + permissionGuard on all routes | 3 | B.5.3 |
| V.5.4 | Permission data matches SecurityPermissions | 2 | B.5.4 |
| V.5.5 | Routes wrapped in AdminLayout | 1 | B.5.5 |
| V.5.6 | Confirm actions extracted to helpers file | 1 | B.6.1 |
| V.5.7 | Confirm actions receive ConfirmActionDeps | 1 | B.6.2 |
| V.5.8 | Confirm actions check permission FIRST | 3 | B.6.3 |
| V.5.9 | Delete checks canDelete BEFORE dialog | 2 | B.6.4 |
| V.5.10 | Dialog types: warning for toggle, danger for delete | 1 | B.6.5 |
| V.5.11 | Dialog uses translation keys with params | 1 | B.6.6 |
| V.5.12 | All user-facing strings use translation keys | 3 | C.1.1, C.1.4 |
| V.5.13 | Translation keys in both en.json and ar.json | 2 | C.1.1 |
| V.5.14 | Error codes mapped in ErpErrorMapperService | 2 | Exec 2.11 |
| V.5.15 | Triple permission enforcement per action | 2 | C.2.3 |
| V.5.16 | Four permissions per entity (VIEW/CREATE/UPDATE/DELETE) | 2 | C.2.4 |

---

## SCORING & VERDICT

| Total Score | Verdict |
|-------------|---------|
| **110–120** | **EXEMPLARY** — Fully compliant, production-ready |
| **95–109** | **APPROVED** — Minor deviations acceptable |
| **80–94** | **APPROVED WITH WARNINGS** — Fix flagged items before merge |
| **60–79** | **NEEDS REVISION** — Significant gaps, rework required |
| **Below 60** | **REJECTED** — Major architectural violations |

---

## AUTOMATIC REJECTION OVERRIDES

Regardless of score, any of these triggers **immediate rejection**:

| # | Trigger | Points Lost | Rules |
|---|---------|------------|-------|
| 1 | `providedIn: 'root'` on Facade or API Service | -10 | B.2.3, B.3.2 |
| 2 | `BehaviorSubject` for state management | -15 | B.3.1 |
| 3 | Missing `standalone: true` on any component | -5 | B.4.1 |
| 4 | Missing `ChangeDetectionStrategy.OnPush` | -5 | B.4.2 |
| 5 | Direct `HttpClient` instead of BaseApiService | -10 | B.2.1 |
| 6 | `router.navigate` for create→edit switch | -5 | B.4.9 |
| 7 | Template-driven forms (`[(ngModel)]`) | -10 | B.4.8 |
| 8 | Missing route guards on any route | -10 | B.5.3 |
| 9 | Separate writable page/size/sort signals | -5 | B.3.10 |
| 10 | `\|\|` for numeric form→DTO mapping | -3 | B.4.16 |
| 11 | Missing `facade.clearCurrentEntity()` in ngOnDestroy | -3 | B.4.11 |
| 12 | Confirm action shows dialog before permission check | -5 | B.6.3 |
| 13 | Delete without canDelete usage check | -3 | B.6.4, C.5.4 |
| 14 | `shareReplay` in CRUD API service | -5 | D.5.2 |
| 15 | Missing `takeUntilDestroyed(this.destroyRef)` before `.subscribe()` in facade | -5 | B.3.12 |
| 16 | Plain property `isEditMode`, `entityId` instead of `signal()` | -3 | S.1.9 |

**Any trigger that brings score below 60 = REJECTED**

---

## CROSS-CUTTING CHECKS

These checks span multiple files and validate end-to-end contract compliance:

| # | Check | Files Involved | What to Verify |
|---|-------|---------------|---------------|
| X.1 | DTO ↔ Backend alignment | model.ts ↔ backend Response DTO | Field names, types, optionality match |
| X.2 | FormMapper ↔ DTO alignment | form.model.ts ↔ model.ts | All fields mapped, none missing |
| X.3 | API ↔ Backend URL alignment | api.service.ts ↔ backend Controller | URLs match `@RequestMapping` patterns |
| X.4 | Permission ↔ Backend alignment | routing ↔ SecurityPermissions.java | Strings match exactly |
| X.5 | Error code ↔ Backend alignment | ErrorMapper ↔ ErrorCodes.java | All backend codes have frontend mappings |
| X.6 | i18n ↔ Template alignment | en.json/ar.json ↔ component templates | All translation keys exist in both languages |
| X.7 | Immutability ↔ Backend alignment | UpdateRequest ↔ backend UpdateRequest | Same fields excluded from both |
| X.8 | Shared code consumption | Feature files ↔ `shared/` barrel exports | All shared utilities, components, base classes are imported — no duplicated logic |

---

## HOW TO RUN THIS SKILL

### Input
Complete feature directory.

### Process
1. **Stage 1:** Verify all required files exist (15 points)
2. **Stage 2:** Validate model and FormMapper quality (15 points)
3. **Stage 3:** Audit API service + facade patterns (30 points)
4. **Stage 4:** Check component compliance (30 points)
5. **Stage 5:** Verify routing, permissions, and i18n (30 points)
6. **Cross-cutting:** Run 7 alignment checks
7. **Score:** Sum all points, apply rejection overrides
8. **Verdict:** Determine pass/fail/warning

### Output Format
```
FRONTEND FEATURE VALIDATION REPORT
=====================================
Feature: <feature-name>
Date: <date>
Validator: validate-frontend-feature

STAGE 1: FILE COMPLETENESS           [XX/15]
STAGE 2: MODEL & FORMMAPPER QUALITY   [XX/15]
STAGE 3: API SERVICE & FACADE         [XX/30]
STAGE 4: COMPONENT QUALITY            [XX/30]
STAGE 5: ROUTING, PERMISSIONS & I18N  [XX/30]

SUBTOTAL:                             [XX/120]

AUTOMATIC REJECTION TRIGGERS:
- [ ] providedIn: 'root' on Facade/ApiService
- [ ] BehaviorSubject for state
- [ ] Missing standalone: true
- [ ] Missing OnPush
- [ ] Direct HttpClient
- [ ] router.navigate for create→edit
- [ ] Template-driven forms
- [ ] Missing route guards
- [ ] Separate page/size/sort signals
- [ ] || for numeric mapping
- [ ] Missing ngOnDestroy cleanup
- [ ] Dialog before permission check
- [ ] Delete without canDelete check
- [ ] shareReplay in CRUD service

PENALTY DEDUCTIONS:                   [-XX]
FINAL SCORE:                          [XX/120]

CROSS-CUTTING ALIGNMENT:
- DTO ↔ Backend:        [PASS/FAIL]
- FormMapper ↔ DTO:     [PASS/FAIL]
- API ↔ Backend URLs:   [PASS/FAIL]
- Permission ↔ Backend: [PASS/FAIL]
- Error codes mapped:   [PASS/FAIL]
- i18n complete:        [PASS/FAIL]
- Immutability aligned: [PASS/FAIL]
- Shared code reuse:   [PASS/FAIL]

VERDICT: EXEMPLARY / APPROVED / APPROVED WITH WARNINGS / NEEDS REVISION / REJECTED
RECOMMENDATIONS: [specific items to fix]
```

---

## RELATED SKILLS

This master validation skill cross-references all enforcement skills. Run these individually for deeper analysis:

| Skill | Purpose |
|-------|---------|
| `enforce-reusability` | Deep dive into shared code consumption — detects duplicated logic across features |
| `enforce-frontend-architecture` | Detailed 63-check architectural validation |
| `enforce-permissions` | 35-check triple-enforcement permission validation |
| `enforce-state-management` | 43-check Signal-based state management validation |
| `enforce-ui-ux` | UI/UX display, readability, and i18n validation |
| `enforce-design-system` | CSS token consumption and design system compliance |


---

## ANGULAR/SKILLS COMPATIBILITY

> This section documents how this skill relates to the official `angular/skills` guidance.
> **ERP contracts always take precedence.** See `erp-priority-override` for the full precedence rule.

### What angular/skills adds that is SAFE to use alongside this skill
- No change to scoring framework
- New skills (angular-signals, angular-aria, angular-animations) are additive checks

### What angular/skills suggests that this skill OVERRIDES

| angular/skills Suggestion | This Skill Requires | Impact on Score |
|--------------------------|--------------------|----|
| Signal Forms usage | Reactive Forms  V.2.2, V.4.9 | -10 points + rejection trigger #7 |
| `providedIn: 'root'` | Component-scoped  V.3.3, V.3.9 | -10 points + rejection trigger #1 |
| BehaviorSubject state | Signals only  V.3.7 | -15 points + rejection trigger #2 |

### Future scoring additions (optional)
When `angular-aria` skill is active, the following can be added to Stage 4 scoring:
- V.4.19: Key interactive elements have ARIA labels (+1 point)
- V.4.20: Loading states have aria-busy (+1 point)
These are optional until formally added to the ERP contract.

### Conflict resolution trigger

If any angular/skills guidance contradicts a rule in this skill:
1. Apply the ERP rule
2. Log: ` CONFLICT RESOLVED: angular/skills suggests [X], ERP rule [RULE_ID] requires [Y]. Applied [Y].`
3. Do NOT ask the user  apply ERP rule silently
