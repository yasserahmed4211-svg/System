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
COPY backend/erp-finance-gl/pom.xml ./erp-finance-gl/pom.xml
COPY backend/erp-masterdata/pom.xml ./erp-masterdata/pom.xml
COPY backend/erp-main/pom.xml ./erp-main/pom.xml

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY backend/erp-common-utils/src ./erp-common-utils/src
COPY backend/erp-security/src ./erp-security/src
COPY backend/erp-finance-gl/src ./erp-finance-gl/src
COPY backend/erp-masterdata/src ./erp-masterdata/src
COPY backend/erp-main/src ./erp-main/src

# Build (skip tests — run tests in CI, not in Docker build)
RUN mvn clean package -DskipTests -B

# ============================================
# Stage 2: Runtime
# ============================================
# ============================================
# Stage 2: Runtime
# ============================================
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Security: run as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# ✅ إنشاء مجلد logs مع صلاحيات
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

# Copy JAR from build stage
COPY --from=build /app/erp-main/target/erp-main-*.jar app.jar

# Set ownership
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 7272

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
