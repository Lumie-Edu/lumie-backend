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

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_id", nullable = false, length = 20)
    private TenantPlan plan;

    @Column(name = "institute_name", length = 200)
    private String instituteName;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "logo_object_key", length = 255)
    private String logoObjectKey;

    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hidden_sidebar_item_ids", nullable = false)
    private List<String> hiddenSidebarItemIds = new ArrayList<>();

    private static final Set<String> ALLOWED_SIDEBAR_ITEM_IDS = Set.of(
            "ai",
            "students", "employees",
            "classes", "assignments", "lectures",
            "grades", "omr-grading", "reports",
            "announcements", "sms", "resources", "qna",
            "textbooks", "reviews"
    );

    @Builder
    private Tenant(TenantSlug slug, String name, String displayName, TenantPlan plan,
                   String instituteName) {
        this.slug = slug;
        this.name = name;
        this.displayName = displayName;
        this.status = TenantStatus.PENDING;
        this.plan = plan != null ? plan : TenantPlan.FREE;
        this.instituteName = instituteName;
    }

    public static Tenant create(String slug, String name, String displayName) {
        return create(slug, name, displayName, TenantPlan.FREE);
    }

    public static Tenant create(String slug, String name, String displayName, TenantPlan plan) {
        return Tenant.builder()
                .slug(TenantSlug.of(slug))
                .name(name)
                .displayName(displayName)
                .plan(plan)
                .build();
    }

    /**
     * Creates a tenant with auto-generated slug for owner registration.
     * Slug format: inst-{uuid8}
     */
    public static Tenant createWithAutoSlug(String instituteName) {
        String autoSlug = "inst-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        return Tenant.builder()
                .slug(TenantSlug.of(autoSlug))
                .name(instituteName)
                .displayName(instituteName)
                .instituteName(instituteName)
                .plan(TenantPlan.FREE)
                .build();
    }

    public void updateLogoObjectKey(String objectKey) {
        this.logoObjectKey = objectKey;
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

    public void updateContact(String phone) {
        if (phone != null) {
            this.phone = phone.isBlank() ? null : phone;
        }
    }

    public void updateHiddenSidebarItemIds(List<String> itemIds) {
        if (itemIds == null) {
            return;
        }
        this.hiddenSidebarItemIds = normalizeSidebarItemIds(itemIds);
    }

    private List<String> normalizeSidebarItemIds(List<String> itemIds) {
        return itemIds.stream()
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
        return hiddenSidebarItemIds == null ? List.of() : normalizeSidebarItemIds(hiddenSidebarItemIds);
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

}
