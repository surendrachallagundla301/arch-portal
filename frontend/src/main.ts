import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { buildAppConfig } from './app/app.config';
import { loadRuntimeConfig } from './app/core/runtime-config';

loadRuntimeConfig()
  .then((config) => bootstrapApplication(App, buildAppConfig(config)))
  .catch((err) => {
    console.error(err);
    document.body.textContent = 'The portal could not start. Check /config.json and that Keycloak is reachable.';
  });
