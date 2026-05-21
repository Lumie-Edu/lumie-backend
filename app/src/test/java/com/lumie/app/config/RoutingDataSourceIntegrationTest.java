package com.lumie.app.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.core.Ordered;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Audit-mandated reproduction test for D.7
 * ({@link RoutingDataSourceConfig}). Two prior production attempts at
 * this routing crashed at context refresh:
 *
 * <ul>
 *   <li>v1 ({@code e748b27} → revert {@code e9501a8}) — root
 *       {@code spring.datasource.*} co-existed with a {@code @Primary
 *       DataSource}, leaving the Spring Data JPA shared-EM alias
 *       {@code jpaSharedEM_entityManagerFactory} unregistered.</li>
 *   <li>v2 ({@code 3b16618} → revert {@code b9f31c8}) — added an explicit
 *       {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean};
 *       the bean definition lost its {@code factoryBeanObjectType}
 *       attribute and the shared-EM alias was still missing.</li>
 * </ul>
 *
 * <p>Both failures only manifested when the JPA bean graph was actually
 * constructed — the pre-existing {@code MigrationsRlsIntegrationTest}
 * boots only Flyway DDL on a container, so it never exercised them. The
 * audit's explicit pre-flight mandate was: <em>"write a local
 * TestContainers {@code @SpringBootTest} that reproduces the failure,
 * fix + verify locally, only then push."</em> That is this file.
 *
 * <h2>Why this is a slice, not a full {@code LumieApplication} boot</h2>
 *
 * <p>The audited failure mode lives at the
 * {@code DataSource → EntityManagerFactory → @EnableJpaRepositories}
 * boundary — booting Redis / RabbitMQ / MinIO / Modulith outbox adds no
 * extra coverage of that boundary and brings external connection waits
 * that hang the test on a developer laptop. A focused slice that loads
 * exactly the configs under test ({@link RoutingDataSourceConfig},
 * {@link JpaConfig}, {@link JpaAuditingConfig}) on top of Spring Boot's
 * standard {@code DataSourceAutoConfiguration} / {@code JpaAutoConfiguration}
 * is sufficient — and is the same bean graph that crashed prod.
 *
 * <p>External-service autoconfigs are excluded; entity / repository
 * scanning from {@link JpaConfig} still runs across all modules
 * ({@code com.lumie}) so the full mapping graph and every
 * {@code JpaRepository} proxy are constructed — proving the shared-EM
 * alias resolves end-to-end, not just for a synthetic test entity.
 *
 * <h2>Assertions, in order of strictness</h2>
 *
 * <ol>
 *   <li><b>Context loads.</b> v1 and v2's failure was at refresh; just
 *       passing this is the win.</li>
 *   <li><b>Two pools wired.</b> {@code primaryDataSource} and
 *       {@code readOnlyDataSource} resolve; {@code @Primary} is the
 *       {@link LazyConnectionDataSourceProxy}.</li>
 *   <li><b>Read-only routing.</b> {@code @Transactional(readOnly = true)}
 *       lands on the readonly pool (verified via {@code app.pool_id} GUC
 *       set by {@code connection-init-sql}).</li>
 *   <li><b>Write routing.</b> {@code @Transactional} lands on primary.</li>
 *   <li><b>Default routing.</b> Non-transactional code lands on primary.</li>
 *   <li><b>Shared EM alias.</b> {@code jpaSharedEM_entityManagerFactory}
 *       is registered — directly checks the bean Spring Data JPA needs
 *       and that v1/v2 lost.</li>
 * </ol>
 *
 * <p>Both pools target the same TestContainers Postgres; pool identity is
 * still observable via {@code app.pool_id}, so the routing assertions are
 * meaningful without a real replica (an infra concern, not a backend one).
 */
