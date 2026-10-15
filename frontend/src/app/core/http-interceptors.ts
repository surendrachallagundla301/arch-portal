import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, retry, switchMap, throwError, timer, catchError } from 'rxjs';
import { AuthService } from './auth.service';
import { RUNTIME_CONFIG } from './runtime-config';

/** Adds the Keycloak bearer token to calls to our own API (never to third-party URLs). */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const config = inject(RUNTIME_CONFIG);
  if (!req.url.startsWith(config.apiUrl)) {
    return next(req);
  }
  const auth = inject(AuthService);
  return from(auth.getToken()).pipe(
    switchMap((token) =>
      next(token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req),
    ),
  );
};

const RETRYABLE_STATUS = new Set([0, 502, 503, 504]);

/**
 * Retries idempotent GETs on network/gateway errors with exponential backoff,
 * sends the user back to login on 401, and converts errors to readable messages.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  return next(req).pipe(
    retry({
      count: req.method === 'GET' ? 2 : 0,
      delay: (error: HttpErrorResponse, attempt) =>
        RETRYABLE_STATUS.has(error.status) ? timer(300 * 2 ** (attempt - 1)) : throwError(() => error),
    }),
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        auth.login();
      }
      return throwError(() => new Error(describeError(error)));
    }),
  );
};

export function describeError(error: HttpErrorResponse): string {
  if (error.status === 0) return 'The server could not be reached. Please try again.';
  if (error.status === 403) return 'You do not have permission to do that.';
  const detail = (error.error as { detail?: string } | null)?.detail;
  return detail ?? `Request failed (HTTP ${error.status}).`;
}
