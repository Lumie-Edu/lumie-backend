package com.lumie.tenant.domain.entity;

import com.lumie.common.domain.BaseEntity;
import com.lumie.common.exception.BusinessException;
import com.lumie.tenant.domain.exception.InvalidTenantStateException;
import com.lumie.tenant.domain.exception.TenantErrorCode;
import com.lumie.tenant.domain.vo.PreferredGradeScale;
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "subjects", columnDefinition = "jsonb", nullable = false)
    private List<String> subjects = new ArrayList<>();

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "logo_object_key", length = 255)
    private String logoObjectKey;

    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_grade_scale", nullable = false, length = 20)
    private PreferredGradeScale preferredGradeScale = PreferredGradeScale.NINE_GRADE;

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
        return createWithAutoSlug(instituteName, List.of());
    }

    public static Tenant createWithAutoSlug(String instituteName, List<String> subjects) {
        String autoSlug = "inst-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        Tenant tenant = Tenant.builder()
                .slug(TenantSlug.of(autoSlug))
                .name(instituteName)
                .displayName(instituteName)
                .instituteName(instituteName)
                .plan(TenantPlan.FREE)
                .build();
        tenant.updateSubjects(subjects);
        return tenant;
    }

    public void updateSubjects(List<String> subjects) {
        this.subjects = subjects == null ? new ArrayList<>() : subjects.stream()
                .filter(subject -> subject != null && !subject.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    public List<String> getSubjects() {
        return subjects == null ? List.of() : List.copyOf(subjects);
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

    public void updatePreferredGradeScale(PreferredGradeScale preferredGradeScale) {
        if (preferredGradeScale != null) {
            this.preferredGradeScale = preferredGradeScale;
        }
    }

    public void completeOnboarding(Instant completedAt) {
        if (this.onboardingCompletedAt != null) {
            throw new BusinessException(TenantErrorCode.ONBOARDING_ALREADY_COMPLETED,
                    "이미 완료된 온보딩입니다.");
        }
        this.onboardingCompletedAt = completedAt != null ? completedAt : Instant.now();
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
