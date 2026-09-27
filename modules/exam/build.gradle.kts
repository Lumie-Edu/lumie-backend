plugins {
    id("java-library")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:internal-api"))
    implementation(project(":libs:messaging"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Redis — callback deduplication via SETNX
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Test (module-specific only — common test deps in root build.gradle.kts)
    testImplementation("org.testcontainers:rabbitmq")
    testImplementation(testFixtures(project(":libs:common")))
    testImplementation(testFixtures(project(":modules:exam")))

    // Test fixtures — ExamFixtures uses Exam/ExamResult/QuestionResult from this module's main.
    // JPA API needed because Exam/ExamResult are @Entity classes (jakarta.persistence on classpath).
    // libs:common needed for TenantScopedEntity (superclass of Exam/ExamResult).
    testFixturesImplementation(project(":modules:exam"))
    testFixturesImplementation(project(":libs:common"))
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
}
