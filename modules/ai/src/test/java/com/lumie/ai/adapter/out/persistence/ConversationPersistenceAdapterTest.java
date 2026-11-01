package com.lumie.ai.adapter.out.persistence;

import com.lumie.ai.domain.entity.Conversation;
import com.lumie.common.tenant.TenantContextHolder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
@DataJpaTest(
        excludeAutoConfiguration = FlywayAutoConfiguration.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
        }
)
@Import({ConversationPersistenceAdapter.class, ConversationPersistenceAdapterTest.AuditingConfig.class})
class ConversationPersistenceAdapterTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    private final ConversationJpaRepository repository;
    private final ConversationPersistenceAdapter adapter;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    @Autowired
    ConversationPersistenceAdapterTest(ConversationJpaRepository repository,
                                       ConversationPersistenceAdapter adapter,
                                       JdbcTemplate jdbcTemplate,
                                       EntityManager entityManager) {
        this.repository = repository;
        this.adapter = adapter;
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @BeforeEach
    void enforceTenantIsolation() {
        AiRlsTestSupport.bindRestrictedTenant(jdbcTemplate, "ai_conversations");
    }

    @Test
    void findsOwnedConversationAndPagesOnlyActiveConversations() {
        TenantContextHolder.setTenant("tenant-a");
        TenantContextHolder.setTenantId(1L);
        Conversation activeOld = adapter.save(conversation(11L, "old"));
        Conversation activeNew = adapter.save(conversation(11L, "new"));
        Conversation inactive = conversation(11L, "inactive");
        inactive.deactivate();
        adapter.save(inactive);
        Conversation otherUser = adapter.save(conversation(12L, "other"));
        repository.flush();

        jdbcTemplate.update("UPDATE ai_conversations SET created_at = ? WHERE id = ?",
                java.sql.Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")), activeOld.getId());
        jdbcTemplate.update("UPDATE ai_conversations SET created_at = ? WHERE id = ?",
                java.sql.Timestamp.from(Instant.parse("2026-01-02T00:00:00Z")), activeNew.getId());

        assertThat(adapter.findByIdAndUserId(activeOld.getId(), 11L)).contains(activeOld);
        assertThat(adapter.findByIdAndUserId(activeOld.getId(), 12L)).isEmpty();
        assertThat(adapter.findByIdAndUserId(999L, 11L)).isEmpty();
        assertThat(adapter.findByUserIdAndIsActiveTrue(12L, PageRequest.of(0, 10)).getContent())
                .containsExactly(otherUser);
        var activePage = adapter.findByUserIdAndIsActiveTrue(
                11L, PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "createdAt")));
        assertThat(activePage.getTotalElements()).isEqualTo(2);
        assertThat(activePage.getContent())
                .hasSize(1)
                .extracting(Conversation::getTitle)
                .containsExactly("old");

        entityManager.clear();
        jdbcTemplate.execute("SET LOCAL app.tenant_id = '2'");
        assertThat(adapter.findByIdAndUserId(activeOld.getId(), 11L)).isEmpty();
        assertThat(adapter.findByUserIdAndIsActiveTrue(11L, PageRequest.of(0, 10))).isEmpty();
    }

    private static Conversation conversation(Long userId, String title) {
        return Conversation.builder().userId(userId).title(title).build();
    }

    @EnableJpaAuditing(dateTimeProviderRef = "auditingInstantProvider")
    static class AuditingConfig {
        @Bean
        DateTimeProvider auditingInstantProvider() {
            return () -> Optional.of(Instant.now());
        }
    }
}
