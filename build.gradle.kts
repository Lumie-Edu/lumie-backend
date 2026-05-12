import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.InventoryHtmlReportRenderer
import com.github.jk1.license.render.JsonReportRenderer
import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("org.springframework.boot") version "3.4.13" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.github.jk1.dependency-license-report") version "2.9"
    id("com.diffplug.spotless") version "6.25.0" apply false
    id("net.ltgt.errorprone") version "3.1.0" apply false
}

allprojects {
    group = "com.lumie"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "net.ltgt.errorprone")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(property("javaVersion").toString().toInt()))
        }
    }

    // Spring Boot BOM available to every subproject. Modules with extra BOMs
    // (Spring AI, Spring Modulith) layer them additively in their own
    // dependencyManagement {} blocks.
    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}")
        }
    }

    dependencies {
        "errorprone"("com.google.errorprone:error_prone_core:2.27.1")

        // Lombok: universal across every subproject (incl. libs/internal-api,
        // which gets compile-only annotations cost-free).
        "compileOnly"("org.projectlombok:lombok:${property("lombokVersion")}")
        "annotationProcessor"("org.projectlombok:lombok:${property("lombokVersion")}")
        "testCompileOnly"("org.projectlombok:lombok:${property("lombokVersion")}")
        "testAnnotationProcessor"("org.projectlombok:lombok:${property("lombokVersion")}")

        // Spring Boot test starter: universal (JUnit5 + AssertJ + Mockito).
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters"))
        options.errorprone {
            // Ramping mode: all checks report as WARN until the baseline 36
            // violations are cleaned. Flip to errors per-rule afterward.
            isEnabled.set(true)
            allErrorsAsWarnings.set(true)
            disable(
                "UnusedVariable",        // covered by IDE
                "MissingSummary",        // Javadoc style noise
                "EmptyCatch",            // intentional in many places
                "InlineMeSuggester",     // library-author concern
            )
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    // Spotless: hygiene-only (no full Google-Java-Format reflow yet).
    // Strips unused imports, normalizes whitespace + line endings, removes tabs.
    // Ramp to googleJavaFormat() when ready for the larger diff.
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            target("src/**/*.java")
            targetExclude("**/build/**", "**/generated/**")
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
            indentWithSpaces(4)
        }
        kotlinGradle {
            target("*.gradle.kts")
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    tasks.named("check") {
        dependsOn("spotlessCheck")
    }
}

// Testcontainers stack: only modules that hit Postgres in integration tests.
// libs/internal-api (pure interfaces) and libs/messaging (AMQP wiring) skip.
configure(subprojects.filter {
    it.path.startsWith(":modules:") || it.path == ":libs:common"
}) {
    dependencies {
        "testImplementation"("org.springframework.boot:spring-boot-testcontainers")
        "testImplementation"("org.testcontainers:junit-jupiter")
        "testImplementation"("org.testcontainers:postgresql")
    }
}

// License compliance — fail build on disallowed licenses (AGPL, GPL, etc.).
// Allowed list lives in config/allowed-licenses.json.
licenseReport {
    outputDir = "$projectDir/build/reports/dependency-license"
    renderers = arrayOf(
        InventoryHtmlReportRenderer("report.html"),
        JsonReportRenderer("licenses.json", false),
    )
    filters = arrayOf(LicenseBundleNormalizer())
    allowedLicensesFile = file("$projectDir/config/allowed-licenses.json")
    excludeBoms = true
    projects = subprojects.toTypedArray()
}

tasks.named("check") {
    dependsOn("checkLicense")
}
