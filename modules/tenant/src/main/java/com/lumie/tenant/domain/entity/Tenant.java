package com.lumie.tenant.domain.entity;

import com.lumie.common.domain.BaseEntity;
import com.lumie.common.exception.BusinessException;
import com.lumie.tenant.domain.exception.InvalidTenantStateException;
import com.lumie.tenant.domain.exception.TenantErrorCode;
import com.lumie.tenant.domain.vo.TenantPlan;
import com.lumie.tenant.domain.vo.TenantSlug;
import com.lumie.tenant.api.TenantStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tenants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Embedded
    private TenantSlug slug;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TenantStatus status;

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

    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hidden_sidebar_item_ids", nullable = false)
    private List<String> hiddenSidebarItemIds = new ArrayList<>();

    private static final Set<String> ALLOWED_SIDEBAR_ITEM_IDS = Set.of(
            "ai",
            "students", "employees",
            "classes", "attendance", "assignments", "lectures",
            "grades", "omr-grading", "reports",
            "announcements", "sms", "resources", "qna", "schedules",
            "textbooks", "spreadsheets", "reviews"
    );

    private static final java.util.regex.Pattern CUSTOM_ID_PATTERN =
            java.util.regex.Pattern.compile("^[a-z0-9][a-z0-9-]{1,28}[a-z0-9]$");

    // Keep in sync with frontend `RESERVED_CUSTOM_IDS` in
    // src/entities/tenant/model/schema.ts and `RESERVED_TOP_SEGMENTS`
    // in middleware.ts. Path-based tenant landing means anything
    // claimed by a real top-level route must be reserved.
    private static final java.util.Set<String> RESERVED_CUSTOM_IDS = java.util.Set.of(
            // App routes
            "admin", "api", "auth", "dashboard",
            "login", "register", "signup", "logout",
            // Marketing routes
            "about", "blog", "cases", "changelog", "features",
            "help", "pricing", "security",
            // Static assets / framework
            "_next", "favicon.ico", "favicon", "images", "fonts",
            "public", "static", "robots", "sitemap",
            // Defensive reserves
            "account", "settings", "oauth", "refresh", "home", "app"
    );

    @Builder
    private Tenant(TenantSlug slug, String name, String displayName, String ownerEmail, TenantPlan plan,
                   String instituteName, String businessRegistrationNumber, String customId) {
        this.slug = slug;
        this.name = name;
        this.displayName = displayName;
        this.ownerEmail = ownerEmail;
        this.status = TenantStatus.PENDING;
        this.plan = plan != null ? plan : TenantPlan.FREE;
        this.instituteName = instituteName;
        this.businessRegistrationNumber = businessRegistrationNumber;
        this.customId = customId;
    }

    public static Tenant create(String slug, String name, String displayName, String ownerEmail) {
        return create(slug, name, displayName, ownerEmail, TenantPlan.FREE);
    }

    public static Tenant create(String slug, String name, String displayName, String ownerEmail, TenantPlan plan) {
        return Tenant.builder()
                .slug(TenantSlug.of(slug))
                .customId(slug.replaceFirst("^inst-", ""))
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
                .customId(autoSlug.replaceFirst("^inst-", ""))
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
        boolean isDefault = this.customId == null || this.customId.equals(this.slug.getValue()) || this.customId.equals(this.slug.getValue().replaceFirst("^inst-", ""));
        boolean alreadySet = !isDefault;

        if (alreadySet) {
            // Once set by user, the only permitted call is an idempotent no-op with the same value.
            // Clearing, blanking, or changing to a different value are all forbidden.
            String incoming = (customId == null || customId.isBlank())
                    ? null
                    : customId.toLowerCase(java.util.Locale.ROOT).trim();
            if (!this.customId.equals(incoming)) {
                throw new BusinessException(TenantErrorCode.CUSTOM_ID_IMMUTABLE,
                        "학원 ID는 한번 설정되면 변경할 수 없습니다.");
            }
            // Same value — idempotent no-op.
            return;
        }

        // First-set path:
        if (customId == null || customId.isBlank()) {
            this.customId = this.slug.getValue().replaceFirst("^inst-", "");
            return;
        }
        String normalized = customId.toLowerCase(java.util.Locale.ROOT).trim();
        if (!CUSTOM_ID_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(TenantErrorCode.INVALID_CUSTOM_ID,
                    "customId는 3-30자의 소문자 영숫자와 하이픈만 사용할 수 있고, 첫 글자와 끝 글자는 영숫자여야 합니다.");
        }
        if (RESERVED_CUSTOM_IDS.contains(normalized)) {
            throw new BusinessException(TenantErrorCode.RESERVED_CUSTOM_ID,
                    "사용할 수 없는 학원 ID입니다: " + normalized);
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

    public void updateHiddenSidebarItemIds(List<String> itemIds) {
        if (itemIds == null) {
            return;
        }
        this.hiddenSidebarItemIds = itemIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .map(this::validatedSidebarItemId)
                .distinct()
                .toList();
    }

    public void completeOnboarding(Instant completedAt) {
        if (this.onboardingCompletedAt != null) {
            throw new BusinessException(TenantErrorCode.ONBOARDING_ALREADY_COMPLETED,
                    "이미 완료된 온보딩입니다.");
        }
        this.onboardingCompletedAt = completedAt != null ? completedAt : Instant.now();
    }

    public List<String> getHiddenSidebarItemIds() {
        return hiddenSidebarItemIds == null ? List.of() : List.copyOf(hiddenSidebarItemIds);
    }

    private String validatedSidebarItemId(String itemId) {
        if (!ALLOWED_SIDEBAR_ITEM_IDS.contains(itemId)) {
            throw new BusinessException(TenantErrorCode.INVALID_SIDEBAR_ITEM_ID,
                    "지원하지 않는 사이드바 메뉴 ID입니다: " + itemId);
        }
        return itemId;
    }

    public String getSlugValue() {
        return slug.getValue();
    }

    public boolean isActive() {
        return this.status == TenantStatus.ACTIVE;
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
        this.customDomain = domain.toLowerCase(java.util.Locale.ROOT).trim();
    }
}
