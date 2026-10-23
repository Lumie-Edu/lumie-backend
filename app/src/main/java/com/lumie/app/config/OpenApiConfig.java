package com.lumie.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.springdoc.core.utils.SpringDocUtils;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
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
                        .description("REST API for the Lumie K-12 academy SaaS monolith. "
                                + "Protected browser routes use an HttpOnly session cookie. "
                                + "Validation, authentication, authorization, business, and unexpected errors "
                                + "use application/problem+json with top-level code and timestamp. "
                                + "The development cookie name is lumie_session; production uses __Host-lumie_session. "
                                + "Some operations apply additional role and ownership checks."))
                .components(new Components());
    }

    @Bean
    public GlobalOpenApiCustomizer sharedContractCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components();
                openApi.setComponents(components);
            }

            components.addSecuritySchemes("sessionCookie", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.COOKIE)
                    .name("__Host-lumie_session")
                    .description("Opaque browser session cookie; development uses lumie_session."));
            openApi.addSecurityItem(new SecurityRequirement().addList("sessionCookie"));
            if (openApi.getPaths() != null) {
                openApi.getPaths().forEach((path, pathItem) -> {
                    pathItem.readOperations().forEach(operation -> operation.getResponses().values()
                            .forEach(OpenApiConfig::normalizeJsonResponse));
                    if (isPublic(path)) {
                        pathItem.readOperations().forEach(operation -> operation.setSecurity(List.of()));
                    } else if (path.equals("/v1/tenants/{slug}/logo")) {
                        pathItem.getGet().setSecurity(List.of());
                    }
                    if (path.equals("/v1/sms") || path.startsWith("/v1/sms/")
                            || path.equals("/v1/sms-templates") || path.startsWith("/v1/sms-templates/")) {
                        pathItem.readOperations().forEach(operation -> {
                            String description = operation.getDescription();
                            operation.setDescription((description == null ? "" : description + " ")
                                    + "Requires OWNER, MANAGER, or INSTRUCTOR at the security filter; "
                                    + "individual operations may require a stricter role.");
                        });
                    }
                });
            }

            ObjectSchema problem = new ObjectSchema();
            problem.setDescription("RFC 9457 error. Extra fields include tenantSlug when available and "
                    + "violations for request validation errors.");
            problem.addProperty("type", new StringSchema().format("uri"));
            problem.addProperty("title", new StringSchema());
            problem.addProperty("status", new IntegerSchema().format("int32"));
            problem.addProperty("detail", new StringSchema());
            problem.addProperty("instance", new StringSchema().format("uri"));
            problem.addProperty("code", new StringSchema());
            problem.addProperty("timestamp", new StringSchema().format("date-time"));
            problem.setRequired(List.of("type", "title", "status", "detail", "instance", "code", "timestamp"));
            problem.setAdditionalProperties(Boolean.TRUE);
            components.addSchemas("ProblemDetail", problem);

            addProblemResponse(components, "BadRequest", "Invalid input or validation failure", "400");
            addProblemResponse(components, "Unauthorized", "Missing or expired session", "401");
            addProblemResponse(components, "Forbidden", "Insufficient role or access", "403");
            addProblemResponse(components, "NotFound", "Resource not found", "404");
            addProblemResponse(components, "Conflict", "Resource or optimistic lock conflict", "409");
            addProblemResponse(components, "Unprocessable", "Business rule violation", "422");
            addProblemResponse(components, "InternalError", "Unexpected server failure", "500");
        };
    }

    private static boolean isPublic(String path) {
        return path.equals("/v1/login")
                || path.equals("/v1/register/owner")
                || path.equals("/v1/payments/paddle/webhook")
                || path.equals("/v1/sms/solapi/webhook")
                || path.startsWith("/v1/tenants/public/");
    }

    private static void normalizeJsonResponse(ApiResponse response) {
        Content content = response.getContent();
        if (content == null || !content.containsKey("*/*")) {
            return;
        }
        MediaType media = content.get("*/*");
        Schema<?> schema = media.getSchema();
        if (schema == null || ("string".equals(schema.getType()) && "binary".equals(schema.getFormat()))) {
            return; // file and logo downloads select a media type at runtime
        }
        content.remove("*/*");
        content.addMediaType("application/json", media);
    }

    private static void addProblemResponse(Components components, String name, String description, String status) {
        components.addResponses(name, new ApiResponse()
                .description(status + " " + description)
                .content(new Content().addMediaType("application/problem+json", new MediaType()
                        .schema(new ObjectSchema().$ref("#/components/schemas/ProblemDetail")))));
    }
}
