---
name: deploy
description: "Generates production deployment artifacts: Dockerfiles, docker-compose.yml, nginx configs, environment files, and production configurations. Analyzes multi-module Spring Boot backend and Angular frontend to produce ready-to-deploy containerized setup."
---

# Skill: deploy

## Name
`deploy`

## Description
Generates all deployment artifacts required to containerize and deploy the ERP system. Analyzes the project structure (backend modules, frontend config, existing properties) and produces Dockerfiles, docker-compose.yml, nginx configuration, and production environment files — all placed in the `deploy/` directory.

## When to Use
- When the user says "deploy", "prepare deployment", "containerize", or "create Docker setup"
- When creating or updating Dockerfiles, docker-compose, or nginx configs
- When preparing a production release
- When adding a new backend module that must be included in the deployment

## Trigger
User says: `deploy`, `prepare deployment`, `dockerize`, `create docker setup`, `production deploy`

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

### 1.2 Frontend Analysis

| Check | How |
|-------|-----|
| Angular project name | Read `frontend/angular.json` → first key under `projects` |
| Output path | Read `angular.json` → `architect.build.options.outputPath` |
| Production config | Verify `configurations.production` exists with `fileReplacements` |
| Environment file | Read `frontend/src/environments/environment.prod.ts` for `apiUrl` |

### 1.3 Existing Deployment State

