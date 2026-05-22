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
    implementation("org.springframework.boot:spring-boot-starter-security")

    // OpenAPI @Schema annotations on response DTOs — compile-time only.
    // springdoc is provided at runtime via the :app module classpath, so the
    // annotations (CLASS retention) are scanned there; the module only needs
    // them to compile in isolation (per-module CI builds).
    compileOnly(libs.springdoc.openapi.webmvc)

    // Apache POI for Excel bulk import
    implementation(libs.apache.poi.ooxml)

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")
}
