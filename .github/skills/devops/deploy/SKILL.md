---
name: deploy
description: "Generates production deployment artifacts: Dockerfiles, docker-compose.yml, nginx configs, environment files, deploy scripts, and production configurations. Uses network_mode: host for backend, Git-based pull deployment, and enforces production Spring profile. Analyzes multi-module Spring Boot backend and Angular frontend to produce ready-to-deploy containerized setup."
---

# Skill: deploy

## Name
`deploy`

## Description
Generates all deployment artifacts required to containerize and deploy the ERP system. Analyzes the project structure (backend modules, frontend config, existing properties) and produces Dockerfiles, docker-compose.yml, nginx configuration, deploy scripts, and production environment files — all placed in the `deploy/` directory.

**Architecture:**
- Backend runs in Docker with `network_mode: host` (shares host network)
- Database (Oracle) runs on the host machine — accessed via `localhost`
- Frontend served via Nginx (reverse proxy to `localhost:7272`)
- All API calls go through Nginx `/api` — no direct backend access
- Deployment is Git pull-based (push → SSH → pull → rebuild)

## When to Use
- When the user says "deploy", "prepare deployment", "containerize", or "create Docker setup"
- When creating or updating Dockerfiles, docker-compose, or nginx configs
- When preparing a production release
- When adding a new backend module that must be included in the deployment

## Trigger
User says: `deploy`, `prepare deployment`, `dockerize`, `create docker setup`, `production deploy`

## Responsibilities

- Analyze project structure (backend modules, frontend config, existing properties)
- Generate Dockerfiles for backend (Spring Boot) and frontend (Nginx)
- Generate `docker-compose.yml` with `network_mode: host` for backend
- Generate Nginx reverse proxy configuration
- Generate deploy scripts for Git-based pull deployment
- Generate production environment files and Spring profile configs

## Constraints

- MUST NOT modify application source code (backend or frontend)
- MUST NOT change database configuration or schema
- MUST NOT modify existing Spring profiles — create production profile only
- MUST NOT expose backend directly — all access through Nginx reverse proxy
- MUST NOT assume infrastructure details — ask if database host/port is unclear

## Output

- Complete deployment artifact set in `deploy/` directory:
  - `deploy/backend/Dockerfile`
  - `deploy/frontend/Dockerfile`
  - `deploy/docker-compose.yml`
  - `deploy/frontend/nginx.conf`
  - `deploy/deploy.sh`
  - Production environment/config files

---

## 1. Project Analysis (Auto-Detect Before Generating)

Before generating ANY deployment artifact, the skill MUST analyze the current project state:

### 1.1 Backend Analysis

| Check | How |
|-------|-----|
| Spring Boot version | Read `backend/pom.xml` → `<parent>` → `spring-boot-starter-parent` version |
| Java version | Read `backend/pom.xml` → `<java.version>` |
| Modules list | Read `backend/pom.xml` → `<modules>` (skip commented-out modules) |
| Bootable JAR module | Identify the module with `<packaging>jar</packaging>` and Spring Boot plugin (typically `erp-main`) |
| Artifact name + version | `<artifactId>` + `<version>` from bootable module (e.g., `erp-main-1.0.0-SNAPSHOT.jar`) |
| Production config file | Search for `application-prod.properties` or `application-prod.yml` in the bootable module's `src/main/resources/` |
| Database driver | Detect from `pom.xml` dependencies (e.g., `ojdbc11` → Oracle, `postgresql` → PostgreSQL) |
| Required env vars | Parse production config for `${VAR_NAME:default}` patterns |
| Spring profile active | Verify `--spring.profiles.active=prod` is set in Dockerfile ENTRYPOINT or docker-compose `SPRING_PROFILES_ACTIVE` |

### 1.2 Frontend Analysis

| Check | How |
|-------|-----|
| Angular project name | Read `frontend/angular.json` → first key under `projects` |
| Output path | Read `angular.json` → `architect.build.options.outputPath` |
| Production config | Verify `configurations.production` exists with `fileReplacements` |
| Environment file | Read `frontend/src/environments/environment.prod.ts` — verify `apiUrl: '/api'` (proxy-relative) |

### 1.3 Existing Deployment State

| Check | How |
|-------|-----|
| `deploy/` directory | Check if it exists and what it contains |
| Existing Dockerfiles | Search `**/Dockerfile*` — update if found, create if not |
| Existing docker-compose | Search `**/docker-compose*` — update if found, create if not |
| Git remote | Verify Git remote is configured for pull-based deployment |

