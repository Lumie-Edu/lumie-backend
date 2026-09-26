package com.lumie.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;

/**
 * springdoc base spec. Browser authentication uses a same-origin HttpOnly
 * session cookie, so generated clients do not attach bearer credentials.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lumieOpenApi() {
        // springdoc reflects java.time.LocalTime as an object ({hour, minute, ...}),
        // but the wire format is a "HH:mm" string (see @JsonFormat on schedule/reservation
        // DTOs). Map it to a string schema so generated clients type these fields as
        // strings instead of an opaque LocalTime object. Applied here (in the eagerly
        // built OpenAPI bean) so it runs deterministically before springdoc inspects
        // any schema.
        SpringDocUtils.getConfig()
                .replaceWithSchema(LocalTime.class, new StringSchema().example("09:00"));

        return new OpenAPI()
                .info(new Info()
                        .title("Lumie Backend API")
                        .version("v1")
                        .description("REST API for the Lumie K-12 academy SaaS monolith."))
                .components(new Components());
    }
}
