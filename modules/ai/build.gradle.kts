plugins {
    id("java-library")
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.ai.bom.get().toString())
    }
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

    // HTTP Client
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Spring AI (OpenAI-compat starter — targeting Gemini's OpenAI endpoint)
    implementation("org.springframework.ai:spring-ai-starter-model-openai")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")
}
