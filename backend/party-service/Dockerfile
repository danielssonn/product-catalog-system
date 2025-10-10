FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy pre-built JAR
COPY party-service/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
