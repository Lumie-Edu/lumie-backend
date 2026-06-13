plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "lumie-backend"

include(
    "app",
    "libs:common",
    "libs:internal-api",
    "libs:messaging",
    "modules:tenant",
    "modules:auth",
    "modules:billing",
    "modules:homepage",
    "modules:student",
    "modules:attendance",
    "modules:staff",
    "modules:exam",
    "modules:content",
    "modules:class",
    "modules:assignment",
    "modules:lecture",
    "modules:ai",
    "modules:notification",
    "modules:file",
    "modules:tuition"
)
