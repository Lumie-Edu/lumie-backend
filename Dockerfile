# syntax=docker/dockerfile:1.7
# Build stage
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

# Copy gradle files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .
COPY config config

# Copy all modules
COPY libs libs
COPY modules modules
COPY app app

# Build with BuildKit cache mounts — Gradle deps + wrapper + per-module
# build outputs persist across image rebuilds. Cuts cold-rebuild time
# from ~6 min to ~30s when only a single source file changes.
RUN --mount=type=cache,target=/root/.gradle,id=gradle-home \
    --mount=type=cache,target=/workspace/.gradle,id=gradle-project \
    chmod +x gradlew && \
    ./gradlew :app:bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -g 1000 spring && \
    adduser -u 1000 -G spring -D spring

WORKDIR /app

COPY --from=builder /workspace/app/build/libs/lumie-backend.jar app.jar

RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

ENV TZ=UTC
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom -Duser.timezone=UTC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
