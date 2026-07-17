# Networking & Routing Guide

This document provides a detailed map of the network topology, port configurations, and request routing logic for the Folio.io platform.

## 1. Network Topology

The system uses a layered networking approach. In production, all external traffic is intercepted by an edge proxy, while internal service communication happens over a private Docker bridge network.

### Traffic Flow Diagram
`User Browser (HTTPS)` $\rightarrow$ `Caddy (Port 443)` $\rightarrow$ `Internal Docker Network` $\rightarrow$ `Target Service`

---

## 2. Production Routing (Caddy)

Caddy acts as the reverse proxy and SSL terminator for the domain `folio-ilyas.duckdns.org`. It is configured to route traffic based on the request path.

### Routing Rules
| Request Path | Target Service | Internal Port | Description |
|--------------|----------------|---------------|-------------|
| `/api/*`     | `backend`       | `8080`        | All REST API requests |
| `/grafana/*` | `grafana`       | `3000`        | Monitoring dashboards |
| `/*` (Default)| `frontend`     | `80`          | React SPA assets & routing |

### SSL/TLS Management
Caddy automatically manages TLS certificates via **Let's Encrypt**. No manual certificate installation is required; Caddy handles the ACME challenge and renewal automatically.

---

## 3. Port Mapping & Exposure

To maximize security, only the edge proxy is exposed to the public internet. All other services are hidden within the Docker network.

### Production Port Map
| Service | Host Port | Internal Port | Exposure | Purpose |
|---------|-----------|---------------|-----------|----------|
| **Caddy** | `80` | `80` | 🌍 Public | HTTP $\rightarrow$ HTTPS Redirect |
| **Caddy** | `443` | `443` | 🌍 Public | Main Entry Point (HTTPS) |
| **Frontend** | - | `80` | 🔒 Internal (Docker network only) | Served by Nginx |
| **Backend** | - | `8080` | 🔒 Internal (Docker network only) | Main Application API |
| **Backend** | - | `8081` | 🔒 Internal (Docker network only) | Actuator / Prometheus metrics |
| **Grafana** | - | `3000` | 🔒 Internal (Docker network only); reachable externally via Caddy's `/grafana/*` path | Dashboards |
| **Postgres** | - | `5432` | 🔒 Internal (Docker network only) | Database access |
| **Prometheus**| - | `9090` | 🔒 Internal (Docker network only) | Metrics storage |
| **Loki** | - | `3100` | 🔒 Internal (Docker network only) | Log storage |

> **Grafana exposure clarification**: Grafana's container port `3000` is **not** bound to the host (no `ports:` entry in `docker-compose.observability.yml`). It is reachable from the public internet **only** via the Caddy reverse proxy at `https://folio-ilyas.duckdns.org/grafana/*`. The port itself is not directly accessible from outside the Docker network.

### Development Port Map (`docker-compose.yml`)
In development mode, some ports are mapped to the host for easier debugging:
- `80` $\rightarrow$ Frontend
- `8080` $\rightarrow$ Backend
- `5432` $\rightarrow$ PostgreSQL (Allows use of external DB tools like pgAdmin)

---

## 4. CORS & Security Headers

### CORS Configuration
The backend implements a strict Cross-Origin Resource Sharing (CORS) policy to prevent unauthorized domains from accessing the API.

- **Dev Mode**: Defaults to allowing `http://localhost:5173`.
- **Prod Mode**: Uses the `APP_CORS_ALLOWED_ORIGINS` environment variable.
- **Config Location**: `backend/src/main/java/com/gc2026/portfolio/config/CorsConfig.java`

### Frontend Proxy (Dev Mode)
During local development, the Vite dev server acts as a proxy to avoid CORS issues:
- Request to `/api` $\rightarrow$ Proxied to `http://localhost:8080`
- Configured in `frontend/vite.config.ts`.

---

## 5. Summary Checklist for Operators

- [ ] **DNS**: Ensure `folio-ilyas.duckdns.org` points to the VPS IP.
- [ ] **Firewall**: Open ports `80` and `443` on the VPS firewall (UFW/iptables).
- [ ] **Caddyfile**: Ensure the domain in the `Caddyfile` matches the DNS record.
- [ ] **Network**: Verify all services are on the same Docker network to allow resolution by service name (e.g., `http://backend:8080`).
