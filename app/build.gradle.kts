plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.ai.bom.get().toString())
        mavenBom(libs.spring.modulith.bom.get().toString())
    }
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:internal-api"))
    implementation(project(":libs:messaging"))

    implementation(project(":modules:tenant"))
    implementation(project(":modules:auth"))
    implementation(project(":modules:billing"))
    implementation(project(":modules:homepage"))
    implementation(project(":modules:student"))
    implementation(project(":modules:staff"))
    implementation(project(":modules:activity-log"))
    implementation(project(":modules:exam"))
    implementation(project(":modules:content"))
    implementation(project(":modules:class"))
    implementation(project(":modules:assignment"))
    implementation(project(":modules:lecture"))
    implementation(project(":modules:attendance"))
    implementation(project(":modules:ai"))
    implementation(project(":modules:notification"))
    implementation(project(":modules:file"))

    // Spring Boot (needed for app module's own classes)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Spring Modulith — transactional outbox for cross-module domain events.
    // Replaces hand-rolled @TransactionalEventListener(AFTER_COMMIT) so events
    // survive pod restarts and listener exceptions.
    // JDBC (not JPA) store: bypasses Hibernate schema-multitenancy and pins
    // event_publication to a fixed schema (see spring.modulith.events.jdbc.schema).
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.modulith:spring-modulith-starter-jdbc")

    // ShedLock — distributed lock for @Scheduled methods. JDBC provider pins the
    // shedlock table to the public schema (cross-tenant). DDL in Flyway V14.
    implementation(libs.shedlock.spring)
    implementation(libs.shedlock.provider.jdbc)

    // springdoc-openapi — exposes /v3/api-docs (OpenAPI 3.1 JSON) and
    // /swagger-ui. The JSON spec is the contract source of truth that
    // lumie-frontend consumes via openapi-typescript codegen.
    implementation(libs.springdoc.openapi.webmvc)

    // Bucket4j — token-bucket rate limiter (in-memory per pod).
    // Future enhancement: redis-backed distributed bucket for accurate
    // multi-pod limits; for now per-pod limits are deliberate (a single
    // attacker IP hitting one pod is bounded).
    implementation(libs.bucket4j.core)

    // Runtime
    runtimeOnly("org.postgresql:postgresql")

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    // Flyway Java API for the migration-chain integration test. Present at
    // runtime transitively (Spring Boot Flyway autoconfig); declared here so
    // it is on the test *compile* classpath too. Versions via Spring Boot BOM.
    testImplementation("org.flywaydb:flyway-core")
    testImplementation("org.flywaydb:flyway-database-postgresql")
}

tasks.bootJar {
    archiveFileName.set("lumie-backend.jar")
}
