# ERP System Deployment Guide

## Prerequisites

- Docker 24+
- Docker Compose V2
- Git
- SSH access to the deployment server
- Oracle reachable on the host machine at `localhost:1892/orclpdb`

## Quick Start

```bash
cd deploy
cp .env.example .env
# edit .env with real values
docker compose --env-file .env up -d --build
```

## Environment Variables

| Variable | Description |
|---|---|
| `DB_URL` | Oracle JDBC URL used directly from `deploy/.env`. Recommended value: `jdbc:oracle:thin:@//localhost:1892/orclpdb`. |
| `DB_USERNAME` | Oracle username for the ERP schema. |
| `DB_PASSWORD` | Oracle password for the ERP schema. |
| `JWT_SECRET` | Random 256-bit secret used to sign JWTs. |

## Architecture

```text
Browser
  -> Nginx (:80)
     -> /api/* -> localhost:7272 (Spring Boot)
     -> /*     -> Angular static files

Spring Boot (:7272)
  -> localhost:1892/orclpdb (Oracle on host)
```

## Git-Based Deployment

```bash
git push
ssh user@server 'bash ~/System/deploy/deploy.sh'
```

The deploy script pulls the latest code, rebuilds both images, restarts the stack, and checks the backend health endpoint.

Use `--env-file .env` when running compose so `deploy/.env` is the source of truth for variable substitution without injecting unrelated keys into the containers.

## Individual Container Builds

```bash
# project root
docker build -f deploy/backend/Dockerfile -t erp-backend .
docker build -f deploy/frontend/Dockerfile -t erp-frontend .
```

## Verification and Logs

```bash
cd deploy
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
wget --spider -q http://localhost:7272/actuator/health && echo healthy
```

## Troubleshooting

| Problem | Cause | Fix |
|---|---|---|
| Compose says an environment variable is not set | `deploy/.env` is missing a required entry | Fill in `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` in `deploy/.env`. |
| Backend container is unhealthy | Oracle is unreachable or credentials are wrong | Check `.env` and confirm Oracle is available on `localhost:1892/orclpdb`. |
| Frontend returns 502 on `/api` | Backend is not ready yet | Wait for backend health to pass, then retry. |
| Port 80 is unavailable | Another service is bound to host port 80 | Stop the conflicting service or move the reverse proxy to another host. |
| `docker compose up` fails during build | Host lacks internet access or package registry access | Verify access to Maven Central and npm registry from the build host. |
| Authentication cookies do not behave as expected | Production cookie domain is hardcoded for a specific host IP | Update the backend production cookie settings to match the real deployment hostname if needed. |

## Notes

- External clients should enter through Nginx on port 80 only.
- The backend runs with the `prod` Spring profile from both Dockerfile and compose for redundancy.
- `.env` must stay untracked; only `.env.example` belongs in version control.
- After cloning on a server, make the deploy script executable with `chmod +x deploy/deploy.sh`.
