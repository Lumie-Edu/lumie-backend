plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.0")
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
