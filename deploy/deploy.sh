#!/bin/bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY_DIR="$PROJECT_DIR/deploy"

echo "=========================================="
echo " ERP System - Deploying"
echo "=========================================="

echo "[1/3] Pulling latest code..."
cd "$PROJECT_DIR"
git pull origin main

echo "[2/3] Rebuilding containers..."
cd "$DEPLOY_DIR"
docker compose down
docker compose --env-file .env up -d --build

echo "[3/3] Verifying deployment..."

echo "Waiting for backend to be healthy..."

for i in {1..10}; do
  if wget --spider -q http://localhost:7272/actuator/health; then
    echo "Backend is healthy"
    break
  fi
  echo "Waiting... ($i)"
  sleep 5
done

echo "=========================================="
echo " Deployment complete"
echo "=========================================="