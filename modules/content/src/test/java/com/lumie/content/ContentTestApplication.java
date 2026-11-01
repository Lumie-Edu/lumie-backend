package com.lumie.content;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Minimal configuration anchor for content module JPA integration tests. */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan("com.lumie.content.domain.entity")
@EnableJpaRepositories("com.lumie.content.adapter.out.persistence")
@EnableJpaAuditing
public class ContentTestApplication {
}
