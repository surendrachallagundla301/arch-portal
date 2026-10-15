# Architecture Review Portal

A full-stack portal for submitting architecture change requests and recording Architecture Review Board
(ARB) decisions. Users sign in with Keycloak. Reviewers see a review queue and record outcomes with notes
and action items. The API looks up application owners in a CMDB and retries when the CMDB fails
temporarily.

**Stack:** Angular 20 (standalone components, signals, RxJS) · Spring Boot 3.4 (Java 21, Spring Security,
Spring Data JPA, Flyway) · Keycloak (OAuth 2.0 / OpenID Connect, PKCE) · PostgreSQL · Docker · Kubernetes
manifests for AWS EKS · GitHub Actions

```
Angular SPA ──(Keycloak login, PKCE)──► Keycloak
     │  Bearer JWT
     ▼
Spring Boot API ──(Spring Data JPA)──► PostgreSQL
     │  RestClient + Spring Retry
     ▼
   CMDB (WireMock locally)
```

## Features

- **Keycloak SSO.** The Angular app uses the Authorization Code flow with PKCE (`keycloak-js`). The
  token is refreshed before each API call and attached by a functional `HttpInterceptor`.
- **Role-based access.** Keycloak realm roles (`realm_access.roles`) are mapped to Spring authorities.
  Only `ARB_REVIEWER` can record decisions. This is enforced at the URL rule, by `@PreAuthorize`, and in
  the UI (route guard and hidden form).
- **RxJS search.** Typing in the search box is debounced. `switchMap` cancels stale requests, and
  `combineLatest` merges the text and status filters.
- **Resilient HTTP.**
  - In the UI, GETs are retried with exponential backoff on network or 5xx gateway errors, and errors
    become readable messages.
  - In the API, the CMDB client retries timeouts and 5xx responses (Spring Retry). If the CMDB stays
    down, the request is still saved, just without an owner.
- **API errors.** Failures come back as RFC 7807 problem details: validation (400), not found (404),
  closed request (409), and optimistic-locking conflicts (409, using JPA `@Version`).
- **Database migrations.** Flyway manages the schema. Hibernate does not generate tables.
- **Deployment configuration.**
  - The frontend reads `/config.json` at startup, so the same image runs in every environment. In
    Kubernetes that file is mounted from a ConfigMap.
  - The API is configured through environment variables from a ConfigMap and a Secret.
  - Actuator liveness and readiness probes are wired into the Deployment.

## Run locally

Requires Docker.

```bash
docker compose up --build
```

| URL | What |
| --- | --- |
| http://localhost:4200 | Portal |
| http://localhost:8080 | Keycloak (admin / admin) |
| http://localhost:8081/actuator/health | API health |

Demo users:
- `alice` / `alice` is a submitter.
- `bob` / `bob` is a submitter and an ARB reviewer.

To see a CMDB owner filled in, submit a request for `billing-service` or `claims-api`.

### Developing without Docker

```bash
# API (needs PostgreSQL and Keycloak running, e.g. `docker compose up postgres keycloak cmdb`)
cd backend && mvn spring-boot:run

# UI
cd frontend && npm install && npm start      # http://localhost:4200
```

## Tests

```bash
cd backend && mvn verify                      # controller, service, repository (H2 + Flyway), CMDB retry, role mapping
cd frontend && npx ng test --watch=false      # interceptors (token, retry, 401), list debounce and filters
```

## Deploy to AWS EKS

```bash
kubectl apply -f k8s/secret.example.yaml      # or sync from AWS Secrets Manager with External Secrets
kubectl apply -k k8s
```

The `k8s/` folder contains:
- A Namespace.
- ConfigMaps: environment variables for the API, plus `config.json` for the UI, mounted with `subPath`.
- An example Secret.
- Deployments with rolling updates, resource limits, non-root security contexts, and startup,
  liveness and readiness probes.
- Services.
- An AWS Load Balancer Controller Ingress that routes `/api` to Spring Boot and everything else to
  Angular.

## Project layout

```
backend/    Spring Boot API (request/, cmdb/, security/, web/)
frontend/   Angular 20 app (core/ auth + interceptors, requests/ feature)
keycloak/   Realm export: roles, PKCE client, demo users
cmdb-mock/  WireMock stubs that stand in for the CMDB
k8s/        EKS manifests
.github/    CI: backend tests, frontend tests and build, image build, manifest validation
```
