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

# Optional: Install tini for better signal handling
RUN apk add --no-cache tini

# Set working directory
WORKDIR /app

# Copy the built jar file from builder stage
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Set required environment variables for Spring to read (used in CompilerController)
ENV RAPIDAPI_KEY=e004417eb7msh105ea63cbc83000p15f4a5jsn620d1c0e206a
ENV RAPIDAPI_HOST=judge0-ce.p.rapidapi.com

# Expose the server port
EXPOSE 8080

# Start the application using tini for clean shutdown
ENTRYPOINT ["/sbin/tini", "--", "java", "-jar", "app.jar"]