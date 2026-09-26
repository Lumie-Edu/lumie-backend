plugins {
    id("java-library")
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:internal-api"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Verification-only compatibility bridge for pre-session browser cookies.
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Test (module-specific only — common test deps in root build.gradle.kts)
    testImplementation("org.springframework.security:spring-security-test")
}