### 1.4 Network Architecture Validation

| Check | How |
|-------|-----|
| Backend network mode | Verify `network_mode: host` in docker-compose for backend |
| DB connectivity | Verify DB_URL uses `localhost` (not `host.docker.internal`) |
| Nginx proxy target | Verify `proxy_pass` uses `http://localhost:7272` (not Docker service name) |
| Frontend API URL | Verify `environment.prod.ts` uses `/api` (not hardcoded IP:PORT) |

---

## 2. Backend Deployment Rules

### 2.1 Dockerfile — Multi-Stage Build

**Path:** `deploy/backend/Dockerfile`

**Canonical Pattern:**

```dockerfile
# ============================================
# Stage 1: Build
# ============================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy parent POM first (cache dependencies)
COPY backend/pom.xml ./pom.xml

# Copy all module POMs
COPY backend/erp-common-utils/pom.xml ./erp-common-utils/pom.xml
COPY backend/erp-security/pom.xml ./erp-security/pom.xml
COPY backend/erp-masterdata/pom.xml ./erp-masterdata/pom.xml
COPY backend/erp-finance-gl/pom.xml ./erp-finance-gl/pom.xml
COPY backend/erp-main/pom.xml ./erp-main/pom.xml

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY backend/erp-common-utils/src ./erp-common-utils/src
COPY backend/erp-security/src ./erp-security/src
COPY backend/erp-masterdata/src ./erp-masterdata/src
COPY backend/erp-finance-gl/src ./erp-finance-gl/src
COPY backend/erp-main/src ./erp-main/src

# Build
RUN mvn clean package -DskipTests -B

# ============================================
# Stage 2: Runtime
# ============================================
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Security: run as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy JAR from build stage
COPY --from=build /app/erp-main/target/erp-main-*.jar app.jar

# Set ownership
RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 7272

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

### 2.2 Backend Dockerfile Enforcement

| ID | Rule | ❌ Forbidden | ✅ Required |
|----|------|-------------|------------|
| BD.1 | Multi-stage build | Single-stage with JDK in runtime | Separate `build` and `runtime` stages |
| BD.2 | JRE-only runtime | `FROM eclipse-temurin:21` (full JDK) | `FROM eclipse-temurin:21-jre-alpine` |
| BD.3 | Non-root user | `USER root` or no USER directive | `RUN adduser` + `USER appuser` |
| BD.4 | Layer caching | Copying all source before dependency resolution | Copy POMs first → `dependency:go-offline` → then copy source |
| BD.5 | Skip tests in Docker | `mvn clean package` (runs tests) | `mvn clean package -DskipTests -B` |
| BD.6 | Active profile | Hardcoded properties in Dockerfile | `--spring.profiles.active=prod` in ENTRYPOINT |
| BD.7 | All modules included | Missing any active module's POM or src | Every uncommented module POM + source copied |
| BD.8 | No secrets in image | `ENV DB_PASSWORD=xxx` in Dockerfile | Secrets via docker-compose `environment:` or `.env` |
| BD.9 | Host network mode | `network_mode: bridge` or Docker networks for backend | `network_mode: host` — backend shares host network |
| BD.10 | DB via localhost | `host.docker.internal` or Docker service name for DB | `localhost` for DB connection (host network = host's localhost) |

---

## 3. Frontend Deployment Rules

### 3.1 Dockerfile — Multi-Stage Build

**Path:** `deploy/frontend/Dockerfile`

**Canonical Pattern:**

```dockerfile
# ============================================
# Stage 1: Build
# ============================================
FROM node:20-alpine AS build
WORKDIR /app

# Copy package files first (cache dependencies)
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --legacy-peer-deps

# Copy source
COPY frontend/ ./

# Build production
RUN npx ng build --configuration=production

# ============================================
# Stage 2: Serve with Nginx
# ============================================
FROM nginx:1.27-alpine AS runtime

# Remove default config
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom nginx config
COPY deploy/frontend/nginx.conf /etc/nginx/conf.d/default.conf

# Copy built Angular app
COPY --from=build /app/dist/n-erp-system/browser /usr/share/nginx/html

# Security: run as non-root (nginx user already exists in alpine)
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### 3.2 Nginx Configuration

