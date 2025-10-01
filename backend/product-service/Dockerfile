FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy parent pom and common module first for better caching
COPY pom.xml .
COPY common/pom.xml ./common/
COPY product-service/pom.xml ./product-service/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY common/src ./common/src
COPY product-service/src ./product-service/src

# Build the application
RUN mvn clean package -DskipTests -pl product-service -am

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/product-service/target/*.jar app.jar

# Expose service port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]