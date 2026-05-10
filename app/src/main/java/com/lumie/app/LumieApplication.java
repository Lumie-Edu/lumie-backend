package com.lumie.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "com.lumie")
@ConfigurationPropertiesScan(basePackages = "com.lumie")
@EnableAsync
public class LumieApplication {

    public static void main(String[] args) {
        SpringApplication.run(LumieApplication.class, args);
    }
}
