package com.lumie.tenant.domain.entity;

import com.lumie.common.exception.BusinessException;
import com.lumie.tenant.domain.exception.InvalidTenantStateException;
import com.lumie.tenant.domain.vo.TenantPlan;
import com.lumie.tenant.api.TenantStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantTest {

    @Test
    @DisplayName("Tenant 생성 시 PENDING 상태")
    void createTenantWithPendingStatus() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", "테스트 학원");

        assertThat(tenant.getSlugValue()).isEqualTo("test-academy");
        assertThat(tenant.getName()).isEqualTo("Test Academy");
        assertThat(tenant.getDisplayName()).isEqualTo("테스트 학원");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.PENDING);
    }

    @Test
    @DisplayName("PENDING -> PROVISIONING 상태 전이")
    void startProvisioning() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);

        tenant.startProvisioning();

        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.PROVISIONING);
    }

    @Test
    @DisplayName("PROVISIONING -> ACTIVE 상태 전이")
    void activate() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);
        tenant.startProvisioning();

        tenant.activate();

        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    @DisplayName("ACTIVE -> SUSPENDED 상태 전이")
    void suspend() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);
        tenant.startProvisioning();
        tenant.activate();

        tenant.suspend();

        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
    }

    @Test
    @DisplayName("SUSPENDED -> ACTIVE 상태 전이 (재활성화)")
    void reactivate() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);
        tenant.startProvisioning();
        tenant.activate();
        tenant.suspend();

        tenant.reactivate();

        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    @DisplayName("잘못된 상태 전이 시 예외 발생")
    void invalidStatusTransition() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);

        assertThatThrownBy(tenant::activate)
                .isInstanceOf(InvalidTenantStateException.class)
                .hasMessageContaining("PROVISIONING");
    }

    @Test
    @DisplayName("Tenant 정보 업데이트")
    void updateInfo() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);

        tenant.updateInfo("Updated Academy", "업데이트된 학원");

        assertThat(tenant.getName()).isEqualTo("Updated Academy");
        assertThat(tenant.getDisplayName()).isEqualTo("업데이트된 학원");
    }

    @Test
    @DisplayName("isActive 검증")
    void isActive() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);
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
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);

        assertThat(tenant.getPlan()).isEqualTo(TenantPlan.FREE);
    }

    @Test
    @DisplayName("Tenant 생성 시 Plan 지정 가능")
    void createTenantWithCustomPlan() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null, TenantPlan.PRO);

        assertThat(tenant.getPlan()).isEqualTo(TenantPlan.PRO);
    }

    @Test
    @DisplayName("Plan 업데이트")
    void updatePlan() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);
        assertThat(tenant.getPlan()).isEqualTo(TenantPlan.FREE);

        tenant.updatePlan(TenantPlan.MAX);

        assertThat(tenant.getPlan()).isEqualTo(TenantPlan.MAX);
    }

    @Test
    @DisplayName("updateLogoObjectKey 호출 시 logoObjectKey 갱신됨")
    void updateLogoObjectKey() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);

        tenant.updateLogoObjectKey("c704d223/branding/logo");

        assertThat(tenant.getLogoObjectKey()).isEqualTo("c704d223/branding/logo");
    }

    @Test
    @DisplayName("숨김 sidebar 항목은 공백과 중복을 정리해 저장한다")
    void updateHiddenSidebarItemIds() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);

        tenant.updateHiddenSidebarItemIds(List.of(" classes ", "classes", "", "reviews"));

        assertThat(tenant.getHiddenSidebarItemIds()).containsExactly("classes", "reviews");
    }

    @Test
    @DisplayName("숨김 sidebar 항목은 외부 리스트 변경으로 변하지 않는다")
    void hiddenSidebarItemIdsAreDefensivelyCopied() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);
        List<String> source = new ArrayList<>(List.of("classes"));

        tenant.updateHiddenSidebarItemIds(source);
        source.add("reviews");

        assertThat(tenant.getHiddenSidebarItemIds()).containsExactly("classes");
        assertThatThrownBy(() -> tenant.getHiddenSidebarItemIds().add("reviews"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("지원하지 않는 sidebar 항목은 저장할 수 없다")
    void updateHiddenSidebarItemIdsRejectsUnknownId() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);

        assertThatThrownBy(() -> tenant.updateHiddenSidebarItemIds(List.of("not-a-menu")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("지원하지 않는 사이드바 메뉴 ID");
    }

    @Test
    @DisplayName("제거된 스프레드시트 sidebar 항목은 저장할 수 없다")
    void updateHiddenSidebarItemIdsRejectsRemovedSpreadsheetId() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);

        assertThatThrownBy(() -> tenant.updateHiddenSidebarItemIds(List.of("spreadsheets")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("지원하지 않는 사이드바 메뉴 ID");
    }

}
