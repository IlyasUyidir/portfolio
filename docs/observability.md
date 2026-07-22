# Observability Guide

This document explains the monitoring and logging stack used to maintain the health and performance of the Folio.io platform.

## 1. Stack Overview

The system implements a Prometheus + Loki + Grafana observability stack, simplified for a single-node VPS deployment. The stack is defined in `docker-compose.observability.yml`.

| Component | Role | Source | Destination |
|-----------|------|---------|-------------|
| **Prometheus** | Metrics Collection | App Actuator, Node Exporter, cAdvisor | TSDB Storage |
| **Loki** | Log Aggregation | Promtail $\rightarrow$ Docker Logs | Chunk Storage |
| **Promtail** | Log Shipper | Docker Socket $\rightarrow$ Loki | Loki API |
| **Grafana** | Visualization | Prometheus & Loki | User Dashboard |

---

## 2. Metrics Pipeline (Prometheus)

The platform monitors three distinct layers of the infrastructure:

### Layer 1: Application Metrics
The backend uses **Spring Boot Actuator** and **Micrometer**.
- **Endpoint**: `http://backend:8081/actuator/prometheus`
- **Scrape Interval**: 15 seconds (defined in `observability/prometheus.yml`).
- **Key Metrics**: JVM heap usage, request latency, HTTP error rates, and custom application metrics tagged with `application=folio-backend`.

### Layer 2: Container Metrics (cAdvisor)
`cAdvisor` provides real-time resource usage and performance data for every running container.
- **Image**: `ghcr.io/google/cadvisor:0.54.0` (pulled from GHCR, not Docker Hub)
- **Metrics**: CPU usage, Memory limits/usage, Network I/O per container.
- **Target**: `cadvisor:8080`
- **ARM64 / Resource Optimisation Flags**:
  - `--housekeeping_interval=15s`: Reduces cAdvisor's internal poll frequency to match Prometheus's scrape interval.
  - `--docker_only=true`: Only monitors Docker containers, ignoring other cgroup sources — reduces CPU overhead on the ARM VPS.
  - `--disable_metrics=disk`: Disables disk metrics collection, which can cause high latency on OCI block volumes.

### Layer 3: Host Metrics (Node Exporter)
`node-exporter` monitors the underlying Linux VPS.
- **Metrics**: Disk space, RAM usage, Load average, CPU interrupts.
- **Target**: `node-exporter:9100`

### Data Retention
- **Prometheus TSDB**: **15 days** — configured via the `--storage.tsdb.retention.time=15d` CLI flag in `docker-compose.observability.yml`.
- **Loki**: **14 days** (`336h`) — configured via `retention_period` in `observability/loki-config.yml` with the compactor's `retention_enabled: true` and `delete_request_store: filesystem`.

---

## 3. Logging Pipeline (Loki & Promtail)

Instead of manually checking `docker logs`, the system uses a centralized logging pipeline.

### How it works:
1. **Discovery**: `Promtail` connects to the Docker socket (`/var/run/docker.sock`) to automatically discover all containers.
2. **Labeling**: Promtail relabels the metadata so that logs are searchable by `container` name and `stream` (stdout/stderr).
3. **Shipping**: Logs are pushed to the **Loki** API (`http://loki:3100/loki/api/v1/push`).
4. **Retention**: Loki is configured with a **14-day retention period** (`retention_period: 336h`) to prevent the VPS disk from filling up.

---

## 4. Visualization (Grafana)

Grafana serves as the single pane of glass for the entire system.

### Access
- **URL**: `https://folio-ilyas.duckdns.org/grafana/` (Proxied via Caddy).
- **Admin**: Credentials managed via `GRAFANA_ADMIN_PASSWORD` environment variable.

### Critical Grafana Environment Variables

