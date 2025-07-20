# -------- Stage 1: Build with Maven --------
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy Maven wrapper and project files
COPY pom.xml . 
COPY .mvn/ .mvn/
COPY mvnw .

# Download dependencies separately for better Docker caching
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code after caching dependencies
COPY src ./src

# Build the project (skip tests)
RUN ./mvnw clean package -DskipTests -B


# -------- Stage 2: Run with JRE --------
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built jar
COPY --from=builder /app/target/*.jar app.jar

# Dynamic port from Railway
ENV PORT=8080

# Expose dynamic port (so Dockerfile is consistent with app)
EXPOSE ${PORT}

# Run the app
CMD ["sh", "-c", "java -jar app.jar"]