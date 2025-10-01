FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy parent pom and common module
COPY pom.xml .
COPY common/pom.xml ./common/
COPY api-gateway/pom.xml ./api-gateway/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY common/src ./common/src
COPY api-gateway/src ./api-gateway/src

# Build the application
RUN mvn clean package -DskipTests -pl api-gateway -am

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/api-gateway/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]