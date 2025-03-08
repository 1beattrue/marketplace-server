FROM gradle:7.5-jdk17 AS build
WORKDIR /app
COPY . /app
RUN ./gradlew build
FROM openjdk:17
COPY --from=build /app/build/libs/marketplace-server-*.jar /app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
