# Setup Guide

This guide explains how to get the Folio.io project running from scratch on a new hardware.

## 1. Prerequisites

Ensure you have the following installed:
- **Docker & Docker Compose**: Required for the database and production-like environments.
- **Java 17+**: Required for local backend development
- **Maven 3.9+**: For building the Java application
- **Node.js 22+ & npm**: Required for the React frontend

## 2. Environment Configuration

The project relies on environment variables for security and connectivity.

1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```
2. Update the following keys in `.env`:
   - `JWT_SECRET`: Must be at least 32 characters long.
   - `SPRING_DATASOURCE_PASSWORD`: Your chosen password for the PostgreSQL database.
   - `VITE_API_BASE_URL`: Set to `http://localhost:8080/api/v1` for local dev.

## 3. Running the Project

### Option A: Dockerized Setup (Recommended for Quick Start)
This method starts the database and the full application stack.

```bash
# Start the stack
docker compose up -d
```
- **Frontend**: Accessible at `http://localhost:80`
- **Backend**: Accessible at `http://localhost:8080`
- **Database**: Accessible at `localhost:5432`

### Option B: Local Development Mode (Hot-Reloading)
Use this for active development where you need fast feedback loops.

**1. Start the Database**
```bash
docker compose up -d postgres
```

**2. Run the Backend**
```bash
cd backend
export JWT_SECRET="your-super-secret-key-min-32-chars-long"
export SPRING_DATASOURCE_PASSWORD="your_password"
./mvnw spring-boot:run
```
The API will start at `http://localhost:8080`. Flyway migrations will automatically update the schema.

**3. Run the Frontend**
```bash
cd frontend
npm install
npm run dev
```
The frontend will start at `http://localhost:5173`. API requests are proxied to the backend via Vite.

## 4. Verification

Once started, verify the system is healthy:
- **Backend Health**: The actuator is always on port `8081` (`management.server.port` in `application.properties`). 
  - **Local dev (Option B / `./mvnw spring-boot:run`)**: hit `http://localhost:8081/actuator/health` directly.
  - **Docker dev (Option A / `docker compose up`)**: port `8081` is **not** exposed to the host by `docker-compose.yml` — only `8080` is. Use `docker exec portfolio-backend wget -qO- http://localhost:8081/actuator/health` instead, or check `http://localhost:8080/actuator/health` (the app port also serves actuator in dev builds where `management.server.port` may not be set separately — **UNVERIFIED for Docker dev mode; needs manual confirmation**).
- **Database**: Ensure the `portfolio_db` is created and the `flyway_schema_history` table is populated.