**Path:** `deploy/frontend/nginx.conf`

**Canonical Pattern:**

```nginx
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml text/javascript;
    gzip_min_length 1000;

    # Angular SPA — fallback to index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Proxy API calls to backend (host network — backend on localhost)
    location /api/ {
        proxy_pass http://localhost:7272/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # Deny dotfiles
    location ~ /\. {
        deny all;
    }
}
```

### 3.3 Frontend Dockerfile Enforcement

| ID | Rule | ❌ Forbidden | ✅ Required |
|----|------|-------------|------------|
| FD.1 | Multi-stage build | Single-stage with Node in runtime | Separate `build` and `runtime` stages |
| FD.2 | Nginx for serving | `ng serve` or `http-server` in production | `nginx:1.27-alpine` |
| FD.3 | SPA fallback | No `try_files` for Angular routes | `try_files $uri $uri/ /index.html` |
| FD.4 | API reverse proxy via localhost | Docker service name (`backend:7272`) or hardcoded IP | `location /api/` proxying to `localhost:7272` (host network) |
| FD.5 | Layer caching | Copy all source before `npm ci` | Copy `package.json` + `package-lock.json` first |
| FD.6 | Production build | `ng build` (dev mode) | `ng build --configuration=production` |
| FD.7 | Security headers | No security headers | X-Frame-Options, X-Content-Type-Options, X-XSS-Protection |
| FD.8 | Gzip enabled | No compression | `gzip on` + `gzip_types` |
| FD.9 | Static asset caching | No cache headers on assets | `expires 1y` + `Cache-Control: public, immutable` |
| FD.10 | Output path matches angular.json | Hardcoded wrong path | Must match `outputPath` from `angular.json` |

---

## 4. Docker Compose — Full Stack

### 4.1 docker-compose.yml

**Path:** `deploy/docker-compose.yml`

**Canonical Pattern:**

```yaml
services:
  # ============================================
  # Backend — Spring Boot (host network)
  # ============================================
  backend:
    build:
      context: ..
      dockerfile: deploy/backend/Dockerfile
    container_name: erp-backend
    network_mode: host
    # No 'ports:' needed — host network binds directly to 7272
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_URL=${DB_URL:-jdbc:oracle:thin:@//localhost:1892/orclpdb}
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:7272/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 40s

  # ============================================
  # Frontend — Angular + Nginx (host network)
  # ============================================
  frontend:
    build:
      context: ..
      dockerfile: deploy/frontend/Dockerfile
    container_name: erp-frontend
    network_mode: host
    # No 'ports:' needed — host network binds directly to 80
    depends_on:
      backend:
        condition: service_healthy
    restart: unless-stopped

# No 'networks:' block — both services use host network
```

### 4.2 Environment File Template

**Path:** `deploy/.env.example`

**Canonical Pattern:**

```env
# ============================================
# ERP System — Production Environment Variables
# ============================================
# Copy this file to .env and fill in actual values
# NEVER commit .env to version control
# ============================================

# Backend
DB_URL=jdbc:oracle:thin:@//localhost:1892/orclpdb
DB_USERNAME=erp_user
DB_PASSWORD=CHANGE_ME
JWT_SECRET=CHANGE_ME_TO_A_RANDOM_256_BIT_SECRET
```

### 4.3 Docker Compose Enforcement

| ID | Rule | ❌ Forbidden | ✅ Required |
|----|------|-------------|------------|
| DC.1 | Build context is project root | `context: .` from `deploy/` dir | `context: ..` (project root) |
| DC.2 | No hardcoded secrets | `DB_PASSWORD: mypass` in yml | `DB_PASSWORD=${DB_PASSWORD}` from `.env` |
| DC.3 | Health check on backend | No health check | `wget` or `curl` to `/actuator/health` |
| DC.4 | Frontend depends on backend | No dependency or `depends_on: backend` (without condition) | `depends_on: backend: condition: service_healthy` |
| DC.5 | Host network mode | `networks:` or `ports:` on backend | `network_mode: host` on both backend and frontend |
| DC.6 | Restart policy | `restart: always` or none | `restart: unless-stopped` |
| DC.7 | DB URL uses localhost | `host.docker.internal` or Docker service name | `jdbc:oracle:thin:@//localhost:1892/orclpdb` |
| DC.8 | `.env.example` provided | No env template | `.env.example` with placeholder values |
| DC.9 | `.env` in `.gitignore` | `.env` committed to repo | `.env` added to `.gitignore` |
| DC.10 | No CORS env needed | `CORS_ALLOWED_ORIGINS` in env | CORS eliminated — all traffic through nginx `/api` |
| DC.11 | No `version:` key | `version: "3.8"` (deprecated in Compose V2) | Omit `version:` — use modern Compose V2 format |
| DC.12 | Oracle service name | Wrong SID or service (`XEPDB1`, `XE`) | `orclpdb` as the Oracle service name |

