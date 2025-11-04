# Stage 1: Build the Java application
# Use a specific, stable base image
FROM maven:3.8.5-openjdk-17 AS builder

# Set the working directory
WORKDIR /app

# Copy the Maven wrapper files and pom.xml first
# This allows Docker to cache the dependencies layer
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# --- THE MOST IMPORTANT FIX ---
# Make the Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies
# This layer is cached as long as pom.xml doesn't change
RUN ./mvnw dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Build the application and package it into a .jar file
RUN ./mvnw clean package -DskipTests

# ---

# Stage 2: Create the final, lightweight runtime image
# Use a smaller JRE (Java Runtime Environment) image
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy ONLY the built .jar file from the 'builder' stage
# This assumes your build produces one .jar file in the target directory
COPY --from=builder /app/target/*.jar app.jar

# Expose port 8080 (the default for Spring Boot)
# Render will automatically map this to 443 (HTTPS)
EXPOSE 8080

# The command to run your application
ENTRYPOINT ["java", "-jar", "app.jar"]
