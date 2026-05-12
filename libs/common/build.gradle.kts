plugins {
    id("java-library")
}

dependencyManagement {
    imports {
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

    // Spring Cache + Caffeine — shared in-process cache for hot read-only
    // lookups (tenant, permission, plan). See CacheConfig.
    api("org.springframework.boot:spring-boot-starter-cache")
    api("com.github.ben-manes.caffeine:caffeine")
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Jackson
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Logging
    api("net.logstash.logback:logstash-logback-encoder:8.0")

    // Test (module-specific only — common test deps in root build.gradle.kts)
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc")
    testRuntimeOnly("org.postgresql:postgresql")
}
