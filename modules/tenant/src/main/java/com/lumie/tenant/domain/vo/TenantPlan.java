package com.lumie.tenant.domain.vo;

public enum TenantPlan {
    FREE,
    BASIC,
    PRO,
    MAX;

    public boolean supportsAdvancedAnalytics() {
        return this == PRO || this == MAX;
    }

    public boolean supportsPrioritySupport() {
        return this == MAX;
    }
}
