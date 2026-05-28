package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.response.InvoiceResponse;
import com.lumie.billing.application.port.out.InvoicePersistencePort;
import com.lumie.billing.application.port.out.PlanPersistencePort;
import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.application.service.SubscriptionChargeExecutor.ChargeOutcome;
import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.entity.Plan;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import com.lumie.billing.domain.vo.InvoiceStatus;
import com.lumie.billing.domain.vo.Money;
import com.lumie.common.exception.ResourceNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionCommandService.chargeFirstInvoice")
class SubscriptionCommandServiceChargeFirstInvoiceTest {

    private static final String TENANT_SLUG = "academy-a";
    private static final Long TENANT_ID = 42L;
    private static final Long SUBSCRIPTION_ID = 101L;
    private static final Long BILLING_KEY_ID = 999L;

    @Mock private SubscriptionPersistencePort subscriptionPersistencePort;
    @Mock private PlanPersistencePort planPersistencePort;
    @Mock private InvoicePersistencePort invoicePersistencePort;
    @Mock private SubscriptionChargeExecutor chargeExecutor;
    @Mock private com.lumie.billing.application.port.out.BillingConfigPort billingConfig;

    @InjectMocks private SubscriptionCommandService service;

    private Subscription paidSubscription;
    private Plan paidPlan;

    @BeforeEach
    void setUp() {
        paidPlan = Plan.builder()
                .id("PRO")
                .name("Pro")
                .monthlyPrice(Money.of(29_000L))
                .displayOrder(1)
                .build();

        paidSubscription = Subscription.create(TENANT_ID, TENANT_SLUG, paidPlan, Instant.now().plusSeconds(86_400));
        ReflectionTestUtils.setField(paidSubscription, "id", SUBSCRIPTION_ID);
        paidSubscription.attachBillingKey(BILLING_KEY_ID);
    }

