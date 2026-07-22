# DevOps Documentation Plan

This is the master index for DevOps documentation for the Folio.io project.
Each file below covers one pillar of the delivery pipeline. They are written
for a developer or operator onboarding onto a fresh machine.

---

## What lives here

| File                 | Audience             | What it covers                                               |
| -------------------- | -------------------- | ------------------------------------------------------------ |
| `setup.md`           | New developer/oncall | Prereqs, env vars, local & Docker dev bring-up               |
| `architecture.md`    | Engineers/internals  | High-level topology, request flow, data model, diagrams      |
| `troubleshooting.md` | On-call/runbook      | Known errors, diagnostics, fixes                             |
| `deployment.md`      | Release engineer     | CI/CD pipeline, images, deploy to VPS, rollback              |
| `infrastructure.md`  | Operator/DevOps      | Terraform (OCI VCN + VM), Ansible (Docker, UFW, deploy user, vault, initial deploy) |
| `observability.md`   | On-call/SRE          | Prometheus, Loki, Grafana, alerting, dashboards              |
| `secrets.md`         | Operator/SecOps.md`  | All required secrets/env vars, how to generate, where stored |
| `database.md`        | DB / Backend         | Flyway migrations, schema overview, backup/restore           |
| `networking.md`      | Operator/SRE         | Caddy reverse proxy, TLS, routing, CORS, port map            |

> Files you mentioned, not yet started, are listed above as the planned set.
> We will fill them one by one; this index tracks what is done (`[x]`) vs
> pending (`[ ]`). Ask the agent to draft any file and it will update the box.

---

## Status

- [x] Plan created (`README.md`, this file)
- [x] Information gathered from repo (dockerfiles, compose files, CI, Caddy,
      nginx, application.properties, Flyway migrations, observability configs)
- [x] `setup.md`
- [x] `architecture.md`
- [x] `deployment.md`
- [x] `observability.md`
- [x] `troubleshooting.md`
- [x] `secrets.md`
- [x] `database.md`
- [x] `networking.md`
- [x] `infrastructure.md` (Terraform + Ansible — added post-audit)

---

## Quick map of what was discovered (source files for each doc)

This is a compact summary of the facts gathered from the repo so the next
writing pass does not need to re-discover them.

### docker-compose files

- `docker-compose.yml` — dev bring-up. Three services: `postgres:15-alpine`
  (host port 5432), backend (built from `./backend`, host port 8080),
  frontend (built from `./frontend`, host port 80). `postgres_data` volume.
  backend `depends_on` postgres `service_healthy`.
- `docker-compose.prod.yml` — prod. Same postgres and app services but: backend
  and frontend use prebuilt GHCR images
  (`ghcr.io/<repo>/folio-{backend,frontend}:<tag|latest>`), expose-only
  (no host ports), backend also exposes 8081 for actuator/metrics, added
  `APP_CORS_ALLOWED_ORIGINS` env, frontend `depends_on` backend
  `service_healthy`. Adds `caddy:2-alpine` on host 80/443 with the Caddyfile
  bind-mounted and `caddy_data`/`caddy_config` volumes.
- `docker-compose.observability.yml` — monitoring stack, stacked on top of
  prod compose. Services: `prometheus:v3.0.1` (9090), `node-exporter:v1.8.2`
  (9100, host pid/proc/sys mounts), `ghcr.io/google/cadvisor:0.54.0` (8080,
  privileged, `--docker_only=true --disable_metrics=disk --housekeeping_interval=15s`),
  `loki:3.3.0` (3100), `promtail:3.3.0` (docker socket + container logs),
  `grafana:11.4.0` (3000, admin password from env, root URL
  `https://folio-ilyas.duckdns.org/grafana/`, sub-path serve). Mem limits set
  on every service. Volumes: prometheus_data, loki_data, grafana_data.

### Caddyfile (prod edge)

- Domain: `folio-ilyas.duckdns.org`
- `/api/*` -> `backend:8080`
- `/grafana/*` -> `grafana:3000`
- everything else -> `frontend:80`
- Caddy handles automatic TLS (Lets Encrypt) for the domain.

### Dockerfiles

- `backend/Dockerfile` — two-stage. Stage 1 `maven:3.9.9-eclipse-temurin-17`
  caches `pom.xml` deps then `mvn clean package -DskipTests`. Stage 2
  `eclipse-temurin:17.0.12_7-jre-jammy`, creates non-root `appuser`, copies
  `app.jar`, `EXPOSE 8080`, healthcheck hits
  `http://localhost:8081/actuator/health`.
- `frontend/Dockerfile` — two-stage. Stage 1 `node:22-alpine` runs `npm ci`,
  takes `VITE_API_BASE_URL` build arg, `npm run build`. Stage 2
  `nginx:1.27-alpine` serves `dist/`, copies `nginx.conf`, healthcheck hits
  `http://127.0.0.1/`.

### frontend/nginx.conf

- gzip on, static assets cached 1y, `/api/` proxied to
  `portfolio-backend:8080/api/` with forwarded headers, SPA fallback
  (`try_files $uri $uri/ /index.html`).

### application.properties

