package com.lumie.notification.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.notification.adapter.out.config.SmsProperties;
import com.lumie.notification.application.dto.request.UpdateSmsSenderSettingApprovalRequest;
import com.lumie.notification.application.dto.request.UpsertSmsSenderSettingRequest;
import com.lumie.notification.application.port.out.SmsSenderSettingPersistencePort;
import com.lumie.notification.domain.entity.SmsSenderSetting;
import com.lumie.notification.domain.vo.SmsSenderSettingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsSenderSettingService 테스트")
class SmsSenderSettingServiceTest {

    @Mock
    private SmsSenderSettingPersistencePort senderSettingPersistencePort;

    @Mock
    private SmsProperties smsProperties;

    @InjectMocks
    private SmsSenderSettingService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        lenient().when(smsProperties.provider()).thenReturn("solapi");
    }

    @Test
    @DisplayName("설정이 없으면 NOT_CONFIGURED 응답을 반환한다")
    void getSenderSetting_withoutRow_returnsNotConfigured() {
        given(senderSettingPersistencePort.findByProvider("solapi")).willReturn(Optional.empty());

        var response = service.getSenderSetting();

        assertThat(response.provider()).isEqualTo("solapi");
        assertThat(response.senderNumber()).isNull();
        assertThat(response.registrationStatus()).isEqualTo(SmsSenderSettingStatus.NOT_CONFIGURED);
    }

    @Test
    @DisplayName("상태 조회는 mock provider일 때 승인 완료로 간주한다")
    void getSenderSettingStatus_mockProvider_returnsApproved() {
        given(smsProperties.provider()).willReturn("mock");

        var response = service.getSenderSettingStatus();

        assertThat(response.provider()).isEqualTo("mock");
        assertThat(response.registrationStatus()).isEqualTo(SmsSenderSettingStatus.APPROVED);
    }

    @Test
    @DisplayName("상태 조회는 SOLAPI 설정 상태만 반환한다")
    void getSenderSettingStatus_solapi_returnsRegistrationStatus() {
        SmsSenderSetting setting = SmsSenderSetting.create("solapi", "021-234-5678");
        given(senderSettingPersistencePort.findByProvider("solapi")).willReturn(Optional.of(setting));

        var response = service.getSenderSettingStatus();

        assertThat(response.provider()).isEqualTo("solapi");
        assertThat(response.registrationStatus()).isEqualTo(SmsSenderSettingStatus.PENDING);
    }

    @Test
    @DisplayName("PUT 요청은 새 설정을 PENDING 상태로 저장한다")
    void upsert_createsPendingSetting() {
        given(senderSettingPersistencePort.findByProvider("solapi")).willReturn(Optional.empty());
        given(senderSettingPersistencePort.save(any(SmsSenderSetting.class))).willAnswer(inv -> inv.getArgument(0));

        var response = service.upsertSenderSetting(new UpsertSmsSenderSettingRequest("021-234-5678"));

        assertThat(response.provider()).isEqualTo("solapi");
        assertThat(response.senderNumber()).isEqualTo("02-1234-5678");
        assertThat(response.registrationStatus()).isEqualTo(SmsSenderSettingStatus.PENDING);
    }

    @Test
    @DisplayName("승인 PATCH는 상태를 APPROVED로 바꾼다")
    void updateApproval_marksApproved() {
        SmsSenderSetting setting = SmsSenderSetting.create("solapi", "021-234-5678");
        given(senderSettingPersistencePort.findByProvider("solapi")).willReturn(Optional.of(setting));
        given(senderSettingPersistencePort.save(any(SmsSenderSetting.class))).willAnswer(inv -> inv.getArgument(0));

        var response = service.updateApproval(new UpdateSmsSenderSettingApprovalRequest(
                UpdateSmsSenderSettingApprovalRequest.ApprovalStatus.APPROVED,
                null
        ));

        assertThat(response.registrationStatus()).isEqualTo(SmsSenderSettingStatus.APPROVED);
        assertThat(response.approvedAt()).isNotNull();
    }

    @Test
    @DisplayName("승인된 번호가 없으면 발송용 번호 조회를 거절한다")
    void requireApprovedSenderNumber_withoutApproval_throws() {
        given(senderSettingPersistencePort.findByProviderAndRegistrationStatus("solapi", SmsSenderSettingStatus.APPROVED))
                .willReturn(Optional.empty());

        assertThatThrownBy(service::requireApprovedSenderNumber)
                .isInstanceOf(BusinessException.class);
    }
}
