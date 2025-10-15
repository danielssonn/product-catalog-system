# Multi-stage build for Auth Service
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy entire backend directory to ensure all module references are available
COPY . .

# Build common module and auth-service
RUN mvn clean install -pl common,auth-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/auth-service/target/auth-service-*.jar app.jar

# Expose port
EXPOSE 8097

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8097/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
