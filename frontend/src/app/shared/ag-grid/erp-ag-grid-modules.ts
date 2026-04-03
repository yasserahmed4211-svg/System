/**
 * ERP AG Grid Module Registration
 *
 * CRITICAL: All AG Grid features MUST have their corresponding modules registered
 * before use. This file provides centralized module registration.
 *
 * Rules:
 * - DO NOT use agSetColumnFilter without SetFilterModule (Enterprise)
 * - DO NOT use agSetColumnFloatingFilter without SetFilterModule (Enterprise)
 * - Community modules are sufficient for most ERP use cases
 *
 * @see https://www.ag-grid.com/angular-data-grid/modules/
 */

import { ModuleRegistry, AllCommunityModule, ValidationModule } from 'ag-grid-community';

/**
 * List of AG Grid modules registered for this ERP application.
 * 
 * Community Modules (included in AllCommunityModule):
 * - ClientSideRowModelModule
 * - CsvExportModule
 * - TextFilterModule (for agTextColumnFilter)
 * - NumberFilterModule (for agNumberColumnFilter)
 * - DateFilterModule (for agDateColumnFilter)
 * - InfiniteRowModelModule
 * 
 * NOTE: Enterprise modules (SetFilterModule, etc.) are NOT included.
 * If you need enterprise features, add them here AND ensure license.
 */
export const ERP_AG_GRID_MODULES = [
  AllCommunityModule,
  ValidationModule
];

/**
 * Flag to track if modules have been registered.
 * Prevents duplicate registration.
 */
let modulesRegistered = false;

/**
 * Register all required AG Grid modules for the ERP application.
 * Should be called once at application startup (e.g., in main.ts or app.config.ts).
 * 
 * Safe to call multiple times - will only register once.
 */
export function registerErpAgGridModules(): void {
  if (modulesRegistered) {
    return;
  }
  
  ModuleRegistry.registerModules(ERP_AG_GRID_MODULES);
  modulesRegistered = true;
}

/**
 * Check if AG Grid modules are registered.
 * Useful for debugging.
 */
export function areAgGridModulesRegistered(): boolean {
  return modulesRegistered;
}

/**
 * IMPORTANT: Available filter types based on registered modules.
 * 
 * With AllCommunityModule (current setup):
 * ✅ 'agTextColumnFilter' - Text filtering
 * ✅ 'agNumberColumnFilter' - Number filtering  
 * ✅ 'agDateColumnFilter' - Date filtering
 * ❌ 'agSetColumnFilter' - Requires SetFilterModule (Enterprise)
 * ❌ 'agMultiColumnFilter' - Requires MultiFilterModule (Enterprise)
 * 
 * DO NOT use agSetColumnFilter unless SetFilterModule is added above!
 */
export const ERP_AVAILABLE_FILTERS = {
  TEXT: 'agTextColumnFilter',
  NUMBER: 'agNumberColumnFilter',
  DATE: 'agDateColumnFilter'
  // SET: 'agSetColumnFilter' - NOT AVAILABLE without Enterprise
} as const;

export type ErpAvailableFilter = typeof ERP_AVAILABLE_FILTERS[keyof typeof ERP_AVAILABLE_FILTERS];
