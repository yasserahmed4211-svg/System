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
COPY --from=build /app/dist/n-erp-system /usr/share/nginx/html

# Security: run as non-root (nginx user already exists in alpine)
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
