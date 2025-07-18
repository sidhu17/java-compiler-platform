# -------- Stage 1: Build with Maven --------
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /app

# Copy POM and preload dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy full source code
COPY src ./src

# Build the project (skip tests for speed)
RUN mvn clean package -DskipTests -B


# -------- Stage 2: Run with JRE --------
FROM eclipse-temurin:21-jre

# Set working directory
WORKDIR /app

# Copy the compiled JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the app port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]