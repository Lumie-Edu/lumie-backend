plugins {
    id("java-library")
    id("java-test-fixtures")
}

// libs/internal-api is a pure-interfaces library with no deps — safe to pull
// into libs/common to wire the LogActivityAspect without a circular graph.
// Direction: libs/internal-api ← libs/common ← modules/*
//            libs/internal-api ← modules/* (direct, for adapter implementations)

dependencyManagement {
    imports {
        mavenBom(libs.spring.modulith.bom.get().toString())
    }
}

dependencies {
    // Internal API — pure interfaces; no transitive deps. Used by LogActivityAspect.
    implementation(project(":libs:internal-api"))

    // Spring
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    // Spring Modulith — exposes @ApplicationModuleListener to module listeners.
    // Version pinned (rather than from BOM) so transitive resolution in consumer
    // modules' compileClasspath finds it without each module re-importing the BOM.
    api(libs.spring.modulith.events.api)

    // ShedLock — exposes @SchedulerLock to module schedulers (multi-pod safety).
    // The LockProvider bean lives in app/ alongside the shared DataSource.
    api(libs.shedlock.spring)

    // Spring Cache + Caffeine — shared in-process cache for hot read-only
    // lookups (tenant, permission, plan). See CacheConfig.
    api("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Jackson
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Logging — implementation: modules log via SLF4J API, not logstash encoder directly
    implementation(libs.logstash.logback.encoder)

    // Test (module-specific only — common test deps in root build.gradle.kts)
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc")
    testRuntimeOnly("org.postgresql:postgresql")

    // Test fixtures — shared utilities consumed via testFixtures(project(":libs:common"))
    // ReflectionTestUtils: no extra deps (java.lang.reflect only)
    // TenantContextExtension: JUnit 5 API (from spring-boot-starter-test on the test classpath)
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api")
    testFixturesImplementation(project(":libs:common"))
}
