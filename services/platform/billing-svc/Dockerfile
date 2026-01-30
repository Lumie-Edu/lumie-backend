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
COPY services/platform/billing-svc services/platform/billing-svc

# Build
RUN chmod +x gradlew && \
    ./gradlew :services:platform:billing-svc:bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -g 1000 spring && \
    adduser -u 1000 -G spring -D spring

WORKDIR /app

COPY --from=builder /workspace/services/platform/billing-svc/build/libs/billing-svc.jar app.jar

RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8080 9090

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
