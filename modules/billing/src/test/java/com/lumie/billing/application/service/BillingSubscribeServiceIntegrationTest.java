package com.lumie.billing.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.billing.adapter.out.persistence.BillingKeyJpaRepository;
import com.lumie.billing.adapter.out.persistence.BillingKeyPersistenceAdapter;
import com.lumie.billing.adapter.out.persistence.InvoiceJpaRepository;
import com.lumie.billing.adapter.out.persistence.PaymentTransactionJpaRepository;
import com.lumie.billing.adapter.out.persistence.PaymentTransactionPersistenceAdapter;
import com.lumie.billing.adapter.out.persistence.PlanJpaRepository;
import com.lumie.billing.adapter.out.persistence.PlanPersistenceAdapter;
import com.lumie.billing.adapter.out.persistence.SubscriptionJpaRepository;
import com.lumie.billing.adapter.out.persistence.SubscriptionPersistenceAdapter;
import com.lumie.billing.application.dto.request.SubscribeRequest;
import com.lumie.billing.application.dto.response.SubscribeResponse;
import com.lumie.billing.application.port.out.PaymentGatewayPort;
import com.lumie.billing.application.port.out.PaymentGatewayPort.BillingKeyIssueResult;
import com.lumie.billing.application.service.SubscriptionChargeExecutor.ChargeOutcome;
import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.entity.Plan;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import com.lumie.billing.domain.vo.Money;
import com.lumie.billing.domain.vo.SubscriptionStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Integration test for {@link BillingSubscribeService} with REAL persistence.
 *
 * <p>Phase E follow-up: the prior mock-based {@code BillingSubscribeServiceTest}
 * silently passed when {@code SubscriptionPersistenceHelper} threw on a
 * null {@code nextChargeAt} contract violation — the helper was mocked so
 * the contract was never exercised. This test wires real
 * helper/persistence/lock beans and only mocks the two adapter ports that
 * cross the network boundary ({@link PaymentGatewayPort},
 * {@link SubscriptionChargeExecutor}). Any future caller↔helper contract
 * drift will surface here.
 */
