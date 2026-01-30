package com.lumie.dbmigrations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Centralized database migration application for LUMIE platform.
 * Runs Flyway migrations and exits.
 *
 * This is deployed as a Kubernetes Job that runs before services start.
 */
@SpringBootApplication
public class DbMigrationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbMigrationsApplication.class, args);
    }
}
