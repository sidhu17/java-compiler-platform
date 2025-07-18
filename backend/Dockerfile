# Stage 1: Build with Maven
FROM maven:3.8.7-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache Maven dependencies
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy all source files and build the jar
COPY src src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Run with JRE
FROM eclipse-temurin:17-jre-alpine

# Optional: Install tini for graceful shutdown handling
RUN apk add --no-cache tini

# Set working directory
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=builder /app/target/java-compiler-backend.jar app.jar

# Set environment variables (used in application.properties)
ENV RAPIDAPI_KEY=e004417eb7msh105ea63cbc83000p15f4a5jsn620d1c0e206a
ENV RAPIDAPI_HOST=judge0-ce.p.rapidapi.com

# Expose backend port
EXPOSE 8080

# Start the application using tini
ENTRYPOINT ["/sbin/tini", "--", "java", "-jar", "app.jar"]