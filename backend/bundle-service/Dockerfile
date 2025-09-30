FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY common/pom.xml ./common/
COPY bundle-service/pom.xml ./bundle-service/

RUN mvn dependency:go-offline -B

COPY common/src ./common/src
COPY bundle-service/src ./bundle-service/src

RUN mvn clean package -DskipTests -pl bundle-service -am

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/bundle-service/target/*.jar app.jar

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]