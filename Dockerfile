FROM gradle:8.12.1-jdk21 AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN ./gradlew --no-daemon clean build

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/identity-provider-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
