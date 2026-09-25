package com.lumie.tenant.domain.entity;

import com.lumie.common.exception.BusinessException;
import com.lumie.tenant.domain.exception.InvalidTenantStateException;
import com.lumie.tenant.domain.exception.TenantErrorCode;
import com.lumie.tenant.domain.vo.TenantPlan;
import com.lumie.tenant.api.TenantStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
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
    @DisplayName("FREE 플랜도 커스텀 도메인을 설정할 수 있다 (요금제 게이트 제거)")
    void setCustomDomainOnFreePlan() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);
        assertThat(tenant.getPlan()).isEqualTo(TenantPlan.FREE);

        tenant.setCustomDomain("ABC.com");

        assertThat(tenant.getCustomDomain()).isEqualTo("abc.com");
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
    @DisplayName("제거된 legacy sidebar 항목은 저장 요청에서 무시한다")
    void updateHiddenSidebarItemIdsIgnoresRemovedLegacyId() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);

        tenant.updateHiddenSidebarItemIds(List.of("classes", "schedules", "reviews"));

        assertThat(tenant.getHiddenSidebarItemIds()).containsExactly("classes", "reviews");
    }

    @Test
    @DisplayName("제거된 legacy sidebar 항목은 조회 응답에서 노출하지 않는다")
    void getHiddenSidebarItemIdsFiltersRemovedLegacyId() throws ReflectiveOperationException {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);
        Field field = Tenant.class.getDeclaredField("hiddenSidebarItemIds");
        field.setAccessible(true);
        field.set(tenant, List.of("classes", "schedules", "reviews"));

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

    // --- customId permanence ---

    @Test
    @DisplayName("customId가 null일 때 처음 설정하면 정상 저장됨")
    void setCustomIdFirstTime() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);
        assertThat(tenant.getCustomId()).isEqualTo(tenant.getSlugValue());

        tenant.updateCustomId("my-academy");

        assertThat(tenant.getCustomId()).isEqualTo("my-academy");
    }

    @Test
    @DisplayName("이미 설정된 customId를 다른 값으로 변경하면 BusinessException(CUSTOM_ID_IMMUTABLE) 발생")
    void changeCustomIdAfterSetThrowsImmutableException() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);
        tenant.updateCustomId("my-academy");

        assertThatThrownBy(() -> tenant.updateCustomId("other-academy"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(TenantErrorCode.CUSTOM_ID_IMMUTABLE));
    }

    @Test
    @DisplayName("이미 설정된 customId와 동일한 값으로 호출하면 예외 없이 no-op")
    void sameCustomIdIsNoOp() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);
        tenant.updateCustomId("my-academy");

        // Must not throw — idempotent no-op.
        tenant.updateCustomId("my-academy");
        tenant.updateCustomId("MY-ACADEMY"); // normalises to same value

        assertThat(tenant.getCustomId()).isEqualTo("my-academy");
    }

    @Test
    @DisplayName("이미 설정된 customId를 null/blank로 지우려 하면 BusinessException(CUSTOM_ID_IMMUTABLE) 발생")
    void clearCustomIdAfterSetThrowsImmutableException() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);
        tenant.updateCustomId("my-academy");

        assertThatThrownBy(() -> tenant.updateCustomId(null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(TenantErrorCode.CUSTOM_ID_IMMUTABLE));

        assertThatThrownBy(() -> tenant.updateCustomId("   "))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(TenantErrorCode.CUSTOM_ID_IMMUTABLE));
    }

    // --- customId first-set validation ---

    @Test
    @DisplayName("customId가 null일 때 포맷 위반 값으로 설정하면 BusinessException(INVALID_CUSTOM_ID) 발생")
    void setCustomIdFirstTimeWithInvalidFormatThrows() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);
        assertThat(tenant.getCustomId()).isEqualTo(tenant.getSlugValue());

        assertThatThrownBy(() -> tenant.updateCustomId("-bad"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(TenantErrorCode.INVALID_CUSTOM_ID));
    }

    @Test
    @DisplayName("customId가 null일 때 예약어로 설정하면 BusinessException(RESERVED_CUSTOM_ID) 발생")
    void setCustomIdFirstTimeWithReservedValueThrows() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);
        assertThat(tenant.getCustomId()).isEqualTo(tenant.getSlugValue());

        // "admin" satisfies CUSTOM_ID_PATTERN (format-valid) but is in RESERVED_CUSTOM_IDS
        assertThatThrownBy(() -> tenant.updateCustomId("admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(TenantErrorCode.RESERVED_CUSTOM_ID));
    }

    @Test
    @DisplayName("customId가 null일 때 oauth는 더 이상 예약어가 아니므로 설정할 수 있다")
    void setCustomIdFirstTimeWithOauthSucceeds() {
        Tenant tenant = Tenant.create("test-academy", "Test Academy", null);
        assertThat(tenant.getCustomId()).isEqualTo(tenant.getSlugValue());

        tenant.updateCustomId("oauth");

        assertThat(tenant.getCustomId()).isEqualTo("oauth");
    }
}
