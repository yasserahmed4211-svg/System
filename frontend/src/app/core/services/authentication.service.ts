import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, tap, map } from 'rxjs/operators';

import { environment } from 'src/environments/environment';
import { UserDto, AuthRequest, AuthResponse } from '../models/auth.model';
import { TokenStoreService } from './token-store.service';

@Injectable({ providedIn: 'root' })
export class AuthenticationService {
  private router = inject(Router);
  private http = inject(HttpClient);
  private tokenStore = inject(TokenStoreService);

  private currentUserSignal = signal<UserDto | null>(null);
  private loadingSignal = signal<boolean>(false);
  isLogin: boolean = false;

  constructor() {
    // On page reload, recover session from TokenStoreService (hydrated from localStorage)
    if (this.tokenStore.hasToken()) {
      this.isLogin = true;

      const storedUser = localStorage.getItem('currentUser');
      if (storedUser) {
        try {
          const userDto = JSON.parse(storedUser) as UserDto;
          this.currentUserSignal.set(userDto);
        } catch {
          // Silent catch - invalid stored data
        }
      }
    }
  }

  public get currentUserValue(): UserDto | null {
    return this.currentUserSignal();
  }

  public get currentUserName(): string | null {
    const currentUser = this.currentUserValue;
    return currentUser ? currentUser.username : null;
  }

  /**
   * Login using the ERP Security System API
   */
  login(username: string, password: string): Observable<boolean> {
    this.loadingSignal.set(true);
    
    const authRequest: AuthRequest = { username, password };
    
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return this.http.post<any>(`${environment.authApiUrl}/api/auth/login-token`, authRequest).pipe(
      tap((response: any) => {
        // Handle both wrapped (response.data) and flat response formats
        const data = response.data || response;
        const token = data.accessToken || data.token || data.access_token;
        
        if (!token) {
          throw new Error('No token received from server');
        }
        
        // Store token in TokenStoreService (in-memory primary, localStorage for recovery)
        this.tokenStore.setAccessToken(token);
        
        const refreshToken = data.refreshToken;
        if (refreshToken) {
          this.tokenStore.setRefreshToken(refreshToken);
        }
        
        const expiresIn = data.expiresIn;
        if (expiresIn) {
          this.tokenStore.setTokenExpiration(expiresIn);
        }

        const userDto: UserDto = {
          id: data.userId || 0,
          username: data.username || username,
          enabled: data.enabled !== undefined ? data.enabled : true,
          roles: data.roles || [],
          permissions: data.permissions || []
        };
        
        // Add "Admin" role for guard compatibility if user has ROLE_ADMIN
        if (!userDto.roles.includes('Admin') && userDto.roles.some(r => r === 'ROLE_ADMIN')) {
          userDto.roles.push('Admin');
        }
        
        this.currentUserSignal.set(userDto);
        localStorage.setItem('currentUser', JSON.stringify(userDto));
        
        this.isLogin = true;
        this.loadingSignal.set(false);
      }),
      map(() => true),
      catchError(() => {
        this.loadingSignal.set(false);
        return of(false);
      })
    );
  }

  isLoggedIn(): boolean {
    return this.isLogin && this.getToken() !== null;
  }

  get isLoading(): boolean {
    return this.loadingSignal();
  }

  logout(): void {
    const accessToken = this.getToken();
    const refreshToken = this.getRefreshToken();

    // If we have no tokens, don't call the backend logout endpoint.
    // This avoids noisy logout loops and still clears the local session.
    if (!accessToken && !refreshToken) {
      this.clearSession();
      return;
    }

    const body = refreshToken ? { refreshToken } : {};

    this.http.post(`${environment.authApiUrl}/api/auth/logout`, body).subscribe({
      next: () => this.clearSession(),
      error: () => this.clearSession()
    });
  }

  /**
   * Clear session and redirect to login without calling logout API
   * Used by error interceptor to avoid infinite loop on 401/403
   */
  clearSessionAndRedirect(): void {
    this.clearSession();
  }

  private clearSession(): void {
    this.tokenStore.clear();
    localStorage.removeItem('currentUser');
    this.isLogin = false;
    this.currentUserSignal.set(null);
    this.loadingSignal.set(false);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return this.tokenStore.getAccessToken();
  }

  getRefreshToken(): string | null {
    return this.tokenStore.getRefreshToken();
  }

  isTokenExpired(): boolean {
    return this.tokenStore.isTokenExpired();
  }

  refreshToken(): Observable<boolean> {
    return this.http.post<AuthResponse>(`${environment.authApiUrl}/api/auth/refresh`, {}).pipe(
      tap((response: AuthResponse) => {
        this.tokenStore.setAccessToken(response.accessToken);
        
        if (response.expiresIn) {
          this.tokenStore.setTokenExpiration(response.expiresIn);
        }
        
        this.loadingSignal.set(false);
      }),
      map(() => true),
      catchError(() => {
        this.logout();
        return of(false);
      })
    );
  }

  register(username: string, password: string): Observable<boolean> {
    const request = { username, password };
    return this.http.post<AuthResponse>(`${environment.authApiUrl}/api/auth/register`, request).pipe(
      tap((response: AuthResponse) => {
        if (response.accessToken) {
          this.tokenStore.setAccessToken(response.accessToken);
          this.isLogin = true;
        }
      }),
      map(() => true),
      catchError(() => of(false))
    );
  }

  hasPermission(permission: string): boolean {
    const currentUser = this.currentUserValue;
    if (!currentUser || !currentUser.permissions) {
      return false;
    }
    return currentUser.permissions.includes(permission);
  }

  hasAnyPermission(permissions: string[]): boolean {
    return permissions.some(permission => this.hasPermission(permission));
  }

  hasAllPermissions(permissions: string[]): boolean {
    return permissions.every(permission => this.hasPermission(permission));
  }

  getPermissions(): string[] {
    const currentUser = this.currentUserValue;
    return currentUser?.permissions || [];
  }
}
