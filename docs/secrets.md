# Secrets & Environment Variables

This document provides a comprehensive map of all required secrets and environment variables for Folio.io. It defines what each variable does, where it is used, and how to generate secure values for it.

## 1. Secret Matrix

| Variable                     | Scope         | Source                        | Description                              | Sensitivity |
| ---------------------------- | ------------- | ----------------------------- | ---------------------------------------- | ----------- |
| `JWT_SECRET`                 | Backend       | `.env` / GitHub Secrets       | Key for signing HS256 JWTs               | 🔴 High     |
| `SPRING_DATASOURCE_PASSWORD` | DB / Backend  | `.env` / GitHub Secrets       | PostgreSQL user password                 | 🔴 High     |
| `VITE_API_BASE_URL`          | Frontend      | `.env` / GH Actions Build-Arg | Production API endpoint URL              | 🟡 Low      |
| `APP_CORS_ALLOWED_ORIGINS`   | Backend       | `.env` / GitHub Secrets       | Allowed origins for CORS                 | 🟡 Low      |
| `GRAFANA_ADMIN_PASSWORD`     | Observability | `.env` / GitHub Secrets       | Initial Grafana admin password           | 🔴 High     |
| `GHCR_PAT`                   | CI/CD         | GitHub Secrets                | Personal Access Token for GHCR **pull** on VPS (see note below) | 🔴 High     |
| `GHCR_REPO`                  | CI/CD         | Computed by CI / GitHub Secrets | Lowercase GHCR repository path (e.g. `ilyasuyidir/portfolio`) used to pull images | 🟡 Low      |
| `SSH_PRIVATE_KEY`            | CI/CD         | GitHub Secrets                | Private key for VPS SSH access           | 🔴 High     |
| `VPS_HOST`                   | CI/CD         | GitHub Secrets                | Production VPS IP/Hostname               | 🟡 Low      |
| `VPS_USER`                   | CI/CD         | GitHub Secrets                | SSH username on the VPS                  | 🟡 Low      |

---

## 2. Generation & Management Guide

### JWT Secret (`JWT_SECRET`)

**Requirement**: Must be at least 32 characters long to satisfy HS256 security requirements.
**Generation**: Use a cryptographically secure random string.

- **Command**: `openssl rand -base64 32`

### Database Password (`SPRING_DATASOURCE_PASSWORD`)

**Requirement**: Strong password for the `portfolio_user`.

- **Command**: `openssl rand -base64 16`

### GitHub Personal Access Token (`GHCR_PAT`)

**Requirement**: A token with `read:packages` scope **only**.

> **Least-Privilege Note**: `GHCR_PAT` is used exclusively to run `docker login ghcr.io` on the VPS so that `docker compose pull` can fetch private images. It is **never** used to push images. Image pushes are performed by the `build-and-push` CI job using the built-in `GITHUB_TOKEN`, which automatically receives `packages: write` permission for the duration of that job. Granting `GHCR_PAT` anything beyond `read:packages` violates least-privilege and is unnecessary.

- **Steps**:
  1. Go to GitHub → Settings → Developer Settings → Personal Access Tokens (classic).
  2. Select **only** `read:packages`.
  3. Copy the token and save it as `GHCR_PAT` in GitHub Actions Secrets.

---

## 3. Storage and Injection Flow

### Local Development

Secrets are stored in a local `.env` file (which is git-ignored).

- **Backend**: Loaded via Spring Boot's environment property resolution.
- **Frontend**: Loaded during the Vite build process (must be prefixed with `VITE_`).

### Production (VPS) — Hybrid Model

Production uses a **hybrid approach** for secrets:

**Automated deploys (CI/CD — primary path):**
1. GitHub Actions reads secrets from **GitHub Actions Secrets**.
2. The `appleboy/ssh-action` exports these variables directly into the shell session on the VPS.
3. `docker compose` reads these exported environment variables to populate the containers.
4. No secret values are written to disk by this path.

**Manual / emergency runs (fallback path):**
- A `~/portfolio/.env` file **does exist on the VPS** and is read automatically by `docker compose` when the compose files are invoked manually (e.g., for emergency restarts or debugging). This file is not committed to the repository and must be maintained separately on the VPS.
- This means a manual `docker compose -f docker-compose.prod.yml up -d` on the VPS will still work without re-exporting all CI env vars.

> **Security implication**: The `.env` file on the VPS is a single point of exposure and must have `chmod 600` and be owned by the deploy user. Rotate its contents whenever GitHub Secrets are rotated.

### Example Export in CI/CD:

```bash
export SPRING_DATASOURCE_PASSWORD="${{ secrets.SPRING_DATASOURCE_PASSWORD }}"
export JWT_SECRET="${{ secrets.JWT_SECRET }}"
export GHCR_REPO="${{ needs.build-and-push.outputs.repo_lower }}"
docker compose -f docker-compose.prod.yml -f docker-compose.observability.yml up -d
```

---

## 4. Security Checklist

- [ ] **Rotate Secrets**: Change `JWT_SECRET` and `SPRING_DATASOURCE_PASSWORD` every 6 months.
- [ ] **Least Privilege**: Ensure the `GHCR_PAT` only has `read:packages` — not write, not delete.
- [ ] **No Hardcoding**: Double-check that no secrets are committed to the repository (verified by `.gitignore`).
- [ ] **SSH Key**: Ensure the `SSH_PRIVATE_KEY` is a dedicated deployment key with limited access to the VPS.
- [ ] **VPS .env permissions**: Verify `chmod 600 ~/portfolio/.env` on the VPS.
