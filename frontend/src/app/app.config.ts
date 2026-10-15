import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { AuthService } from './core/auth.service';
import { authInterceptor, errorInterceptor } from './core/http-interceptors';
import { RUNTIME_CONFIG, RuntimeConfig } from './core/runtime-config';

export function buildAppConfig(runtimeConfig: RuntimeConfig): ApplicationConfig {
  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      provideZoneChangeDetection({ eventCoalescing: true }),
      { provide: RUNTIME_CONFIG, useValue: runtimeConfig },
      provideRouter(routes, withComponentInputBinding()),
      provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
      provideAppInitializer(() => inject(AuthService).init()),
    ],
  };
}