| Check | How |
|-------|-----|
| `deploy/` directory | Check if it exists and what it contains |
| Existing Dockerfiles | Search `**/Dockerfile*` — update if found, create if not |
| Existing docker-compose | Search `**/docker-compose*` — update if found, create if not |

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

    # Proxy API calls to backend
    location /api/ {
        proxy_pass http://backend:7272/api/;
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
| FD.4 | API reverse proxy | Hardcoded backend URL in frontend | `location /api/` proxying to `backend:7272` |
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
version: "3.8"

services:
  # ============================================
  # Backend — Spring Boot
  # ============================================
  backend:
    build:
      context: ..
      dockerfile: deploy/backend/Dockerfile
    container_name: erp-backend
    ports:
      - "${BACKEND_PORT:-7272}:7272"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_URL=${DB_URL:-jdbc:oracle:thin:@//host.docker.internal:1892/orclpdb}
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
      - CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS:-http://localhost,http://localhost:80}
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:7272/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 40s
    networks:
      - erp-network

  # ============================================
  # Frontend — Angular + Nginx
  # ============================================
  frontend:
    build:
      context: ..
      dockerfile: deploy/frontend/Dockerfile
    container_name: erp-frontend
    ports:
      - "${FRONTEND_PORT:-80}:80"
    depends_on:
      backend:
        condition: service_healthy
    restart: unless-stopped
    networks:
      - erp-network

networks:
  erp-network:
    driver: bridge
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
BACKEND_PORT=7272
DB_URL=jdbc:oracle:thin:@//host.docker.internal:1892/orclpdb
DB_USERNAME=erp_user
DB_PASSWORD=CHANGE_ME
JWT_SECRET=CHANGE_ME_TO_A_RANDOM_256_BIT_SECRET
CORS_ALLOWED_ORIGINS=http://localhost,http://localhost:80

# Frontend
FRONTEND_PORT=80
```

### 4.3 Docker Compose Enforcement

| ID | Rule | ❌ Forbidden | ✅ Required |
|----|------|-------------|------------|
| DC.1 | Build context is project root | `context: .` from `deploy/` dir | `context: ..` (project root) |
| DC.2 | No hardcoded secrets | `DB_PASSWORD: mypass` in yml | `DB_PASSWORD=${DB_PASSWORD}` from `.env` |
| DC.3 | Health check on backend | No health check | `wget` or `curl` to `/actuator/health` |
| DC.4 | Frontend depends on backend | No dependency or `depends_on: backend` (without condition) | `depends_on: backend: condition: service_healthy` |
| DC.5 | Named network | Default bridge | Named `erp-network` with bridge driver |
| DC.6 | Restart policy | `restart: always` or none | `restart: unless-stopped` |
| DC.7 | Port variables | Hardcoded ports | `${BACKEND_PORT:-7272}` and `${FRONTEND_PORT:-80}` |
| DC.8 | `.env.example` provided | No env template | `.env.example` with placeholder values |
| DC.9 | `.env` in `.gitignore` | `.env` committed to repo | `.env` added to `.gitignore` |

---

## 5. Frontend Environment Integration

When the nginx reverse proxy is configured (`location /api/`), the frontend `environment.prod.ts` should use a relative API URL:

### 5.1 environment.prod.ts — Proxy-Aware

**Before (hardcoded):**
```typescript
// ❌ Hardcoded server IP
export const environment = {
  production: true,
  apiUrl: 'http://89.117.37.75:7272',
  authApiUrl: 'http://89.117.37.75:7272'
};
```

**After (proxy-relative):**
```typescript
// ✅ Uses nginx reverse proxy
export const environment = {
  production: true,
  apiUrl: '/api',
  authApiUrl: '/api'
};
```

> **Note:** Only update `environment.prod.ts` if the user explicitly asks to switch to proxy mode. Otherwise, preserve the existing hardcoded URLs and document the proxy option.

---

## 6. File Update Rules

### 6.1 Files the Skill Creates (in `deploy/`)

| File | Purpose |
|------|---------|
| `deploy/backend/Dockerfile` | Multi-stage backend build |
| `deploy/frontend/Dockerfile` | Multi-stage frontend build |
| `deploy/frontend/nginx.conf` | Nginx config with SPA fallback + API proxy |
| `deploy/docker-compose.yml` | Full-stack orchestration |
| `deploy/.env.example` | Environment variable template |
| `deploy/README.md` | Deployment instructions |

### 6.2 Files the Skill MAY Update (with user confirmation)

| File | Update | Condition |
|------|--------|-----------|
| `frontend/src/environments/environment.prod.ts` | Switch to proxy-relative URLs | Only if user requests proxy mode |
| `.gitignore` | Add `deploy/.env` | If not already present |
| `backend/erp-main/src/main/resources/application-prod.properties` | Externalize remaining hardcoded values | Only if user requests |

### 6.3 Files the Skill Must NEVER Modify

- `backend/pom.xml` — read-only for analysis
- `frontend/angular.json` — read-only for analysis
- `frontend/package.json` — read-only for analysis
- Any source code files under `backend/*/src/` or `frontend/src/app/`

---

## 7. Deploy README

**Path:** `deploy/README.md`

**Must Include:**

1. **Prerequisites** — Docker, Docker Compose versions
2. **Quick Start** — Copy `.env.example` → `.env`, fill values, run `docker-compose up`
3. **Environment Variables** — Table of all variables with descriptions
4. **Architecture Diagram** — ASCII showing frontend → nginx → backend → DB
5. **Individual Container Builds** — How to build/run backend or frontend alone
6. **Troubleshooting** — Common issues (port conflicts, DB connection, CORS)

---

## 8. Safety Rules

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

---

## 9. Adaptation Rules

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

---

## 10. Output Format

When the skill is invoked, produce output in this order:

```
1. [ANALYSIS]    — Print detected project config (versions, modules, paths)
2. [GENERATE]    — Create all files in deploy/
3. [VERIFY]      — Run enforcement checks (all BD.*, FD.*, DC.*, SF.* rules)
4. [SUMMARY]     — List all created/updated files with brief descriptions
```

### Analysis Output Example:

```
📋 Project Analysis:
  Backend:  Spring Boot 4.0.1 / Java 21
  Modules:  erp-common-utils, erp-security, erp-masterdata, erp-finance-gl, erp-main
  Boot JAR: erp-main-1.0.0-SNAPSHOT.jar
  Prod Config: application-prod.properties
  DB Driver: Oracle (ojdbc11)
  Env Vars: DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, CORS_ALLOWED_ORIGINS

  Frontend: Angular (n-erp-system)
  Output:   dist/n-erp-system/browser
  Prod API: http://89.117.37.75:7272
```

---

## 11. Enforcement Summary

### Complete Check Matrix

| Category | IDs | Count |
|----------|-----|-------|
| Backend Dockerfile | BD.1 – BD.8 | 8 |
| Frontend Dockerfile | FD.1 – FD.10 | 10 |
| Docker Compose | DC.1 – DC.9 | 9 |
| Safety | SF.1 – SF.9 | 9 |
| **Total** | | **36** |

All 36 checks MUST pass before the skill marks deployment artifacts as ready.

---

## 12. Meta

| Property | Value |
|----------|-------|
| Skill ID | `deploy` |
| Category | `devops` |
| Phase | Post-development |
| Depends On | Completed backend + frontend implementation |
| Inputs | `backend/pom.xml`, `frontend/angular.json`, `application-prod.properties`, `environment.prod.ts` |
| Outputs | `deploy/` directory with 6 files |
| Enforcement Checks | 36 |

## 13. PRE-DEPLOY VALIDATION (CRITICAL)

Before generating ANY deployment artifacts, the system MUST validate:

1. No TODO or placeholder values remain
2. No hardcoded development URLs (localhost) in production configs
3. All environment variables are externalized
4. No debug or dev logging enabled
5. All required backend endpoints are reachable
6. Frontend build is successful in production mode

If any issue is detected:

→ STOP
→ report issues
→ DO NOT generate deployment files
