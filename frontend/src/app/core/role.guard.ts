import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, PortalRole } from './auth.service';

/** Blocks a route unless the signed-in user has the given realm role. */
export function roleGuard(role: PortalRole): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    return auth.hasRole(role) ? true : inject(Router).createUrlTree(['/requests']);
  };
}
