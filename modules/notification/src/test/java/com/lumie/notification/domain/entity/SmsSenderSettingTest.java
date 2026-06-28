package com.lumie.notification.domain.entity;

import com.lumie.notification.domain.vo.SmsSenderSettingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SmsSenderSetting 도메인 엔티티 테스트")
class SmsSenderSettingTest {

    @Test
    @DisplayName("create()는 번호를 정규화하고 PENDING 상태로 시작한다")
    void create_initializesPendingSetting() {
        SmsSenderSetting setting = SmsSenderSetting.create("solapi", "021-234-5678");

        assertThat(setting.getProvider()).isEqualTo("solapi");
        assertThat(setting.getSenderNumber()).isEqualTo("0212345678");
        assertThat(setting.getRegistrationStatus()).isEqualTo(SmsSenderSettingStatus.PENDING);
    }

    @Test
    @DisplayName("approve()는 승인 상태와 타임스탬프를 기록한다")
    void approve_marksApproved() {
        SmsSenderSetting setting = SmsSenderSetting.create("solapi", "021-234-5678");

        setting.approve();

        assertThat(setting.getRegistrationStatus()).isEqualTo(SmsSenderSettingStatus.APPROVED);
        assertThat(setting.getApprovedAt()).isNotNull();
        assertThat(setting.getRejectedAt()).isNull();
        assertThat(setting.getRejectionReason()).isNull();
    }

    @Test
    @DisplayName("submit()은 기존 승인 설정을 재제출 상태로 되돌린다")
    void submit_resetsApprovalStateWhenNumberChanges() {
        SmsSenderSetting setting = SmsSenderSetting.create("solapi", "021-234-5678");
        setting.approve();

        setting.submit("029-999-8888");

        assertThat(setting.getSenderNumber()).isEqualTo("0299998888");
        assertThat(setting.getRegistrationStatus()).isEqualTo(SmsSenderSettingStatus.PENDING);
        assertThat(setting.getApprovedAt()).isNull();
    }
}
