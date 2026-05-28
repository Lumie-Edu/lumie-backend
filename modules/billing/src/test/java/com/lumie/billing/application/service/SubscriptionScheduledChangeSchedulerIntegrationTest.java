package com.lumie.billing.application.service;

import com.lumie.billing.adapter.in.scheduling.SubscriptionScheduledChangeScheduler;
import com.lumie.billing.adapter.out.persistence.SubscriptionJpaRepository;
import com.lumie.billing.adapter.out.persistence.SubscriptionPersistenceAdapter;
import com.lumie.billing.domain.entity.Plan;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.vo.Money;
import com.lumie.billing.domain.vo.SubscriptionStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link SubscriptionScheduledChangeScheduler} + {@link SubscriptionTransitionHelper}.
 *
 * <p>Uses {@code @DataJpaTest} with a TestContainers Postgres instance. Flyway is disabled —
 * Hibernate {@code create-drop} sets up the schema from entity annotations. The class-level
 * {@code NOT_SUPPORTED} disables the test-transaction wrapper so that per-row {@code REQUIRES_NEW}
 * transactions in {@link SubscriptionTransitionHelper} can see committed test data. Cleanup is
 * handled manually in {@code @AfterEach} via TRUNCATE.
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
        SubscriptionTransitionHelper.class,
        SubscriptionScheduledChangeSchedulerIntegrationTest.JpaAuditingTestConfig.class
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DisplayName("SubscriptionScheduledChangeScheduler integration")
class SubscriptionScheduledChangeSchedulerIntegrationTest {

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
    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final SubscriptionPersistenceAdapter subscriptionPersistenceAdapter;
    private final SubscriptionTransitionHelper transitionHelper;

    private SubscriptionScheduledChangeScheduler scheduler;

    SubscriptionScheduledChangeSchedulerIntegrationTest(
            TransactionTemplate txTemplate,
            JdbcTemplate jdbcTemplate,
            SubscriptionJpaRepository subscriptionJpaRepository,
            SubscriptionPersistenceAdapter subscriptionPersistenceAdapter,
            SubscriptionTransitionHelper transitionHelper) {
        this.txTemplate = txTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.subscriptionJpaRepository = subscriptionJpaRepository;
        this.subscriptionPersistenceAdapter = subscriptionPersistenceAdapter;
        this.transitionHelper = transitionHelper;
    }

    @BeforeEach
    void setUp() {
        scheduler = new SubscriptionScheduledChangeScheduler(subscriptionPersistenceAdapter, transitionHelper);
        // Seed plans inside a committed transaction so all tests can see them
        txTemplate.executeWithoutResult(tx -> {
            Plan proPlan = Plan.builder()
                    .id("PRO").name("Pro")
                    .monthlyPrice(Money.of(99_000L))
                    .yearlyPrice(Money.of(990_000L))
                    .displayOrder(2)
                    .build();
            Plan basicPlan = Plan.builder()
                    .id("BASIC").name("Basic")
                    .monthlyPrice(Money.of(29_000L))
                    .yearlyPrice(Money.of(290_000L))
                    .displayOrder(1)
                    .build();
            em.persist(proPlan);
            em.persist(basicPlan);
        });
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE subscriptions, plans RESTART IDENTITY CASCADE");
    }

    // ─── Helpers for persisting test data in a committed transaction ──────────

    private Long persistAndCommit(Subscription sub) {
        return txTemplate.execute(tx -> {
            em.persist(sub);
            em.flush();
            return sub.getId();
        });
    }

    private Subscription loadFresh(Long id) {
        return txTemplate.execute(tx -> em.find(Subscription.class, id));
    }

    // ─── B-flow: expireTrials ─────────────────────────────────────────────────

