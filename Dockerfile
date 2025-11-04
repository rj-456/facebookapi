#Build stage

#Using a Maven image with Temurin 21 for the build

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .

#Note: Ensure your project's pom.xml is configured to target Java 21

RUN mvn clean package -DskipTests

#Package stage

#Using the smaller, stable Temurin 21 JDK Alpine image for the final runtime

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

#Copy the built artifact from the build stage

COPY --from=build /app/target/facebookapi-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
