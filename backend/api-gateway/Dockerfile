# Multi-stage build for API Gateway
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy parent POM and common module first (for caching)
COPY pom.xml .
COPY common ./common

# Install parent POM first, then build common module
RUN mvn -N install && \
    mvn -f common/pom.xml clean install -DskipTests

# Copy API Gateway source
COPY api-gateway/pom.xml ./api-gateway/
COPY api-gateway/src ./api-gateway/src

# Build the application (skip tests for faster builds)
RUN mvn -f api-gateway/pom.xml clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/api-gateway/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", \
    "-Xms512m", \
    "-Xmx1024m", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-Dspring.profiles.active=docker", \
    "-jar", "app.jar"]
