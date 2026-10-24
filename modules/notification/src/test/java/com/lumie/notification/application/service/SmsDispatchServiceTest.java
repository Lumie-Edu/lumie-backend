package com.lumie.notification.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.RlsTenantTransactionBinder;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.notification.adapter.out.config.SmsProperties;
import com.lumie.notification.application.port.out.SmsProviderPort;
import com.lumie.notification.domain.entity.SmsMessage;
import com.lumie.notification.domain.exception.SmsErrorCode;
import com.lumie.notification.domain.repository.SmsMessageRepository;
import com.lumie.notification.domain.vo.Recipient;
import com.lumie.notification.domain.vo.RecipientRole;
import com.lumie.notification.domain.vo.RecipientType;
import com.lumie.notification.domain.vo.SmsMessageType;
import com.lumie.notification.domain.vo.SmsStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmsDispatchServiceTest {

    @Mock private SmsMessageRepository messageRepository;
    @Mock private SmsProviderRegistry providerRegistry;
    @Mock private SmsProviderPort providerPort;
    @Mock private SmsSenderSettingService senderSettingService;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private RlsTenantTransactionBinder rlsTenantTransactionBinder;

    private SmsDispatchService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenant("inst-test");
        TenantContextHolder.setTenantId(1L);
        SmsProperties properties = new SmsProperties("mock", 50, 3, null);
        service = new SmsDispatchService(
                messageRepository,
                providerRegistry,
                properties,
                senderSettingService,
                transactionTemplate,
                rlsTenantTransactionBinder
        );
        given(transactionTemplate.execute(any())).willAnswer(inv -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        });
        lenient().doAnswer(inv -> {
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> callback = inv.getArgument(0);
            callback.accept(new SimpleTransactionStatus());
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        lenient().when(messageRepository.claimDispatchAttempt(
                anyLong(), any(), any(), anyInt(), any(), any()
        )).thenReturn(1);
        lenient().when(senderSettingService.resolveSenderNumberForDispatch(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void dispatchMessage_marksAcceptedProviderResultAsDispatched() throws Exception {
        SmsMessage message = message("0212345678");
        given(providerRegistry.provider()).willReturn(providerPort);
        given(providerPort.providerName()).willReturn("solapi");
        given(providerPort.send(any())).willReturn(new SmsProviderPort.SendResult(
                "solapi",
                "G4V_TEST",
                List.of(new SmsProviderPort.RecipientResult(0, "M_TEST_1", "2000", "정상 접수"))
        ));
        given(messageRepository.findById(7L)).willReturn(Optional.of(message));
        given(messageRepository.save(any(SmsMessage.class))).willAnswer(inv -> inv.getArgument(0));

        service.dispatchMessage(7L);

        assertThat(message.getStatus()).isEqualTo(SmsStatus.DISPATCHED);
        assertThat(message.getProvider()).isEqualTo("solapi");
        assertThat(message.getProviderGroupId()).isEqualTo("G4V_TEST");
        assertThat(message.getLastProviderStatus()).isEqualTo("DISPATCHED");
        assertThat(message.getRecipients()).singleElement().satisfies(recipient -> {
            assertThat(recipient.deliveryStatus()).isEqualTo(SmsStatus.DISPATCHED);
            assertThat(recipient.providerMessageId()).isEqualTo("M_TEST_1");
        });
        verify(messageRepository).claimDispatchAttempt(eq(7L), eq(SmsStatus.PENDING), eq("solapi"), eq(3), any(), any());
        verify(rlsTenantTransactionBinder, atLeast(2)).bindRequiredTenantToCurrentTransaction();
        ArgumentCaptor<SmsProviderPort.SendCommand> commandCaptor = ArgumentCaptor.forClass(SmsProviderPort.SendCommand.class);
        verify(providerPort).send(commandCaptor.capture());
        assertThat(commandCaptor.getValue().from()).isEqualTo("0212345678");
    }

    @Test
    void dispatchMessage_failsFastWhenTenantIdIsMissing() {
        TenantContextHolder.clear();
        given(providerRegistry.provider()).willReturn(providerPort);
        given(providerPort.providerName()).willReturn("solapi");
        doThrow(new IllegalStateException("Tenant ID is not set in context"))
                .when(rlsTenantTransactionBinder)
                .bindRequiredTenantToCurrentTransaction();

        assertThatThrownBy(() -> service.dispatchMessage(7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tenant ID is not set");
        verify(messageRepository, never()).claimDispatchAttempt(anyLong(), any(), any(), anyInt(), any(), any());
        verify(providerPort, never()).send(any());
    }

    @Test
    void dispatchMessage_marksDeliveredProviderResultAsSent() throws Exception {
        SmsMessage message = message("0212345678");
        given(providerRegistry.provider()).willReturn(providerPort);
        given(providerPort.providerName()).willReturn("mock");
        given(providerPort.send(any())).willReturn(new SmsProviderPort.SendResult(
                "mock",
                "mock-7",
                List.of(new SmsProviderPort.RecipientResult(0, "mock-7-0", "4000", "MOCK_DELIVERED"))
        ));
        given(messageRepository.findById(7L)).willReturn(Optional.of(message));
        given(messageRepository.save(any(SmsMessage.class))).willAnswer(inv -> inv.getArgument(0));

        service.dispatchMessage(7L);

        assertThat(message.getStatus()).isEqualTo(SmsStatus.SENT);
        assertThat(message.getSuccessCount()).isEqualTo(1);
        assertThat(message.getFailCount()).isZero();
        assertThat(message.getProviderGroupId()).isEqualTo("mock-7");
    }

    @Test
    void dispatchMessage_doesNotSendWhenAtomicClaimFails() {
        given(providerRegistry.provider()).willReturn(providerPort);
        given(providerPort.providerName()).willReturn("solapi");
        given(messageRepository.claimDispatchAttempt(anyLong(), any(), any(), anyInt(), any(), any()))
                .willReturn(0);

        service.dispatchMessage(7L);

        verify(providerPort, never()).send(any());
    }

    @Test
    void dispatchDueMessages_excludesFreshDispatchClaimsFromSelector() {
        Instant now = Instant.parse("2026-06-28T00:00:00Z");
        given(messageRepository.findDueDispatchIds(
                eq(SmsStatus.PENDING),
                eq(now),
                eq(3),
                eq(now.minus(java.time.Duration.ofMinutes(5))),
                any()
        )).willReturn(List.of());

        int count = service.dispatchDueMessages(now);

        assertThat(count).isZero();
        verify(messageRepository).findDueDispatchIds(
                eq(SmsStatus.PENDING),
                eq(now),
                eq(3),
                eq(now.minus(java.time.Duration.ofMinutes(5))),
                any()
        );
    }

    @Test
    void dispatchMessage_mapsSolapiFailureCodeAsFailed() throws Exception {
        SmsMessage message = message("0212345678");
        given(providerRegistry.provider()).willReturn(providerPort);
        given(providerPort.providerName()).willReturn("solapi");
        given(providerPort.send(any())).willReturn(new SmsProviderPort.SendResult(
                "solapi",
                "G4V_TEST",
                List.of(new SmsProviderPort.RecipientResult(0, "M_TEST_1", "2062", "잔액 부족"))
        ));
        given(messageRepository.findById(7L)).willReturn(Optional.of(message));
        given(messageRepository.save(any(SmsMessage.class))).willAnswer(inv -> inv.getArgument(0));

        service.dispatchMessage(7L);

        assertThat(message.getStatus()).isEqualTo(SmsStatus.FAILED);
        assertThat(message.getSuccessCount()).isZero();
        assertThat(message.getFailCount()).isEqualTo(1);
        assertThat(message.getRecipients()).singleElement().satisfies(recipient -> {
            assertThat(recipient.deliveryStatus()).isEqualTo(SmsStatus.FAILED);
            assertThat(recipient.providerStatusCode()).isEqualTo("2062");
        });
    }

    @Test
    void applyWebhookReport_verifiesStoredProviderIdentifiersBeforeUpdating() throws Exception {
        SmsMessage message = dispatchedMessage("0212345678");
        given(messageRepository.findByProviderGroupId("G4V_TEST")).willReturn(Optional.of(message));

        assertThatThrownBy(() -> service.applyWebhookReport(new SmsDispatchService.SolapiWebhookReport(
                7L,
                0,
                "M_OTHER",
                "G4V_TEST",
                "4000",
                "수신 완료"
        ))).isInstanceOf(com.lumie.common.exception.BusinessException.class);

        assertThat(message.getStatus()).isEqualTo(SmsStatus.DISPATCHED);
        assertThat(message.getRecipients()).singleElement()
                .extracting(Recipient::deliveryStatus)
                .isEqualTo(SmsStatus.DISPATCHED);
    }

    @Test
    void applyWebhookReport_fallsBackToMessageIdWhenProviderGroupWasNotStored() throws Exception {
        SmsMessage message = dispatchedMessageWithoutProviderGroup("0212345678");
        given(messageRepository.findByProviderGroupId("G4V_LIVE")).willReturn(Optional.empty());
        given(messageRepository.findById(7L)).willReturn(Optional.of(message));
        given(messageRepository.save(any(SmsMessage.class))).willAnswer(inv -> inv.getArgument(0));

        service.applyWebhookReport(new SmsDispatchService.SolapiWebhookReport(
                7L,
                0,
                "M_TEST_1",
                "G4V_LIVE",
                "4000",
                "수신 완료"
        ));

        assertThat(message.getStatus()).isEqualTo(SmsStatus.SENT);
        assertThat(message.getProviderGroupId()).isEqualTo("G4V_LIVE");
        assertThat(message.getSuccessCount()).isEqualTo(1);
        assertThat(message.getRecipients()).singleElement().satisfies(recipient -> {
            assertThat(recipient.deliveryStatus()).isEqualTo(SmsStatus.SENT);
            assertThat(recipient.providerStatusCode()).isEqualTo("4000");
        });
    }

    @Test
    void applyWebhookReport_preservesStoredProviderGroupWhenCallbackOmitsGroup() throws Exception {
        SmsMessage message = dispatchedMessage("0212345678");
        given(messageRepository.findById(7L)).willReturn(Optional.of(message));
        given(messageRepository.save(any(SmsMessage.class))).willAnswer(inv -> inv.getArgument(0));

        service.applyWebhookReport(new SmsDispatchService.SolapiWebhookReport(
                7L,
                0,
                "M_TEST_1",
                null,
                "4000",
                "수신 완료"
        ));

        assertThat(message.getStatus()).isEqualTo(SmsStatus.SENT);
        assertThat(message.getProviderGroupId()).isEqualTo("G4V_TEST");
    }

    @Test
    void applyWebhookReport_ignoresStaleCallbackAfterTerminalState() throws Exception {
        SmsMessage message = dispatchedMessage("0212345678");
        given(messageRepository.findByProviderGroupId("G4V_TEST")).willReturn(Optional.of(message));
        given(messageRepository.save(any(SmsMessage.class))).willAnswer(inv -> inv.getArgument(0));

        service.applyWebhookReport(new SmsDispatchService.SolapiWebhookReport(
                7L,
                0,
                "M_TEST_1",
                "G4V_TEST",
                "4000",
                "수신 완료"
        ));
        service.applyWebhookReport(new SmsDispatchService.SolapiWebhookReport(
                7L,
                0,
                "M_TEST_1",
                "G4V_TEST",
                "3000",
                "통신사 처리 중"
        ));

        assertThat(message.getStatus()).isEqualTo(SmsStatus.SENT);
        assertThat(message.getRecipients()).singleElement()
                .extracting(Recipient::deliveryStatus)
                .isEqualTo(SmsStatus.SENT);
    }

    @Test
    void dispatchMessage_usesApprovedSenderNumberFallbackForLegacyRows() throws Exception {
        SmsMessage message = message(null);
        given(providerRegistry.provider()).willReturn(providerPort);
        given(providerPort.providerName()).willReturn("solapi");
        given(senderSettingService.resolveSenderNumberForDispatch(null)).willReturn("029998888");
        given(providerPort.send(any())).willReturn(new SmsProviderPort.SendResult(
                "solapi",
                "G4V_TEST",
                List.of(new SmsProviderPort.RecipientResult(0, "M_TEST_1", "2000", "정상 접수"))
        ));
        given(messageRepository.findById(7L)).willReturn(Optional.of(message));
        given(messageRepository.save(any(SmsMessage.class))).willAnswer(inv -> inv.getArgument(0));

        service.dispatchMessage(7L);

        ArgumentCaptor<SmsProviderPort.SendCommand> commandCaptor = ArgumentCaptor.forClass(SmsProviderPort.SendCommand.class);
        verify(providerPort).send(commandCaptor.capture());
        assertThat(commandCaptor.getValue().from()).isEqualTo("029998888");
    }

    @Test
    void dispatchMessage_recordsFailureWhenLegacyFallbackHasNoApprovedSender() throws Exception {
        SmsMessage message = message(null);
        given(providerRegistry.provider()).willReturn(providerPort);
        given(providerPort.providerName()).willReturn("solapi");
        given(senderSettingService.resolveSenderNumberForDispatch(null)).willThrow(new BusinessException(
                SmsErrorCode.SENDER_SETTING_NOT_APPROVED,
                "Approved SMS sender number is required before sending"
        ));
        given(messageRepository.findById(7L)).willReturn(Optional.of(message));
        given(messageRepository.save(any(SmsMessage.class))).willAnswer(inv -> inv.getArgument(0));

        service.dispatchMessage(7L);

        verify(providerPort, never()).send(any());
        assertThat(message.getStatus()).isEqualTo(SmsStatus.PENDING);
        assertThat(message.getProvider()).isEqualTo("solapi");
        assertThat(message.getLastProviderStatus()).isEqualTo("FAILED_RETRYABLE");
        assertThat(message.getLastProviderError()).contains("Approved SMS sender number");
    }

    @Test
    void dispatchMessage_mockProviderAllowsNullSenderNumber() throws Exception {
        SmsMessage message = message(null);
        given(providerRegistry.provider()).willReturn(providerPort);
        given(providerPort.providerName()).willReturn("mock");
        given(providerPort.send(any())).willReturn(new SmsProviderPort.SendResult(
                "mock",
                "mock-7",
                List.of(new SmsProviderPort.RecipientResult(0, "mock-7-0", "4000", "MOCK_DELIVERED"))
        ));
        given(messageRepository.findById(7L)).willReturn(Optional.of(message));
        given(messageRepository.save(any(SmsMessage.class))).willAnswer(inv -> inv.getArgument(0));

        service.dispatchMessage(7L);

        ArgumentCaptor<SmsProviderPort.SendCommand> commandCaptor = ArgumentCaptor.forClass(SmsProviderPort.SendCommand.class);
        verify(providerPort).send(commandCaptor.capture());
        assertThat(commandCaptor.getValue().from()).isNull();
        verify(senderSettingService, never()).resolveSenderNumberForDispatch(any());
    }

    private SmsMessage message(String senderNumber) throws Exception {
        SmsMessage message = SmsMessage.create(
                10L,
                senderNumber,
                RecipientType.PARENT,
                List.of(Recipient.pending("01012345678", "홍길동", 100L, null, RecipientRole.PARENT, "안내")),
                "제목",
                "안내",
                null,
                null,
                SmsMessageType.SMS,
                1,
                20,
                20
        );
        Field id = SmsMessage.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(message, 7L);
        return message;
    }

    private SmsMessage dispatchedMessage(String senderNumber) throws Exception {
        SmsMessage message = message(senderNumber);
        message.markAsDispatched(
                "solapi",
                "G4V_TEST",
                List.of(message.getRecipients().get(0).dispatched("M_TEST_1", "2000", "정상 접수")),
                "DISPATCHED"
        );
        return message;
    }

    private SmsMessage dispatchedMessageWithoutProviderGroup(String senderNumber) throws Exception {
        SmsMessage message = message(senderNumber);
        message.markAsDispatched(
                "solapi",
                null,
                List.of(message.getRecipients().get(0).dispatched("M_TEST_1", "2000", "정상 접수")),
                "DISPATCHED"
        );
        return message;
    }
}
