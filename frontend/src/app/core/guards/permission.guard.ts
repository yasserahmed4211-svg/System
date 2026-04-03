import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';

import { PermissionService } from '../services/permission.service';

/**
 * PermissionGuard - Functional guard for checking route permissions.
 *
 * Reads from `route.data`:
 *   • `permission`  — single string  (backward-compatible)
 *   • `permissions`  — string[]       (any-match / OR logic)
 *   • `allPermissions` — string[]     (all-match / AND logic)
 *
 * If none are defined, the route is allowed.
 */
export const permissionGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const permService = inject(PermissionService);

  // Single permission (existing convention)
  const single: string | undefined = route.data['permission'];
  // Multiple — any match
  const anyPerms: string[] | undefined = route.data['permissions'];
  // Multiple — all match
  const allPerms: string[] | undefined = route.data['allPermissions'];

  // If nothing is defined, allow navigation
  if (!single && !anyPerms?.length && !allPerms?.length) {
    return true;
  }

  // Check single permission (backward compatible)
  if (single && !permService.hasPermission(single)) {
    router.navigate(['/access-denied'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  // Check any-match permissions
  if (anyPerms?.length && !permService.hasAnyPermission(anyPerms)) {
    router.navigate(['/access-denied'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  // Check all-match permissions
  if (allPerms?.length && !permService.hasAllPermissions(allPerms)) {
    router.navigate(['/access-denied'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  return true;
};
