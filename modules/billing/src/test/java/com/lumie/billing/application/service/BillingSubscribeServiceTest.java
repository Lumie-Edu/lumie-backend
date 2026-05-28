package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.request.SubscribeRequest;
import com.lumie.billing.application.dto.response.SubscribeResponse;
import com.lumie.billing.application.port.out.PaymentGatewayPort;
import com.lumie.billing.application.port.out.PaymentGatewayPort.BillingKeyIssueResult;
import com.lumie.billing.application.port.out.PlanPersistencePort;
import com.lumie.billing.application.service.SubscriptionChargeExecutor.ChargeOutcome;
import com.lumie.billing.domain.entity.BillingKey;
import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.entity.Plan;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import com.lumie.billing.domain.vo.Money;
import com.lumie.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingSubscribeService")
class BillingSubscribeServiceTest {

    private static final Long TENANT_ID = 15L;
    private static final String TENANT_SLUG = "demo";
    private static final String IDEMPOTENCY_KEY = "subscribe-01";
    private static final String AUTH_KEY = "bln_abc";
    private static final String CUSTOMER_KEY = "lumie-tenant-15";

    @Mock private PaymentGatewayPort paymentGatewayPort;
    @Mock private PlanPersistencePort planPersistencePort;
    @Mock private BillingKeyPersistenceHelper billingKeyPersistenceHelper;
    @Mock private SubscriptionPersistenceHelper subscriptionPersistenceHelper;
    @Mock private SubscriptionChargeExecutor chargeExecutor;
    @Mock private PaymentTransactionLogger transactionLogger;

    @InjectMocks private BillingSubscribeService service;

    private Plan paidPlan;
    private Plan freePlan;

    @BeforeEach
    void setUp() {
        paidPlan = Plan.builder()
                .id("PRO").name("Pro")
                .monthlyPrice(Money.of(99_000L))
                .displayOrder(2).build();
        freePlan = Plan.builder()
                .id("FREE").name("Free")
                .monthlyPrice(Money.zero())
                .displayOrder(0).build();
    }

    @Test
    @DisplayName("happy path: issue → replaceActive → attach+changePlan → charge PAID → SubscribeResponse")
    void subscribe_success() {
        SubscribeRequest req = new SubscribeRequest(AUTH_KEY, CUSTOMER_KEY, "PRO");

        given(planPersistencePort.findById("PRO")).willReturn(Optional.of(paidPlan));
        given(paymentGatewayPort.issueBillingKey(AUTH_KEY, CUSTOMER_KEY))
                .willReturn(new BillingKeyIssueResult(true, "pg_key_xyz",
                        "1234-****-****-5678", "현대", CUSTOMER_KEY, null));

        BillingKey savedKey = BillingKey.register(TENANT_ID, "pg_key_xyz", CUSTOMER_KEY,
                "1234-****-****-5678", "현대");
        ReflectionTestUtils.setField(savedKey, "id", 99L);
        given(billingKeyPersistenceHelper.replaceActive(any(BillingKey.class))).willReturn(savedKey);

        Subscription upgraded = Subscription.create(TENANT_ID, TENANT_SLUG, paidPlan,
                Instant.now().plusSeconds(86_400));
        ReflectionTestUtils.setField(upgraded, "id", 1L);
        upgraded.attachBillingKey(99L);
        given(subscriptionPersistenceHelper.attachBillingKeyAndUpgrade(TENANT_SLUG, 99L, paidPlan, null))
                .willReturn(upgraded);

        Invoice paidInvoice = Invoice.create("INV-15-20260528-AB",
                1L, TENANT_ID, Money.of(99_000L), Money.zero(),
                "Pro 월정액", Instant.now(),
                Instant.now().plusSeconds(86_400), Instant.now().plusSeconds(86_400),
                "SUB-1-xyz");
        ReflectionTestUtils.setField(paidInvoice, "id", 500L);
        paidInvoice.markPaid("toss_payment_key", Instant.now());
        given(chargeExecutor.charge(upgraded)).willReturn(ChargeOutcome.paid(paidInvoice));

        SubscribeResponse response = service.subscribe(TENANT_ID, TENANT_SLUG, req, IDEMPOTENCY_KEY);

        assertThat(response.subscription().planId()).isEqualTo("PRO");
        assertThat(response.billingKey().id()).isEqualTo(99L);
        assertThat(response.invoice().id()).isEqualTo(500L);
        verify(billingKeyPersistenceHelper).replaceActive(any(BillingKey.class));
        verify(subscriptionPersistenceHelper).attachBillingKeyAndUpgrade(TENANT_SLUG, 99L, paidPlan, null);
        verify(chargeExecutor).charge(upgraded);
    }