    @Test
    @DisplayName("expireTrials: trial with expiresAt in past → transitions to EXPIRED")
    void expireTrials_expiresAtPassed_transitionsToExpired() {
        Long id = persistAndCommit(buildTrial(1L, "trial-academy",
                Instant.now().minus(Duration.ofDays(1))));

        scheduler.processScheduledTransitions();

        assertThat(loadFresh(id).getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
    }

    @Test
    @DisplayName("expireTrials: paid subscription (billingKeyId set) is not expired")
    void expireTrials_billingKeyNotNull_noTransition() {
        Long id = persistAndCommit(buildPaid(2L, "paid-academy"));

        scheduler.processScheduledTransitions();

        assertThat(loadFresh(id).getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("expireTrials: trial with expiresAt in future is not expired")
    void expireTrials_expiresAtFuture_noTransition() {
        Long id = persistAndCommit(buildTrial(3L, "future-trial",
                Instant.now().plus(Duration.ofDays(10))));

        scheduler.processScheduledTransitions();

        assertThat(loadFresh(id).getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    // ─── F-flow: applyPendingDowngrades ──────────────────────────────────────

    @Test
    @DisplayName("applyPendingDowngrades: scheduledChangeAt past → swaps plan and clears pending")
    void applyPendingDowngrades_scheduledPassed_swapsPlanAndClearsPending() {
        Long id = persistAndCommit(buildPaidWithPendingDowngrade(4L, "downgrade-academy",
                Instant.now().minus(Duration.ofMinutes(5))));

        scheduler.processScheduledTransitions();

        Subscription result = loadFresh(id);
        assertThat(result.getPlan().getId()).isEqualTo("BASIC");
        assertThat(result.getPendingPlan()).isNull();
        assertThat(result.getScheduledChangeAt()).isNull();
    }

    @Test
    @DisplayName("applyPendingDowngrades: scheduledChangeAt in future → no change")
    void applyPendingDowngrades_scheduledFuture_noChange() {
        Long id = persistAndCommit(buildPaidWithPendingDowngrade(5L, "future-downgrade",
                Instant.now().plus(Duration.ofDays(5))));

        scheduler.processScheduledTransitions();

        Subscription result = loadFresh(id);
        assertThat(result.getPlan().getId()).isEqualTo("PRO");
        assertThat(result.getPendingPlan()).isNotNull();
    }

    @Test
    @DisplayName("applyPendingDowngrades: no pendingPlan set → no change")
    void applyPendingDowngrades_noPending_noChange() {
        Long id = persistAndCommit(buildPaid(6L, "no-pending"));

        scheduler.processScheduledTransitions();

        assertThat(loadFresh(id).getPlan().getId()).isEqualTo("PRO");
    }

    // ─── G-flow: finalizeCancellations ────────────────────────────────────────

    @Test
    @DisplayName("finalizeCancellations: cancelledAt set + nextChargeAt past → transitions to CANCELLED")
    void finalizeCancellations_gracePeriodEnded_transitionsToCancelled() {
        Long id = persistAndCommit(buildCancelling(7L, "cancelling-academy",
                Instant.now().minus(Duration.ofHours(1))));

        scheduler.processScheduledTransitions();

        assertThat(loadFresh(id).getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    @Test
    @DisplayName("finalizeCancellations: cancelledAt set but nextChargeAt future → no change")
    void finalizeCancellations_cancelledButNextChargeFuture_noChange() {
        Long id = persistAndCommit(buildCancelling(8L, "future-cancel",
                Instant.now().plus(Duration.ofDays(3))));

        scheduler.processScheduledTransitions();

        assertThat(loadFresh(id).getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("finalizeCancellations: not cancelled (cancelledAt null) → no change")
    void finalizeCancellations_notCancelled_noChange() {
        Long id = persistAndCommit(buildPaid(9L, "active-no-cancel"));

        scheduler.processScheduledTransitions();

        assertThat(loadFresh(id).getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    // ─── cross-row isolation ─────────────────────────────────────────────────

    @Test
    @DisplayName("processScheduledTransitions: domain failure on row A does not prevent row B from transitioning")
    void processScheduledTransitions_oneFailure_othersStillProcess() {
        // Row A: trial candidate, but corrupt it to trigger expireTrial() domain guard
        Long failId = persistAndCommit(buildTrial(10L, "fail-trial",
                Instant.now().minus(Duration.ofDays(1))));
        txTemplate.executeWithoutResult(tx ->
                em.createNativeQuery("UPDATE subscriptions SET billing_key_id = 9999 WHERE id = :id")
                        .setParameter("id", failId)
                        .executeUpdate());

        // Row B: legitimate expired trial
        Long goodId = persistAndCommit(buildTrial(11L, "good-trial",
                Instant.now().minus(Duration.ofDays(1))));

        scheduler.processScheduledTransitions();

        assertThat(loadFresh(goodId).getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
    }

    // ─── MonthlySubscriptionChargeScheduler guard ─────────────────────────────

    @Test
    @DisplayName("findActiveWithNextChargeAtOrBefore: excludes cancel-grace subscriptions")
    void findActiveWithNextChargeAtOrBefore_skipsCancelGraceSubscriptions() {
        // cancel-grace (G-flow): should NOT be in charge candidates
        persistAndCommit(buildCancelling(20L, "cancel-grace",
                Instant.now().minus(Duration.ofHours(1))));
        // Normal ACTIVE → SHOULD be charged
        persistAndCommit(buildPaidWithNextCharge(21L, "charge-me",
                Instant.now().minus(Duration.ofHours(1))));

        List<Subscription> candidates = subscriptionPersistenceAdapter
                .findActiveWithNextChargeAtOrBefore(Instant.now());

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).getTenantId()).isEqualTo(21L);
    }

    // ─── JPA Auditing config ──────────────────────────────────────────────────

    /**
     * Provides {@code @EnableJpaAuditing} so {@link com.lumie.common.domain.BaseEntity}
     * fields are populated in tests. The production config lives in {@code app/} which is
     * not on the test classpath here.
     */
    @EnableJpaAuditing(dateTimeProviderRef = "testAuditingInstantProvider")
    static class JpaAuditingTestConfig {

        @Bean
        public DateTimeProvider testAuditingInstantProvider() {
            return () -> java.util.Optional.of(java.time.Instant.now());
        }
    }

    // ─── Test data builders (return entity; caller wraps in txTemplate) ────────

    private Subscription buildTrial(Long tenantId, String slug, Instant expiresAt) {
        Plan pro = txTemplate.execute(tx -> em.find(Plan.class, "PRO"));
        Subscription sub = Subscription.createTrial(tenantId, slug, pro);
        ReflectionTestUtils.setField(sub, "expiresAt", expiresAt);
        return sub;
    }

    private Subscription buildPaid(Long tenantId, String slug) {
        Plan pro = txTemplate.execute(tx -> em.find(Plan.class, "PRO"));
        Subscription sub = Subscription.createFree(tenantId, slug, pro);
        sub.subscribe(100L + tenantId, pro, Instant.now().plus(Duration.ofDays(30)));
        return sub;
    }

    private Subscription buildPaidWithNextCharge(Long tenantId, String slug, Instant nextChargeAt) {
        Plan pro = txTemplate.execute(tx -> em.find(Plan.class, "PRO"));
        Subscription sub = Subscription.createFree(tenantId, slug, pro);
        sub.subscribe(100L + tenantId, pro, nextChargeAt);
        return sub;
    }

    private Subscription buildPaidWithPendingDowngrade(Long tenantId, String slug, Instant scheduledAt) {
        Plan pro = txTemplate.execute(tx -> em.find(Plan.class, "PRO"));
        Plan basic = txTemplate.execute(tx -> em.find(Plan.class, "BASIC"));
        Subscription sub = Subscription.createFree(tenantId, slug, pro);
        sub.subscribe(100L + tenantId, pro, Instant.now().plus(Duration.ofDays(30)));
        sub.scheduleDowngrade(basic, scheduledAt);
        return sub;
    }

    private Subscription buildCancelling(Long tenantId, String slug, Instant nextChargeAt) {
        Subscription sub = buildPaidWithNextCharge(tenantId, slug, nextChargeAt);
        sub.scheduleCancellation();
        return sub;
    }
}
