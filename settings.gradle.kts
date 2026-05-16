plugins {
    alias(libs.plugins.foojay.resolver)
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
    "modules:admin",
    "modules:activity-log",
    "modules:exam",
    "modules:content",
    "modules:class",
    "modules:assignment",
    "modules:lecture",
    "modules:ai",
    "modules:notification",
    "modules:file"
)
