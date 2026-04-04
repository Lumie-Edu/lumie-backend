package com.lumie.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.lumie")
@ConfigurationPropertiesScan(basePackages = "com.lumie")
public class LumieApplication {

    public static void main(String[] args) {
        SpringApplication.run(LumieApplication.class, args);
    }
}
