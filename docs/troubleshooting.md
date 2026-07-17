# Troubleshooting Guide & Runbook

This document serves as a first-response guide for common errors encountered during development, deployment, and production operations of Folio.io.

## 1. Application & Authentication Issues

### JWT Validation Errors
**Symptom**: Users cannot log in or are unexpectedly logged out; `401 Unauthorized` responses.
- **Cause 1: Secret Length**: The `JWT_SECRET` is less than 32 characters.
  - **Fix**: Update `.env` or GitHub Secrets with a secure 32+ character key and restart the backend.
- **Cause 2: Clock Drift**: Token expiration occurs before the `issuedAt` time or is expired.
  - **Fix**: Check VPS system time using `date`. Synchronize with `ntp`.
- **Cause 3: Cookie Issues**: Browser blocking HttpOnly cookies.
  - **Fix**: Ensure the domain matches the `Caddyfile` configuration and that you are accessing the site via HTTPS in production.

### Rate Limit Hit (`429 Too Many Requests`)
**Symptom**: `429` error on `/api/v1/auth/*` endpoints.
- **Cause**: Bucket4j filter is capping requests to 10 per minute per IP.
- **Fix**: Wait 60 seconds. If this happens in testing, increase the limit in `RateLimitFilter.java`.

---

## 2. Database & Persistence Issues

### Backend Fails to Start (Postgres Connectivity)
**Symptom**: Backend container enters a restart loop; logs show `Connection refused` or `PSQLException`.
- **Cause 1: Healthcheck Delay**: Backend started before Postgres was fully ready.
  - **Fix**: The `depends_on: condition: service_healthy` in `docker-compose.yml` usually prevents this. If it persists, check `docker ps` to see if `portfolio-postgres` is actually `healthy`.
- **Cause 2: Password Mismatch**: `SPRING_DATASOURCE_PASSWORD` in backend does not match `POSTGRES_PASSWORD` in the DB container.
  - **Fix**: Verify that the same secret is passed to both services in the compose file.

### Flyway Migration Failures
**Symptom**: Backend logs show `Migration checksum mismatch` or `FlywayException`.
- **Cause**: A migration script (e.g., `V1__init_schema.sql`) was edited after it was already applied to the database.
- **Fix**: 
  - **Dev**: Wipe the volume (`docker compose down -v`) and restart.
  - **Prod**: Create a new migration version (`V3__...`) to apply the change, or manually fix the `flyway_schema_history` table (caution: backup first!).

---

## 3. Deployment & CI/CD Failures

### Deployment Pipeline Fails at `deploy-production`
**Symptom**: GitHub Action fails during the SSH step.
- **Cause 1: SSH Key/Permissions**: `SSH_PRIVATE_KEY` is incorrect or the user does not have sudo/docker permissions on the VPS.
  - **Fix**: Verify the key in GitHub Secrets and ensure `VPS_USER` is in the `docker` group on the host.
- **Cause 2: GHCR Login Failed**: `GHCR_PAT` expired or has insufficient permissions.
  - **Fix**: Generate a new Personal Access Token with `read:packages` and update GitHub Secrets.

### Frontend Shows "API Not Found" or CORS Errors
**Symptom**: Frontend loads, but all API calls fail with `CORS error` or `404`.
- **Cause 1: Wrong API Base URL**: `VITE_API_BASE_URL` was baked into the image incorrectly.
  - **Fix**: Check the build logs in GitHub Actions to ensure the build-arg was passed correctly.
- **Cause 2: Caddy Routing**: Caddy is not routing `/api/*` to the backend.
  - **Fix**: Check the `Caddyfile` on the VPS and run `docker compose logs caddy`.

### Caddy Silently Ignoring Caddyfile Changes After Deploy
**Symptom**: The Caddyfile on disk is correct, `caddy reload` reports success, but the running Caddy container continues serving the old routing configuration.

**Root Cause — Inode Bind Mount**: Docker single-file bind mounts (`./Caddyfile:/etc/caddy/Caddyfile:ro`) attach to the **inode** of the file at container start time. When the host file is replaced via an atomic rename (which `git pull`, `scp`, and `sftp` all do for safe writes), the running container keeps a file descriptor to the **old, orphaned inode**. `caddy reload` re-reads this stale view and reports success because it sees no error — it simply re-loads the old content.

**Diagnosis**: Compare the host file against the container's view:
```bash
# On the VPS:
cat ~/portfolio/Caddyfile
docker exec portfolio-caddy cat /etc/caddy/Caddyfile
```
If the two outputs differ, this is the bug.

**Fix**: Force-recreate the Caddy container so Docker re-opens the bind mount on the current inode:
```bash
docker compose -f docker-compose.prod.yml -f docker-compose.observability.yml \
  up -d --force-recreate --no-deps caddy
```

> **Note**: The CI/CD pipeline (`ci-cd.yml`) already runs this force-recreate step on every deploy, so this issue only manifests during manual Caddyfile updates that bypass the pipeline.

---

## 4. Observability Issues

### Grafana Dashboards are Empty
**Symptom**: Grafana is running, but no data is showing for Prometheus or Loki.
- **Cause 1: Datasource URL**: Grafana cannot reach `http://prometheus:9090` or `http://loki:3100`.
  - **Fix**: Ensure all services are in the same Docker network. Check `docker network inspect`.
- **Cause 2: Scrape Failure**: Prometheus cannot reach the backend actuator.
  - **Fix**: Verify the backend is exposing metrics on port `8081`. Try `curl http://localhost:8081/actuator/prometheus` from the VPS.

### Logs are Missing in Loki
**Symptom**: No logs appearing in Grafana Explore for Loki.
- **Cause**: Promtail cannot read the Docker socket.
  - **Fix**: Ensure the `docker-compose.observability.yml` mounts `/var/run/docker.sock:/var/run/docker.sock:ro`.

### Loki Crash-Loop: `compactor.delete-request-store should be configured when retention is enabled`
**Symptom**: The `portfolio-loki` container enters a crash loop. Logs show:
```
compactor.delete-request-store should be configured when retention is enabled
```

**Root Cause**: Loki 3.x made `delete_request_store` a **required field** whenever `compactor.retention_enabled: true` is set. If `delete_request_store` is absent from the `compactor` block, Loki refuses to start.

**Fix**: Add `delete_request_store: filesystem` to the `compactor` block in `observability/loki-config.yml`:
```yaml
compactor:
  working_directory: /loki/compactor
  delete_request_store: filesystem   # required by Loki 3.x when retention_enabled: true
  retention_enabled: true
```

> **Current status**: This fix has already been applied in the repo. This runbook entry is preserved so the root cause is understood if it re-appears after a config rollback.
