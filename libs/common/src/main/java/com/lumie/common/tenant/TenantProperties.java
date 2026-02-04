package com.lumie.common.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for tenant context handling.
 */
@ConfigurationProperties(prefix = "lumie.tenant")
public class TenantProperties {

    /**
     * Enable tenant context handling. Default: true
     */
    private boolean enabled = true;

    /**
     * Enable Hibernate multi-tenancy resolver. Default: true
     * Set to false for services that don't use Hibernate (e.g., file-svc).
     */
    private boolean hibernateEnabled = true;

    /**
     * Paths to exclude from tenant context requirement.
     * Default: /actuator/**, /health/**
     */
    private List<String> excludePaths = new ArrayList<>(List.of(
            "/actuator/**",
            "/health/**"
    ));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isHibernateEnabled() {
        return hibernateEnabled;
    }

    public void setHibernateEnabled(boolean hibernateEnabled) {
        this.hibernateEnabled = hibernateEnabled;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }
}
