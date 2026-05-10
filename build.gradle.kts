import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.InventoryHtmlReportRenderer
import com.github.jk1.license.render.JsonReportRenderer

plugins {
    java
    id("org.springframework.boot") version "3.4.13" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.github.jk1.dependency-license-report") version "2.9"
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
