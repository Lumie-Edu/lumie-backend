FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

COPY libs libs

COPY services/core/ai-svc services/core/ai-svc

RUN chmod +x gradlew && \
    ./gradlew :services:core:ai-svc:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -g 1000 spring && \
    adduser -u 1000 -G spring -D spring

WORKDIR /app

COPY --from=builder /workspace/services/core/ai-svc/build/libs/ai-svc.jar app.jar

RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
