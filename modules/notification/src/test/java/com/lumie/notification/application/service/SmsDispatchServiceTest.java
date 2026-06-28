package com.lumie.notification.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.notification.adapter.out.config.SmsProperties;
import com.lumie.notification.application.port.out.SmsProviderPort;
import com.lumie.notification.domain.entity.SmsMessage;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmsDispatchServiceTest {

    @Mock private SmsMessageRepository messageRepository;
    @Mock private SmsProviderRegistry providerRegistry;
    @Mock private SmsProviderPort providerPort;
    @Mock private TransactionTemplate transactionTemplate;

    private SmsDispatchService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenant("inst-test");
        TenantContextHolder.setTenantId(1L);
        SmsProperties properties = new SmsProperties("mock", "0212345678", 50, 3, null);
        service = new SmsDispatchService(messageRepository, providerRegistry, properties, transactionTemplate);
        given(transactionTemplate.execute(any())).willAnswer(inv -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        });
        org.mockito.Mockito.doAnswer(inv -> {
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> callback = inv.getArgument(0);
            callback.accept(new SimpleTransactionStatus());
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void dispatchMessage_marksAcceptedProviderResultAsDispatched() throws Exception {
        SmsMessage message = message();
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
        assertThat(message.getDispatchAttemptCount()).isEqualTo(1);
        assertThat(message.getRecipients()).singleElement().satisfies(recipient -> {
            assertThat(recipient.deliveryStatus()).isEqualTo(SmsStatus.DISPATCHED);
            assertThat(recipient.providerMessageId()).isEqualTo("M_TEST_1");
        });
        verify(providerPort).send(any());
    }

    @Test
    void dispatchMessage_marksDeliveredProviderResultAsSent() throws Exception {
        SmsMessage message = message();
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

    private SmsMessage message() throws Exception {
        SmsMessage message = SmsMessage.create(
                10L,
                RecipientType.GUARDIAN,
                List.of(Recipient.pending("01012345678", "홍길동", 100L, null, RecipientRole.GUARDIAN, "안내")),
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
}
