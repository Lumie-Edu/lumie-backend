package com.lumie.tenant.domain.entity;

import com.lumie.tenant.domain.exception.InvalidTenantStateException;
import com.lumie.tenant.domain.vo.TenantPlan;
import com.lumie.tenant.domain.vo.TenantStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantTest {

    @Test
    @DisplayName("Tenant 생성 시 PENDING 상태")
    void createTenantWithPendingStatus() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", "테스트 학원", "owner@test.com");

        assertThat(tenant.getSlugValue()).isEqualTo("test-academy");
        assertThat(tenant.getName()).isEqualTo("Test Academy");
        assertThat(tenant.getDisplayName()).isEqualTo("테스트 학원");
        assertThat(tenant.getOwnerEmail()).isEqualTo("owner@test.com");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.PENDING);
        assertThat(tenant.getSchemaName()).isEqualTo("tenant_test_academy");
    }

    @Test
    @DisplayName("PENDING -> PROVISIONING 상태 전이")
    void startProvisioning() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null, null);

        tenant.startProvisioning();

        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.PROVISIONING);
    }

    @Test
    @DisplayName("PROVISIONING -> ACTIVE 상태 전이")
    void activate() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null, null);
        tenant.startProvisioning();

        tenant.activate();

        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    @DisplayName("ACTIVE -> SUSPENDED 상태 전이")
    void suspend() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null, null);
        tenant.startProvisioning();
        tenant.activate();

        tenant.suspend();

        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
    }

    @Test
    @DisplayName("SUSPENDED -> ACTIVE 상태 전이 (재활성화)")
    void reactivate() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null, null);
        tenant.startProvisioning();
        tenant.activate();
        tenant.suspend();

        tenant.reactivate();

        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    @DisplayName("잘못된 상태 전이 시 예외 발생")
    void invalidStatusTransition() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null, null);

        assertThatThrownBy(tenant::activate)
                .isInstanceOf(InvalidTenantStateException.class)
                .hasMessageContaining("PROVISIONING");
    }

    @Test
    @DisplayName("Tenant 정보 업데이트")
    void updateInfo() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null, null);

        tenant.updateInfo("Updated Academy", "업데이트된 학원");

        assertThat(tenant.getName()).isEqualTo("Updated Academy");
        assertThat(tenant.getDisplayName()).isEqualTo("업데이트된 학원");
    }

    @Test
    @DisplayName("isActive 검증")
    void isActive() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null, null);
        assertThat(tenant.isActive()).isFalse();

        tenant.startProvisioning();
        assertThat(tenant.isActive()).isFalse();

        tenant.activate();
        assertThat(tenant.isActive()).isTrue();

        tenant.suspend();
        assertThat(tenant.isActive()).isFalse();
    }

    @Test
    @DisplayName("Tenant 생성 시 기본 Plan은 FREE")
    void createTenantWithDefaultPlan() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null, null);

        assertThat(tenant.getPlan()).isEqualTo(TenantPlan.FREE);
        assertThat(tenant.supportsCustomDomains()).isFalse();
    }

    @Test
    @DisplayName("Tenant 생성 시 Plan 지정 가능")
    void createTenantWithCustomPlan() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null, null, TenantPlan.PRO);

        assertThat(tenant.getPlan()).isEqualTo(TenantPlan.PRO);
        assertThat(tenant.supportsCustomDomains()).isTrue();
    }

    @Test
    @DisplayName("Plan 업데이트")
    void updatePlan() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null, null);
        assertThat(tenant.getPlan()).isEqualTo(TenantPlan.FREE);

        tenant.updatePlan(TenantPlan.ENTERPRISE);

        assertThat(tenant.getPlan()).isEqualTo(TenantPlan.ENTERPRISE);
        assertThat(tenant.supportsCustomDomains()).isTrue();
    }

    @Test
    @DisplayName("getOrCreateSettings 호출 시 Settings 생성됨")
    void getOrCreateSettingsCreatesSettings() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null, null);

        // Settings는 명시적 호출 전까지 null
        TenantSettings settings = tenant.getOrCreateSettings();

        assertThat(settings).isNotNull();
        assertThat(settings.getTenant()).isEqualTo(tenant);
        assertThat(settings.getTheme()).isEmpty();
    }
}
