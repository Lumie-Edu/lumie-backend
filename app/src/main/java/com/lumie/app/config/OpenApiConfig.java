package com.lumie.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc base spec. Declares the bearer-JWT scheme so generated clients
 * know to attach {@code Authorization: Bearer <access-token>} on protected
 * endpoints. Endpoint-level overrides ({@code @SecurityRequirements} on
 * {@code /v1/login} etc.) opt out individual operations.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lumieOpenApi() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Lumie Backend API")
                        .version("v1")
                        .description("REST API for the Lumie K-12 academy SaaS monolith."))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
