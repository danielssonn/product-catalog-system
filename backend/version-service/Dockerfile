FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY common/pom.xml ./common/
COPY version-service/pom.xml ./version-service/

RUN mvn dependency:go-offline -B

COPY common/src ./common/src
COPY version-service/src ./version-service/src

RUN mvn clean package -DskipTests -pl version-service -am

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/version-service/target/*.jar app.jar

EXPOSE 8087

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8087/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]