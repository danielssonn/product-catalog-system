FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy parent pom
COPY ../pom.xml ./pom.xml

# Copy common module
COPY ../common ./common

# Copy service
COPY . ./catalog-service

# Build
WORKDIR /app/catalog-service
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/catalog-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]