@Tag("integration")
@Testcontainers
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DataJpaTest(
        excludeAutoConfiguration = FlywayAutoConfiguration.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
        }
)
@Import({
        SubscriptionPersistenceAdapter.class,
        BillingKeyPersistenceAdapter.class,
        PlanPersistenceAdapter.class,
        PaymentTransactionPersistenceAdapter.class,
        SubscriptionPersistenceHelper.class,
        BillingKeyPersistenceHelper.class,
        BillingOperationLockService.class,
        PaymentTransactionLogger.class,
        BillingSubscribeService.class,
        BillingSubscribeServiceIntegrationTest.MockedPortsConfig.class,
        BillingSubscribeServiceIntegrationTest.JpaAuditingTestConfig.class
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DisplayName("BillingSubscribeService integration — real persistence, mocked PG")
class BillingSubscribeServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @PersistenceContext
    private EntityManager em;

    private final TransactionTemplate txTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final BillingSubscribeService billingSubscribeService;
    private final PaymentGatewayPort paymentGatewayPort;
    private final SubscriptionChargeExecutor chargeExecutor;
    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final BillingKeyJpaRepository billingKeyJpaRepository;

    private static final Long TENANT_ID = 42L;
    private static final String TENANT_SLUG = "academy-integration";

    BillingSubscribeServiceIntegrationTest(
            TransactionTemplate txTemplate,
            JdbcTemplate jdbcTemplate,
            BillingSubscribeService billingSubscribeService,
            PaymentGatewayPort paymentGatewayPort,
            SubscriptionChargeExecutor chargeExecutor,
            SubscriptionJpaRepository subscriptionJpaRepository,
            BillingKeyJpaRepository billingKeyJpaRepository) {
        this.txTemplate = txTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.billingSubscribeService = billingSubscribeService;
        this.paymentGatewayPort = paymentGatewayPort;
        this.chargeExecutor = chargeExecutor;
        this.subscriptionJpaRepository = subscriptionJpaRepository;
        this.billingKeyJpaRepository = billingKeyJpaRepository;
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(paymentGatewayPort, chargeExecutor);
        // billing_operation_locks is not a JPA entity — it's a raw SQL table
        // created by Flyway V38. This test disables Flyway (uses Hibernate
        // create-drop on entities) so we create the lock table by hand.
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS public.billing_operation_locks (
                    tenant_id        BIGINT       NOT NULL,
                    operation_type   VARCHAR(50)  NOT NULL,
                    idempotency_key  VARCHAR(128) NOT NULL,
                    acquired_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
                    CONSTRAINT billing_operation_locks_pkey PRIMARY KEY (tenant_id, operation_type)
                )
                """);
        // Seed PRO plan inside a committed transaction so subscribe() can find it
        txTemplate.executeWithoutResult(tx -> {
            Plan pro = Plan.builder()
                    .id("PRO").name("Pro")
                    .monthlyPrice(Money.of(99_000L))
                    .yearlyPrice(Money.of(990_000L))
                    .displayOrder(2).build();
            em.persist(pro);
        });
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute(
                "TRUNCATE billing_operation_locks, payment_transactions, invoices, "
                        + "subscriptions, billing_keys, plans RESTART IDENTITY CASCADE");
    }

    // ─── C-flow: Trial → Paid ──────────────────────────────────────────────────

    @Test
    @DisplayName("C-flow: Trial → Paid persists billing_key + updates subscription end-to-end")
    void subscribe_fromTrial_persistsBillingKeyAndPaidSubscription() {
        Subscription trial = persistTrial();
        stubIssueBillingKeySuccess();
        stubChargePaid();

        SubscribeResponse response = billingSubscribeService.subscribe(
                TENANT_ID, TENANT_SLUG,
                new SubscribeRequest("auth_abc", "lumie-tenant-42", "PRO"),
                "key-c-flow-01-trial-to-pro-001"  // 32 chars matches header policy
        );

        assertThat(response.subscription().planId()).isEqualTo("PRO");
        Subscription persisted = subscriptionJpaRepository.findById(trial.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(persisted.getBillingKeyId()).isNotNull();
        assertThat(persisted.getExpiresAt()).isNull();   // trial expiry cleared
        assertThat(persisted.getNextChargeAt()).isNotNull(); // initial stamp from orchestrator
        assertThat(billingKeyJpaRepository.count()).isEqualTo(1);
    }

    // ─── D-flow: Cancelled → Paid fresh start ──────────────────────────────────

    @Test
    @DisplayName("D-flow: CANCELLED → Paid resets startedAt and clears cancelledAt")
    void subscribe_fromCancelled_freshStart() {
        Subscription cancelled = persistTrial();
        // Manually transition to CANCELLED via DB so the test does not depend on
        // the full cancel→finalize flow (covered by the scheduler integration test).
        txTemplate.executeWithoutResult(tx -> jdbcTemplate.update(
                "UPDATE subscriptions SET status = 'CANCELLED', cancelled_at = now() WHERE id = ?",
                cancelled.getId()));
        Instant beforeSubscribe = Instant.now();
        stubIssueBillingKeySuccess();
        stubChargePaid();

        billingSubscribeService.subscribe(
                TENANT_ID, TENANT_SLUG,
                new SubscribeRequest("auth_xyz", "lumie-tenant-42", "PRO"),
                "key-d-flow-cancelled-to-pro-001"
        );

        Subscription persisted = subscriptionJpaRepository.findById(cancelled.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(persisted.getCancelledAt()).isNull();
        assertThat(persisted.getBillingKeyId()).isNotNull();
        assertThat(persisted.getStartedAt()).isAfterOrEqualTo(beforeSubscribe); // fresh start
    }

    // Concurrency (operation lock) and free-plan refusal are covered by the
    // existing unit tests (BillingOperationLockServiceTest's 100-thread race,
    // BillingSubscribeServiceTest#subscribe_freePlan_rejected). This file's
    // value is the helper-contract verification with real persistence — kept
    // to two cases so Spring context startup cost stays proportionate.

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Subscription persistTrial() {
        return txTemplate.execute(tx -> {
            Plan proPlan = em.find(Plan.class, "PRO");
            Subscription trial = Subscription.createTrial(TENANT_ID, TENANT_SLUG, proPlan);
            em.persist(trial);
            em.flush();
            return trial;
        });
    }

    private void stubIssueBillingKeySuccess() {
        given(paymentGatewayPort.issueBillingKey(anyString(), anyString()))
                .willReturn(new BillingKeyIssueResult(true, "pg_key_xyz",
                        "1234-****-****-5678", "현대", "lumie-tenant-42", null));
    }

    private void stubChargePaid() {
        Invoice fakeInvoice = Invoice.create("INV-42-20260528-AB",
                999L, TENANT_ID, Money.of(99_000L), Money.zero(),
                "Pro 월정액", Instant.now(),
                Instant.now().plusSeconds(86_400),
                Instant.now().plusSeconds(86_400),
                "SUB-999-xyz");
        fakeInvoice.markPaid("toss_payment_key", Instant.now());
        given(chargeExecutor.charge(any(Subscription.class)))
                .willReturn(ChargeOutcome.paid(fakeInvoice));
    }

    // ─── Test-only @Configuration: mocked outbound ports + JPA auditing ────────

    static class MockedPortsConfig {
        @Bean PaymentGatewayPort paymentGatewayPort() { return Mockito.mock(PaymentGatewayPort.class); }
        @Bean SubscriptionChargeExecutor subscriptionChargeExecutor() { return Mockito.mock(SubscriptionChargeExecutor.class); }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
    }

    @EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
    static class JpaAuditingTestConfig {
        @Bean DateTimeProvider auditingDateTimeProvider() {
            return () -> java.util.Optional.of(Instant.now());
        }
    }
}
