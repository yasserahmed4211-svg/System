import { Injectable, inject, computed } from '@angular/core';

import { AuthenticationService } from './authentication.service';

/**
 * PermissionService — single source of truth for UI permission checks.
 *
 * Delegates to `AuthenticationService.currentUserValue.permissions` but
 * provides a richer API surface with normalisation, caching, and
 * convenience methods used by guards, directives, and components.
 *
 * Components should inject **this** service instead of calling
 * `AuthenticationService.hasPermission()` directly.
 */
@Injectable({ providedIn: 'root' })
export class PermissionService {
  private readonly authService = inject(AuthenticationService);

  /** Reactive set of current permissions (updates when user signal changes). */
  readonly permissions = computed<ReadonlySet<string>>(() => {
    const user = this.authService.currentUserValue;
    return new Set(user?.permissions ?? []);
  });

  // ── single permission ────────────────────────────────────────────

  /**
   * Returns `true` if the current user holds `permission`.
   * Accepts both `PERM_USER_VIEW` and `USER.VIEW` formats.
   */
  hasPermission(permission: string): boolean {
    const normalised = this.normalize(permission);
    return this.permissions().has(normalised);
  }

  // ── multiple permissions ─────────────────────────────────────────

  /**
   * Returns `true` if the user holds **any** of the given permissions (OR).
   */
  hasAnyPermission(permissions: string[]): boolean {
    return permissions.some(p => this.hasPermission(p));
  }

  /**
   * Returns `true` if the user holds **all** of the given permissions (AND).
   */
  hasAllPermissions(permissions: string[]): boolean {
    return permissions.every(p => this.hasPermission(p));
  }

  // ── role helpers (delegate to auth) ──────────────────────────────

  hasRole(role: string): boolean {
    const user = this.authService.currentUserValue;
    return user?.roles?.includes(role) ?? false;
  }

  hasAnyRole(roles: string[]): boolean {
    return roles.some(r => this.hasRole(r));
  }

  // ── normalisation ────────────────────────────────────────────────

  /**
   * Normalise a permission key to the canonical `PERM_*` format.
   *  - `PERM_USER_CREATE` → as-is
   *  - `USER.CREATE`      → `PERM_USER_CREATE`
   *  - `user-create`      → `PERM_USER_CREATE`
   */
  private normalize(permission: string): string {
    if (!permission) return '';
    const trimmed = permission.trim();
    if (trimmed.startsWith('PERM_')) return trimmed;
    return `PERM_${trimmed.replace(/\s+/g, '').replace(/[.\-:]/g, '_').toUpperCase()}`;
  }
}
