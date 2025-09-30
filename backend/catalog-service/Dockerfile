FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy parent pom and common module first for better caching
COPY pom.xml .
COPY common/pom.xml ./common/
COPY catalog-service/pom.xml ./catalog-service/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY common/src ./common/src
COPY catalog-service/src ./catalog-service/src

# Build the application
RUN mvn clean package -DskipTests -pl catalog-service -am

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/catalog-service/target/*.jar app.jar

# Expose service port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]