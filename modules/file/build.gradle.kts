import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification

plugins {
    id("java-library")
}

val cucumberTagFilter = providers.gradleProperty("cucumberTags")
val lifecycleClassPatterns = listOf(
    "**/FileMetadata*.class",
    "**/FileStorageOperation*.class",
    "**/TemporaryObjectUploadService*.class",
    "**/FileCommandService*.class",
    "**/FileQueryService*.class",
    "**/FileMetadataLookupService*.class",
    "**/StorageOperationProcessor*.class",
    "**/TextbookFolderCommandService*.class",
    "**/FileServiceAdapter*.class",
    "**/MinioStorageAdapter*.class",
)

val lifecycleClassNames = listOf(
    "com.lumie.file.domain.entity.FileMetadata*",
    "com.lumie.file.domain.entity.FileStorageOperation*",
    "com.lumie.file.application.service.TemporaryObjectUploadService*",
    "com.lumie.file.application.service.FileCommandService*",
    "com.lumie.file.application.service.FileQueryService*",
    "com.lumie.file.application.service.FileMetadataLookupService*",
    "com.lumie.file.application.service.StorageOperationProcessor*",
    "com.lumie.file.application.service.TextbookFolderCommandService*",
    "com.lumie.file.adapter.in.internal.FileServiceAdapter*",
    "com.lumie.file.adapter.out.storage.MinioStorageAdapter*",
)

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:internal-api"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation(libs.shedlock.spring)

    // MinIO SDK
    implementation(libs.minio)

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Test (module-specific only — common test deps in root build.gradle.kts)
    testImplementation("org.testcontainers:minio")
    testImplementation(libs.cucumber.java)
    testImplementation(libs.cucumber.junit.platform)
    testImplementation(libs.cucumber.picocontainer)
    testImplementation("org.junit.platform:junit-platform-suite")
}

tasks.withType<Test>().configureEach {
    val tagExpression = cucumberTagFilter.orNull?.takeIf(String::isNotBlank)
    if (name == "test") {
        // Keep the scenario suite out of ordinary unit-test discovery.
        exclude("**/AttachmentLifecycleCucumberSuite.class")
        systemProperty("cucumber.filter.tags", tagExpression ?: "@__cucumber_disabled__")
    } else {
        tagExpression?.let { systemProperty("cucumber.filter.tags", it) }
    }
}

val lifecycleClasses = sourceSets.main.get().output.classesDirs.asFileTree.matching {
    include(lifecycleClassPatterns)
}

tasks.register<JacocoCoverageVerification>("jacocoFeatureCoverageVerification") {
    group = "verification"
    description = "Requires 80% line and branch coverage for attachment lifecycle classes."
    dependsOn(tasks.named("test"), tasks.named("integrationTest"))
    executionData.setFrom(fileTree(layout.buildDirectory.dir("jacoco")) {
        include("test.exec", "integrationTest.exec")
    })
    classDirectories.setFrom(lifecycleClasses)
    sourceDirectories.setFrom(sourceSets.main.get().allSource.srcDirs)
    violationRules {
        rule {
            element = "CLASS"
            includes = lifecycleClassNames
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
