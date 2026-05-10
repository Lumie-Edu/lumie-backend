plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.0")
        mavenBom("org.springframework.modulith:spring-modulith-bom:1.4.4")
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
    implementation(project(":modules:admin"))
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

    // Runtime
    runtimeOnly("org.postgresql:postgresql")

    // Lombok
    compileOnly("org.projectlombok:lombok:${property("lombokVersion")}")
    annotationProcessor("org.projectlombok:lombok:${property("lombokVersion")}")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

tasks.bootJar {
    archiveFileName.set("lumie-backend.jar")
}
