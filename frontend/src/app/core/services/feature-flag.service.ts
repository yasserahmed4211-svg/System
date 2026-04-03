import { Injectable, signal, computed } from '@angular/core';

import { FeatureConfig, DEFAULT_FEATURE_CONFIG } from 'src/app/shared/models/feature-config.model';

/**
 * FeatureFlagService — runtime feature-flag store.
 *
 * Initialised with `DEFAULT_FEATURE_CONFIG` (mirrors current hard-coded UI).
 * Supports partial patches so production environments can override only
 * the flags they need (e.g. hide demo creds, disable empty modules).
 *
 * Usage in components:
 * ```ts
 * readonly flags = inject(FeatureFlagService);
 * showDemo = this.flags.isEnabled('showDemoCredentials');
 * ```
 *
 * Usage in templates via signal:
 * ```html
 * @if (flags.config().showDemoCredentials) { ... }
 * ```
 */
@Injectable({ providedIn: 'root' })
export class FeatureFlagService {
  private readonly _config = signal<FeatureConfig>({ ...DEFAULT_FEATURE_CONFIG });

  /** Reactive config snapshot — usable in templates. */
  readonly config = this._config.asReadonly();

  // ── query helpers ────────────────────────────────────────────────

  /**
   * Returns `true` if the given flag is enabled.
   */
  isEnabled(flag: keyof FeatureConfig): boolean {
    return this._config()[flag];
  }

  /**
   * Returns a computed signal for a specific flag — ideal for template binding.
   */
  flag(key: keyof FeatureConfig) {
    return computed(() => this._config()[key]);
  }

  // ── mutation ─────────────────────────────────────────────────────

  /**
   * Merge a partial config into the store.
   * Only overrides the supplied keys; others remain unchanged.
   */
  patch(partial: Partial<FeatureConfig>): void {
    this._config.update(current => ({ ...current, ...partial }));
  }

  /**
   * Replace the entire config (e.g. loaded from backend at startup).
   */
  loadConfig(config: FeatureConfig): void {
    this._config.set({ ...config });
  }

  /** Reset to defaults. */
  reset(): void {
    this._config.set({ ...DEFAULT_FEATURE_CONFIG });
  }
}
