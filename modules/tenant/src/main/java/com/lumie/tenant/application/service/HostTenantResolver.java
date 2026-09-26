package com.lumie.tenant.application.service;

import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.domain.entity.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Resolves the tenant addressed by an HTTP Host header.
 *
 * <p>Basic hosts are derived from immutable tenant slugs, while custom host
 * bindings come from GitOps configuration. This is the single backend-side
 * host-to-tenant resolver; callers never select a tenant through a request
 * header, path segment, or request body.
 */
@Service
@RequiredArgsConstructor
public class HostTenantResolver {

    private static final Pattern DNS_LABEL = Pattern.compile("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?");

    private final TenantPersistencePort tenants;

    @Value("${tenant.edge.base-domain:lumie-edu.com}")
    private String baseDomain;

    @Value("${tenant.edge.root-hosts:lumie-edu.com}")
    private String rootHosts;

    @Value("${tenant.edge.custom-domain-map:}")
    private String customDomainMap;

    @Value("${tenant.edge.login-theme-map:}")
    private String loginThemeMap;

    /**
     * Returns empty only for a configured Lumie root host. Every other host
     * is a tenant candidate and is therefore either resolved or rejected by
     * the caller as unknown.
     */
    public Optional<ResolvedTenant> resolve(String rawHost) {
        String host = normalizeHost(rawHost);
        if (host.isBlank() || rootHostSet().contains(host)) {
            return Optional.empty();
        }

        String slug = customDomains().get(host);
        String legacyBasicHostSlug = null;
        if (slug == null && host.endsWith("." + normalizedBaseDomain())) {
            String label = host.substring(0, host.length() - normalizedBaseDomain().length() - 1);
            if (DNS_LABEL.matcher(label).matches()) {
                slug = "inst-" + label;
                // Existing tenants may predate the immutable inst-* namespace.
                // Prefer the standard form, then accept the legacy slug until a
                // separately scheduled data migration can retire it.
                legacyBasicHostSlug = label;
            }
        }
        if (slug == null) {
            return Optional.empty();
        }

        Optional<Tenant> resolvedTenant = tenants.findBySlug(slug);
        if (resolvedTenant.isEmpty() && legacyBasicHostSlug != null) {
            resolvedTenant = tenants.findBySlug(legacyBasicHostSlug);
        }
        return resolvedTenant
                .map(tenant -> new ResolvedTenant(tenant, customDomainFor(tenant.getSlugValue())));
    }

    public boolean isRootHost(String rawHost) {
        return rootHostSet().contains(normalizeHost(rawHost));
    }

    public String defaultHost(String slug) {
        String label = slug.startsWith("inst-") ? slug.substring(5) : slug;
        return label + "." + normalizedBaseDomain();
    }

    public String loginThemeFor(String slug) {
        return parseMap(loginThemeMap).getOrDefault(slug, "default");
    }

    private String customDomainFor(String slug) {
        return customDomains().entrySet().stream()
                .filter(entry -> entry.getValue().equals(slug))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private java.util.Set<String> rootHostSet() {
        return Stream.of(rootHosts.split(","))
                .map(HostTenantResolver::normalizeHost)
                .filter(host -> !host.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private String normalizedBaseDomain() {
        return normalizeHost(baseDomain);
    }

    private Map<String, String> customDomains() {
        return parseMap(customDomainMap);
    }

    private Map<String, String> parseMap(String value) {
        return Stream.of(value.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .map(entry -> entry.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toUnmodifiableMap(
                        parts -> normalizeHost(parts[0]),
                        parts -> parts[1].trim().toLowerCase(Locale.ROOT),
                        (left, right) -> { throw new IllegalStateException("duplicate tenant edge host"); }
                ));
    }

    private static String normalizeHost(String value) {
        if (value == null) return "";
        return value.split(",", 2)[0].trim().toLowerCase(Locale.ROOT)
                .replaceFirst(":\\d+$", "").replaceFirst("\\.$", "");
    }

    public record ResolvedTenant(Tenant tenant, String customDomain) {
        public boolean isActive() {
            return tenant.isActive();
        }
    }
}
