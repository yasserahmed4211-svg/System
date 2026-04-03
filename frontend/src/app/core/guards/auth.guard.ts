import { inject } from '@angular/core';
import { Router, CanActivateFn, CanActivateChildFn } from '@angular/router';

import { AuthenticationService } from '../services/authentication.service';

/**
 * AuthGuard - Functional guard for checking if user is authenticated
 * Use this for canActivate on protected routes
 */
export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const authService = inject(AuthenticationService);

  // Check if user is logged in (has valid, non-expired token AND user data)
  if (authService.isLoggedIn() && authService.currentUserValue && !authService.isTokenExpired()) {
    return true;
  }

  // Not logged in or token expired - redirect to login
  router.navigate(['/security/login'], { queryParams: { returnUrl: state.url } });
  return false;
};

/**
 * AuthGuard for child routes - Functional guard
 * Use this for canActivateChild on layout routes
 */
export const authGuardChild: CanActivateChildFn = (route, state) => {
  return authGuard(route, state);
};
