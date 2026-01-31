# Build stage (using Debian-based image for gRPC plugin compatibility)
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

# Copy gradle files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Copy library modules
COPY libs libs

# Copy service
COPY services/support/file-svc services/support/file-svc

# Build and cleanup temp files for Kaniko compatibility
RUN chmod +x gradlew && \
    ./gradlew :services:support:file-svc:bootJar -x test --no-daemon && \
    rm -rf /tmp/hsperfdata_* /root/.gradle

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -g 1000 spring && \
    adduser -u 1000 -G spring -D spring

WORKDIR /app

COPY --from=builder /workspace/services/support/file-svc/build/libs/file-svc.jar app.jar

RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
