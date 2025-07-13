# Stage 1: Build with Maven
FROM maven:3.8.7-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy Maven wrapper and POM for dependency caching
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and package
COPY src src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Run with JRE
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar from builder
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]