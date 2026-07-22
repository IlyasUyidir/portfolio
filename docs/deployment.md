# Deployment Guide

This document describes the automated CI/CD pipeline used to deliver Folio.io from GitHub to the production VPS.

## 1. Pipeline Overview

The project uses **GitHub Actions** for a fully automated "Test $\rightarrow$ Build $\rightarrow$ Deploy" workflow. The pipeline is defined in `.github/workflows/ci-cd.yml`.

### Pipeline Stages
1. **Test**: Parallel execution of backend (JUnit/Spring) and frontend (Vitest/ESLint) tests.
2. **Build & Push**: Creation of multi-platform Docker images and upload to GitHub Container Registry (GHCR).
3. **Deploy**: SSH-based update of the production VPS.

---

## 2. Detailed Stage Breakdown

### Stage 1: Testing
To ensure stability, the pipeline runs tests on every push and pull request to `main`.
- **Backend Tests**: Spawns an ephemeral `postgres:15` container to run integration tests via `./mvnw test`.
- **Frontend Tests**: Sets up Node 22, installs dependencies via `npm ci`, runs `npm run lint`, and executes the test suite via `npm run test -- --run`.

### Stage 2: Build & Push (GHCR)
Triggered only on pushes to the `main` branch.
- **Authentication**: Logs into GHCR using the **built-in `GITHUB_TOKEN`** (not `GHCR_PAT`). `GITHUB_TOKEN` is automatically granted `packages: write` by the job's `permissions` block.
- **Single-Platform (ARM64)**: Uses `docker/setup-qemu-action` and `docker/setup-buildx-action` to build images for **`linux/arm64` only**. This is intentional: the production VPS is an Oracle Cloud `VM.Standard.A1.Flex` ARM64 instance, so no `amd64` image is required. If you need to run this stack on an x86 machine you must either re-add `linux/amd64` to the `platforms:` field in each `build-and-push` step or pull using emulation.
- **Registry**: Images are pushed to **GitHub Container Registry (GHCR)**.
- **Tagging Strategy**: Every image is tagged with both the unique Git commit SHA (`${{ github.sha }}`) and the `latest` tag.
- **Caching**: Uses `type=gha` cache to speed up subsequent builds.
- **Frontend Build-Arg**: `VITE_API_BASE_URL=https://folio-ilyas.duckdns.org/api/v1` is baked into the frontend image at build time.

### Stage 3: Production Deployment
The deployment is gated by a **GitHub Environment (`production`)**, requiring a manual reviewer approval before proceeding.

**The Deployment Process:**
1. **SSH Access**: The pipeline connects to the VPS using `appleboy/ssh-action` via an SSH key.
2. **Code Update**: `cd`s into `/opt/folio` and performs a `git pull origin main` to update the compose files and Caddyfile.
3. **Auth**: Logs into GHCR using `GHCR_PAT` (a Personal Access Token with `read:packages` scope only — used to pull images, not push them).
4. **Update**:
    - Exports `GHCR_REPO`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`, `GRAFANA_ADMIN_PASSWORD`, `BACKEND_IMAGE_TAG`, and `FRONTEND_IMAGE_TAG` as shell env vars.
    - Pulls the latest images for `backend` and `frontend`.
    - Executes `docker compose -f docker-compose.prod.yml -f docker-compose.observability.yml up -d --remove-orphans`.
    - Force-recreates the `caddy` container (`--force-recreate --no-deps caddy`) to guarantee that any Caddyfile inode change is picked up (see `troubleshooting.md` → *Caddy Silently Ignoring Caddyfile Changes*).
    - Force-recreates the `grafana` container (`--force-recreate --no-deps grafana`) to ensure environment variable changes (e.g. `GF_SERVER_ROOT_URL`) always take effect. Without this, Docker would not restart Grafana if the image tag had not changed.
    - Runs `docker compose ps` to confirm all containers are healthy.
5. **Failure Gate**: The remote script runs under `set -e`; any failed command aborts the deploy and fails the GitHub Action.

---

## 3. Infrastructure Configuration

### Production Compose Files
The production environment uses a layered compose approach:
- `docker-compose.prod.yml`: Defines the core app services (Postgres, Backend, Frontend) and the Caddy proxy.
- `docker-compose.observability.yml`: Adds the monitoring stack (Prometheus, Loki, Grafana, etc.) on top of the core services.

### Environment Variables
Secrets are managed via GitHub Actions Secrets and injected into the VPS environment during deployment:
- `VPS_HOST`, `VPS_USER`, `SSH_PRIVATE_KEY` (For SSH access)
- `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET` (App secrets)
- `GRAFANA_ADMIN_PASSWORD` (Observability secret)
- `GHCR_PAT` (Registry pull access — `read:packages` only)
- `GHCR_REPO` (Computed from the repo name; passed as env var by CI)

---

## 4. Rollback Strategy

> **⚠️ UNVERIFIED — This procedure has not been tested end-to-end.** The steps below are logically correct based on how the compose files consume image tags, but a full rollback simulation has not been performed. Treat this as a draft runbook until verified.

Because images are tagged with the Git SHA, rolling back is straightforward in principle:

1. Identify the last stable Git SHA from `git log` or the GitHub Actions run history.
2. On the VPS, export the old SHA as both image tags and re-run the stack:
   ```bash
   cd /opt/folio
   export GHCR_REPO="ilyasuyidir/portfolio"
   export SPRING_DATASOURCE_PASSWORD="<from .env or GitHub Secrets>"
   export JWT_SECRET="<from .env or GitHub Secrets>"
   export GRAFANA_ADMIN_PASSWORD="<from .env or GitHub Secrets>"
   export BACKEND_IMAGE_TAG=<old-sha>
   export FRONTEND_IMAGE_TAG=<old-sha>
   docker compose -f docker-compose.prod.yml -f docker-compose.observability.yml pull backend frontend
   docker compose -f docker-compose.prod.yml -f docker-compose.observability.yml up -d --remove-orphans
   docker compose -f docker-compose.prod.yml -f docker-compose.observability.yml up -d --force-recreate --no-deps caddy
   docker compose -f docker-compose.prod.yml -f docker-compose.observability.yml up -d --force-recreate --no-deps grafana
   ```
3. **If the rollback target includes a Caddyfile change**: also check out the old Caddyfile revision on the VPS (or restore from backup), then force-recreate Caddy as shown above. A plain `caddy reload` will not reliably apply Caddyfile changes after a file replacement — see `troubleshooting.md` for the inode bind-mount issue.
