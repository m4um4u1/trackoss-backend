# Multi-stage build for TrackOSS Backend
# Stage 1: Build the application
FROM docker.io/openjdk:21-jdk-slim AS builder

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Make gradlew executable
RUN chmod +x gradlew

# Copy source code
COPY src/ src/

# Build the application (skip tests for faster builds)
RUN ./gradlew build -x test --no-daemon

# Stage 2: Create the runtime image
FROM docker.io/eclipse-temurin:21-jre AS runtime

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Change ownership to app user
RUN chown appuser:appuser app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/routes || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
