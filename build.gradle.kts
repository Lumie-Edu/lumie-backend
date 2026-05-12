import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.InventoryHtmlReportRenderer
import com.github.jk1.license.render.JsonReportRenderer

plugins {
    java
    id("org.springframework.boot") version "3.4.13" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.github.jk1.dependency-license-report") version "2.9"
    id("com.diffplug.spotless") version "6.25.0" apply false
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

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(property("javaVersion").toString().toInt()))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters"))
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
