plugins {
    id("java-library")
}

dependencies {
    api(project(":libs:common"))

    // Spring AMQP
    api("org.springframework.boot:spring-boot-starter-amqp")
}
