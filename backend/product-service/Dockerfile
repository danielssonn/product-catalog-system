# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy pre-built jar (build locally with: mvn clean package -DskipTests)
COPY product-service/target/*.jar app.jar

# Expose service port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]