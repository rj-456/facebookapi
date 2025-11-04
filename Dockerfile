# Stage 1: Build the application
FROM openjdk:17-slim-bullseye as builder

WORKDIR /build

# Copy the Maven wrapper
COPY .mvn/ .mvn
COPY mvnw .

# Copy the pom.xml and download dependencies
COPY pom.xml .
RUN ./mvnw dependency:go-offline -B

# Copy the source code and build the JAR
COPY src/ src/
RUN ./mvnw package -DskipTests

# Stage 2: Create the final image
FROM openjdk:17-slim-bullseye

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Expose the port your app runs on
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]