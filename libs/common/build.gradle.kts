plugins {
    id("java-library")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}")
        mavenBom("org.springframework.modulith:spring-modulith-bom:1.4.4")
    }
}

dependencies {
    // Spring
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    // Spring Modulith — exposes @ApplicationModuleListener to module listeners.
    // Version pinned (rather than from BOM) so transitive resolution in consumer
    // modules' compileClasspath finds it without each module re-importing the BOM.
    api("org.springframework.modulith:spring-modulith-events-api:1.4.4")

    // ShedLock — exposes @SchedulerLock to module schedulers (multi-pod safety).
    // The LockProvider bean lives in app/ alongside the shared DataSource.
    api("net.javacrumbs.shedlock:shedlock-spring:5.16.0")
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Lombok
    compileOnly("org.projectlombok:lombok:${property("lombokVersion")}")
    annotationProcessor("org.projectlombok:lombok:${property("lombokVersion")}")

    // Jackson
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Logging
    api("net.logstash.logback:logstash-logback-encoder:8.0")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.postgresql:postgresql")
    testCompileOnly("org.projectlombok:lombok:${property("lombokVersion")}")
    testAnnotationProcessor("org.projectlombok:lombok:${property("lombokVersion")}")
}
