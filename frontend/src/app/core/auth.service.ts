import { Injectable, computed, inject, signal } from '@angular/core';
import Keycloak from 'keycloak-js';
import { RUNTIME_CONFIG } from './runtime-config';

export type PortalRole = 'SUBMITTER' | 'ARB_REVIEWER';

/**
 * Thin wrapper around keycloak-js. Uses the Authorization Code flow with PKCE,
 * exposes the session as signals, and refreshes the access token before each API call.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly config = inject(RUNTIME_CONFIG);
  private keycloak?: Keycloak;

  private readonly _authenticated = signal(false);
  private readonly _username = signal<string | null>(null);
  private readonly _roles = signal<string[]>([]);

  readonly authenticated = this._authenticated.asReadonly();
  readonly username = this._username.asReadonly();
  readonly isReviewer = computed(() => this._roles().includes('ARB_REVIEWER'));

  async init(): Promise<void> {
    this.keycloak = new Keycloak({
      url: this.config.keycloak.url,
      realm: this.config.keycloak.realm,
      clientId: this.config.keycloak.clientId,
    });

    const authenticated = await this.keycloak.init({
      onLoad: 'login-required',
      pkceMethod: 'S256',
      checkLoginIframe: false,
    });

    this.keycloak.onTokenExpired = () => {
      this.keycloak?.updateToken(30).catch(() => this.login());
    };
    this.syncState(authenticated);
  }

  hasRole(role: PortalRole): boolean {
    return this._roles().includes(role);
  }

  /** Returns a valid access token, refreshing it if it expires within 30 seconds. */
  async getToken(): Promise<string | undefined> {
    if (!this.keycloak?.authenticated) {
      return undefined;
    }
    await this.keycloak.updateToken(30);
    return this.keycloak.token;
  }

  login(): Promise<void> {
    return this.keycloak?.login() ?? Promise.resolve();
  }

  logout(): Promise<void> {
    return this.keycloak?.logout({ redirectUri: window.location.origin }) ?? Promise.resolve();
  }

  private syncState(authenticated: boolean): void {
    const parsed = this.keycloak?.tokenParsed as
      | { preferred_username?: string; realm_access?: { roles?: string[] } }
      | undefined;
    this._authenticated.set(authenticated);
    this._username.set(parsed?.preferred_username ?? null);
    this._roles.set(parsed?.realm_access?.roles ?? []);
  }
}