---

## 5. Frontend Environment Integration (MANDATORY)

All frontend API calls MUST go through the Nginx reverse proxy (`/api`). Direct backend access via IP:PORT is **forbidden** in production.

### 5.1 environment.prod.ts — Proxy-Relative (REQUIRED)

**❌ FORBIDDEN — Hardcoded IP:PORT:**
```typescript
export const environment = {
  production: true,
  apiUrl: 'http://89.117.37.75:7272',     // ❌ Direct backend access
  authApiUrl: 'http://89.117.37.75:7272'   // ❌ Bypasses nginx
};
```

**✅ REQUIRED — Proxy-Relative:**
```typescript
export const environment = {
  production: true,
  apiUrl: '/api',
  authApiUrl: '/api'
};
```

### 5.2 Frontend Environment Enforcement

| ID | Rule | ❌ Forbidden | ✅ Required |
|----|------|-------------|------------|
| FE.1 | Proxy-relative API URL | Any absolute URL (`http://...`) in `environment.prod.ts` | `apiUrl: '/api'` |
| FE.2 | No hardcoded IPs | `89.117.37.75`, `localhost:7272`, or any IP:PORT | Relative path `/api` only |
| FE.3 | Auth via same proxy | Separate `authApiUrl` with different host | `authApiUrl: '/api'` (same proxy) |

---

## 6. CORS Elimination

### 6.1 Architecture Rule

With Nginx as the single entry point, CORS is **completely eliminated**:

```
Browser → Nginx (:80) → /api/* → localhost:7272 (backend)
                       → /*    → static Angular files
```

Both frontend and API are served from the **same origin** (Nginx on port 80). No cross-origin requests exist.

### 6.2 CORS Enforcement

| ID | Rule | ❌ Forbidden | ✅ Required |
|----|------|-------------|------------|
| CR.1 | No CORS env vars | `CORS_ALLOWED_ORIGINS` in docker-compose or `.env` | Remove CORS config entirely |
| CR.2 | No direct backend calls | Frontend calling `http://IP:7272/api` | Frontend calling `/api` (same-origin via nginx) |
| CR.3 | Single entry point | Multiple exposed ports (80 + 7272) to clients | Only port 80 (nginx) exposed to external clients |

> **Note:** Backend CORS config in `application-prod.properties` may remain for development flexibility, but in production deployment, Nginx handles all routing — CORS headers are unnecessary.

---

## 7. File Update Rules

### 7.1 Files the Skill Creates (in `deploy/`)

| File | Purpose |
|------|---------|
| `deploy/backend/Dockerfile` | Multi-stage backend build |
| `deploy/frontend/Dockerfile` | Multi-stage frontend build |
| `deploy/frontend/nginx.conf` | Nginx config with SPA fallback + API proxy to localhost |
| `deploy/docker-compose.yml` | Full-stack orchestration (host network) |
| `deploy/.env.example` | Environment variable template |
| `deploy/deploy.sh` | Git pull + rebuild deploy script |
| `deploy/README.md` | Deployment instructions |

### 7.2 Files the Skill MUST Update

| File | Update | Condition |
|------|--------|-----------|
| `frontend/src/environments/environment.prod.ts` | Set `apiUrl: '/api'` and `authApiUrl: '/api'` | ALWAYS — proxy-relative is mandatory |
| `.gitignore` | Add `deploy/.env`, `docker-compose.override.yml`, `*.log.*` | If not already present |

### 7.3 Files the Skill Must NEVER Modify

- `backend/pom.xml` — read-only for analysis
- `frontend/angular.json` — read-only for analysis
- `frontend/package.json` — read-only for analysis
- Any source code files under `backend/*/src/` or `frontend/src/app/`
- Backend business logic or frontend application logic

---

## 8. Deploy README

**Path:** `deploy/README.md`

**Must Include:**

