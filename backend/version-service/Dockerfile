FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy pre-built JAR from local build
COPY version-service/target/*.jar app.jar

EXPOSE 8090

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8090/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]