- datasource overridable via `SPRING_DATASOURCE_*` env, default localhost.
- `spring.jpa.hibernate.ddl-auto=validate` (Flyway owns schema).
- Flyway enabled, `classpath:db/migration`, `baseline-on-migrate=true`.
- JWT secret + 24h expiration from env.
- `server.port=8080`, `management.server.port=8081` (actuator on separate
  port — Prometheus scrapes `backend:8081`).
- Actuator exposes health, prometheus, info, metrics.
  Metrics tagged `application=folio-backend` for Prometheus labels.

### Flyway migrations

- `V1__init_schema.sql` — creates `users`, `categories`, `transactions`,
  `budgets`, `goals`, `goal_contributions` + indexes. Amounts stored as
  BIGINT (centimes), transactions have `is_deleted` (soft delete).
- `V2__create_revoked_tokens_table.sql` — `revoked_tokens` for JWT blacklist
  with expiry index.

### CI/CD — `.github/workflows/ci-cd.yml`

- triggers: push to main, PR to main.
- job `test-backend` — ephemeral postgres:15 service container, JDK 17
  (temurin), runs `./mvnw test` with test env injected (datasource url,
  test password, ci jwt secret, cors origin).
- job `test-frontend` — Node 22, `npm ci`, `npm run lint`, `npm run test
-- --run`.
- job `build-and-push` — gated on push to main only (`github.event_name ==
'push' && github.ref == 'refs/heads/main'`). QEMU + Buildx setup, logs in
  to GHCR with `${{ secrets.GITHUB_TOKEN }}`. Lowercases repo name. Builds
  multi-platform (linux/amd64,linux/arm64) backend and frontend images,
  pushes with two tags: `<sha>` and `latest`. Uses `type=gha` build cache.
  Frontend image gets `VITE_API_BASE_URL` build arg baked in.
- job `deploy-production` — gated on `production` environment (required
  reviewer). Uses `appleboy/ssh-action` to SSH into the VPS (`secrets.VPS_HOST`,
  `VPS_USER`, `SSH_PRIVATE_KEY`) and run:
  `git pull origin main`, `docker login ghcr.io` with `GHCR_PAT`, export the
  image tags + secrets as env, `docker compose -f docker-compose.prod.yml
-f docker-compose.observability.yml pull backend frontend`, `up -d
--remove-orphans`, force-recreate `caddy`, then `ps`. `set -e` so any
  step fails the deploy (enforced by recent commit).

### Observability configs (in `observability/`)

- `prometheus.yml` — 15s scrape interval, three jobs:
  `folio-backend` @ `backend:8081` (`/actuator/prometheus`),
  `node-exporter` @ `node-exporter:9100`, `cadvisor` @ `cadvisor:8080`.
- `loki-config.yml` — auth disabled, filesystem storage, TSDB schema v13,
  14-day retention (`336h`), compactor + retention enabled.
- `promtail-config.yml` — Docker SD over the host socket, relabels
  container name -> `container` label, log stream -> `stream` label.
- `grafana/provisioning/datasources/datasources.yml` — auto-provisions
  Prometheus (default) + Loki datasources.
- `grafana/provisioning/dashboards/dashboards.yml` — points a file provider
  at `/etc/grafana/provisioning/dashboards/json` (no JSON dashboards in the
  repo yet — future work).

### Secrets referenced by CI / compose (collected for `secrets.md`)

From `.env.example` + CI `secrets.*` + compose env: `JWT_SECRET`,
`JWT_EXPIRATION_MS`, `SPRING_DATASOURCE_PASSWORD`,
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`VITE_API_BASE_URL`, `APP_CORS_ALLOWED_ORIGINS`,
`GRAFANA_ADMIN_PASSWORD`, `BACKEND_IMAGE_TAG`, `FRONTEND_IMAGE_TAG`,
`GHCR_REPO`, `VPS_HOST`, `VPS_USER`, `SSH_PRIVATE_KEY`, `GHCR_PAT`.

### Network / port map (for `networking.md`)

| Host port | Container      | Internal | Notes                                 |
| --------- | -------------- | -------- | ------------------------------------- |
| 80        | caddy          | 80       | HTTP, redirected to HTTPS             |
| 443       | caddy          | 443      | HTTPS, auto TLS                       |
| -         | frontend       | 80       | expose only behind caddy              |
| -         | backend        | 8080     | app traffic, expose only              |
| -         | backend        | 8081     | actuator / prometheus scrape          |
| -         | grafana        | 3000     | reachable via `/grafana/*`            |
| -         | prometheus     | 9090     | internal only                         |
| -         | loki           | 3100     | internal only                         |
| 5432      | postgres (dev) | 5432     | dev compose only; prod is expose-only |

### Git

- remote: `https://github.com/IlyasUyidir/portfolio` (HTTPS; VPS clones over HTTPS, not SSH)
- main branch deploys; PR branches trigger tests but no build/deploy.
- feature branches naming: `fix/block-N-...` (seen in remotes).

---

## How to use this plan

1. Pick the next unchecked file above.
2. Tell the agent: `draft docs/<file>.md` (e.g. `draft docs/setup.md`).
3. The agent writes the section using the facts above (citing file paths),
   you review, it marks the box `[x]`.
4. Repeat. Later files (troubleshooting, observability) benefit from the
   earlier ones existing — `setup.md` and `architecture.md` are the best
   starting points.
