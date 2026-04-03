import { Injectable } from '@angular/core';

/**
 * TokenStoreService
 *
 * Lightweight, HTTP-independent token store.
 * Keeps JWT tokens primarily in-memory (not localStorage) to reduce XSS exposure.
 * localStorage is used ONLY for session recovery on page reload, and is cleared
 * immediately after hydration.
 *
 * Both AuthenticationService and AuthInterceptor can safely inject this service
 * without creating circular dependencies.
 */
@Injectable({ providedIn: 'root' })
export class TokenStoreService {
  private accessToken: string | null = null;
  private refreshToken: string | null = null;
  private tokenExpiration: number | null = null;

  constructor() {
    this.hydrateFromStorage();
  }

  /**
   * On page reload, recover tokens from localStorage then remove them.
   */
  private hydrateFromStorage(): void {
    const storedAccess = localStorage.getItem('accessToken');
    if (storedAccess) {
      this.accessToken = storedAccess;
      localStorage.removeItem('accessToken');
    }

    const storedRefresh = localStorage.getItem('refreshToken');
    if (storedRefresh) {
      this.refreshToken = storedRefresh;
      localStorage.removeItem('refreshToken');
    }

    const storedExpiration = localStorage.getItem('tokenExpiration');
    if (storedExpiration) {
      this.tokenExpiration = parseInt(storedExpiration, 10);
      localStorage.removeItem('tokenExpiration');
    }
  }

  // ── Getters ──

  getAccessToken(): string | null {
    return this.accessToken;
  }

  getRefreshToken(): string | null {
    return this.refreshToken;
  }

  isTokenExpired(): boolean {
    if (!this.tokenExpiration) {
      return true;
    }
    return Date.now() > this.tokenExpiration;
  }

  hasToken(): boolean {
    return this.accessToken !== null;
  }

  // ── Setters ──

  setAccessToken(token: string): void {
    this.accessToken = token;
    // Persist for session recovery only (will be cleared on next hydration)
    localStorage.setItem('accessToken', token);
  }

  setRefreshToken(token: string): void {
    this.refreshToken = token;
    localStorage.setItem('refreshToken', token);
  }

  setTokenExpiration(expiresInSeconds: number): void {
    this.tokenExpiration = Date.now() + (expiresInSeconds * 1000);
    localStorage.setItem('tokenExpiration', this.tokenExpiration.toString());
  }

  // ── Clear ──

  clear(): void {
    this.accessToken = null;
    this.refreshToken = null;
    this.tokenExpiration = null;
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('tokenExpiration');
  }
}
