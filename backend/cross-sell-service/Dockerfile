FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY common/pom.xml ./common/
COPY cross-sell-service/pom.xml ./cross-sell-service/

RUN mvn dependency:go-offline -B

COPY common/src ./common/src
COPY cross-sell-service/src ./cross-sell-service/src

RUN mvn clean package -DskipTests -pl cross-sell-service -am

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/cross-sell-service/target/*.jar app.jar

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]