import { InjectionToken } from '@angular/core';

/**
 * Settings that differ per environment. Loaded at startup from /config.json so the
 * same container image can run anywhere; in Kubernetes the file is mounted from a ConfigMap.
 */
export interface RuntimeConfig {
  apiUrl: string;
  keycloak: {
    url: string;
    realm: string;
    clientId: string;
  };
}

export const RUNTIME_CONFIG = new InjectionToken<RuntimeConfig>('RUNTIME_CONFIG');

export async function loadRuntimeConfig(path = '/config.json'): Promise<RuntimeConfig> {
  const response = await fetch(path, { cache: 'no-store' });
  if (!response.ok) {
    throw new Error(`Could not load ${path} (HTTP ${response.status})`);
  }
  return (await response.json()) as RuntimeConfig;
}
