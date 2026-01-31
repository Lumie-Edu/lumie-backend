plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "lumie-backend"

include(
    "libs:common",
    "libs:grpc-api",
    "libs:messaging",
    "libs:db-migrations",
    "services:platform:tenant-svc",
    "services:platform:auth-svc",
    "services:platform:billing-svc",
    "services:core:academy-svc",
    "services:core:exam-svc"
)
