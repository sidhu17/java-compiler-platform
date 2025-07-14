# Stage 1: Build with Maven
FROM maven:3.8.7-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache dependencies
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy project files and build
COPY src src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Run with JRE
FROM eclipse-temurin:17-jre-alpine

# Install tini for graceful shutdown (optional but good for containers)
RUN apk add --no-cache tini

WORKDIR /app

# Copy built jar from builder
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Set environment variables (used in your code)
ENV RAPIDAPI_KEY=your-rapidapi-key
ENV RAPIDAPI_HOST=judge0-ce.p.rapidapi.com

# Expose port
EXPOSE 8080

# Use tini as entrypoint for signal forwarding
ENTRYPOINT ["/sbin/tini", "--", "java", "-jar", "app.jar"]