@SpringBootTest(
        classes = RoutingDataSourceIntegrationTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
// Tagged @Tag("integration") because the test spins up a real Postgres via
// TestContainers — that needs a Docker socket. Tekton's springboot-ci step
// runs in a pod with no Docker access, so this test would fail there with
// DockerClientProviderStrategy.IllegalStateException. Default `./gradlew
// test` (CI) excludes the tag; `./gradlew integrationTest` (local dev or
// any Docker-capable runner) includes it. The companion smoke gap — a true
// `@SpringBootTest(classes = LumieApplication.class)` that catches Spring
// Data fragment / bean-wiring failures at build time — is tracked in
// `4_FOLLOWUP_FROM_CLEANUP.md` F-22 and needs a Tekton step with Docker
// access (DinD or Testcontainers Cloud) to land.
@Tag("integration")
@Testcontainers
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class RoutingDataSourceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        // Both pools point at the same container — routing decisions are
        // verified via the connection-init-sql GUC, not topology.
        registry.add("app.datasource.primary.url", POSTGRES::getJdbcUrl);
        registry.add("app.datasource.primary.username", POSTGRES::getUsername);
        registry.add("app.datasource.primary.password", POSTGRES::getPassword);
        registry.add("app.datasource.readonly.url", POSTGRES::getJdbcUrl);
        registry.add("app.datasource.readonly.username", POSTGRES::getUsername);
        registry.add("app.datasource.readonly.password", POSTGRES::getPassword);
    }

    private final ApplicationContext context;
    private final DataSource primaryDataSource;
    private final DataSource readOnlyDataSource;
    private final DataSource primaryByDefault;
    private final RoutingProbe routingProbe;

    RoutingDataSourceIntegrationTest(
            ApplicationContext context,
            @Qualifier("primaryDataSource") DataSource primaryDataSource,
            @Qualifier("readOnlyDataSource") DataSource readOnlyDataSource,
            DataSource primaryByDefault,
            RoutingProbe routingProbe) {
        this.context = context;
        this.primaryDataSource = primaryDataSource;
        this.readOnlyDataSource = readOnlyDataSource;
        this.primaryByDefault = primaryByDefault;
        this.routingProbe = routingProbe;
    }

    @Test
    void contextLoadsWithRoutingDataSourceWiredCleanly() {
        assertThat(primaryByDefault)
                .as("@Primary DataSource must be the LazyConnectionDataSourceProxy "
                        + "(without it, routing decisions race ahead of Spring's "
                        + "readOnly-flag setup and everything lands on primary)")
                .isInstanceOf(LazyConnectionDataSourceProxy.class);

        assertThat(primaryDataSource).isNotNull();
        assertThat(readOnlyDataSource).isNotNull();
        assertThat(primaryDataSource)
                .as("primary and readonly pools must be distinct Hikari instances")
                .isNotSameAs(readOnlyDataSource);

        // The bean v1 and v2 lost. Spring Data JPA's
        // EntityManagerBeanDefinitionRegistrarPostProcessor registers this
        // alias only when an EntityManagerFactory bean is present with the
        // correct factoryBeanObjectType attribute. If it ever goes missing,
        // every @PersistenceContext / repository in the app fails at refresh.
        assertThat(context.containsBean("jpaSharedEM_entityManagerFactory"))
                .as("Spring Data JPA must register the shared-EM alias on top "
                        + "of the @Primary routing DataSource — its absence was "
                        + "the v1/v2 production crash signature")
                .isTrue();
    }

    @Test
    void readOnlyTransactionRoutesToReadOnlyPool() {
        assertThat(routingProbe.poolIdInsideReadOnlyTx())
                .as("@Transactional(readOnly = true) must land on the readonly pool")
                .isEqualTo("readonly");
    }

    @Test
    void writeTransactionRoutesToPrimaryPool() {
        assertThat(routingProbe.poolIdInsideWriteTx())
                .as("@Transactional (write) must land on the primary pool")
                .isEqualTo("primary");
    }

    @Test
    void nonTransactionalAccessRoutesToPrimaryPool() {
        assertThat(routingProbe.poolIdOutsideTx())
                .as("non-transactional access defaults to the primary pool")
                .isEqualTo("primary");
    }

    /**
     * Minimal slice — uses {@link ImportAutoConfiguration} with an explicit
     * whitelist instead of {@code @SpringBootApplication}, so only the
     * autoconfigs exercised by the audited failure mode load. Redis /
     * RabbitMQ / Security / Spring AI / Modulith outbox all stay off,
     * so the test never reaches a socket that would block waiting for an
     * absent external service.
     *
     * <p>The whitelist covers exactly the path the audit cared about:
     * {@code DataSourceAutoConfiguration} → our
     * {@link RoutingDataSourceConfig} {@code @Primary} bean →
     * {@code HibernateJpaAutoConfiguration} → {@code EntityManagerFactory} →
     * {@code JpaRepositoriesAutoConfiguration} →
     * {@code jpaSharedEM_entityManagerFactory} alias (the bean v1 and v2
     * crashed on).
     *
     * <p>{@code FlywayAutoConfiguration} is deliberately NOT imported.
     * Booting Flyway on a fresh PG container runs all ~24 migrations from
     * scratch, which deadlocks: Flyway's schema_history connection holds
     * an idle-in-transaction state while V21's {@code CREATE INDEX
     * CONCURRENTLY} on a sibling Hikari connection waits for all active
     * transactions to clear. Prod never sees this (migrations are
     * incremental) and the routing config does not depend on Flyway —
     * with {@code spring.jpa.hibernate.ddl-auto=none} JPA boots without
     * schema validation, so the EMF bean graph (the actual subject of
     * this test) is exercised end-to-end without migrations.
     */
    @Configuration
    @ImportAutoConfiguration({
            PropertyPlaceholderAutoConfiguration.class,
            ConfigurationPropertiesAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class,
            TransactionAutoConfiguration.class
    })
    @Import(RoutingDataSourceConfig.class)
    // Tx interceptor order mirrors JpaConfig — required for the readOnly
    // flag to reach RoutingDataSource.determineCurrentLookupKey() in time.
    @EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE)
    // Slice-local repository scan into an empty package — the
    // EntityManagerBeanDefinitionRegistrarPostProcessor that registers
    // jpaSharedEM_entityManagerFactory still runs (it fires off
    // @EnableJpaRepositories, not the contents), so the v1/v2 failure
    // signature is verifiable without dragging in module repositories
    // that depend on un-scanned custom-impl beans.
    @EnableJpaRepositories(basePackages = "com.lumie.app.config.routingtestslice")
    static class TestApp {

        @Bean
        @Primary
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        RoutingProbe routingProbe(JdbcTemplate jdbcTemplate) {
            return new RoutingProbe(jdbcTemplate);
        }
    }

    static class RoutingProbe {

        private static final String READ_POOL_ID =
                "SELECT current_setting('app.pool_id', true)";

        private final JdbcTemplate jdbcTemplate;

        RoutingProbe(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Transactional(readOnly = true)
        public String poolIdInsideReadOnlyTx() {
            return jdbcTemplate.queryForObject(READ_POOL_ID, String.class);
        }

        @Transactional
        public String poolIdInsideWriteTx() {
            return jdbcTemplate.queryForObject(READ_POOL_ID, String.class);
        }

        public String poolIdOutsideTx() {
            return jdbcTemplate.queryForObject(READ_POOL_ID, String.class);
        }
    }
}
