# Master Lookups — File Tuning Document

## Blueprint Level
**Level 2** — Page-Based CRUD (Page A: Search/List + Page B: Create/Edit)

## Routes

| Route | Component | Permission | Description |
|-------|-----------|------------|-------------|
| `/master-data/master-lookups` | `MasterLookupSearchComponent` | `PERM_MASTER_LOOKUP_VIEW` | Page A — Search/List |
| `/master-data/master-lookups/create` | `MasterLookupEntryComponent` | `PERM_MASTER_LOOKUP_CREATE` | Page B — Create |
| `/master-data/master-lookups/edit/:id` | `MasterLookupEntryComponent` | `PERM_MASTER_LOOKUP_UPDATE` | Page B — Edit |

## Shared Components Consumed

| Component / Service | Usage |
|---------------------|-------|
| `ErpListComponent` | Base class for search page (pagination, sort, reload) |
| `SpecificationFilterComponent` | Advanced filter panel in search page |
| `ErpEmptyStateComponent` | No-data / error states in both pages |
| `ErpFormFieldComponent` | All form fields in entry page (auto error resolution) |
| `ErpSectionComponent` | Section grouping in entry form |
| `ErpBackButtonComponent` | Back navigation in entry page |
| `ErpPermissionDirective` | Permission gating on buttons/sections |
| `ErpDialogService` | Confirm dialogs (delete, toggle active) |
| `ErpNotificationService` | Success/error/warning notifications |
| `createActiveColumnDef` | Active status column in AG Grid |
| `createErpGridOptions` | Grid options (pagination, RTL) |
| `registerErpAgGridModules` | Centralized AG Grid module registration |
| `ERP_DEFAULT_COL_DEF` | Default column definition |
| `createAgGridTheme` | Dark/light AG Grid theme |
| `ErpErrorMapperService` | Backend error code mapping in facade |
| `extractBackendErrorCode/Message` | Error extraction utilities |

## Facade Boundary

- **Single facade**: `MasterLookupFacade`
- Manages state for: Master Lookups list, current lookup, lookup details, loading/saving/error signals
- All CRUD operations flow through facade
- Components never call API service directly

## Localization

- i18n prefix: `MASTER_LOOKUPS.*`
- Common keys: `COMMON.*`, `VALIDATION.*`, `MESSAGES.*`, `ERRORS.*`
- No hardcoded text in templates or TypeScript
- RTL supported via `createErpGridOptions({ enableRtl })` and global styles

## Feedback & Confirmations

| Action | Feedback |
|--------|----------|
| Create success | `MESSAGES.CREATE_SUCCESS` notification |
| Update success | `MESSAGES.UPDATE_SUCCESS` notification |
| Delete success | `MESSAGES.DELETE_SUCCESS` notification |
| Toggle active success | `MESSAGES.ACTIVATE_SUCCESS` / `MESSAGES.DEACTIVATE_SUCCESS` |
| Form invalid | `MESSAGES.FORM_INVALID` warning |
| No permission | `MESSAGES.NO_PERMISSION` warning |
| Delete confirmation | `ErpDialogService.confirm()` with danger type |
| Toggle active confirmation | `ErpDialogService.confirm()` with warning type |
| Cannot delete (has details) | `MASTER_LOOKUPS.CANNOT_DELETE_HAS_DETAILS` warning |
