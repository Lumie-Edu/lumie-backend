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

    // HTTP Client — used by ChatbotClient (BE→chatbot-svc SSE proxy)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")
}
