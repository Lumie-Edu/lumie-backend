package com.lumie.tenant.domain.entity;

import com.lumie.common.domain.BaseEntity;
import com.lumie.tenant.domain.exception.InvalidTenantStateException;
import com.lumie.tenant.domain.vo.TenantPlan;
import com.lumie.tenant.domain.vo.TenantSlug;
import com.lumie.tenant.domain.vo.TenantStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tenants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private TenantSlug slug;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TenantStatus status;

    @Column(name = "schema_name", length = 63)
    private String schemaName;

    @Column(name = "owner_email", length = 255)
    private String ownerEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_id", nullable = false, length = 20)
    private TenantPlan plan;

    @Column(name = "institute_name", length = 200)
    private String instituteName;

    @Column(name = "business_registration_number", length = 12)
    private String businessRegistrationNumber;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "logo_object_key", length = 255)
    private String logoObjectKey;

    @Column(name = "custom_id", length = 50)
    private String customId;

    @Column(name = "custom_domain", length = 253)
    private String customDomain;

    private static final java.util.regex.Pattern CUSTOM_ID_PATTERN =
            java.util.regex.Pattern.compile("^[a-z0-9][a-z0-9-]{1,28}[a-z0-9]$");

    private static final java.util.Set<String> RESERVED_CUSTOM_IDS = java.util.Set.of(
            // App-level reserved
            "login", "admin", "signup", "register", "api", "auth", "app", "account",
            "dashboard", "home", "oauth", "refresh", "logout", "settings", "public",
            "static", "_next", "images", "favicon", "robots", "sitemap",
            // Subdomain reservations (prevent collision with operational hosts)
            "www", "dev", "staging", "prod", "production", "qa", "test", "mail",
            "cdn", "assets", "support", "help", "status", "blog", "shop", "m", "mobile",
            "docs", "about"
    );

    @Builder
    private Tenant(TenantSlug slug, String name, String displayName, String ownerEmail, TenantPlan plan,
                   String instituteName, String businessRegistrationNumber) {
        this.slug = slug;
        this.name = name;
        this.displayName = displayName;
        this.ownerEmail = ownerEmail;
        this.status = TenantStatus.PENDING;
        this.schemaName = slug.toSchemaName();
        this.plan = plan != null ? plan : TenantPlan.FREE;
        this.instituteName = instituteName;
        this.businessRegistrationNumber = businessRegistrationNumber;
    }

    public static Tenant create(String slug, String name, String displayName, String ownerEmail) {
        return create(slug, name, displayName, ownerEmail, TenantPlan.FREE);
    }

    public static Tenant create(String slug, String name, String displayName, String ownerEmail, TenantPlan plan) {
        return Tenant.builder()
                .slug(TenantSlug.of(slug))
                .name(name)
                .displayName(displayName)
                .ownerEmail(ownerEmail)
                .plan(plan)
                .build();
    }

    /**
     * Creates a tenant with auto-generated slug for owner registration.
     * Slug format: inst-{uuid8}
     */
    public static Tenant createWithAutoSlug(String instituteName, String businessRegistrationNumber, String ownerEmail) {
        String autoSlug = "inst-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        return Tenant.builder()
                .slug(TenantSlug.of(autoSlug))
                .name(instituteName)
                .displayName(instituteName)
                .ownerEmail(ownerEmail)
                .instituteName(instituteName)
                .businessRegistrationNumber(businessRegistrationNumber)
                .plan(TenantPlan.FREE)
                .build();
    }

    public void updateLogoObjectKey(String objectKey) {
        this.logoObjectKey = objectKey;
    }

    public void updateCustomId(String customId) {
        if (customId == null || customId.isBlank()) {
            this.customId = null;
            return;
        }
        String normalized = customId.toLowerCase(java.util.Locale.ROOT).trim();
        if (!CUSTOM_ID_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "customId는 3-30자의 소문자 영숫자와 하이픈만 사용할 수 있고, 첫 글자와 끝 글자는 영숫자여야 합니다.");
        }
        if (RESERVED_CUSTOM_IDS.contains(normalized)) {
            throw new IllegalArgumentException("사용할 수 없는 학원 ID입니다: " + normalized);
        }
        this.customId = normalized;
    }

    public static boolean isReservedCustomId(String customId) {
        return customId != null && RESERVED_CUSTOM_IDS.contains(customId.toLowerCase(java.util.Locale.ROOT).trim());
    }

    public void startProvisioning() {
        validateStateTransition(TenantStatus.PENDING);
        this.status = TenantStatus.PROVISIONING;
    }

    public void activate() {
        validateStateTransition(TenantStatus.PROVISIONING);
        this.status = TenantStatus.ACTIVE;
    }

    public void suspend() {
        validateStateTransition(TenantStatus.ACTIVE);
        this.status = TenantStatus.SUSPENDED;
    }

    public void reactivate() {
        validateStateTransition(TenantStatus.SUSPENDED);
        this.status = TenantStatus.ACTIVE;
    }

    private void validateStateTransition(TenantStatus requiredStatus) {
        if (this.status != requiredStatus) {
            throw new InvalidTenantStateException(this.status, requiredStatus);
        }
    }

    public void markAsDeleted() {
        this.status = TenantStatus.DELETED;
    }

    public void updateInfo(String name, String displayName) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (displayName != null) {
            this.displayName = displayName;
        }
    }

    public void updateContact(String phone, String address) {
        if (phone != null) {
            this.phone = phone.isBlank() ? null : phone;
        }
        if (address != null) {
            this.address = address.isBlank() ? null : address;
        }
    }

    public String getSlugValue() {
        return slug.getValue();
    }

    public boolean isActive() {
        return this.status == TenantStatus.ACTIVE;
    }

    public boolean supportsCustomDomains() {
        return this.plan != null && this.plan.supportsCustomDomains();
    }

    public void updatePlan(TenantPlan plan) {
        if (plan != null) {
            this.plan = plan;
        }
    }

    public void setCustomDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            this.customDomain = null;
            return;
        }
        if (!supportsCustomDomains()) {
            throw new IllegalStateException("Tenant plan does not support custom domains: plan=" + plan);
        }
        this.customDomain = domain.toLowerCase(java.util.Locale.ROOT).trim();
    }
}
