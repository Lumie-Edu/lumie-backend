FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Copy libs (db-migrations module)
COPY libs/db-migrations libs/db-migrations

# Build the migration jar
RUN chmod +x gradlew && \
    ./gradlew :libs:db-migrations:bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar
COPY --from=builder /workspace/libs/db-migrations/build/libs/db-migrations.jar app.jar

# Run migrations and exit
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