1. **Prerequisites** — Docker, Docker Compose versions, Git, SSH access
2. **Quick Start** — Copy `.env.example` → `.env`, fill values, run `docker compose up -d --build`
3. **Environment Variables** — Table of all variables with descriptions
4. **Architecture Diagram** — ASCII showing: `Browser → Nginx(:80) → /api → localhost:7272(backend) → localhost:1892(Oracle/orclpdb)`
5. **Git-Based Deployment** — Push from dev → SSH to server → `git pull` → `docker compose up -d --build`
6. **Individual Container Builds** — How to build/run backend or frontend alone
7. **Troubleshooting** — Common issues (port conflicts, DB connection, Oracle service name)

---

## 9. Safety Rules

| ID | Rule |
|----|------|
| SF.1 | NEVER put real passwords, secrets, or API keys in any generated file |
| SF.2 | NEVER commit `.env` — always use `.env.example` with placeholder values |
| SF.3 | NEVER use `latest` tags for base images — always pin versions |
| SF.4 | NEVER run containers as root — always create and switch to non-root user |
| SF.5 | NEVER expose database ports in docker-compose unless explicitly requested |
| SF.6 | ALWAYS use multi-stage builds to minimize image size |
| SF.7 | ALWAYS include health checks for backend services |
| SF.8 | ALWAYS set `restart: unless-stopped` (not `always`) |
| SF.9 | ALWAYS use `.dockerignore` if applicable to exclude unnecessary files |
| SF.10 | Database MUST use persistent storage — never run DB without volume or external persistence |
| SF.11 | Deploy script must assume SSH is pre-configured — NEVER include SSH credentials |
| SF.12 | ALWAYS verify `spring.profiles.active=prod` before any production deployment |

---

## 10. Git-Based Deployment

### 10.1 Deployment Flow

```
Developer Machine:                    Production Server:
┌─────────────────┐                  ┌──────────────────────┐
│ git add .        │                  │                      │
│ git commit -m "" │  ──── push ───→ │  (GitHub/GitLab)     │
│ git push         │                  │                      │
└─────────────────┘                  └──────────┬───────────┘
                                                │
                                     SSH to server
                                                │
                                     ┌──────────▼───────────┐
                                     │ cd ~/System           │
                                     │ git pull              │
                                     │ cd deploy             │
                                     │ docker compose up -d  │
                                     │   --build             │
                                     └──────────────────────┘
```

### 10.2 Deploy Script

**Path:** `deploy/deploy.sh`

**Canonical Pattern:**

```bash
#!/bin/bash
# ============================================
# ERP System — Production Deploy Script
# ============================================
# Usage: ssh user@server 'bash ~/System/deploy/deploy.sh'
# Assumes: Git repo cloned at ~/System
# ============================================

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY_DIR="$PROJECT_DIR/deploy"

echo "=========================================="
echo " ERP System — Deploying..."
echo "=========================================="

# Step 1: Pull latest code
echo "[1/3] Pulling latest code..."
cd "$PROJECT_DIR"
git pull

# Step 2: Build and restart containers
echo "[2/3] Building and starting containers..."
cd "$DEPLOY_DIR"
docker compose up -d --build

# Step 3: Verify
echo "[3/3] Verifying deployment..."
echo "Waiting for backend health check..."
sleep 10
if wget --spider -q http://localhost:7272/actuator/health 2>/dev/null; then
    echo "✅ Backend is healthy"
else
    echo "⚠️  Backend health check pending — check logs: docker compose logs backend"
fi

echo "=========================================="
echo " Deployment complete!"
echo "=========================================="
```

### 10.3 Deploy Script Enforcement

| ID | Rule | ❌ Forbidden | ✅ Required |
|----|------|-------------|------------|
| GD.1 | Script uses `set -euo pipefail` | No error handling | `set -euo pipefail` at top |
| GD.2 | No hardcoded paths | `cd /home/user/project` | Use `$(dirname "$0")/..` for relative resolution |
| GD.3 | No credentials in script | `git pull https://user:pass@...` | Assumes SSH key auth is pre-configured |
| GD.4 | Script is executable | Missing execute permission | `chmod +x deploy.sh` documented in README |
| GD.5 | Post-deploy verification | No health check after deploy | Verify backend `/actuator/health` after restart |

---

## 11. Gitignore Validation

### 11.1 Required Entries

The skill MUST verify these entries exist in the project `.gitignore`:

