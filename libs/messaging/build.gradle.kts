plugins {
    id("java-library")
}

dependencies {
    api(project(":libs:common"))

    // Spring AMQP — implementation (not api) so consumers don't get AMQP on their compile classpath.
    // Only exam declares amqp directly; constants class needs no runtime AMQP.
    implementation("org.springframework.boot:spring-boot-starter-amqp")
}
