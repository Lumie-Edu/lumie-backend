import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.InventoryHtmlReportRenderer
import com.github.jk1.license.render.JsonReportRenderer
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    java
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.mgmt) apply false
    alias(libs.plugins.dependency.license.report)
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.errorprone) apply false
}

// Version-catalog accessors are only available in the root script's own
// scope (and the plugins {} block) — NOT inside allprojects/subprojects
// closures, where `libs` resolves to a per-subproject extension lookup that
// doesn't exist. Capture the values here, reference them below.
val javaLanguageVersion = libs.versions.java.get()
val springBootBomVersion = libs.versions.spring.boot.get()
val flywayVersion = libs.versions.flyway.get()
val errorproneCoreDep = libs.errorprone.core
val lombokDep = libs.lombok
val springdocDep = libs.springdoc.openapi.webmvc

allprojects {
    group = "com.lumie"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "net.ltgt.errorprone")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaLanguageVersion.toInt()))
        }
    }

    // Spring Boot BOM available to every subproject. Modules with extra BOMs
    // (Spring AI, Spring Modulith) layer them additively in their own
    // dependencyManagement {} blocks.
    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootBomVersion")
        }
        dependencies {
            dependency("org.flywaydb:flyway-core:$flywayVersion")
            dependency("org.flywaydb:flyway-database-postgresql:$flywayVersion")
        }
    }

    dependencies {
        "errorprone"(errorproneCoreDep)

        // Lombok: universal across every subproject (incl. libs/internal-api,
        // which gets compile-only annotations cost-free).
        "compileOnly"(lombokDep)
        "annotationProcessor"(lombokDep)
        "testCompileOnly"(lombokDep)
        "testAnnotationProcessor"(lombokDep)

        // springdoc (swagger annotations) compile-only & universal: response DTOs
        // across modules carry @Schema(nullable = …) so the generated FE client
        // types nullable fields correctly. Runtime springdoc lives on :app only.
        "compileOnly"(springdocDep)

        // Spring Boot test starter: universal (JUnit5 + AssertJ + Mockito).
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
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

    // Integration tests (TestContainers + Spring `@SpringBootTest`) are
    // tagged `@Tag("integration")`. Default `test` skips them so dev builds
    // stay fast; CI or explicit `./gradlew integrationTest` runs the full
    // suite. Opt into a single combined run via `-Pintegration`.
    val runIntegration = project.hasProperty("integration")

    tasks.withType<Test>().configureEach {
        jvmArgs("-Xshare:off")
        useJUnitPlatform {
            if (!runIntegration && name == "test") {
                excludeTags("integration")
            }
        }
    }

    tasks.register<Test>("integrationTest") {
        description = "Runs @Tag(\"integration\") tests (TestContainers + Spring boot slice)."
        group = "verification"
        useJUnitPlatform { includeTags("integration") }
        shouldRunAfter("test")
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

    // JaCoCo: coverage report generated after every test run. The 30%
    // verification rule is defined but intentionally NOT wired into `check`
    // yet — same ramp philosophy as errorprone (allErrorsAsWarnings) and
    // spotless (hygiene-only): introduce the tooling without breaking builds
    // while module coverage climbs from ~0%. Run explicitly via
    // `./gradlew jacocoTestCoverageVerification`; promote it onto `check`
    // alongside the CI informational-gate flip once baseline modules clear 30%.
    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named<Test>("test"))
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.named<Test>("test") {
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        violationRules {
            rule {
                limit {
                    minimum = "0.30".toBigDecimal()
                }
            }
        }
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

tasks.register<Exec>("lumieLint") {
    description = "Runs Lumie backend repository lint sweeps."
    group = "verification"
    commandLine("bash", "scripts/lumie-lint/sweep.sh", "backend", "--paths", "*")
}

tasks.named("check") {
    dependsOn("lumieLint")
}

// Tilt syncs source into the running development Pod. Rebuild every project
// JAR after an incremental compile because the direct development classpath
// contains the module artifacts rather than Gradle's bootRun class directories.
tasks.register("devClasses") {
    dependsOn(subprojects.map { it.tasks.named("classes") })
    dependsOn(subprojects.map { it.tasks.named("jar") })
}

// jk1.license plugin's tasks aren't config-cache compatible (they retain
// Project references). Mark them explicitly so the rest of the build can
// still benefit from the config cache. Until upstream adds support, these
// three tasks always re-execute from scratch.
listOf("checkLicensePreparation", "checkLicense", "generateLicenseReport").forEach { taskName ->
    tasks.named(taskName) {
        notCompatibleWithConfigurationCache(
            "jk1.license plugin retains Project references (upstream issue)."
        )
    }
}