    @Test
    @DisplayName("성공: 외부 PG 결제 성공 → InvoiceResponse(PAID) 반환")
    void chargeFirstInvoice_success() {
        Invoice paidInvoice = Invoice.create(
                "INV-42-20260527-ABCDEF",
                SUBSCRIPTION_ID, TENANT_ID,
                Money.of(29_000L), Money.zero(),
                "Pro 월정액",
                Instant.now(), Instant.now().plusSeconds(86_400),
                Instant.now().plusSeconds(86_400),
                "SUB-101-xyz");
        ReflectionTestUtils.setField(paidInvoice, "id", 500L);
        paidInvoice.markPaid("toss_payment_key_abc", Instant.now());

        given(subscriptionPersistencePort.findActiveByTenantSlug(TENANT_SLUG))
                .willReturn(Optional.of(paidSubscription));
        given(invoicePersistencePort.findBySubscriptionId(SUBSCRIPTION_ID))
                .willReturn(List.of());
        given(chargeExecutor.charge(paidSubscription))
                .willReturn(ChargeOutcome.paid(paidInvoice));

        InvoiceResponse response = service.chargeFirstInvoice(TENANT_SLUG);

        assertThat(response.status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(response.amount()).isEqualTo(29_000L);
        assertThat(response.paymentKey()).isEqualTo("toss_payment_key_abc");
        verify(chargeExecutor).charge(paidSubscription);
    }

    @Test
    @DisplayName("멱등: 이미 PAID Invoice 존재 → 기존 invoice 반환, PG 호출 없음")
    void chargeFirstInvoice_alreadyPaid_returnsExisting() {
        Invoice existing = Invoice.create(
                "INV-42-20260520-OLD",
                SUBSCRIPTION_ID, TENANT_ID,
                Money.of(29_000L), Money.zero(),
                "Pro 월정액",
                Instant.now().minusSeconds(3600), Instant.now().plusSeconds(86_400),
                Instant.now().plusSeconds(86_400),
                "SUB-101-old");
        ReflectionTestUtils.setField(existing, "id", 400L);
        existing.markPaid("toss_payment_key_old", Instant.now().minusSeconds(3000));

        given(subscriptionPersistencePort.findActiveByTenantSlug(TENANT_SLUG))
                .willReturn(Optional.of(paidSubscription));
        given(invoicePersistencePort.findBySubscriptionId(SUBSCRIPTION_ID))
                .willReturn(List.of(existing));

        InvoiceResponse response = service.chargeFirstInvoice(TENANT_SLUG);

        assertThat(response.id()).isEqualTo(400L);
        assertThat(response.status()).isEqualTo(InvoiceStatus.PAID);
        verify(chargeExecutor, never()).charge(any());
    }

    @Test
    @DisplayName("실패: BillingKey 미연결 → INVALID_SUBSCRIPTION_STATE 예외")
    void chargeFirstInvoice_noBillingKey() {
        Subscription noKey = Subscription.create(TENANT_ID, TENANT_SLUG, paidPlan, Instant.now().plusSeconds(86_400));
        ReflectionTestUtils.setField(noKey, "id", SUBSCRIPTION_ID);
        // do NOT attachBillingKey
        given(subscriptionPersistencePort.findActiveByTenantSlug(TENANT_SLUG))
                .willReturn(Optional.of(noKey));

        assertThatThrownBy(() -> service.chargeFirstInvoice(TENANT_SLUG))
                .isInstanceOf(BillingException.class)
                .hasFieldOrPropertyWithValue("errorCode", BillingErrorCode.INVALID_SUBSCRIPTION_STATE);
        verify(chargeExecutor, never()).charge(any());
    }

    @Test
    @DisplayName("실패: executor가 SKIPPED 반환 → INVALID_SUBSCRIPTION_STATE 예외 (reason 포함)")
    void chargeFirstInvoice_executorSkipped() {
        given(subscriptionPersistencePort.findActiveByTenantSlug(TENANT_SLUG))
                .willReturn(Optional.of(paidSubscription));
        given(invoicePersistencePort.findBySubscriptionId(SUBSCRIPTION_ID))
                .willReturn(List.of());
        given(chargeExecutor.charge(paidSubscription))
                .willReturn(ChargeOutcome.skipped("BILLING_KEY_REVOKED"));

        assertThatThrownBy(() -> service.chargeFirstInvoice(TENANT_SLUG))
                .isInstanceOf(BillingException.class)
                .hasFieldOrPropertyWithValue("errorCode", BillingErrorCode.INVALID_SUBSCRIPTION_STATE)
                .hasMessageContaining("BILLING_KEY_REVOKED");
    }

    @Test
    @DisplayName("실패: Free plan 구독 → INVALID_SUBSCRIPTION_STATE 예외 (결제 불필요)")
    void chargeFirstInvoice_freePlan_rejected() {
        Plan freePlan = Plan.builder()
                .id("FREE")
                .name("Free")
                .monthlyPrice(Money.zero())
                .displayOrder(0)
                .build();
        Subscription free = Subscription.createFree(TENANT_ID, TENANT_SLUG, freePlan);
        ReflectionTestUtils.setField(free, "id", SUBSCRIPTION_ID);
        given(subscriptionPersistencePort.findActiveByTenantSlug(TENANT_SLUG))
                .willReturn(Optional.of(free));

        assertThatThrownBy(() -> service.chargeFirstInvoice(TENANT_SLUG))
                .isInstanceOf(BillingException.class)
                .hasFieldOrPropertyWithValue("errorCode", BillingErrorCode.INVALID_SUBSCRIPTION_STATE);
        verify(chargeExecutor, never()).charge(any());
    }

    @Test
    @DisplayName("실패: Toss PG 실패 → PAYMENT_FAILED 예외")
    void chargeFirstInvoice_pgFailure() {
        given(subscriptionPersistencePort.findActiveByTenantSlug(TENANT_SLUG))
                .willReturn(Optional.of(paidSubscription));
        given(invoicePersistencePort.findBySubscriptionId(SUBSCRIPTION_ID))
                .willReturn(List.of());
        given(chargeExecutor.charge(paidSubscription))
                .willReturn(ChargeOutcome.failed("INVALID_CARD"));

        assertThatThrownBy(() -> service.chargeFirstInvoice(TENANT_SLUG))
                .isInstanceOf(BillingException.class)
                .hasFieldOrPropertyWithValue("errorCode", BillingErrorCode.PAYMENT_FAILED)
                .hasMessageContaining("INVALID_CARD");
    }

    @Test
    @DisplayName("실패: 활성 구독 없음 → ResourceNotFoundException")
    void chargeFirstInvoice_subscriptionMissing() {
        given(subscriptionPersistencePort.findActiveByTenantSlug(TENANT_SLUG))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.chargeFirstInvoice(TENANT_SLUG))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
