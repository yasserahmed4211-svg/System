# Bugs Fixed Log

## Bug #1: API Response Not Unwrapped (Pages Registry)

**Date:** 2026-01-25

**Symptom:**  
Pages Registry grid shows empty - no data displayed even though Network tab shows successful API response (200 OK).

**Root Cause:**  
Backend returns data wrapped in `ApiResponse` structure:
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "content": [...],
    "totalElements": 5
  }
}
```

But frontend expected raw Spring Data Page:
```json
{
  "content": [...],
  "totalElements": 5
}
```

**Affected File:**  
`src/app/modules/security/pages-registry/services/pages-api.service.ts`

**Fix:**  
Add `map(response => response.data)` to unwrap `ApiResponse` wrapper in all API methods:

```typescript
// Before (WRONG)
return this.http.get<PagedResponse<PageDto>>(url);

// After (CORRECT)
return this.http.get<ApiResponse<PagedResponse<PageDto>>>(url).pipe(
  map(response => response.data)
);
```

**Quick Check:**  
If API returns `{ success, message, data }` wrapper but grid is empty:
1. Check if service uses `map(response => response.data)` to unwrap
2. Import `ApiResponse` from models and `map` from rxjs

---

## Bug #2: Filters Not Working (Pages Registry)

**Date:** 2026-01-25

**Symptom:**  
- "Contains" filter works correctly
- "Equals" filter doesn't work - grid shows no results or all results
- Only module, active, and search filters work

**Root Cause:**  
Backend API only supports 3 query parameters:
- `module` (exact match)
- `active` (exact match)
- `search` (text search in name/code)

When users applied "Equals" filter on fields like `pageCode`, `nameEn`, `nameAr`, or `route`, the facade only extracted LIKE filters for the `search` param. EQ filters on text fields were ignored.

**Affected Files:**  
- `src/app/modules/security/pages-registry/facades/pages.facade.ts`
- `src/app/modules/security/pages-registry/components/pages-registry.component.ts`

**Fix:**  
1. Map both EQ and LIKE filters on text fields to the `search` parameter
2. Simplify available operators to only `eq` and `like`

```typescript
// Before (WRONG - only LIKE filters)
const likeFilters = filters.filter((f) => f.op === 'LIKE');
search = likeFilters[0]?.value;

