package com.lumie.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.app.FullContextTestBase;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Reconciles every live MVC method/path with the checked-in API inventory and requires
 * each handler to have a single, explicit authorization classification. Keep exceptions
 * here narrow and named: adding a new handler without deciding its policy must fail.
 */
class StaffRouteMatrixTest extends FullContextTestBase {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    @Test
    void everyLiveRouteMatchesApiInventoryAndHasOnePolicyClassification() throws Exception {
        Set<String> live = new TreeSet<>();
        List<String> unclassified = new ArrayList<>();
        mappings.getHandlerMethods().forEach((mapping, handler) -> {
            for (String path : mapping.getPatternValues()) {
                if (!isApiRoute(path)) continue;
                Set<RequestMethod> methods = mapping.getMethodsCondition().getMethods();
                if (methods.isEmpty()) {
                    unclassified.add("HTTP method missing: " + handler);
                    continue;
                }
                for (RequestMethod method : methods) {
                    String route = method.name() + " " + path;
                    if (isApiRoute(path)) live.add(route);
                    if (policy(path, handler) == null) {
                        unclassified.add(route + " -> " + handler);
                    }
                }
            }
        });

        Set<String> documented = openApiRoutes();
        Set<String> undocumentedByOpenApi = Set.of(
                "PATCH /internal/sms/sender-settings/{tenantSlug}/approval",
                "POST /v1/sms/solapi/webhook");
        assertThat(live).as("MVC handlers missing from the API inventory")
                .containsExactlyInAnyOrderElementsOf(union(documented, undocumentedByOpenApi));
        assertThat(unclassified).as("Routes requiring an explicit policy or exclusion").isEmpty();
        assertThat(live).as("Expected route inventory size; review intentional API changes")
                .hasSizeGreaterThanOrEqualTo(234);
    }

    private static Set<String> openApiRoutes() throws Exception {
        try (InputStream input = StaffRouteMatrixTest.class
                .getResourceAsStream("/openapi/api-docs.json")) {
            JsonNode paths = JSON.readTree(input).path("paths");
            Set<String> routes = new TreeSet<>();
            paths.fields().forEachRemaining(path -> path.getValue().fieldNames().forEachRemaining(method -> {
                if (isApiRoute(path.getKey()) && Set.of("get", "put", "post", "delete", "patch", "head", "options", "trace")
                        .contains(method)) {
                    routes.add(method.toUpperCase(Locale.ROOT) + " " + path.getKey());
                }
            }));
            return routes;
        }
    }

    private static boolean isApiRoute(String path) {
        return path.startsWith("/v1/") || path.startsWith("/internal/");
    }

    private static Set<String> union(Set<String> documented, Set<String> explicitExclusions) {
        Set<String> routes = new TreeSet<>(documented);
        routes.addAll(explicitExclusions);
        return routes;
    }

    private static String policy(String path, HandlerMethod handler) {
        String controller = handler.getBeanType().getSimpleName();
        String method = handler.getMethod().getName();
        if (path.startsWith("/internal/")) return "INTERNAL_AUTH";
        if (path.startsWith("/actuator/") || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui") || path.equals("/tenant-root")) return "OPS_OR_PUBLIC";
        if (path.startsWith("/v1/reviews/")) return "DEFERRED_REVIEW";
        if (path.equals("/v1/login") || path.equals("/v1/register/owner")
                || path.equals("/v1/tenants/public/current-branding")
                || (path.matches("/v1/tenants/[^/]+/logo") && method.equals("get"))
                || path.equals("/v1/sms/solapi/webhook") || path.equals("/v1/payments/paddle/webhook"))
            return "PUBLIC_OR_VERIFIED_WEBHOOK";
        if (path.startsWith("/v1/me") || path.startsWith("/v1/logout")) return "AUTHENTICATED_SELF";
        if (path.startsWith("/v1/files/") || path.equals("/v1/files")) return "FILE_ENTITY_POLICY";
        if (path.startsWith("/v1/billing-keys") || path.startsWith("/v1/billing/subscribe")
                || path.startsWith("/v1/payments/") || path.startsWith("/v1/subscriptions/")
                || path.startsWith("/v1/alimtalk/credits")) return "OWNER_ONLY";
        if (path.startsWith("/v1/plans") || path.equals("/v1/billing/config")) return "AUTHENTICATED_CATALOG";
        if (path.startsWith("/v1/tenants/") || path.equals("/v1/tenants")) return "TENANT_POLICY";
        if (path.startsWith("/v1/classes/me") || path.startsWith("/v1/lectures/student/")
                || path.startsWith("/v1/students/me/") || path.startsWith("/v1/assignments/me")
                || path.matches("/v1/assignments/\\{[^/]+}/me.*") || path.startsWith("/v1/qna/me")
                || (path.equals("/v1/qna") && method.equals("post"))
                || path.startsWith("/v1/statistics/students/") || path.startsWith("/v1/reports/students/")
                || (path.startsWith("/v1/results/") && path.endsWith("/questions"))
                || path.endsWith("/omr-image")) return "STUDENT_OR_OBJECT_POLICY";
        if (path.startsWith("/v1/announcements/") || path.equals("/v1/announcements")
                || path.startsWith("/v1/qna/")) return "CONTENT_ROLE_AND_OWNERSHIP";
        if (path.startsWith("/v1/sms/") || path.startsWith("/v1/sms-templates/")) return "STAFF_MESSAGING";
        if (controller.equals("AuthController")) return "AUTHENTICATED_SELF_OR_PUBLIC";
        if (path.startsWith("/v1/")) return "TENANT_STAFF_OR_SPECIFIC_ROLE";
        return null;
    }
}
