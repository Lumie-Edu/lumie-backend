plugins {
    id("java-library")
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

    // MinIO SDK
    implementation("io.minio:minio:8.5.14")

    // MapStruct
    implementation("org.mapstruct:mapstruct:${property("mapstructVersion")}")
    annotationProcessor("org.mapstruct:mapstruct-processor:${property("mapstructVersion")}")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Test (module-specific only — common test deps in root build.gradle.kts)
    testImplementation("org.testcontainers:rabbitmq")
}
