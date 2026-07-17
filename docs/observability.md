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
- **Metrics**: CPU usage, Memory limits/usage, Network I/O per container.
- **Target**: `cadvisor:8080`

### Layer 3: Host Metrics (Node Exporter)
`node-exporter` monitors the underlying Linux VPS.
- **Metrics**: Disk space, RAM usage, Load average, CPU interrupts.
- **Target**: `node-exporter:9100`

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
- **Admin**: Credentials managed via `GRAFANA_ADMIN_PASSWORD`.

### Provisioning
To ensure consistency across redeploys, datasources are auto-provisioned via `observability/grafana/provisioning/datasources/datasources.yml`:
- **Prometheus**: Connected to `http://prometheus:9090`
- **Loki**: Connected to `http://loki:3100`

---

## 5. Summary of Observability Ports

| Service | Internal Port | Access | Purpose |
|---------|---------------|--------|---------|
| Prometheus | 9090 | Internal | Metric storage & Querying |
| Loki | 3100 | Internal | Log storage & Querying |
| Grafana | 3000 | Internal (Docker network only); publicly reachable via Caddy `/grafana/*` | Dashboards & Visualization |
| Node Exporter | 9100 | Internal | Host OS Metrics |
| cAdvisor | 8080 | Internal | Container Metrics |
