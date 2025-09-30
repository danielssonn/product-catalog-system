FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY common/pom.xml ./common/
COPY audit-service/pom.xml ./audit-service/

RUN mvn dependency:go-offline -B

COPY common/src ./common/src
COPY audit-service/src ./audit-service/src

RUN mvn clean package -DskipTests -pl audit-service -am

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/audit-service/target/*.jar app.jar

EXPOSE 8084

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]