```gitignore
# Deploy secrets
deploy/.env

# Docker overrides
docker-compose.override.yml

# Log files
*.log.*

# Build artifacts
**/target/
**/dist/
**/node_modules/
```

### 11.2 Gitignore Enforcement

| ID | Rule | ❌ Forbidden | ✅ Required |
|----|------|-------------|------------|
| GI.1 | `.env` ignored | `deploy/.env` committed to repo | `deploy/.env` in `.gitignore` |
| GI.2 | Override ignored | `docker-compose.override.yml` committed | `docker-compose.override.yml` in `.gitignore` |
| GI.3 | Logs ignored | `*.log.*` files committed | `*.log.*` in `.gitignore` |

---

## 12. Production Mode Enforcement (CRITICAL)

The system MUST ensure the backend always runs in **production mode**, never development.

### 12.1 Production Profile Rules

**❌ WRONG PRACTICE:**
- Running with default profile (dev)
- Hardcoding dev configurations inside `application.properties`
- Missing `spring.profiles.active=prod` anywhere in the runtime chain

**✅ REQUIRED — Docker-Controlled (Preferred):**

The production profile MUST be activated via one of these methods (in priority order):

1. **Dockerfile ENTRYPOINT** (highest priority):
```dockerfile
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

2. **docker-compose environment** (redundant safety):
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=prod
```

3. **Config file fallback** (last resort — if Docker config is missing):
```properties
# application.properties
spring.profiles.active=prod
```

### 12.2 Production Profile Enforcement

| ID | Rule | ❌ Forbidden | ✅ Required |
|----|------|-------------|------------|
| PM.1 | Profile set in Dockerfile | No `--spring.profiles.active` in ENTRYPOINT | `--spring.profiles.active=prod` in ENTRYPOINT |
| PM.2 | Profile set in docker-compose | No `SPRING_PROFILES_ACTIVE` in environment | `SPRING_PROFILES_ACTIVE=prod` as redundant safety |
| PM.3 | `application-prod.*` exists | No production config file | `application-prod.properties` or `application-prod.yml` in bootable module |
| PM.4 | No dev logging in prod | `logging.level.org.hibernate.SQL=DEBUG` | `logging.level.org.hibernate.SQL=WARN` |
| PM.5 | No `ddl-auto` in prod | `spring.jpa.hibernate.ddl-auto=update/create` | `spring.jpa.hibernate.ddl-auto=none` |
| PM.6 | No `show-sql` in prod | `spring.jpa.show-sql=true` | `spring.jpa.show-sql=false` |

### 12.3 Pre-Deploy Profile Validation

Before generating deployment artifacts, verify:

```
✓ application-prod.properties (or .yml) exists
✓ ddl-auto = none
✓ show-sql = false
✓ Logging levels are WARN or above for frameworks
✓ Dockerfile ENTRYPOINT includes --spring.profiles.active=prod
✓ docker-compose includes SPRING_PROFILES_ACTIVE=prod
```

If ANY check fails → **STOP** → report issues → **DO NOT** generate deployment files.

---

## 13. Adaptation Rules

The canonical patterns above are based on the current project analysis. The skill MUST adapt when:

| Scenario | Adaptation |
|----------|------------|
| New module added to `backend/pom.xml` | Add its POM + src COPY lines to backend Dockerfile |
| Module removed/commented out | Remove its COPY lines from backend Dockerfile |
| `angular.json` output path changes | Update nginx COPY path in frontend Dockerfile |
| Database changes from Oracle to PostgreSQL | Update `DB_URL` default in `.env.example` and docker-compose |
| Backend port changes from 7272 | Update EXPOSE, health check URL, proxy_pass, and env defaults |
| `application-prod.properties` adds new `${VAR}` | Add new variable to `.env.example` and docker-compose `environment:` |
| Frontend `environment.prod.ts` adds new fields | Document in deploy README |
| Oracle service name changes from `orclpdb` | Update all DB_URL defaults across `.env.example` and docker-compose |

---

## 14. Output Format

When the skill is invoked, produce output in this order:

```
1. [ANALYSIS]    — Print detected project config (versions, modules, paths, network mode)
2. [VALIDATE]    — Run pre-deploy checks (prod profile, env vars, no dev configs)
3. [GENERATE]    — Create all files in deploy/
4. [VERIFY]      — Run enforcement checks (all BD.*, FD.*, DC.*, SF.*, FE.*, CR.*, GD.*, GI.*, PM.* rules)
5. [SUMMARY]     — List all created/updated files with brief descriptions
```

