package com.lumie.app.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Marks every property of a <em>response</em> DTO schema as {@code required} so the
 * generated TypeScript client (orval) types each field as present rather than
 * optional ({@code field: T} instead of {@code field?: T}).
 *
 * <p>Java records always populate every component, so a response body always carries
 * every key; "required" here means present-key, not non-null. Value-nullability stays
 * driven by {@code @Schema(nullable = true)} on the record component (orval then emits
 * {@code field: T | null}). springdoc otherwise only marks a property required when it
 * carries a Bean Validation annotation, which response records do not have — leaving
 * every response field optional and breaking direct field access on the FE.
 *
 * <p>Scope is computed from the spec, not from a name convention: a schema is treated
 * as a response DTO when it is reachable from an operation's {@code responses[*]} content
 * and NOT reachable from any {@code requestBody}. Request DTOs are left untouched (their
 * optional fields stay optional), and schemas used in both contexts are skipped to avoid
 * marking a request field required.
 */
@Configuration
public class OpenApiResponseRequiredCustomizer {

    @Bean
    public GlobalOpenApiCustomizer responseRequiredPropertiesCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            final Map<String, Schema> schemas = openApi.getComponents().getSchemas();

            final Set<String> responseSeed = new HashSet<>();
            final Set<String> requestSeed = new HashSet<>();
            if (openApi.getPaths() != null) {
                for (PathItem path : openApi.getPaths().values()) {
                    for (Operation op : path.readOperations()) {
                        if (op.getResponses() != null) {
                            for (ApiResponse response : op.getResponses().values()) {
                                collectContentRefs(response.getContent(), responseSeed);
                            }
                        }
                        final RequestBody body = op.getRequestBody();
                        if (body != null) {
                            collectContentRefs(body.getContent(), requestSeed);
                        }
                    }
                }
            }

            final Set<String> responseClosure = closure(responseSeed, schemas);
            final Set<String> requestClosure = closure(requestSeed, schemas);

            for (String name : responseClosure) {
                if (requestClosure.contains(name)) {
                    continue; // shared request/response schema — leave required as-is
                }
                final Schema<?> schema = schemas.get(name);
                if (schema == null || schema.getProperties() == null || schema.getProperties().isEmpty()) {
                    continue;
                }
                schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
            }
        };
    }

    private static void collectContentRefs(Content content, Set<String> out) {
        if (content == null) {
            return;
        }
        for (MediaType mediaType : content.values()) {
            addRefs(mediaType.getSchema(), out);
        }
    }

    /** Transitive closure over component schemas, following every $ref reachable from the seed. */
    private static Set<String> closure(Set<String> seed, Map<String, Schema> schemas) {
        final Set<String> seen = new HashSet<>();
        final Deque<String> stack = new ArrayDeque<>(seed);
        while (!stack.isEmpty()) {
            final String name = stack.pop();
            if (!seen.add(name)) {
                continue;
            }
            final Schema<?> schema = schemas.get(name);
            if (schema == null) {
                continue;
            }
            final Set<String> next = new HashSet<>();
            addRefs(schema, next);
            stack.addAll(next);
        }
        return seen;
    }

    /** Names of component schemas directly referenced by the given schema node (recursing into composed/array/map shapes). */
    private static void addRefs(Schema<?> schema, Set<String> out) {
        if (schema == null) {
            return;
        }
        if (schema.get$ref() != null) {
            out.add(refName(schema.get$ref()));
        }
        addRefs(schema.getItems(), out);
        addRefsAll(schema.getAllOf(), out);
        addRefsAll(schema.getAnyOf(), out);
        addRefsAll(schema.getOneOf(), out);
        if (schema.getProperties() != null) {
            for (Object value : schema.getProperties().values()) {
                addRefs((Schema<?>) value, out);
            }
        }
        if (schema.getAdditionalProperties() instanceof Schema<?> additional) {
            addRefs(additional, out);
        }
    }

    private static void addRefsAll(Collection<Schema> list, Set<String> out) {
        if (list != null) {
            for (Schema<?> schema : list) {
                addRefs(schema, out);
            }
        }
    }

    private static String refName(String ref) {
        final int slash = ref.lastIndexOf('/');
        return slash >= 0 ? ref.substring(slash + 1) : ref;
    }
}
