FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY common/pom.xml ./common/
COPY event-publisher-service/pom.xml ./event-publisher-service/

RUN mvn dependency:go-offline -B

COPY common/src ./common/src
COPY event-publisher-service/src ./event-publisher-service/src

RUN mvn clean package -DskipTests -pl event-publisher-service -am

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/event-publisher-service/target/*.jar app.jar

EXPOSE 8085

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]