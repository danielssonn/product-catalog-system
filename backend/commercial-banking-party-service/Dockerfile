FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy pre-built JAR
COPY commercial-banking-party-service/target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
