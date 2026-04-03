# ERP System — Deployment Guide

## Prerequisites

- **Docker** >= 24.x
- **Docker Compose** >= 2.x
- An accessible Oracle database (default port `1892`)

---

## Architecture

```
Browser
  │
  ▼
┌─────────────────────────┐
│  Frontend (nginx:80)    │
│  Angular SPA            │
│  ┌───────────────────┐  │
│  │  /api/* → proxy   │  │
│  └────────┬──────────┘  │
└───────────┼─────────────┘
            │
            ▼
┌─────────────────────────┐
│  Backend (Spring Boot)  │
│  Port 7272              │
│  Profile: prod          │
└───────────┬─────────────┘
            │
            ▼
     Oracle Database
     (external / host)
```

---

## Quick Start

### 1. Create environment file

```bash
cd deploy/
cp .env.example .env
```

Edit `.env` with real values:

| Variable | Description |
|---|---|
| `DB_URL` | Oracle JDBC URL (e.g., `jdbc:oracle:thin:@//host:1892/orclpdb`) |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | Random 256-bit secret for JWT signing |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins |
| `BACKEND_PORT` | Host port mapped to backend (default: `7272`) |
| `FRONTEND_PORT` | Host port mapped to frontend (default: `80`) |

### 2. Build and start

```bash
# From the deploy/ directory
docker compose up --build -d
```

### 3. Verify

```bash
# Check all containers are running
docker compose ps

# Backend health
curl http://localhost:7272/actuator/health

# Frontend
open http://localhost
```

---

## Individual Container Builds

### Backend only

```bash
# From project root
docker build -f deploy/backend/Dockerfile -t erp-backend .
docker run -p 7272:7272 \
  -e DB_URL=jdbc:oracle:thin:@//host.docker.internal:1892/orclpdb \
  -e DB_USERNAME=erp_user \
  -e DB_PASSWORD=secret \
  -e JWT_SECRET=your-secret \
  erp-backend
```

### Frontend only

```bash
# From project root
docker build -f deploy/frontend/Dockerfile -t erp-frontend .
docker run -p 80:80 erp-frontend
```

---

## Logs

```bash
# All services
docker compose logs -f

# Backend only
docker compose logs -f backend

# Frontend only
docker compose logs -f frontend
```

---

## Stopping

```bash
docker compose down
```

---

## Troubleshooting

| Problem | Cause | Fix |
|---|---|---|
| Backend fails to start | DB unreachable | Verify `DB_URL` and that Oracle DB is running |
| Frontend shows blank page | Wrong nginx config | Check `docker compose logs frontend` |
| API calls return 502 | Backend not healthy yet | Frontend waits for backend health check; wait and retry |
| Port already in use | Port conflict | Change `BACKEND_PORT` or `FRONTEND_PORT` in `.env` |
| CORS errors | Wrong allowed origins | Update `CORS_ALLOWED_ORIGINS` in `.env` |
| JWT errors | Weak/missing secret | Set a strong 256-bit `JWT_SECRET` |

---

## Notes

- The frontend Nginx proxies all `/api/*` requests to the backend container — no hardcoded backend IP needed inside the container network.
- The backend runs with `--spring.profiles.active=prod`, loading `application-prod.properties`.
- Secrets are injected at runtime via the `.env` file — never stored in images.
