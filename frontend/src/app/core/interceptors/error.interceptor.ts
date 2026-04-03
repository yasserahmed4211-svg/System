import { Injectable, Injector, inject } from '@angular/core';
import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AuthenticationService } from '../services/authentication.service';
import { ErrorLoggerService } from '../services/error-logger.service';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  private injector = inject(Injector);
  private errorLogger = inject(ErrorLoggerService);

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(request).pipe(
      catchError((err: HttpErrorResponse) => {
        // Centralised error logging for every HTTP failure
        this.errorLogger.logHttpError(err, request.method);

        // Skip redirect for auth endpoints to avoid infinite loop
        const isAuthEndpoint = request.url.includes('/api/auth/');

        if (!isAuthEndpoint) {
          if (err.status === 401) {
            // Unauthorised — session expired / invalid token → clear & redirect to login
            const authService = this.injector.get(AuthenticationService);
            authService.clearSessionAndRedirect();
          } else if (err.status === 403) {
            // Forbidden — user authenticated but lacks permission → show access-denied
            const router = this.injector.get(Router);
            router.navigate(['/access-denied']);
          }
        }

        // Pass through the full HttpErrorResponse to preserve error details
        // Components can access err.error for the full response body
        return throwError(() => err);
      })
    );
  }
}
