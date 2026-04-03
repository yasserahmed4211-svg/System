/**
 * FeatureConfig — runtime feature flags and UI visibility rules.
 *
 * All flags default to a safe value so the UI renders identically
 * to the current hard-coded behavior.  Future backends or admin panels
 * can populate a different config at startup.
 */
export interface FeatureConfig {
  // ── authentication ───────────────────────────────────────────────
  /** Show the "quick-fill demo credentials" role picker on the login page. */
  showDemoCredentials: boolean;

  /** Show social-login buttons (Google, Twitter, Facebook). */
  showSocialLogin: boolean;

  /** Show the "Register" link on the login page. */
  showRegistration: boolean;

  /** Show the "Forgot password" link on the login page. */
  showForgotPassword: boolean;

  // ── layout ───────────────────────────────────────────────────────
  /** Show the theme / layout configuration sidebar. */
  showThemeConfigurator: boolean;

  /** Show the app-version badge in the sidebar footer. */
  showVersionBadge: boolean;

  // ── modules ──────────────────────────────────────────────────────
  /** Enable HR module routes/navigation. */
  moduleHrEnabled: boolean;

  /** Enable Finance module routes/navigation. */
  moduleFinanceEnabled: boolean;

  /** Enable Inventory module routes/navigation. */
  moduleInventoryEnabled: boolean;

  /** Enable Maintenance module routes/navigation. */
  moduleMaintenanceEnabled: boolean;

  /** Enable Procurement module routes/navigation. */
  moduleProcurementEnabled: boolean;

  /** Enable Reports module routes/navigation. */
  moduleReportsEnabled: boolean;

  /** Enable Sales module routes/navigation. */
  moduleSalesEnabled: boolean;
}

/**
 * Default configuration — mirrors the current hard-coded behavior exactly.
 * Changing these values has no effect unless the consuming component reads them.
 */
export const DEFAULT_FEATURE_CONFIG: Readonly<FeatureConfig> = {
  showDemoCredentials: true,
  showSocialLogin: true,
  showRegistration: true,
  showForgotPassword: true,
  showThemeConfigurator: true,
  showVersionBadge: true,
  moduleHrEnabled: true,
  moduleFinanceEnabled: true,
  moduleInventoryEnabled: true,
  moduleMaintenanceEnabled: true,
  moduleProcurementEnabled: true,
  moduleReportsEnabled: true,
  moduleSalesEnabled: true,
};
