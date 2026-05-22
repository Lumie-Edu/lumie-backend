package com.lumie.app.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Replaces the nested Spring Data {@code Pageable} query parameter
 * ({@code {name: pageable, schema: {$ref: Pageable}}}) with flat
 * {@code page} / {@code size} / {@code sort} scalar query params, so the generated
 * FE client emits real query arguments instead of an unusable nested object.
 *
 * <p>This is the targeted equivalent of {@code springdoc.default-flat-param-object=true}
 * — that global flag also flattens multipart {@code @RequestPart} bodies into query params
 * (springdoc-openapi#2282), which silently breaks every file-upload endpoint. Scoping the
 * flatten to {@code Pageable} avoids that. The emitted param shape mirrors springdoc's own
 * {@code @ParameterObject Pageable} output (already used on the student list endpoint).
 */
@Configuration
public class OpenApiPageableFlattenCustomizer {

    private static final String PAGEABLE_SCHEMA = "Pageable";

    @Bean
    public GlobalOpenApiCustomizer pageableFlattenCustomizer() {
        return openApi -> {
            if (openApi.getPaths() != null) {
                for (PathItem path : openApi.getPaths().values()) {
                    for (Operation op : path.readOperations()) {
                        flattenPageable(op);
                    }
                }
            }
            // The Pageable component schema is now unreferenced — drop it so it is not
            // emitted as a dead type.
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                openApi.getComponents().getSchemas().remove(PAGEABLE_SCHEMA);
            }
        };
    }

    private static void flattenPageable(Operation op) {
        if (op.getParameters() == null) {
            return;
        }
        boolean flattened = op.getParameters().removeIf(OpenApiPageableFlattenCustomizer::isPageable);
        if (flattened) {
            op.getParameters().addAll(flatPageableParams());
        }
    }

    private static boolean isPageable(Parameter parameter) {
        if ("pageable".equals(parameter.getName())) {
            return true;
        }
        final Schema<?> schema = parameter.getSchema();
        return schema != null && schema.get$ref() != null && schema.get$ref().endsWith("/" + PAGEABLE_SCHEMA);
    }

    private static List<Parameter> flatPageableParams() {
        final List<Parameter> params = new ArrayList<>(3);
        params.add(new Parameter().name("page").in("query").required(false)
                .description("Zero-based page index (0..N)")
                .schema(new IntegerSchema().format("int32").minimum(BigDecimal.ZERO)));
        params.add(new Parameter().name("size").in("query").required(false)
                .description("The size of the page to be returned")
                .schema(new IntegerSchema().format("int32").minimum(BigDecimal.ONE)));
        params.add(new Parameter().name("sort").in("query").required(false)
                .description("Sorting criteria in the format: property,(asc|desc). "
                        + "Default sort order is ascending. Multiple sort criteria are supported.")
                .schema(new ArraySchema().items(new StringSchema())));
        return params;
    }
}