// After (CORRECT - both EQ and LIKE filters)
const searchableFields = ['pageCode', 'nameEn', 'nameAr', 'route'];
const textFilters = filters.filter((f) => 
  searchableFields.includes(f.field) && (f.op === 'LIKE' || f.op === 'EQ')
);
search = textFilters[0]?.value;
```

**Available Operators Now:**
```typescript
get availableOperators(): SpecOperatorOption[] {
  return [
    { value: 'eq', label: 'Equals' },
    { value: 'like', label: 'Contains' }
  ];
}
```

**Quick Check:**  
If text filters don't work:
1. Check console logs: `[PagesFacade] loadPages called with filters:`
2. Verify filters are mapped to `search` param
3. Ensure both EQ and LIKE operators map to search parameter

---

## Bug #4: Infinite Change Detection When Opening Filters

**Date:** 2026-01-28

**Symptom:**
When opening/using the filter UI (AG Grid filter panel / Specification Filter), the app crashes with:
`RuntimeError: NG0103: Infinite change detection while refreshing application views`

**Root Cause:**
The filter UI triggers frequent view refreshes. Some screens/components were using Angular **Signals** `effect()` to read a signal and then **update component state** (or call change detection) in a way that could be re-tracked and re-triggered repeatedly.

Two main contributors were found:
1. **Duplicate RTL sync**: `LanguageService.setLanguage()` was manually calling `themeService.isRTLMode.set(...)` and `applyDirectionToDOM()`, while the `LanguageService` constructor already had an `effect()` that syncs RTL + DOM based on the language signal.
2. **Tracked side-effects inside `effect()`**: Multiple components were doing updates inside `effect()` that were not isolated (ex: updating `theme` / calling `loadUserMenu()` / calling `markForCheck()`), which can create a feedback loop when a UI interaction (like opening filters) causes repeated change detection.

**Affected Files (examples):**
- `src/app/core/services/language.service.ts`
- `src/app/theme/layout/admin-layout/navigation/nav-content/nav-content.component.ts`
- `src/app/theme/layout/admin-layout/nav-bar/nav-right/nav-right.component.ts`
- `src/app/theme/layout/admin-layout/navigation/navigation.component.ts`
- `src/app/theme/shared/components/scrollbar/scrollbar.component.ts`
- `src/app/modules/security/pages-registry/components/pages-registry.component.ts`
- `src/app/modules/security/user-management/components/user-list.component.ts`

**Fix:**
1. **Single source of truth for RTL + DOM updates**
  - Removed duplicated RTL sync / DOM update from `LanguageService.setLanguage()`.
  - Kept RTL syncing + DOM direction changes inside the `LanguageService` constructor `effect()`.

2. **Isolate side-effects inside signals `effect()` using `untracked()`**
  - Wrapped state updates inside `untracked(() => { ... })` to prevent tracking accidental dependencies and avoid feedback loops.
  - Applied to layout components (navigation/nav-right/scrollbar) and list screens (pages registry + user list) where `effect()` updates UI state.

**Quick Check / Repro:**
1. Open Pages Registry or User Management list.
2. Open filters (Specification Filter or AG Grid column filter).
3. Before fix: crash with NG0103.
4. After fix: no crash; filtering works normally.

---

## Bug #3: Active Status Not Reflecting & Delete Button Hidden

**Date:** 2026-01-25

**Symptom:**  
After adding/editing a page:
- Active status toggle in edit modal doesn't reflect in the grid
- Delete button (deactivate) is hidden for all rows
- Grid doesn't update after create/update operations
- Delete button doesn't actually activate/deactivate pages

**Root Cause:**  
1. **Delete button hidden**: Button had `*ngIf="p.active"` condition, so it only showed for active pages
2. **Grid not refreshing**: After create/update/deactivate, the grid wasn't being refreshed properly
3. **Missing data reload**: `loadModules()` wasn't called after changes
4. **Missing reactivate functionality**: No API call to reactivate inactive pages

**Affected Files:**  
- `src/app/modules/security/pages-registry/components/page-actions-cell/page-actions-cell.component.ts`
- `src/app/modules/security/pages-registry/facades/pages.facade.ts`
- `src/app/modules/security/pages-registry/services/pages-api.service.ts`
- `src/app/modules/security/pages-registry/components/pages-registry.component.ts`
- `src/app/modules/security/pages-registry/components/pages-registry.component.html`

**Fix:**  

**1. Always Show Delete Button (Keep Trash Icon):**
```typescript
// Keep trash icon always, change button color based on state
<button 
  [class]="p.active ? 'btn btn-sm btn-danger' : 'btn btn-sm btn-warning'"
  [title]="p.active ? 'Deactivate' : 'Activate'"
>
  <i class="ti ti-trash"></i>  <!-- Always trash icon -->
</button>
```

**2. Add Reactivate API Endpoint:**
```typescript
// New method in pages-api.service.ts
reactivatePage(id: number): Observable<PageDto> {
  return this.http.put<ApiResponse<PageDto>>(`${this.baseUrl}/${id}/reactivate`, {}).pipe(
    map(response => response.data)
  );
}
```

**3. Add Toggle Active Functionality:**
```typescript
// New method in pages.facade.ts
togglePageActive(page: PageDto, onSuccess?: () => void): void {
  if (page.active) {
    this.deactivatePage(page.id, onSuccess);
  } else {
    this.reactivatePage(page.id, onSuccess);
  }
}
```

**4. Dynamic Confirmation Modal:**
```html
<!-- Message and button change based on active state -->
<p>{{ pageToDeactivate?.active ? 'Confirm Delete' : 'Confirm Activate' }}</p>
<button [class]="pageToDeactivate?.active ? 'btn btn-danger' : 'btn btn-warning'">
  {{ pageToDeactivate?.active ? 'Delete' : 'Activate' }}
</button>
```

**5. Reload All Data After Operations:**
```typescript
// Added to createPage, updatePage, deactivatePage, reactivatePage
this.loadPages();       // Refresh main grid
this.loadActivePages(); // Refresh dropdown data
this.loadModules();     // Refresh module list
```

**Quick Check:**  
If grid doesn't update after changes:
1. Check console logs for success messages
2. Verify `loadPages()` is called after operation
3. Check AG Grid's rowData is updating (use Angular DevTools)
4. Ensure signals are triggering change detection

**Visual Behavior:**
- **Active pages**: 🔴 Red button with trash icon → Click to "Deactivate"
- **Inactive pages**: 🟠 Warning/Orange button with trash icon → Click to "Activate"

**Backend Endpoints Used:**
- `PUT /api/pages/{id}/deactivate` - Deactivate a page (set active = false)
- `PUT /api/pages/{id}/reactivate` - Reactivate a page (set active = true)

---