| Variable | Value | Purpose |
|---|---|---|
| `GF_SECURITY_ADMIN_PASSWORD` | `${GRAFANA_ADMIN_PASSWORD}` | Initial admin password |
| `GF_USERS_ALLOW_SIGN_UP` | `"false"` | Disables public self-registration |
| `GF_SERVER_ROOT_URL` | `https://folio-ilyas.duckdns.org/grafana/` | Required for correct asset URLs and redirects when served under a sub-path |
| `GF_SERVER_SERVE_FROM_SUB_PATH` | `"true"` | **Critical**: Enables Grafana to correctly strip the `/grafana` prefix from all internal URLs when served behind Caddy's `/grafana/*` route |

> Without `GF_SERVER_SERVE_FROM_SUB_PATH: "true"`, Grafana asset links (CSS, JS, API calls) will use the root path `/` instead of `/grafana/`, breaking the UI when accessed via the Caddy proxy.

### Provisioning
To ensure consistency across redeploys, all Grafana configuration is auto-provisioned from `observability/grafana/provisioning/`:

#### Datasources (`datasources/datasources.yml`)
- **Prometheus** (`uid: prometheus`): Connected to `http://prometheus:9090` — set as **default** datasource.
- **Loki** (`uid: loki`): Connected to `http://loki:3100`.

#### Dashboards (`dashboards/dashboards.yml`)
A file-based dashboard provider watches `/etc/grafana/provisioning/dashboards/json` every 30 seconds. JSON dashboard files placed there are automatically imported.
- `observability/grafana/provisioning/dashboards/json/folio-overview.json`: A pre-built overview dashboard is present in the repo and is loaded on Grafana startup.

---

## 5. Alerting

Grafana alerts are provisioned from `observability/grafana/provisioning/alerting/rules.yml` into the **`folio-core-alerts`** group (folder: `Folio Alerts`). Evaluation interval: **1 minute**.

| Alert UID | Title | Severity | Condition | Fire Delay | No-Data State |
|---|---|---|---|---|---|
| `folio-backend-down` | Backend down | `critical` | `up{job="folio-backend"} == 0` (backend unreachable by Prometheus) | 1 minute | `Alerting` |
| `folio-5xx-spike` | 5xx error rate spike | `warning` | 5xx rate > 5% of all requests over a 5m window | 2 minutes | `OK` |

### Alert Details

#### `folio-backend-down` (critical)
- **Query**: `up{job="folio-backend"}` — Prometheus's built-in scrape health metric.
- **Logic**: If the backend actuator endpoint (`backend:8081/actuator/prometheus`) is unreachable, this metric drops to `0`. An alert fires after 1 continuous minute.
- **No-data behaviour**: Treated as `Alerting` — if Prometheus itself cannot evaluate the query, the alert fires (fail-safe).

#### `folio-5xx-spike` (warning)
- **Query**:
  ```promql
  (sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
  / sum(rate(http_server_requests_seconds_count[5m])) or vector(0)) * 100
  ```
- **Logic**: Calculates the percentage of HTTP responses that are 5xx. The `or vector(0)` fallback ensures the alert evaluates to 0% (not a missing value) when no traffic exists.
- **Threshold**: Fires if the 5xx rate exceeds **5%** sustained for **2 minutes**.
- **No-data behaviour**: `OK` — no traffic is not an error condition.

> **Note**: Alerting rules use `noDataState` and `execErrState` — alerting channels (email, Slack, PagerDuty) must be configured separately in Grafana if notifications are required. Currently no contact points are provisioned.

---

## 6. Summary of Observability Ports

| Service | Internal Port | Access | Purpose |
|---------|---------------|--------|---------|
| Prometheus | 9090 | Internal | Metric storage & Querying |
| Loki | 3100 | Internal | Log storage & Querying |
| Grafana | 3000 | Internal (Docker network only); publicly reachable via Caddy `/grafana/*` | Dashboards & Visualization |
| Node Exporter | 9100 | Internal | Host OS Metrics |
| cAdvisor | 8080 | Internal | Container Metrics |