### Analysis Output Example:

```
📋 Project Analysis:
  Backend:  Spring Boot 4.0.1 / Java 21
  Modules:  erp-common-utils, erp-security, erp-masterdata, erp-finance-gl, erp-main
  Boot JAR: erp-main-1.0.0-SNAPSHOT.jar
  Prod Config: application-prod.properties
  DB Driver: Oracle (ojdbc11)
  DB URL:   jdbc:oracle:thin:@//localhost:1892/orclpdb
  Env Vars: DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET
  Network:  host (no Docker bridge)
  Profile:  prod (Dockerfile + docker-compose)

  Frontend: Angular (n-erp-system)
  Output:   dist/n-erp-system/browser
  API URL:  /api (proxy-relative via nginx)

  Deployment: Git pull-based (deploy.sh)
```

---

## 15. Enforcement Summary

### Complete Check Matrix

| Category | IDs | Count |
|----------|-----|-------|
| Backend Dockerfile | BD.1 – BD.10 | 10 |
| Frontend Dockerfile | FD.1 – FD.10 | 10 |
| Docker Compose | DC.1 – DC.12 | 12 |
| Frontend Environment | FE.1 – FE.3 | 3 |
| CORS Elimination | CR.1 – CR.3 | 3 |
| Safety | SF.1 – SF.12 | 12 |
| Git Deployment | GD.1 – GD.5 | 5 |
| Gitignore | GI.1 – GI.3 | 3 |
| Production Mode | PM.1 – PM.6 | 6 |
| **Total** | | **64** |

All 64 checks MUST pass before the skill marks deployment artifacts as ready.

---

## 16. Pre-Deploy Validation (CRITICAL)

Before generating ANY deployment artifacts, the system MUST validate:

### 16.1 Backend Validation

| Check | ❌ Fail Condition | ✅ Pass Condition |
|-------|------------------|------------------|
| Spring profile | No `--spring.profiles.active=prod` in Dockerfile or compose | Profile set in both Dockerfile ENTRYPOINT and compose env |
| Prod config exists | No `application-prod.properties` or `application-prod.yml` | File exists in bootable module `src/main/resources/` |
| No dev configs active | `ddl-auto=update`, `show-sql=true`, `DEBUG` logging | `ddl-auto=none`, `show-sql=false`, `WARN` logging |
| DB service name | `XEPDB1`, `XE`, or wrong Oracle SID | `orclpdb` in DB_URL |
| DB uses localhost | `host.docker.internal` or any non-localhost | `localhost` in DB_URL |

### 16.2 Frontend Validation

| Check | ❌ Fail Condition | ✅ Pass Condition |
|-------|------------------|------------------|
| No hardcoded IPs | `http://89.117.37.75` or any IP in `environment.prod.ts` | `apiUrl: '/api'` only |
| No localhost URLs | `http://localhost:7272` in `environment.prod.ts` | Relative `/api` path |
| Production build | No `configurations.production` in `angular.json` | Production config with fileReplacements |

### 16.3 Network Validation

| Check | ❌ Fail Condition | ✅ Pass Condition |
|-------|------------------|------------------|
| Nginx proxy target | `proxy_pass http://backend:7272` (Docker service name) | `proxy_pass http://localhost:7272` |
| Backend reachable via nginx | Direct IP:PORT access from frontend | All API calls through `/api` → nginx → localhost:7272 |

If ANY check fails → **STOP** → report ALL issues → **DO NOT** generate deployment files.

---

## 17. Meta

| Property | Value |
|----------|-------|
| Skill ID | `deploy` |
| Category | `devops` |
| Phase | Post-development |
| Depends On | Completed backend + frontend implementation |
| Inputs | `backend/pom.xml`, `frontend/angular.json`, `application-prod.properties`, `environment.prod.ts`, `.gitignore` |
| Outputs | `deploy/` directory with 7 files (Dockerfiles, docker-compose, nginx, .env.example, deploy.sh, README) |
| Enforcement Checks | 64 |
| Network Mode | `host` (no Docker bridge networks) |
| Deployment Model | Git pull-based (`push → SSH → pull → rebuild`) |
| Database | Oracle on host — `localhost:1892/orclpdb` |
