# Multi-stage build for Spring Boot application with Java 21
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and project configuration
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# Ensure gradlew has executable permissions and unix line endings
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Pre-fetch dependencies for better Docker layer caching
RUN ./gradlew dependencies --no-daemon || true

# Copy application source code
COPY src ./src

# Build production executable JAR (skipping tests since database is not available during image build)
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Run as non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy executable JAR built from the build stage
COPY --from=builder /app/build/libs/salary-management-0.0.1-SNAPSHOT.jar app.jar

# Expose default HTTP port
EXPOSE 8080

# Default PORT environment variable (overridden dynamically by Render)
ENV PORT=8080

# Start the application using Render's PORT environment variable, defaulting to 8080
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Dserver.port=${PORT:-8080} -jar app.jar"]
