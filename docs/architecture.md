# System Architecture

This document provides a high-level technical overview of the Folio.io platform, describing how components interact, how data flows, and the underlying data model.

## 1. High-Level Topology

The system is deployed as a set of containerized services orchestrated via Docker Compose. In production, an edge proxy handles SSL termination and request routing.

### Component Diagram (Logical Flow)
`User Browser` $\rightarrow$ `Caddy (Edge Proxy)` $\rightarrow$ `Frontend (Nginx/React)` $\rightarrow$ `Backend (Spring Boot)` $\rightarrow$ `PostgreSQL`

### Service Breakdown
- **Edge Proxy (Caddy)**: Handles HTTPS (Automatic TLS via Let's Encrypt) and routes traffic based on URL paths.
- **Frontend**: A React SPA served by Nginx. It handles the UI logic and communicates with the backend via a REST API.
- **Backend**: A Spring Boot application providing a stateless REST API. It handles authentication, business logic, and data persistence.
- **Database**: PostgreSQL 15 storing user data, financial transactions, budgets, and goals.
- **Observability Stack**: A sidecar suite consisting of Prometheus (metrics), Loki (logs), and Grafana (visualization).

---

## 2. Request Flow & Networking

### Production Traffic Routing (Caddy)
Caddy acts as the single entry point for the domain `folio-ilyas.duckdns.org`:
- `/api/*` $\rightarrow$ Proxied to `backend:8080`
- `/grafana/*` $\rightarrow$ Proxied to `grafana:3000`
- `/*` (All other) $\rightarrow$ Proxied to `frontend:80`

### Internal API Flow
1. **Frontend**: Sends an HTTP request with an `auth_token` in an **HttpOnly cookie**.
2. **Backend Filters**:
    - `RateLimitFilter`: Uses Bucket4j to limit requests (e.g., 10 req/min on auth endpoints).
    - `JwtFilter`: Validates the JWT cookie. If valid, it extracts the `userId` and `userRole` and attaches them to the request context.
3. **Controller**: Validates input using `@Valid` and delegates to the Service layer.
4. **Service Layer**: Performs business logic (e.g., centimes arithmetic) and enforces **IDOR protection** by ensuring the resource belongs to the authenticated `userId`.
5. **Repository**: Executes JPQL/SQL queries against PostgreSQL.

---

## 3. Data Model

The system uses a relational schema managed by **Flyway** migrations.

### Core Entities
- **Users**: Stores credentials and roles (`STANDARD`, `PREMIUM`, `ADMIN`).
- **Categories**: Linked to users. Supports "System" categories (immutable) and "Custom" categories.
- **Transactions**: The core ledger. Includes a `type` (REVENU/DEPENSE) and an `is_deleted` flag for **soft-deletion**.
- **Budgets**: Monthly limits per category. Unique constraint on `(user, category, year, month)`.
- **Goals & Contributions**: Tracks savings targets and the history of contributions toward those targets.
- **Revoked Tokens**: A blacklist of JWTs used to implement immediate logout.

### Key Design Decisions
- **Centimes Storage**: All monetary values are stored as `BIGINT` in centimes (e.g., 10.00 DH $\rightarrow$ 1000) to avoid floating-point precision errors.
- **Stateless Auth**: JWTs are used for scalability, but the `revoked_tokens` table allows the server to invalidate sessions.
- **Soft Delete**: Transactions are never physically removed from the DB, ensuring auditability.

---

## 4. Observability Architecture

The platform implements a full-stack monitoring solution:

- **Metrics**: `Spring Boot Actuator` $\rightarrow$ `Prometheus` $\rightarrow$ `Grafana`
- **Logs**: `Docker Logs` $\rightarrow$ `Promtail` $\rightarrow$ `Loki` $\rightarrow$ `Grafana`
- **Infrastructure**: `Node Exporter` (OS metrics) and `cAdvisor` (Container metrics) $\rightarrow$ `Prometheus`
