import { Injectable, inject } from '@angular/core';
import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { environment } from 'src/environments/environment';
import { TokenStoreService } from '../services/token-store.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private tokenStore = inject(TokenStoreService);

  /**
   * Intercepts HTTP requests to add JWT token in Authorization header.
   * Reads token from TokenStoreService (in-memory) to avoid circular dependency
   * and reduce localStorage exposure.
   */
  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.tokenStore.getAccessToken();
    const isApiUrl = request.url.startsWith('/api') ||
      (environment.apiUrl !== '' && request.url.startsWith(environment.apiUrl)) ||
      (environment.authApiUrl !== '' && request.url.startsWith(environment.authApiUrl));
    const isAuthEndpoint = request.url.includes('/api/auth/');

    if (isApiUrl && !isAuthEndpoint) {
      const headers: Record<string, string> = {};

      if (token && !this.tokenStore.isTokenExpired()) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      if (Object.keys(headers).length > 0) {
        request = request.clone({ setHeaders: headers });
      }
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        return throwError(() => error);
      })
    );
  }
}
