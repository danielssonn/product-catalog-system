FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy pre-built JAR
COPY capital-markets-party-service/target/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