    @Test
    @DisplayName("plan id FREE → INVALID_SUBSCRIPTION_STATE (subscribe is for paid only)")
    void subscribe_freePlan_rejected() {
        given(planPersistencePort.findById("FREE")).willReturn(Optional.of(freePlan));

        assertThatThrownBy(() -> service.subscribe(TENANT_ID, TENANT_SLUG,
                new SubscribeRequest(AUTH_KEY, CUSTOMER_KEY, "FREE"), IDEMPOTENCY_KEY))
                .isInstanceOf(BillingException.class)
                .hasFieldOrPropertyWithValue("errorCode", BillingErrorCode.INVALID_SUBSCRIPTION_STATE);

        verify(paymentGatewayPort, never()).issueBillingKey(anyString(), anyString());
        verify(billingKeyPersistenceHelper, never()).replaceActive(any());
    }

    @Test
    @DisplayName("plan id missing → ResourceNotFoundException, no PG call")
    void subscribe_planMissing() {
        given(planPersistencePort.findById("BOGUS")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.subscribe(TENANT_ID, TENANT_SLUG,
                new SubscribeRequest(AUTH_KEY, CUSTOMER_KEY, "BOGUS"), IDEMPOTENCY_KEY))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(paymentGatewayPort, never()).issueBillingKey(anyString(), anyString());
    }

    @Test
    @DisplayName("Toss issueBillingKey 실패 → TOSS_API_ERROR, DB 변경 없음")
    void subscribe_tossIssueFailed() {
        SubscribeRequest req = new SubscribeRequest(AUTH_KEY, CUSTOMER_KEY, "PRO");
        given(planPersistencePort.findById("PRO")).willReturn(Optional.of(paidPlan));
        given(paymentGatewayPort.issueBillingKey(AUTH_KEY, CUSTOMER_KEY))
                .willReturn(new BillingKeyIssueResult(false, null, null, null, CUSTOMER_KEY,
                        "AUTH_KEY_EXPIRED"));

        assertThatThrownBy(() -> service.subscribe(TENANT_ID, TENANT_SLUG, req, IDEMPOTENCY_KEY))
                .isInstanceOf(BillingException.class)
                .hasFieldOrPropertyWithValue("errorCode", BillingErrorCode.TOSS_API_ERROR);

        verify(billingKeyPersistenceHelper, never()).replaceActive(any());
        verify(subscriptionPersistenceHelper, never())
                .attachBillingKeyAndUpgrade(anyString(), any(), any(), any());
        verify(chargeExecutor, never()).charge(any());
    }

    @Test
    @DisplayName("PG charge FAILED → PAYMENT_FAILED 402, subscription은 새 plan으로 commit된 상태 유지")
    void subscribe_chargeFailed() {
        SubscribeRequest req = new SubscribeRequest(AUTH_KEY, CUSTOMER_KEY, "PRO");
        given(planPersistencePort.findById("PRO")).willReturn(Optional.of(paidPlan));
        given(paymentGatewayPort.issueBillingKey(AUTH_KEY, CUSTOMER_KEY))
                .willReturn(new BillingKeyIssueResult(true, "pg_key", "1234-****-****-5678",
                        "현대", CUSTOMER_KEY, null));

        BillingKey savedKey = BillingKey.register(TENANT_ID, "pg_key", CUSTOMER_KEY,
                "1234-****-****-5678", "현대");
        ReflectionTestUtils.setField(savedKey, "id", 99L);
        given(billingKeyPersistenceHelper.replaceActive(any())).willReturn(savedKey);

        Subscription upgraded = Subscription.create(TENANT_ID, TENANT_SLUG, paidPlan, null);
        ReflectionTestUtils.setField(upgraded, "id", 1L);
        upgraded.attachBillingKey(99L);
        given(subscriptionPersistenceHelper.attachBillingKeyAndUpgrade(eq(TENANT_SLUG), eq(99L), any(), any()))
                .willReturn(upgraded);

        given(chargeExecutor.charge(upgraded)).willReturn(ChargeOutcome.failed("INVALID_CARD"));

        assertThatThrownBy(() -> service.subscribe(TENANT_ID, TENANT_SLUG, req, IDEMPOTENCY_KEY))
                .isInstanceOf(BillingException.class)
                .hasFieldOrPropertyWithValue("errorCode", BillingErrorCode.PAYMENT_FAILED)
                .hasMessageContaining("INVALID_CARD");

        // The earlier DB mutations DID commit (helper-scoped tx). The scheduler
        // will retry the charge at the next billing cycle.
        verify(billingKeyPersistenceHelper).replaceActive(any());
        verify(subscriptionPersistenceHelper).attachBillingKeyAndUpgrade(eq(TENANT_SLUG), eq(99L), any(), any());
    }
}
